import groovy.transform.Field
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
import nc.devops.shared.library.tests.ComponentTestPropertiesIdentityMapper
import nc.devops.shared.library.tests.IntegrationTestsMapper
import nc.devops.shared.library.tests.model.IntegrationTests
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.utils.CronUtils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.ArtifactoryBuilder
import org.jfrog.hudson.ArtifactoryServer

@Field static final pollSCMStrategy = '* * * * *'

def call(Closure body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    final String OPENSHIFT_TOKEN_CREDENTIALS = "openshift-token-credentials"
    final String OKD_TOKEN_CREDENTIALS = "shared-okd-token-credentials"
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    ProjectManagementProvider projectManagementProvider
    def lockArgs

    if (pipelineConfig.skipPublishing == null) {
        pipelineConfig.skipPublishing = false
    }

    boolean publishingEnabledForThisBranch
    boolean deploymentEnabledForThisBranch

    pipelineConfig.useWrapper = pipelineConfig.useWrapper ?: false

    if (pipelineConfig.skipPublishing == false && pipelineConfig.publishingConfig == null) {
        ArtifactoryServer defaultArtifactoryServer = Jenkins.getInstance().getExtensionList(ArtifactoryBuilder.DescriptorImpl.class)[0].getArtifactoryServers()[0]

        pipelineConfig.publishingConfig = [
                credentials        : defaultArtifactoryServer.getDeployerCredentialsConfig().getCredentialsId(),
                serverId           : defaultArtifactoryServer.getName(),
                repoType           : RepoType.ARTIFACTORY,
                prefixBuildInfoName: System.getenv("BUILD_INFO_REPOSITORY_PREFIX")
        ]
    }

    if (pipelineConfig.continuousDelivery) {
        pipelineConfig.continuousDelivery.deliveryCluster = params.DELIVERY_CLUSTER ?: pipelineConfig.continuousDelivery.deliveryCluster ?: System.getenv('DEFAULT_CLUSTER')
        def defaultContinuousDelivery = [
                openshiftToken      : pipelineConfig.continuousDelivery.deliveryCluster == System.getenv('DEFAULT_CLUSTER') ? OPENSHIFT_TOKEN_CREDENTIALS : OKD_TOKEN_CREDENTIALS,
                idleAfterSuccess    : false,
                forceDeployment     : false,
                defaultTimeOut      : 5,
                projectName         : gitUtils.prepareProjectName(env.JOB_NAME as String),
                deploymentParameters: [],
                kubernetesCluster   : [credentialsId: System.getenv("KUBERNETES_SA_TOKEN") ?: 'aks-token',
                                       serverUrl    : System.getenv("KUBERNETES_CLUSTER_URL") ?: 'https://accelerators-aks-dns-8c106221.hcp.uksouth.azmk8s.io:443',
                                       namespace    : System.getenv("KUBERNETES_SA_NAMESPACE") ?: 'dev',
                                       caCertificate: System.getenv("KUBERNETES_CA_CERT"),
                                       clusterName  : System.getenv("KUBERNETES_CLUSTER_NAME") ?: 'k8s',
                                       contextName  : System.getenv("KUBERNETES_CONTEXT_NAME") ?: 'k8s']
        ]
        pipelineConfig.continuousDelivery = defaultContinuousDelivery + pipelineConfig.continuousDelivery
        pipelineConfig.continuousDelivery.projectName = params.PROJECT_NAME ?: pipelineConfig.continuousDelivery.projectName
        pipelineConfig.continuousDelivery.projectManagementProvider = (pipelineConfig.continuousDelivery.projectManagementProvider as ProjectManagementProviderType)
                ?: ProjectManagementProviderType.OPENSHIFT_BUILD_WITHOUT_JENKINS_LOCK

        projectManagementProvider = new ProjectManagementProviderFactory().
                createProjectManagementProvider(pipelineConfig.continuousDelivery.projectManagementProvider as ProjectManagementProviderType, pipelineConfig, this as CpsScript)
        lockArgs = projectManagementProvider.getLockableResourcesPluginOptions()
    }

    final String BUILD_DIRECTORY = pipelineConfig.directory ?: "."

    BuildTool buildTool
    DeploymentProvider deploymentProvider
    IntegrationTests integrationTests
    String shortCommitID

    if (pipelineConfig.integrationTests != null) {
        pipelineConfig.integrationTests = params.SKIP_INTEGRATION_TEST ? [:] : pipelineConfig.integrationTests
        integrationTests = new IntegrationTestsMapper().create(pipelineConfig?.integrationTests as Map<String, Map<String, Object>>)
    } else {
        pipelineConfig.skipIntegrationTest = params.SKIP_INTEGRATION_TEST ?: pipelineConfig.skipIntegrationTest ?: false
        integrationTests = new IntegrationTestsMapper().createFromLegacySource(pipelineConfig)
    }

    BuildToolType buildToolType
    TemplateProcessingToolType templateProcessingToolType
    DeploymentProviderType deploymentProviderType
    List<String> additionalTags = []
    String projectName

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')
    def OKD_AGENT_LABEL = params.DELIVERY_AGENT ?: pipelineConfig.continuousDelivery?.deliveryAgent ?: AGENT_LABEL
    def buildBadge = addEmbeddableBadgeConfiguration(id: "buildBadge", subject: "Latest Build")

    final String pollExpression = CronUtils.calculatePollSCMExpression(this)
    final String cronExpression = CronUtils.calculateCronExpression(this, pipelineConfig)

    final int OPENSHIFT_LABEL_COMMIT_LENGTH = 8

    def reinitAfterJenkinsRestart = {
        buildTool.setCpsScript(this as CpsScript)
    }

    echo pipelineConfig.toMapString()
    pipeline {
        agent none
        triggers {
            pollSCM(pollExpression)
            cron(cronExpression)
        }
        environment {
            JIRA_SITE = "${System.getenv('JIRA_SITE_NAME')}"
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
            stage('Initialize Build') {
                agent {
                    label "${AGENT_LABEL}"
                }
                stages {
                    stage('Init build-tool') {
                        steps {
                            script {
                                buildToolType = pipelineConfig.buildToolType as BuildToolType
                                buildTool = createBuildTool(pipelineConfig, getThisObject(), buildToolType)
                                publishingEnabledForThisBranch = isActionEnabledForNonMainBranch(pipelineConfig.publishingFromNonMainBranchEnabled ?: false)
                                shortCommitID = env.GIT_COMMIT?.take(OPENSHIFT_LABEL_COMMIT_LENGTH) ?: "manually_created"
                                additionalTags.add(shortCommitID)
                            }
                        }
                    }

                    stage('Init deployment provider and processing tool') {
                        when {
                            expression { pipelineConfig.continuousDelivery }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    String releaseVersion = getArtifactVersion(buildToolType)
                                    pipelineConfig.continuousDelivery.deploymentParameters += ["COMMIT_ID=$shortCommitID",
                                                                                               "DOCKER_IMAGE_VERSION=$releaseVersion",
                                                                                               "PROJECT_NAME=$pipelineConfig.continuousDelivery.projectName"]
                                    deploymentEnabledForThisBranch = isActionEnabledForNonMainBranch(pipelineConfig.continuousDelivery.forceDeployment)
                                }
                            }
                            script {
                                deploymentProviderType = (pipelineConfig.continuousDelivery.deploymentProviderType as DeploymentProviderType) ?: DeploymentProviderType.OPENSHIFT
                                templateProcessingToolType = (pipelineConfig.continuousDelivery.templateProcessingTool as TemplateProcessingToolType) ?: TemplateProcessingToolType.OC

                                DeploymentProviderParameters deploymentProviderParameters = new DeploymentProviderParameters(
                                        deliveryCluster: pipelineConfig.continuousDelivery.deliveryCluster,
                                        defaultTimeout: pipelineConfig.continuousDelivery.defaultTimeOut,
                                        projectName: pipelineConfig.continuousDelivery.projectName,
                                        requiredComponents: pipelineConfig.continuousDelivery?.requiredComponents,
                                        templatePath: pipelineConfig.continuousDelivery?.templatePath,
                                        gitCommitId: env.GIT_COMMIT?.take(OPENSHIFT_LABEL_COMMIT_LENGTH) ?: "manually_created",
                                        credentialParameters: pipelineConfig.continuousDelivery?.credentialParameters,
                                        deploymentParameters: pipelineConfig.continuousDelivery?.deploymentParameters,
                                        bppr: false,
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
                                    buildBadge.setStatus("running")
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
                            dir(integrationTests.component?.externalRepository?.cloneDirectory ?: BUILD_DIRECTORY) {
                                script {

                                    if (integrationTests.component.externalRepository) {
                                        git url: integrationTests.component.externalRepository.url,
                                                credentialsId: integrationTests.component.externalRepository.credentials,
                                                branch: integrationTests.component.externalRepository.branch
                                    }

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
                                        buildTool.staticCodeAnalysis(new StaticCodeAnalysisParams(branchName: branchName()))
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
                            script {
                                qualityGateStep(pipelineConfig.sonarqubeDisableWebhook)
                            }
                        }
                    }

                    stage('Publish to Universal Repository') {
                        when {
                            expression { pipelineConfig.skipPublishing == false && publishingEnabledForThisBranch }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    withCredentials([usernamePassword(credentialsId: pipelineConfig.publishingConfig.credentials,
                                            usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                        buildTool.publishArtifacts(additionalTags)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            stage('Continuous Delivery') {
                when {
                    beforeAgent true
                    expression { pipelineConfig.continuousDelivery && deploymentEnabledForThisBranch }
                }
                agent {
                    label "${OKD_AGENT_LABEL}"
                }
                options {
                    lock lockArgs
                }
                stages {
                    stage('Create Project') {
                        steps {
                            script {
                                projectName = deploymentProvider.useProject(projectManagementProvider)
                            }
                        }
                    }

                    stage('Deploy Application') {
                        steps {
                            script {
                                withJenkinsRedeployed(reinitAfterJenkinsRestart) {
                                    deploymentProvider.deployApplication()
                                }
                            }
                        }
                    }

                    stage('Verify readiness of application') {
                        steps {
                            script {
                                withJenkinsRedeployed(reinitAfterJenkinsRestart, true) {
                                    deploymentProvider.verifyReadiness()
                                }
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
                            dir(integrationTests.publicApi?.externalRepository?.cloneDirectory ?: BUILD_DIRECTORY) {
                                script {
                                    if (integrationTests.publicApi.externalRepository) {
                                        git url: integrationTests.publicApi.externalRepository.url,
                                                credentialsId: integrationTests.publicApi.externalRepository.credentials,
                                                branch: integrationTests.publicApi.externalRepository.branch
                                    }

                                    List<ProcessedTestProperty> integrationTestProperties = deploymentProvider.processIntegrationTestParams(integrationTests.publicApi.testProperties)

                                    if (integrationTests.legacyMode) {
                                        buildTool.integrationTest(integrationTestProperties)
                                    } else {
                                        buildTool.integrationTestPublicApi(integrationTestProperties)
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
                            dir(integrationTests.internalApi?.externalRepository?.cloneDirectory ?: BUILD_DIRECTORY) {
                                script {
                                    if (integrationTests.internalApi.externalRepository) {
                                        git url: integrationTests.internalApi.externalRepository.url,
                                                credentialsId: integrationTests.internalApi.externalRepository.credentials,
                                                branch: integrationTests.internalApi.externalRepository.branch
                                    }

                                    List<ProcessedTestProperty> testProperties = deploymentProvider.processIntegrationTestParams(integrationTests.internalApi.testProperties)
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
                                    String path = integrationTests.legacyMode ? buildTool.getIntegrationTestResultPath() : buildTool.getInternalApiTestResultPath()
                                    junit allowEmptyResults: true, testResults: path
                                }
                            }
                        }
                    }


                    stage('Idle application') {
                        when {
                            expression { pipelineConfig.continuousDelivery.idleAfterSuccess }
                        }
                        steps {
                            script {
                                deploymentProvider.idleApplication()
                            }
                        }
                        post {
                            always {
                                script {
                                    if (pipelineConfig.continuousDelivery) {
                                        projectManagementProvider.unlockResource()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        post {
            failure {
                node("${AGENT_LABEL}") {
                    script {
                        buildBadge.setStatus("failed")
                        notifyViaEmailAboutFailure(pipelineConfig.mailRecipients)
                    }
                }
            }
            success {
                node("${AGENT_LABEL}") {
                    script {
                        buildBadge.setStatus("success")
                    }
                }
            }
            always {
                node("${AGENT_LABEL}") {
                    junit allowEmptyResults: true, testResults: buildTool.getTestResultPath()
                    publishXrayReport(pipelineConfig?.xray)
                }
            }
        }
    }
}

private BuildTool createBuildTool(Map pipelineConfig, def script, BuildToolType buildToolType) {
    final BuildToolParameters buildToolParameters = createBuildToolParameters(pipelineConfig, script)
    script.echo "Using $buildToolParameters"
    final RepoType repoType = pipelineConfig.publishingConfig?.repoType as RepoType ?: RepoType.GENERIC
    ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, this)
    BuildObject buildObject = new BuildObjectFactory(this).createBuildObject(repoType, buildToolType, pipelineConfig.buildToolName as String, buildToolParameters)
    return new BuildToolFactory().createBuildTool(buildToolType, buildObject, repository, buildToolParameters)
}

private BuildToolParameters createBuildToolParameters(Map pipelineConfig, def script) {
    BuildToolParameters buildToolParameters = new BuildToolParameters(
            mavenSettingsConfig: pipelineConfig.mavenSettingsConfig,
            sonarProfile: pipelineConfig.sonarProfile,
            buildToolCustomClass: pipelineConfig.buildToolCustomClass,
            useWrapper: pipelineConfig.useWrapper as boolean,
            loggingLevel: pipelineConfig.buildToolLoggingLevel != null ? pipelineConfig.buildToolLoggingLevel as LoggingLevel : LoggingLevel.DEFAULT
    )
    if (pipelineConfig.skipPublishing == false) {
        buildToolParameters.serverId = pipelineConfig.publishingConfig?.serverId
        buildToolParameters.prefixBuildInfoName = pipelineConfig.publishingConfig?.prefixBuildInfoName
        buildToolParameters.releaseRepo = pipelineConfig.publishingConfig?.releaseRepo
        buildToolParameters.snapshotRepo = pipelineConfig.publishingConfig?.snapshotRepo

        script.withCredentials([script.usernamePassword(credentialsId: pipelineConfig.publishingConfig?.credentials,
                usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            buildToolParameters.binaryRepoUsername = env.USERNAME
            buildToolParameters.binaryRepoPassword = env.PASSWORD
            buildToolParameters.pushRegistryUsername = env.USERNAME
            buildToolParameters.pushRegistryPassword = env.PASSWORD
        }
    }
    return buildToolParameters
}

private boolean isActionEnabledForNonMainBranch(boolean requestInclusion) {
    return branchName() == 'master' || requestInclusion
}

/**
 * Note: Works properly only inside agent as it calls env.GIT_BRANCH
 */
private String branchName() {
    if (localMode.isEnabled()) {
        return localMode.branchNameForLocalMode()
    } else {
        if (env.GIT_BRANCH == null) {
            throw new RuntimeException('GIT_BRANCH environment variable is not set, but required.')
        } else {
            return env.GIT_BRANCH.replaceAll('origin/', '')
        }
    }
}

private String getArtifactVersion(BuildToolType buildToolType) {
    def version
    switch (buildToolType) {
        case BuildToolType.GRADLE:
            def projectProperties = fileUtils.readFile('gradle.properties')
            version = projectProperties['version']
            break
        case BuildToolType.MAVEN:
            def pom = readMavenPom file: 'pom.xml'
            version = pom.getVersion()
            break
        default:
            version = "UNKNOWN"
    }
    return version
}

def withJenkinsRedeployed(Closure init, boolean reExecute = false, Closure body) {
    try {
        body.call()
        init()
    } catch (Exception e) {
        if (e.message != null && e.message.contains("Resume after a restart not supported for non-blocking synchronous steps")) {
            echo "Jenkins redeployed - Ignored exception: " + e.toString()
            echo "Reinitializing after jenkins restart"
            init()
            if (reExecute) {
                echo "Reexecuting code"
                body.call()
            }
        } else {
            throw e
        }
    }
}