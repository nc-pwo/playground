def call(body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
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
        agent {
            label "${AGENT_LABEL}"
        }
        parameters{
            string(name: 'project_name', description: 'Name of the project to create', defaultValue: '')
        }
        environment {
            TFS_CREDENTIALS = credentials("nccgv001-https")
            TOKEN = credentials("openshift-credentials")
            PROJECT_NAME = "${params.project_name}"
        }
        stages {
            stage('Validate parameters') {
                steps {
                    script {
                        validateParameters()
                        printParameters()
                    }
                }
            }
            stage('Checkout repository') {
                steps {
                    script {
                        sshagent(credentials: [SSH_AGENT_CREDENTIALS_ID]) {
                            sh 'git checkout origin/master'
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
            stage('Delete existing project') {
                steps {
                    script {
                        openshift.withCredentials(TOKEN) {
                            deleteExistingProject()
                        }
                    }
                }
            }
            stage('Create Project') {
                steps {
                    script {
                        openshift.withCredentials(TOKEN) {
                            openshift.newProject(env.PROJECT_NAME)
                            openshift.raw('adm', 'policy', 'add-scc-to-user', 'anyuid', "system:serviceaccount:${env.PROJECT_NAME}:default")
                        }
                    }
                }
            }
            stage('Create Applications') {
                steps {
                    script {
                        openshift.withProject(env.PROJECT_NAME) {
                            openshift.raw('adm', 'policy', 'add-role-to-user', 'view', "${pipelineConfig.user}")
                            createApplications()
                            println "Environment '${env.PROJECT_NAME}' successfully created"
                        }
                    }
                }
            }
        }
    }
}

void printParameters() {
    println("\n\n>> DEPLOYMENT INFORMATION BEGIN <<\n " +
            params.collect { k, v -> "${k} = ${v}" }.join('\n') +
            "project:$env.PROJECT_NAME" +
            "\n>> DEPLOYMENT INFORMATION END <<\n")
}

private void createApplications() {
    yamlFile.list.each {
        def model = buildModel(it)
        openshift.create(model)
    }
}

private void deleteExistingProject() {
    def exists = openshift.selector("project", env.PROJECT_NAME).count()
    if (exists == 0) {
        return
    }
    openshift.raw('delete', 'project', env.PROJECT_NAME)
    timeout(5) { // Throw exception after 5 minutes
        openshift.selector("project", env.PROJECT_NAME).watch {
            return (it.count() == 0)
        }
    }
}

private void validateParameters() {
    for (entry in params) {
        if (entry.value.size() == 0) {
            currentBuild.result = 'ABORTED'
            error('Parameters are not valid.')
        }
    }
}

private def buildModel(it) {
    boolean isPropFileExisted = fileExists("./${it}/template.properties")
    if (it.contains("config-server")) {
        return openshift.process("-f", "./${it}/template.yml",
                isPropFileExisted ? "--param-file" : "", isPropFileExisted ? "./${it}/template.properties" : "",
                "-p", "DEFAULT_REPO_USERNAME=$env.TFS_CREDENTIALS_USR",
                "-p", "DEFAULT_REPO_PASSWORD=$env.TFS_CREDENTIALS_PSW")
    }

    return openshift.process("-f", "./${it}/template.yml",
            isPropFileExisted ? "--param-file" : "", isPropFileExisted ? "./${it}/template.properties" : "")
}