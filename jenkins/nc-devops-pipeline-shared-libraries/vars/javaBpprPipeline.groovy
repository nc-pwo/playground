import jenkins.model.Jenkins
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.buildtool.*
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.cd.DeploymentProvider
import nc.devops.shared.library.cd.DeploymentProviderFactory
import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.DeploymentProviderType
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.project.ProjectManagementProviderFactory
import nc.devops.shared.library.cd.project.ProjectManagementProviderType
import nc.devops.shared.library.cd.templates.TemplateProcessingToolType
import nc.devops.shared.library.imagerepository.factory.ImageRepositoryFactory
import nc.devops.shared.library.imagerepository.model.ImageRepository
import nc.devops.shared.library.imagerepository.model.ImageRepositoryParameters
import nc.devops.shared.library.imagerepository.type.ImageRepoType
import nc.devops.shared.library.tests.ComponentTestPropertiesIdentityMapper
import nc.devops.shared.library.tests.IntegrationTestsMapper
import nc.devops.shared.library.tests.model.IntegrationTests
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.ArtifactoryBuilder
import org.jfrog.hudson.ArtifactoryServer

def call(body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    final String BUILD_DIRECTORY = pipelineConfig.directory ?: "."
    final int OPENSHIFT_LABEL_COMMIT_LENGTH = 8

    ProjectManagementProvider projectManagementProvider
    def lockArgs

    pipelineConfig.useWrapper = pipelineConfig.useWrapper ?: false
    if (pipelineConfig.continuousDelivery) {
        def jobName = env.JOB_NAME // ex: JOB_NAME = bppr_nc-devops-hello-world
        def defaultContinuousDelivery = [
                projectNameSuffix            : 'bppr',
                projectName                  : jobName.replaceAll('bppr_', '').replaceAll('/', '-'),
                projectNameDelimiter         : '-',
                manualTest                   : false,
                deleteAfterSuccess           : true,
                defaultTimeOut               : 5,
                deploymentParameters         : [],
                integrationTestWithKubernetes: false,
                agentInheritFrom             : "Kubernetes",
                agentServiceAccount          : "builder",
                kubernetesCluster            : [credentialsId: System.getenv("KUBERNETES_SA_TOKEN") ?: 'aks-token',
                                                serverUrl    : System.getenv("KUBERNETES_CLUSTER_URL") ?: 'https://accelerators-aks-dns-8c106221.hcp.uksouth.azmk8s.io:443',
                                                namespace    : System.getenv("KUBERNETES_SA_NAMESPACE") ?: 'dev',
                                                caCertificate: System.getenv("KUBERNETES_CA_CERT"),
                                                clusterName  : System.getenv("KUBERNETES_CLUSTER_NAME") ?: 'k8s',
                                                contextName  : System.getenv("KUBERNETES_CONTEXT_NAME") ?: 'k8s']
        ]
        pipelineConfig.continuousDelivery.deliveryCluster = params.DELIVERY_CLUSTER ?: pipelineConfig.continuousDelivery.deliveryCluster ?: System.getenv('DEFAULT_CLUSTER')
        pipelineConfig.continuousDelivery = defaultContinuousDelivery + pipelineConfig.continuousDelivery
        pipelineConfig.continuousDelivery.projectManagementProvider = (pipelineConfig.continuousDelivery.projectManagementProvider as ProjectManagementProviderType)
                ?: ProjectManagementProviderType.OPENSHIFT_BPPR_WITHOUT_JENKINS_LOCK
        projectManagementProvider = new ProjectManagementProviderFactory().
                createProjectManagementProvider(pipelineConfig.continuousDelivery.projectManagementProvider as ProjectManagementProviderType, pipelineConfig, this as CpsScript)
        lockArgs = projectManagementProvider.getLockableResourcesPluginOptions()
    }
    if (pipelineConfig.dockerImagesRepository == null) {
        ArtifactoryServer defaultArtifactoryServer = Jenkins.get().getExtensionList(ArtifactoryBuilder.DescriptorImpl.class)[0].getArtifactoryServers()[0]
        pipelineConfig.dockerImagesRepository = [
                type                : ImageRepoType.ARTIFACTORY,
                pushCredentialsId   : defaultArtifactoryServer.getDeployerCredentialsConfig().getCredentialsId(),
                deleteCredentialsId : defaultArtifactoryServer.getDeployerCredentialsConfig().getCredentialsId(),
                stagingRepositoryUrl: System.getProperty("IMAGE_REPOSITORY_URL") ?: "https://artifactory.nchosting.dk/nc-dvo-docker-staging-local"
        ]
    }

    env.SOURCE_BRANCH = env.SOURCE_BRANCH ?: "refs/remotes/origin/${env.BRANCH_NAME}"
    env.PR_NUMBER = gitUtils.getPullRequestNumber(env.CHANGE_ID, env.SOURCE_BRANCH)

    boolean isPublishingSuccessful = false
    BuildTool buildTool
    DeploymentProvider deploymentProvider
    BuildToolType buildToolType
    TemplateProcessingToolType templateProcessingToolType
    DeploymentProviderType deploymentProviderType
    DeploymentProviderParameters deploymentProviderParameters
    String projectName
    IntegrationTests integrationTests

    if (pipelineConfig.integrationTests != null) {
        integrationTests = new IntegrationTestsMapper().create(pipelineConfig?.integrationTests as Map<String, Map<String, Object>>)
    } else {
        integrationTests = new IntegrationTestsMapper().createFromLegacySource(pipelineConfig)
    }

    ImageRepository imageRepository
    ImageRepositoryParameters imageRepositoryParameter
    ImageRepoType imageRepoType

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }
    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')

    echo pipelineConfig.toMapString()

    pipeline {
        agent {
            label "${AGENT_LABEL}"
        }
        parameters {
            string(name: 'COMMIT_ID', description: 'Git commit ID of the build', defaultValue: '')
            string(name: 'SOURCE_BRANCH', description: 'Source Branch', defaultValue: '')
            string(name: 'PR_SOURCE_BRANCH', description: 'Pull Request Source Branch', defaultValue: '')
            string(name: 'PR_TARGET_BRANCH', description: 'Pull Request Target Branch', defaultValue: '')
        }

        environment {
            PR_NUMBER = "${env.SOURCE_BRANCH.split('/')[2]}"
            SOURCE_BRANCH_NAME = "${params.PR_SOURCE_BRANCH.replaceAll('refs/heads/', '')}"
            TARGET_BRANCH_NAME = "${params.PR_TARGET_BRANCH ? params.PR_TARGET_BRANCH.replaceAll('refs/heads/', '') : 'master'}"
        }

        tools {
            jdk pipelineConfig.jdk ?: 'Default'
        }
        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: ''),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: ''))
            )
        }

        stages {
            stage('Init build-tool') {
                steps {
                    script {
                        imageRepoType = pipelineConfig.dockerImagesRepository.type as ImageRepoType ?: ImageRepoType.OPENSHIFT
                        imageRepositoryParameter = new ImageRepositoryParameters(pipelineConfig.dockerImagesRepository as Map)
                        imageRepository = ImageRepositoryFactory.createImageRepoObject(imageRepoType, imageRepositoryParameter, this)

                        RepoType repoType = RepoType.GENERIC
                        buildToolType = pipelineConfig.buildToolType as BuildToolType
                        BuildToolParameters buildToolParameters = createBuildToolParameters(pipelineConfig, imageRepository)

                        ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, this)
                        BuildObject selectedBuildObject = new BuildObjectFactory(this).createBuildObject(repoType, buildToolType, pipelineConfig.buildToolName as String, buildToolParameters)
                        buildTool = new BuildToolFactory().createBuildTool(buildToolType, selectedBuildObject, repository, buildToolParameters)
                    }
                }
            }

            stage('Init deployment provider and processing tool') {
                when {
                    expression { pipelineConfig.continuousDelivery }
                }
                steps {
                    script {
                        projectName = pipelineConfig.continuousDelivery.projectName
                        deploymentProviderType = (pipelineConfig.continuousDelivery.deploymentProviderType as DeploymentProviderType) ?: DeploymentProviderType.OPENSHIFT
                        templateProcessingToolType = (pipelineConfig.continuousDelivery.templateProcessingTool as TemplateProcessingToolType) ?: TemplateProcessingToolType.OC

                        deploymentProviderParameters = new DeploymentProviderParameters(
                                deliveryCluster: pipelineConfig.continuousDelivery.deliveryCluster,
                                sourceBranch: env.SOURCE_BRANCH_NAME,
                                projectNameSuffix: pipelineConfig.continuousDelivery.projectNameSuffix,
                                projectNameDelimiter: pipelineConfig.continuousDelivery.projectNameDelimiter,
                                projectName: projectName,
                                pullRequestId: env.PR_NUMBER,
                                defaultTimeout: pipelineConfig.continuousDelivery.defaultTimeOut,
                                requiredComponents: pipelineConfig.continuousDelivery?.requiredComponents,
                                templatePath: pipelineConfig.continuousDelivery?.templatePath,
                                credentialParameters: pipelineConfig.continuousDelivery?.credentialParameters,
                                deploymentParameters: pipelineConfig.continuousDelivery?.deploymentParameters,
                                gitCommitId: env.GIT_COMMIT?.take(OPENSHIFT_LABEL_COMMIT_LENGTH) ?: "manually_created",
                                bppr: true,
                                logger: { String message -> this.echo message },
                                parallel: { map -> this.parallel map },
                                timeout: { int timeoutInMins, Closure timeoutBody -> this.timeout timeoutInMins, timeoutBody },
                                sleep: { int sleepInMins -> this.sleep sleepInMins },
                                cpsScript: this as CpsScript,
                                kubernetesCluster: pipelineConfig.continuousDelivery?.kubernetesCluster
                        )

                        deploymentProvider = new DeploymentProviderFactory().createDeploymentProvider(deploymentProviderType, templateProcessingToolType, deploymentProviderParameters)
                    }
                }
            }

            stage('Clean') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.clean()
                        }
                    }
                }
            }

            stage('Build') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.buildArtifacts()
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.unitTest()
                        }
                    }
                }
            }

            stage('Component test') {
                when {
                    beforeAgent true
                    expression {
                        integrationTests.component.runTest
                    }
                }
                agent {
                    label integrationTests.component.getAgentLabelOrDefault(AGENT_LABEL)
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            List<ProcessedTestProperty> testProperties = new ComponentTestPropertiesIdentityMapper().map(integrationTests.component.testProperties)
                            buildTool.clean()
                            buildTool.componentTest(testProperties)
                        }
                    }
                }
                post {
                    always {
                        junit allowEmptyResults: true, testResults: buildTool.getComponentTestResultPath()
                    }
                }
            }


            stage('Sonarqube Analysis') {
                when {
                    expression { pipelineConfig.sonarqubeServerKey }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            withSonarQubeEnv(pipelineConfig.sonarqubeServerKey) {
                                buildTool.staticCodeAnalysis(new PullRequestCodeAnalysisParams(
                                        pullRequestNumber: env.PR_NUMBER,
                                        pullRequestSourceBranchName: env.SOURCE_BRANCH_NAME,
                                        pullRequestBaseBranchName: env.TARGET_BRANCH_NAME,
                                        repoUrl: env.GIT_URL
                                ))
                            }
                        }
                    }
                }
            }

            stage('Quality Gate') {
                when {
                    expression { pipelineConfig.sonarqubeServerKey }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            qualityGateStep(pipelineConfig.sonarqubeDisableWebhook)
                        }
                    }
                }
            }

            stage('Continuous Delivery') {
                when {
                    expression {
                        pipelineConfig.continuousDelivery
                    }
                }
                options {
                    lock lockArgs
                }
                stages {
                    stage('Create BPPR Project') {
                        steps {
                            script {
                                projectName = deploymentProvider.useProject(projectManagementProvider)
                                imageRepository.setProjectName(projectName)

                            }
                        }
                    }

                    stage('Publish BPPR images') {
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    String dockerRepositoryName = imageRepository.getDockerRepositoryName()
                                    imageRepository.withCredentials(pipelineConfig.dockerImagesRepository.pushCredentialsId) {
                                        buildTool.publishImages(dockerRepositoryName, projectName)
                                    }
                                    isPublishingSuccessful = true
                                    echo "BPPR images are published to ${dockerRepositoryName}/${projectName}"
                                }
                            }
                        }
                    }

                    stage('Deploy BPPR application(s)') {
                        steps {
                            script {
                                deploymentProvider.deployApplication()
                            }
                        }
                    }

                    stage('Verify readiness of BPPR application(s)') {
                        steps {
                            script {
                                deploymentProvider.verifyReadiness()
                            }
                        }
                    }
                    stage('Integration test - Public Api') {
                        when {
                            expression {
                                integrationTests.publicApi.runTest
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    List<ProcessedTestProperty> testProperties = deploymentProvider.processIntegrationTestParams(integrationTests.publicApi.testProperties)
                                    if (integrationTests.legacyMode) {
                                        buildTool.integrationTest(testProperties)
                                    } else {
                                        buildTool.integrationTestPublicApi(testProperties)
                                    }

                                }
                            }
                        }
                        post {
                            always {
                                script {
                                    String path = integrationTests.legacyMode ? buildTool.getIntegrationTestResultPath() : buildTool.getPublicApiTestResultPath()
                                    junit allowEmptyResults: true, testResults: path
                                }
                            }
                        }
                    }

                    stage('Integration test - Internal Api') {
                        when {
                            beforeAgent true
                            expression {
                                integrationTests.internalApi.runTest
                            }
                        }
                        agent {
                            kubernetes {
                                label "${projectName}"
                                namespace projectName
                                inheritFrom integrationTests.internalApi.agentInheritFrom
                                serviceAccount integrationTests.internalApi.agentServiceAccount
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    List<ProcessedTestProperty> testProperties = deploymentProvider.processIntegrationTestParamsWithoutCluster(integrationTests.internalApi.testProperties)
                                    if (integrationTests.legacyMode) {
                                        buildTool.integrationTest(testProperties)
                                    } else {
                                        buildTool.integrationTestInternalApi(testProperties)
                                    }
                                }
                            }
                        }
                        post {
                            always {
                                script {
                                    String path = integrationTests.legacyMode ? buildTool.getIntegrationTestResultPath() : buildTool.internalApiTestResultPath()
                                    junit allowEmptyResults: true, testResults: path
                                }
                            }
                        }
                    }

                    stage('Manual test confirmation') {
                        when {
                            expression {
                                pipelineConfig.continuousDelivery.manualTest
                            }
                        }
                        steps {
                            script {
                                def confirm_test_pass = input message: 'Have manual tests passed?',
                                        parameters: [
                                                choice(name: '', choices: ['yes', 'no'].join('\n'), description: 'Please confirm'),
                                        ]

                                if (confirm_test_pass == 'yes') {
                                    env.CONFIRM_TEST_PASS = confirm_test_pass
                                } else {
                                    error "Manual tests have failed"
                                }
                            }
                        }
                    }

                    stage('Delete BPPR environment') {
                        when {
                            expression {
                                pipelineConfig.continuousDelivery.deleteAfterSuccess
                            }
                        }
                        steps {
                            script {
                                projectManagementProvider.deleteProject(deploymentProviderParameters)
                            }
                        }
                        post {
                            always {
                                script {
                                    projectManagementProvider.unlockResource()
                                }
                            }
                        }
                    }
                }
            }
        }

        post {
            failure {
                script {
                    notifyViaEmailAboutFailure(pipelineConfig.mailRecipients)
                }
            }

            always {
                junit allowEmptyResults: true, testResults: buildTool.getTestResultPath()
                cleanWs()
                script {
                    if (isPublishingSuccessful) {
                        imageRepository.delete()
                    }
                }
            }
        }
    }
}

private BuildToolParameters createBuildToolParameters(def pipelineConfig, ImageRepository dockerImageRepository) {
    BuildToolParameters buildToolParameters = new BuildToolParameters(
            sonarProfile: pipelineConfig.sonarProfile,
            buildToolCustomClass: pipelineConfig.buildToolCustomClass,
            useWrapper: pipelineConfig.useWrapper as boolean,
            loggingLevel: pipelineConfig.buildToolLoggingLevel ? pipelineConfig.buildToolLoggingLevel as LoggingLevel : LoggingLevel.DEFAULT
    )
    dockerImageRepository.setPushCredentials(buildToolParameters)
    return buildToolParameters
}