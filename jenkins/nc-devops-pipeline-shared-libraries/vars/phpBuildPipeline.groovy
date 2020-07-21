import groovy.transform.Field

@Field static final pollSCMStrategy = '* * * * *'

def call(body) {
    def pipelineConfig = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()
    final String cronExpression = nc.devops.shared.library.utils.CronUtils.calculatePollSCMExpression(this)
    final String SSH_AGENT_CREDENTIALS_ID = pipelineConfig.credentialsId ?: "nc-devops-credentials"

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')

    pipeline {
        agent {
            label "$AGENT_LABEL"
        }
        triggers {
            pollSCM(cronExpression)
        }
        tools { nodejs 'Default' }
        stages {
            stage('Checkout') {
                steps {
                    script {
                        sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                            checkout scm
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
                                sh "${scannerHome}/bin/sonar-scanner "
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