import dms.devops.shared.library.buildtool.dmsBuildTool
import dms.devops.shared.library.buildtool.dmsBuildToolFactory
import dms.devops.shared.library.buildtool.parameters.BuildToolParametersFactory
import dms.devops.shared.library.version.VersionType
import dms.devops.shared.library.version.VersionUtil
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.buildtool.*
import org.jfrog.hudson.ArtifactoryBuilder
import org.jfrog.hudson.ArtifactoryServer
import hudson.AbortException

def call(Closure body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    def defaultPipelineConfig = [
            agentLabel         : 'master',
            useWrapper         : true,
            skipPublishing     : false,
            gitSshCredentials  : 'bitbucket-ssh-credentials',
            buildConfig        : [extraArgs: null],
            componentTestConfig: [agentLabel: null],
            directory          : '.',
            publishTestResults : true,
            skipComponentTest  : false,
            cleaningStrategy   : [
                    daysToKeep: '14',
                    numToKeep : '10'
            ]

    ]

    pipelineConfig = defaultPipelineConfig + pipelineConfig

    if (pipelineConfig.skipPublishing == false && pipelineConfig.publishingConfig == null) {
        ArtifactoryServer defaultArtifactoryServer = Jenkins.getInstance().getExtensionList(ArtifactoryBuilder.DescriptorImpl.class)[0].getArtifactoryServers()[0]

        pipelineConfig.publishingConfig = [
                credentials        : defaultArtifactoryServer.getDeployerCredentialsConfig().getCredentialsId(),
                serverId           : defaultArtifactoryServer.getName(),
                repoType           : RepoType.ARTIFACTORY,
                prefixBuildInfoName: System.getenv("BUILD_INFO_REPOSITORY_PREFIX")
        ]
    }


    final String BUILD_DIRECTORY = pipelineConfig.directory

    dmsBuildTool buildTool

    if (System.getenv('LOCAL_MODE_ENABLED') == 'true') {
        COMPONENT_TEST_AGENT_LABEL = pipelineConfig.agentLabel
    } else {
        COMPONENT_TEST_AGENT_LABEL = pipelineConfig.componentTestConfig.agentLabel ?: pipelineConfig.agentLabel
    }

    def isCommitTagged = false
    boolean testsExecuted = false
    String version = ""
    final String LATEST_RELEASE_TAG = "latest-release"

    echo pipelineConfig.toMapString()

    pipeline {
        agent {
            label pipelineConfig.agentLabel
        }
        environment {
            JIRA_SITE = "${System.getenv('JIRA_SITE_NAME')}"
            DOCKER_RESOURCE_LOCK_LABEL = "${System.getenv('DOCKER_RESOURCE_LOCK_LABEL')}"
        }
        parameters {
            choice(name: 'releaseType', choices: ['MINOR', 'PATCH', 'MAJOR'], description: 'Release type')
        }
        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: ''),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: ''))
            )
            copyArtifactPermission('xray_publish_report_devops')
            disableConcurrentBuilds()
        }
        stages {
            stage('Init build-tool') {
                steps {
                    script {
                        buildTool = createBuildTool(pipelineConfig, getThisObject(), BuildToolType.GRADLE)
                        buildTool.prepareTestExcludeArgs(this)
                        buildTool.prepareDockerExcludeArgs(this)
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

            stage('Create Release') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            version = buildTool.currentVersion(this)
                            if (!VersionUtil.isSnapshotVersion(version)) {
                                isCommitTagged = true
                                error("Current version is $version. Unable to release already released version")
                            }
                            sshagent(credentials: [pipelineConfig.gitSshCredentials]) {
                                buildTool.createRelease(params.releaseType as VersionType)
                            }

                            version = buildTool.currentVersion(this)
                            currentBuild.displayName = "${env.BUILD_DISPLAY_NAME} release ${version}"
                        }
                    }
                }
            }

            stage('Compile') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.compile()
                        }
                    }
                }
            }

            stage('Unit Test') {
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
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            withDockerAgentLock(pipelineConfig.agentLabel) {
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
                }
//              agent { // Will be fixed with task dms-3637
//                  label "${COMPONENT_TEST_AGENT_LABEL}"
//              }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            withDockerAgentLock(pipelineConfig.agentLabel) {
                                catchError(stageResult: 'FAILURE') {
                                    buildTool.functionalTest()
                                }
                            }
                        }
                    }
                }
            }

            stage('Sonarqube Analysis') {
                when {
                    expression { pipelineConfig.sonarqubeServerKey }
                    expression { currentBuild.result != "FAILURE" }
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
                }
                steps {
                    script {
                        qualityGateStep(pipelineConfig.sonarqubeDisableWebhook)
                    }
                }
            }

            stage('Build artifacts') {
                when {
                    expression { currentBuild.result != "FAILURE" }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.buildArtifacts(pipelineConfig.buildConfig.extraArgs)
                        }
                    }
                }
            }

            stage('Publish artifacts') {
                when {
                    expression { !pipelineConfig.skipPublishing }
                    expression { currentBuild.result != "FAILURE" }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            withCredentials([usernamePassword(credentialsId: pipelineConfig.publishingConfig?.credentials,
                                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                buildTool.publishArtifacts([LATEST_RELEASE_TAG])
                            }
                        }
                    }
                }
            }

            stage('Push Tag to repository') {
                when {
                    expression { currentBuild.result != "FAILURE" }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            sshagent(credentials: [pipelineConfig.gitSshCredentials]) {
                                buildTool.pushRelease()
                            }
                        }
                    }
                }
            }

        }
        post {
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
                        step( [ $class: 'JacocoPublisher' ] )
                    } catch (AbortException ex) {
                        echo "$ex"
                    }
                }
            }
            success {
                script {
                    echo "Succesfully released version ${version}"
                }
            }
            failure {
                script {
                    if (!isCommitTagged) {
                        sh("git tag -d release-${version}")
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
    final BuildToolParameters buildToolParameters = new BuildToolParametersFactory(script).create(pipelineConfig)
    final RepoType repoType = pipelineConfig.publishingConfig?.repoType as RepoType ?: RepoType.GENERIC
    ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, this)
    BuildObject buildObject = new BuildObjectFactory(this).createBuildObject(repoType, buildToolType, pipelineConfig.buildToolName as String, buildToolParameters)
    return new dmsBuildToolFactory().createBuildTool(buildToolType, buildObject, repository, buildToolParameters)
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

def withDockerAgentLock(String agentName, Closure body) {
    if (agentName == "docker") {
        lock(label: env.DOCKER_RESOURCE_LOCK_LABEL, quantity: 1) {
            body.call()
        }
    } else {
        body.call()
    }
}