import nc.devops.shared.library.artifacts.ArtifactsMetadataRepository
import nc.devops.shared.library.artifacts.RepoType
import nc.devops.shared.library.buildtool.BuildObjectFactory
import nc.devops.shared.library.buildtool.BuildToolParameters
import nc.devops.shared.library.buildtool.BuildToolType
import nc.devops.shared.library.buildtool.RepositoryFactory
import nc.devops.shared.library.gradle.Gradle
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo

def call(body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    if (pipelineConfig.publishingConfig == null) {
        pipelineConfig.publishingConfig = [
                credentials: 'nc-dvo-operator-credentials',
                serverId   : 'art-1',
                repoType   : RepoType.ARTIFACTORY
        ]
    }
    pipelineConfig.useWrapper = pipelineConfig.useWrapper ?: false
    pipelineConfig.skipPublishing = pipelineConfig.skipPublishing ?: false

    def releaseBadge = addEmbeddableBadgeConfiguration(id: "releaseBadge", subject: "Release Build")

    ArtifactsMetadataRepository binaryRepository = RepositoryFactory.createMetadataRepo(pipelineConfig.publishingConfig.repoType, this)
    BuildToolParameters buildToolParameters = new BuildToolParameters(
            useWrapper: pipelineConfig.useWrapper as boolean
    )
    Gradle selectedGradle = new BuildObjectFactory(this).createBuildObject(pipelineConfig.publishingConfig.repoType as RepoType,
            BuildToolType.GRADLE,
            pipelineConfig.buildToolName as String, buildToolParameters) as Gradle
    final String BUILD_DIRECTORY = pipelineConfig.directory ?: "."
    final String SSH_AGENT_CREDENTIALS_ID = pipelineConfig.credentialsId ?: "nc-devops-credentials"
    final String ARTIFACT = pipelineConfig.artifact ?: "jar"

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')

    pipeline {
        agent {
            label "${AGENT_LABEL}"
        }
        environment {
            BINARY_REPO_CREDENTIALS = credentials("${pipelineConfig.publishingConfig.credentials}")
            BINARY_REPO_USER = "${env.BINARY_REPO_CREDENTIALS_USR}"
            BINARY_REPO_PASS = "${env.BINARY_REPO_CREDENTIALS_PSW}"
            PUSH_REGISTRY = credentials('nc-dvo-operator-credentials')
            PUSH_REGISTRY_USER = "${env.PUSH_REGISTRY_USR}"
            PUSH_REGISTRY_PASS = "${env.PUSH_REGISTRY_PSW}"
        }
        tools {
            jdk pipelineConfig.jdk ?: 'Default'
        }
        options {
            timestamps()
        }

        stages {
            stage('Checkout to local branch') {
                steps {
                    script {
                        releaseBadge.setStatus("running")
                    }
                    checkout([$class: 'GitSCM', branches: scm.branches, extensions: scm.extensions + [[$class: 'CleanCheckout', $class: 'LocalBranch']], userRemoteConfigs: scm.userRemoteConfigs ])
                }
            }

            stage('Clean') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            selectedGradle 'clean'
                        }
                    }
                }
            }

            stage('Compile') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            selectedGradle ARTIFACT
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            selectedGradle 'test -x ' + ARTIFACT
                        }
                    }
                }
            }

            stage('Commit release version') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                                selectedGradle 'commitRelease'
                            }
                        }
                    }
                }
            }

            stage('Build') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            selectedGradle 'build -x test ' + ARTIFACT
                        }
                    }
                }
            }

            stage('Publish to Binaries Repository') {
                when {
                    expression { !pipelineConfig.skipPublishing }
                }
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            def buildInfo = selectedGradle "publish -DbinaryRepoUsername=${BINARY_REPO_USER} -DbinaryRepoPassword=${BINARY_REPO_PASS} -PpushRegistryUsername=${PUSH_REGISTRY_USER} -PpushRegistryPassword=${PUSH_REGISTRY_PASS}"
                            if (buildInfo != null && buildInfo instanceof BuildInfo) {
                                buildInfo.setName("nc-dvo-main-" + buildInfo.getName())
                            }
                            binaryRepository.publishBuildInfo(pipelineConfig.publishingConfig.serverId, buildInfo)
                        }
                    }
                }
            }

            stage('Prepare next release version') {
                steps {
                    dir(BUILD_DIRECTORY) {
                        script {
                            sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                                selectedGradle 'prepareNextRelease'
                            }
                        }
                    }
                }
            }

            stage('Generate and publish changelog') {
                when {
                    expression {
                        pipelineConfig.changelogConfig
                    }
                }
                steps {
                    script {
                        sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                            changelogGenerationStep(pipelineConfig.changelogConfig)
                        }
                    }
                }
            }
        }
        post {
            failure {
                script {
                    releaseBadge.setStatus("failing")
                    notifyViaEmailAboutFailure(pipelineConfig.mailRecipients)
                }
            }
            success {
                script {
                    releaseBadge.setStatus("passing")
                }
            }
        }
    }
}

