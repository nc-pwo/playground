import groovy.transform.Field

@Field static final pollSCMStrategy = '* * * * *'

def call(body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
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
        agent "$AGENT_LABEL"
        triggers {
            pollSCM(cronExpression)
        }
        stages {
            stage('Checkout repository') {
                steps {
                    script {
                        sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                            checkout([$class: 'GitSCM', branches: [[name: "*/${pipelineConfig.branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "${pipelineConfig.credentials}", url: "${pipelineConfig.repository_url}"]]])
                        }
                    }
                }
            }
            stage('Parse YAML file') {
                steps {
                    script {
                        yamlFile = readYaml file: "./${pipelineConfig.path_yaml}"
                    }
                }
            }
            stage('Adding/updating template') {
                steps {
                    script {
                        insertTemplates(it)
                    }
                }
            }
        }
    }
}

private void insertTemplates(it) {
    openshift.withCluster() {
        openshift.withProject('openshift') {
            yamlFile.list.each {
                openshift.apply("-f", "./${it}/template.yml")
            }
        }
    }
}