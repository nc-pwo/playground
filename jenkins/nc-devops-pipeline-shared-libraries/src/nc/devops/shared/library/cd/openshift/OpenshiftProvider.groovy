package nc.devops.shared.library.cd.openshift

import hudson.AbortException
import nc.devops.shared.library.cd.AbstractDeploymentProvider
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty
import nc.devops.shared.library.time.TimedExecution

import java.time.temporal.ChronoUnit

class OpenshiftProvider extends AbstractDeploymentProvider implements Serializable {
    private final DeploymentProviderParameters deploymentProviderParameters
    private final TemplateProcessingTool<OpenshiftProcessedModel> templateProcessingTool
    private final Openshift openshift
    private Closure logger, parallel, timeout, sleep
    private List<ApplicationParameters> applicationConfigurationList
    private List<OpenshiftProcessedModel> processedTemplates = []
    private String projectName

    OpenshiftProvider(def deploymentTool, DeploymentProviderParameters parameters) {
        this.openshift = deploymentTool
        this.deploymentProviderParameters = parameters
        this.templateProcessingTool = parameters.templateProcessingTool
        this.logger = parameters.logger
        this.parallel = parameters.parallel
        this.timeout = parameters.timeout
        this.sleep = parameters.sleep
        this.projectName = parameters.projectName
        this.applicationConfigurationList = getApplicationsParameterList(parameters)
    }

    @Override
    String useProject(ProjectManagementProvider managementProvider) {
        this.projectName = managementProvider.lockResource(deploymentProviderParameters)
        managementProvider.useProject(deploymentProviderParameters)
        return projectName
    }

