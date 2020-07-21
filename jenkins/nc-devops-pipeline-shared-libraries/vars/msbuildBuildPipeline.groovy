import groovy.transform.Field

@Field static final pollSCMStrategy = '* * * * *'

def call(body) {
    def pipelineConfig = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    def versionsByName = [:] // versions by solution name
    def nowDateString = new Date().format("yyyyMMdd-HHmmss")
    // nowDateString is a string representing the current date+time

    final VERSIONS_FILE_PATH = 'versions.txt'

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'slave-windows')
    final String cronExpression = nc.devops.shared.library.utils.CronUtils.calculatePollSCMExpression(this)

    pipeline {
        agent { label "${AGENT_LABEL}" }
        triggers {
            pollSCM(cronExpression)
        }
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
            stage('Startup') {
                steps {
                    script {
                        versionsByName = fileUtils.readFile(VERSIONS_FILE_PATH)
                    }
                }
            }
            stage('Package Clean') {
                steps {
                    script {
                        for (def solutionDefinition : pipelineConfig.solutionDefinitions) {
                            String directory = fileUtils.getDirectory(solutionDefinition.solutionPath)
                            bat "RD /S /Q ${directory}\\packages || exit 0"
                        }
                    }
                }
            }
            stage('NuGet Restore') {
                steps {
                    script {
                        for (def solutionDefinition : pipelineConfig.solutionDefinitions) {
                            bat "nuget restore ${solutionDefinition.solutionPath}"
                        }
                    }
                }
            }
            stage('Build') {
                steps {
                    script {
                        for (def solutionDefinition : pipelineConfig.solutionDefinitions) {
                            if (env.SONAR_HOST_URL) {
                                withSonarQubeEnv(pipelineConfig.sonarqubeServerKey) {
                                    def scannerHome = tool 'SonarQube MSBuild Scanner'
                                    bat "${scannerHome}\\SonarScanner.MSBuild.exe begin " +
                                            "/s:${pwd()}\\SonarQube.Analysis.${solutionDefinition.solutionName}.xml " +
                                            "/n:${solutionDefinition.solutionName} " +
                                            "/v:${versionsByName[solutionDefinition.solutionName] ?: nowDateString} " +
                                            "/k:${solutionDefinition.solutionName} "

                                    bat "msbuild ${solutionDefinition.solutionPath}"

                                    bat "${scannerHome}\\SonarScanner.MSBuild.exe end "
                                }

                                if (solutionDefinition.isWaitForQualityGate) {
                                    timeout(time: 30, unit: 'MINUTES') {
                                        def qg = waitForQualityGate()
                                        print "Quality Gate reported status ${qg.status}"
                                        if (qg.status != 'OK') {
                                            error "Quality Gate status not OK"
                                        }
                                    }
                                }
                            } else {
                                bat "msbuild ${solutionDefinition.solutionPath}"
                            }

                            if (solutionDefinition.testFilesPattern != null) {
                                def testAssemblyFiles = findFiles(glob: "${solutionDefinition.testFilesPattern}")
                                if (testAssemblyFiles.size() > 0) {
                                    bat "vstest.console ${testAssemblyFiles.join(' ')} /Logger:trx;LogFileName=${solutionDefinition.solutionName}-${nowDateString}.trx"
                                }
                            }
                        }
                    }
                }
                post {
                    always {
                        step([$class: 'MSTestPublisher', testResultsFile: "**/*-${nowDateString}.trx", failOnError: true, keepLongStdio: true])
                    }
                }
            }
            stage('Create Artifact') {
                steps {
                    script {
                        for (def artifactDefinition : pipelineConfig.artifactDefinitions) {
                            bat "powershell -ExecutionPolicy Bypass -File .\\${artifactDefinition.scriptPath}"
                        }
                    }
                }
            }
            stage('Publish Artifact to Artifactory') {
                steps {
                    script {
                        for (def artifactDefinition : pipelineConfig.artifactDefinitions) {
                            def server = Artifactory.newServer url: "${artifactDefinition.repository}", username: "${env.ARTIFACTORY_USERNAME}", password: "${env.ARTIFACTORY_PASSWORD}"
                            def uploadSpec = """{
							  "files": [
								{
								  "pattern": "${artifactDefinition.artifactPath}",
								  "target": "${versionsByName[artifactDefinition.solutionName] ?: nowDateString}"
								}
							 ]
							}"""
                            server.upload(uploadSpec)
                        }
                    }
                }
            }
        }
    }
}
