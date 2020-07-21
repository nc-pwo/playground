import dms.devops.shared.library.buildtool.dmsBuildToolFactory
import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.buildtool.*

def call(Closure body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    def defaultPipelineConfig = [
            useWrapper         : true,
            skipComponentTest  : false,
            buildConfig        : [extraArgs: null],
            componentTestConfig: [agentLabel: null],
            cleaningStrategy   : [
                    daysToKeep: '7',
                    numToKeep : '4'
            ]
    ]
    pipelineConfig = defaultPipelineConfig + pipelineConfig

    final String BUILD_DIRECTORY = pipelineConfig.directory ?: "."

    dmsBuildTool buildTool
    BuildToolType buildToolType

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

        tools {
            jdk pipelineConfig.jdk ?: 'Default'
        }
        environment {
            DOCKER_RESOURCE_LOCK_LABEL = "${System.getenv('DOCKER_RESOURCE_LOCK_LABEL')}"
            GIT_COMMIT_SHORT = "${env.GIT_COMMIT}".take(8)
        }
        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: '14'),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: '30'))
            )
        }

        stages {
            stage('Init build-tool') {
                steps {
                    script {
                        RepoType repoType = RepoType.GENERIC
                        buildToolType = BuildToolType.GRADLE
                        BuildToolParameters buildToolParameters = new BuildToolParameters(
                                sonarProfile: pipelineConfig.sonarProfile,
                                useWrapper: pipelineConfig.useWrapper as boolean,
                                buildToolCustomClass: pipelineConfig.buildToolCustomClass
                        )

                        ArtifactsMetadataRepository repository = RepositoryFactory.createMetadataRepo(repoType, this)
                        BuildObject selectedBuildObject = new BuildObjectFactory(this).createBuildObject(repoType, buildToolType, pipelineConfig.buildToolName as String, buildToolParameters)
                        buildTool = new dmsBuildToolFactory().createBuildTool(buildToolType, selectedBuildObject, repository, buildToolParameters)
                        buildTool.prepareTestExcludeArgs(this)
                        buildTool.prepareDockerExcludeArgs(this)

                        String version = buildTool.currentVersion(this)
                        currentBuild.displayName = "${env.BUILD_DISPLAY_NAME} ${version} ${env.GIT_COMMIT_SHORT}"
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
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            withSonarQubeEnv(pipelineConfig.sonarqubeServerKey) {
                                buildTool.staticCodeAnalysis(new PullRequestCodeAnalysisParams(
                                        pullRequestNumber: env.CHANGE_ID,
                                        pullRequestSourceBranchName: env.CHANGE_BRANCH,
                                        pullRequestBaseBranchName: env.CHANGE_TARGET,
                                        repoUrl: env.CHANGE_URL
                                ))
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
                    dir(BUILD_DIRECTORY) {
                        script {
                            qualityGateStep(pipelineConfig.sonarqubeDisableWebhook)
                        }
                    }
                }
            }

            stage('Build Image') {
                when {
                    expression { currentBuild.result != "FAILURE" }
                    expression { buildTool.dockerExcludeArgs.contains("buildImage") }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            buildTool.buildImage()
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    def testResultPath = buildTool.getTestResultPath()
                    junit allowEmptyResults: true, testResults: testResultPath
                    step( [ $class: 'JacocoPublisher' ] )
                }
            }
            failure {
                script {
                    notifyViaEmailAboutFailure(pipelineConfig.mailRecipients)
                }
            }
            success {
                cleanWs()
            }
            cleanup {
                dmsMasterCleanup()
            }
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