    @Override
    def deployApplication() {
        List<Map> parallelActionsList = new ArrayList<>()
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            applicationConfigurationList.each {
                if (deploymentProviderParameters.isBppr() && it.hasBpprImage) {
                    it.deploymentParameters = (["PROJECT_NAME=${projectName}", "COMMIT_ID=${deploymentProviderParameters.getGitCommitId()}"] + (it.deploymentParameters ?: [])) as List<String>
                }
                OpenshiftProcessedModel processedModel = templateProcessingTool.processTemplate(it)
                logger.call("Template processing output: ${processedModel.models.size()} models:")
                logger.call("${processedModel.models.collect { modelToString(it) }.join('\n')}")

                Map parallelActions = processedModel.models.collectEntries { model ->
                    [(modelToString(model)): {
                        try {
                            logger.call("Applying resource: ${modelToString(model)}")
                            openshift.apply(model)
                        } catch (AbortException e) {
                            logger.call(e.toString())
                            if (!e.getMessage().contains('apply returned an error')) {
                                throw e
                            }
                            logger.call("Error '${e.toString()}' occurred while applying resource. Replacing resource...")
                            replaceResource(model)
                        }
                        logger.call('Resource exists now.')
                    }
                    ]
                }
                processedTemplates.add(processedModel)
                logger.call("Application name : ${processedModel.applicationName}")
                parallelActionsList.add(parallelActions)
            }
        }
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            parallelActionsList.each { parallelActions ->
                parallel.call(parallelActions)
            }
            processedTemplates.each { processedModel ->
                importImagesToProjectImageStream(processedModel.models)
                processedModel.models.findAll { it.kind in ["Deployment", "DeploymentConfig"] }.each {
                    try {
                        openshift.selector('dc', processedModel.applicationName).rollout().latest()
                    } catch (AbortException e) {
                        if (e.getMessage().contains("already in progress")) {
                            logger.call("Skipping rollout for ${processedModel.applicationName} - it is already in progress")
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
    }

    private void replaceResource(model) {
        if (model.kind == "PersistentVolumeClaim") {
            logger.call("Resource kind is PersistentVolumeClaim. Skipping recreation")
            return
        }
        try {
            logger.call("Replacing resource...")
            openshift.replace(model)
        } catch (AbortException e) {
            if (!e.getMessage().contains("replace returned an error")) {
                throw e
            }
            logger.call("Error '${e.toString()}' occurred while applying resource. Recreating resource...")
            logger.call("Removing...")
            openshift.delete(model)
            logger.call("Creating...")
            openshift.create(model)
        }
    }

    private void importImagesToProjectImageStream(def models) {
        models.findAll { it.kind == 'ImageStream' }.each { model ->
            String imageName = model.metadata.name
            String imageVersion = model.spec.tags.name
            String from = model.spec.tags.from.name
            from = from.replaceAll("[\\[\\]]", "")
            imageVersion = imageVersion.replaceAll("[\\[\\]]", "")
            String image = "$imageName:$imageVersion"
            logger.call "Importing image: $imageName"
            importImage(image, from)
        }
    }

    private void importImage(String image, String from) {
        openshift.raw("import-image -n ${projectName} $image --from=$from --confirm")
    }

    @Override
    void verifyReadiness() {
        processedTemplates.each { OpenshiftProcessedModel processedModel ->
            processedModel.models.kind.each { String type ->
                logger.call("Veryfing $type")
                if ("Job" == type) {
                    verifyJobCompleted(processedModel.applicationName)
                } else if (type in ["Deployment", "DeploymentConfig", "BuildConfig", "Pod", "ReplicaSet"]) {
                    verifyAppReadiness(processedModel.applicationName)
                }
            }
        }
    }

    void verifyAppReadiness(String applicationName) {
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            TimedExecution timedExecution = new TimedExecution(sleep, logger, deploymentProviderParameters.defaultTimeout, ChronoUnit.MINUTES)
            timedExecution.doUntilTrue {
                return areAllReplicasReady(applicationName)
            }
            logger.call("$applicationName application is ready")
        }
    }

    private boolean areAllReplicasReady(String appName) {
        def latestDeploymentVersion = openshift.selector('dc', appName).object().status.latestVersion
        def rc = openshift.selector('rc', "${appName}-${latestDeploymentVersion}")
        boolean completed = true
        rc.withEach {
            if (!completed)
                return
            def rcMap = it.object()
            completed = (rcMap.status.replicas.equals(rcMap.status.readyReplicas))
        }
        return completed
    }

    void verifyJobCompleted(String applicationName) {
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            TimedExecution timedExecution = new TimedExecution(sleep, logger, deploymentProviderParameters.defaultTimeout, ChronoUnit.MINUTES)
            timedExecution.doUntilTrue {
                def jobStatus = openshift.selector("jobs/$applicationName").object().status
                logger.call("job Status: $jobStatus")
                return jobStatus.succeeded
            }
            logger.call("$applicationName job is completed")
        }
    }

    @Override
    List<ProcessedTestProperty> processIntegrationTestParams(List<TestProperty> integrationTestParams) {
        return openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            convertIntegrationTestParams(integrationTestParams,
                    {
                        it.appHostForValue ? "http://" + openshift.selector("routes/${it.propertyValue}")
                                .object().getAt('spec').host : it.propertyValue
                    })
        } as List<ProcessedTestProperty>
    }

    @Override
    List<ProcessedTestProperty> processIntegrationTestParamsWithoutCluster(List<TestProperty> integrationTestParams) {
        return openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            convertIntegrationTestParams(integrationTestParams, { it.propertyValue })
        } as List<ProcessedTestProperty>
    }

    @Override
    void idleApplication() {
        openshift.withClusterAndProject(projectName, deploymentProviderParameters.deliveryCluster) {
            processedTemplates.each { OpenshiftProcessedModel processedModel ->
                openshift.idle(processedModel.applicationName as String)
                logger.call("Application ${processedModel.applicationName} is idled successfully")
            }
        }
    }

    private static String modelToString(def model) {
        return "${model.kind}: ${model.metadata.name}"
    }
}
