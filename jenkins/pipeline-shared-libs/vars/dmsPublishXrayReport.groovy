import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import groovyx.net.http.RESTClient
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import dms.devops.shared.library.xray.runner.dmsTestRunnerStrategyFactory
import nc.devops.shared.library.xray.runner.TestRunnerType

def call(Closure body) {
    // evaluate the body block, and collect configuration into the 'pipelineConfig' object
    def pipelineConfig = [:]
    def xray = [testIssueProjectKey             : 'dms',
                testExecutionProjectKey         : 'EXE',
                testAcceptanceCriteriaProjectKey: 'AC',
                xrayCredentialsId               : 'xray-credentials',
                jiraCredentialsId               : 'jira-credentials',
                baseXrayUrl                     : 'https://xray.cloud.xpand-it.com/',
                baseXrayUrlSuffix               : 'api/v1',
                baseJiraUrl                     : 'https://ufstidms.atlassian.net/',
                loggingEnabled                  : false,
    ]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()


    def AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'gradle')
    def unzipFolder
    def commitInfo

    pipeline {
        agent {
            label "${AGENT_LABEL}"
        }
        environment {
            JIRA_SITE = "${System.getenv('JIRA_SITE_NAME')}"
        }
        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: '14'),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: '150'))
            )
        }

        parameters {
            string(name: 'BUILD_JOB_NAME', description: 'Exp: build_test-conf')

            string(name: 'BUILD_NUMBER', description: 'Exp: 1')
        }

        stages {
            stage('Validate parameters') {
                steps {
                    script {
                        if (params.BUILD_JOB_NAME == null || params.BUILD_NUMBER == null)
                            error("Please provide BUILD_JOB_NAME and BUILD_NUMBER")
                        unzipFolder = "artifact/${params.BUILD_JOB_NAME}/${params.BUILD_NUMBER}/"
                        xray.repoName = params.BUILD_JOB_NAME.replace("release_", "").replace("build_", "")
                        currentBuild.displayName = "${params.BUILD_JOB_NAME}#${params.BUILD_NUMBER}"
                    }
                }
            }

            stage('Extract artifact') {
                steps {
                    script {
                        copyArtifacts(
                                projectName: params.BUILD_JOB_NAME,
                                selector: specific(params.BUILD_NUMBER),
                                target: unzipFolder
                        )
                        def commitInfoContent = readFile "${unzipFolder}commit/commitInfo.json"
                        try {
                            commitInfo = readJSON text: commitInfoContent
                            echo ("Running Xray report on commit: ${commitInfo.id}")
                            def shortCommitId = commitInfo.id.take(8)
                            currentBuild.displayName += ", commit#${shortCommitId}"
                        }catch(IllegalArgumentException exception){
                            echo("ERROR when reading commit file: ${exception}")
                        }
                    }
                }
            }

            stage('Xray test') {
                steps {
                    script {
                        def testResultsPath = "build/test-results/test"
                        def defaultTests = [
                                [resultsPath: testResultsPath,
                                 runnerType : 'SPOCK'],
                                [resultsPath: testResultsPath,
                                 runnerType : 'JUNIT']
                        ]
                        UsernamePasswordCredentialsImpl xrayCreds = CredentialsProvider.findCredentialById(xray?.xrayCredentialsId, StandardUsernamePasswordCredentials.class, this.$build(), [])
                        try {
                            xray += [script          : xray.script ?: this,
                                     xrayClientId    : xrayCreds.username,
                                     xrayClientSecret: xrayCreds.password,
                                     workspace       : "$env.WORKSPACE/$unzipFolder",
                                     tests           : defaultTests]
                        }
                        catch (NullPointerException e) {
                            throw new IllegalArgumentException('Credentials with given id not found!', e)
                        }
                        return xray.tests.collect {
                            Closure restClientFactory = { -> new RESTClient(xray.baseXrayUrl) }
                            new dmsTestRunnerStrategyFactory()
                                    .create(TestRunnerType.valueOf(it.runnerType),
                                            xray.restClientFactory ?: restClientFactory,
                                            it.resultsPath)
                                    .importExecutionResults(xray, false)
                        }.flatten()
                    }
                }
            }
        }
        post {
            failure {
                script {
                    if (commitInfo?.committerEmail) {
                        pipelineConfig.mailRecipients << commitInfo.committerEmail
                    }
                    def mailRecipientsList = pipelineConfig.mailRecipients
                    echo 'Sending emails to given recipients...'

                    emailext(to: mailRecipientsList ? mailRecipientsList.join(";") : '',
                            subject: '$DEFAULT_SUBJECT',
                            body: '$DEFAULT_CONTENT',
                            postsendScript: '$DEFAULT_POSTSEND_SCRIPT',
                            presendScript: '$DEFAULT_PRESEND_SCRIPT')
                }
            }
            cleanup {
                dmsMasterCleanup()
                cleanWs()
            }
        }
    }
}
