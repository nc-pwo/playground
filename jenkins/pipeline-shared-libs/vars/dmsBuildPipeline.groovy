import dms.devops.shared.library.buildtool.dmsBuildTool
import dms.devops.shared.library.buildtool.dmsBuildToolFactory
import dms.devops.shared.library.version.VersionUtil
import groovy.json.JsonSlurper
import hudson.AbortException
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.buildtool.*
import nc.devops.shared.library.buildtool.logging.LoggingLevel
import nc.devops.shared.library.utils.CronUtils
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jfrog.hudson.ArtifactoryBuilder
import org.jfrog.hudson.ArtifactoryServer

def call(Closure body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    def defaultPipelineConfig = [
            useWrapper         : true,
            publishTestResults : true,
            skipPublishing     : false,
            skipComponentTest  : false,
            buildConfig        : [extraArgs: null],
            componentTestConfig: [agentLabel: null],
            cleaningStrategy   : [
                    daysToKeep: '7',
                    numToKeep : '8'
            ],
            deploySnapshot : true
    ]
    pipelineConfig = defaultPipelineConfig + pipelineConfig


    pipelineConfig.skipIntegrationTest = params.SKIP_INTEGRATION_TEST ?: pipelineConfig.skipIntegrationTest ?: false

    if (pipelineConfig.skipPublishing == false && pipelineConfig.publishingConfig == null) {
        ArtifactoryServer defaultArtifactoryServer = Jenkins.getInstance().getExtensionList(ArtifactoryBuilder.DescriptorImpl.class)[0].getArtifactoryServers()[0]

        pipelineConfig.publishingConfig = [
                credentials        : defaultArtifactoryServer.getDeployerCredentialsConfig().getCredentialsId(),
                serverId           : defaultArtifactoryServer.getName(),
                repoType           : RepoType.ARTIFACTORY,
                prefixBuildInfoName: System.getenv("BUILD_INFO_REPOSITORY_PREFIX")
        ]
    }

    final String BUILD_DIRECTORY = pipelineConfig.directory ?: "."

    dmsBuildTool buildTool
    BuildToolType buildToolType

    def DEPLOYMENT_ENV = 'dev03'
    def DEPLOYMENT_PREFIX = 'deploy_deploy_'
    def AGENT_LABEL, COMPONENT_TEST_AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')
    if (System.getenv('LOCAL_MODE_ENABLED') == 'true') {
        COMPONENT_TEST_AGENT_LABEL = AGENT_LABEL
    } else {
        COMPONENT_TEST_AGENT_LABEL = pipelineConfig.componentTestConfig.agentLabel ?: AGENT_LABEL
    }

    def buildBadge = addEmbeddableBadgeConfiguration(id: "buildBadge", subject: "Latest Build")
    def integrationTestDirName = pipelineConfig.outerIntegrationTestsParams?.integrationTestsDir ?: BUILD_DIRECTORY

    final String cronExpression = CronUtils.calculateCronExpression(this, pipelineConfig)

    def reinitAfterJenkinsRestart = {
        buildTool.setCpsScript((CpsScript) this)
    }

    def isCommitTagged = false
    boolean testsExecuted = false

    String version = ""

    echo pipelineConfig.toMapString()

    pipeline {
        agent {
            label "${AGENT_LABEL}"
        }
        triggers {
            bitbucketPush()
            cron(cronExpression)
        }
        environment {
            JIRA_SITE = "${System.getenv('JIRA_SITE_NAME')}"
            DOCKER_RESOURCE_LOCK_LABEL = "${System.getenv('DOCKER_RESOURCE_LOCK_LABEL')}"
            GIT_COMMIT_SHORT = "${env.GIT_COMMIT}".take(8)
            ARTIFACTORY_CREDENTIALS = credentials('artifactory-reader-token')
        }
        tools {
            jdk pipelineConfig.jdk ?: 'Default'
        }
        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: '14'),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: '30'))
            )
            copyArtifactPermission('xray_publish_report_devops,deploy_*')
        }

        stages {
            stage('Initialize Build') {
                stages {
                    stage('Init build-tool') {
                        steps {
                            script {
                                buildToolType = BuildToolType.GRADLE
                                buildTool = createBuildTool(pipelineConfig, getThisObject(), buildToolType)
                                buildTool.prepareTestExcludeArgs(this)
                                buildTool.prepareDockerExcludeArgs(this)
                                version = buildTool.currentVersion(this)
                                currentBuild.displayName = "${env.BUILD_DISPLAY_NAME} ${version} ${env.GIT_COMMIT_SHORT}"
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

                    stage('Retrieve version from git') {
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    List<FileWrapper> jenkinfilesFileList = findFiles(glob: "**/release.jenkinsfile")
                                    def isRepoHasReleaseConfig = jenkinfilesFileList.size > 0

                                    isCommitTagged = isRepoHasReleaseConfig && !VersionUtil.isSnapshotVersion(version) && VersionUtil.isConcreteVersion(version);

                                    if (isCommitTagged) {
                                        echo("Version $version is already released, The build pipeline will be skipped")
                                    } else {
                                        echo("Current Version: $version")
                                    }
                                }
                            }
                        }
                    }

                    stage('Compile') {
                        when {
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    buildTool.compile()
                                }
                            }
                        }
                    }

                    stage('Unit Test') {
                        when {
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    testsExecuted = true
                                    catchError(stageResult: 'FAILURE') {
                                        buildTool.unitTest()
                                    }
                                }
                            }
                        }
                    }

                    stage('Integration Test') {
                        when {
                            beforeAgent true
                            expression { buildTool.testExcludeArgs.contains("integrationTest") }
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    withDockerAgentLock(AGENT_LABEL) {
                                        catchError(stageResult: 'FAILURE') {
                                            buildTool.integrationTest()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    stage('Functional Test') {
                        when {
                            beforeAgent true
                            allOf {
                                expression { pipelineConfig.skipComponentTest == false }
                                expression { buildTool.testExcludeArgs.contains("functionalTest") }
                            }
                            expression {
                                !isCommitTagged
                            }
                        }
//                        agent { // Will be fixed with task dms-3637
//                            label "${COMPONENT_TEST_AGENT_LABEL}"
//                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    withDockerAgentLock(AGENT_LABEL) {
                                        catchError(stageResult: 'FAILURE') {
                                            buildTool.functionalTest()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    stage('Build') {
                        when {
                            expression { currentBuild.result != "FAILURE" }
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    buildTool.buildArtifacts(pipelineConfig.buildConfig.extraArgs)
                                }
                            }
                        }
                    }

                    stage('Sonarqube Analysis') {
                        when {
                            expression { pipelineConfig.sonarqubeServerKey }
                            expression { currentBuild.result != "FAILURE" }
                            expression {
                                !isCommitTagged
                            }
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
                            expression { currentBuild.result != "FAILURE" }
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            script {
                                qualityGateStep(pipelineConfig.sonarqubeDisableWebhook)
                            }
                        }
                    }

                    stage('Build Image') {
                        when {
                            expression { currentBuild.result != "FAILURE" }
                            expression { buildTool.dockerExcludeArgs.contains("buildImage") }
                            expression { !isCommitTagged }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    buildTool.buildImage()
                                }
                            }
                        }
                    }

                    stage('Publish to Universal Repository') {
                        when {
                            expression { pipelineConfig.skipPublishing == false }
                            expression { currentBuild.result != "FAILURE" }
                            expression {
                                !isCommitTagged
                            }
                        }
                        steps {
                            dir(BUILD_DIRECTORY) {
                                script {
                                    withCredentials([usernamePassword(credentialsId: pipelineConfig.publishingConfig?.credentials,
                                            usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                        buildTool.publishArtifacts()
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
                script {
                    notifyViaEmailAboutFailure(pipelineConfig.mailRecipients)
                }
            }
            success {
                script {
                    if (pipelineConfig.skipPublishing == false) {
                        echo "Successfully published artifact with version ${version} from commit ${env.GIT_COMMIT_SHORT}"
                    }
                    if (pipelineConfig.deploySnapshot == true && buildTool.dockerExcludeArgs.contains("buildImage")) {
                        try {
                            build job: DEPLOYMENT_PREFIX + gitUtils.getRepositoryName(env.GIT_URL),
                                    parameters: [string(name: 'applicationVersion', value: "${version}"),
                                                 string(name: 'configurationVersion', value: getLatestDeploymentValues()),
                                                 string(name: 'envName', value: "${DEPLOYMENT_ENV}"),
                                                 string(name: 'mailRecipients', value: getLastCommitterEmail())], wait: false
                        } catch (AbortException ex) {
                            print 'Skip deploying because can not found deployment pipeline'
                        }
                    }
                }
            }
            always {
                script {
                    if (pipelineConfig.publishTestResults && testsExecuted) {
                        def xray = [
                                commitId   : env.GIT_COMMIT,
                                jobName    : env.JOB_BASE_NAME,
                                buildNumber: env.BUILD_NUMBER
                        ]
                        dmsXrayReportRunner(xray)
                    }
                    try {
                        def testResultPath = buildTool.getTestResultPath()
                        junit allowEmptyResults: true, testResults: testResultPath
                        step([$class: 'JacocoPublisher'])
                    } catch (AbortException ex) {
                        echo "$ex"
                    }
                }
            }
            cleanup {
                dmsMasterCleanup()
            }
        }
    }
}

private dmsBuildTool createBuildTool(Map pipelineConfig, def script, BuildToolType buildToolType) {
    final BuildToolParameters buildToolParameters = createBuildToolParameters(pipelineConfig, script)
    final RepoType repoType = pipelineConfig.publishingConfig?.repoType as RepoType ?: RepoType.GENERIC
    ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, this)
    BuildObject buildObject = new BuildObjectFactory(this).createBuildObject(repoType, buildToolType, pipelineConfig.buildToolName as String, buildToolParameters)
    return new dmsBuildToolFactory().createBuildTool(buildToolType, buildObject, repository, buildToolParameters)
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

private String getLatestDeploymentValues() {
    String deploymentValues = sh label: 'Get list of deployment values',
            script: "curl -su $ARTIFACTORY_CREDENTIALS https://artifactory.nchosting.dk/api/search/pattern?pattern=ufstdms-generic-dev-local:deployment-values/deployment-values-?.?.?.zip",
            returnStdout: true
    def jsonSlurper = new JsonSlurper()
    def files = jsonSlurper.parseText(deploymentValues).files
    if (files.isEmpty()) {
        return '0.1.0-SNAPSHOT'
    } else {
        String currentVersion = ''
        files.each {
            if (currentVersion.isEmpty()) {
                currentVersion = extractVersion(it)
            } else {
                currentVersion = VersionUtil.getLatestVersion(currentVersion, extractVersion(it))
            }
        }
        return currentVersion
    }
}

private extractVersion(def fileName) {
    def CONCRETE_VERSION_PATTERN = ~/\d+\.\d+\.\d+/
    def matcher = (fileName =~ CONCRETE_VERSION_PATTERN)
    return matcher.find() ? matcher[0] : ''
}

private getLastCommitterEmail() {
    String email = sh label: 'Get Last committer email ',
            script: "git log --format='%ae' | head -1",
            returnStdout: true
    return email
}

def withDockerAgentLock(String agentName, Closure body) {
    if (agentName == "docker") {
        lock(label: env.DOCKER_RESOURCE_LOCK_LABEL, quantity: 1) {
            body.call()
        }
    } else {
        body.call()
    }
}