def call(body) {
    def pipelineConfig = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    final String SSH_AGENT_CREDENTIALS_ID = pipelineConfig.credentialsId ?: "nc-devops-credentials"

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')

    pipeline {
        agent "$AGENT_LABEL"
        tools { nodejs 'Default' }
        parameters {
            string(name: 'COMMIT_ID', description: 'Git Commit ID', defaultValue: '')
            string(name: 'PULL_REQUEST_ID', description: 'Pull Request ID', defaultValue: '')
            string(name: 'SOURCE_BRANCH', description: 'Source Branch', defaultValue: '')
            string(name: 'TARGET_BRANCH', description: 'Target Branch', defaultValue: '')
        }
        stages {
            stage("Pre-merge pull request with the target branch") {
                steps {
                    script {
                        sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                            bat "git checkout ${params.TARGET_BRANCH - 'refs/heads/'}"
                            bat "git merge ${params.COMMIT_ID}"
                        }
                    }
                }
            }
            stage('Sonarqube Analysis') {
                steps {
                    script {
                        if (pipelineConfig.sonarqubeServerKey) {
                            withSonarQubeEnv(pipelineConfig.sonarqubeServerKey) {
                                def scannerHome = tool 'SonarQube Scanner'
                                sh "${scannerHome}/bin/sonar-scanner " +
                                        "-Dsonar.pullrequest.branch=${params.SOURCE_BRANCH - 'refs/heads/'} " +
                                        "-Dsonar.pullrequest.key=${params.PULL_REQUEST_ID} " +
                                        "-Dsonar.pullrequest.base=${params.TARGET_BRANCH - 'refs/heads/'} " +
                                        " "
                            }
                        }
                    }
                }
            }
            stage('Quality Gate') {
                steps {
                    script {
                        if (pipelineConfig.sonarqubeServerKey) {
                            timeout(time: 30, unit: 'MINUTES') {
                                def qg = waitForQualityGate()
                                print "Quality Gate reported status ${qg.status}"
                                if (qg.status != 'OK') {
                                    error "Quality Gate status not OK"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
