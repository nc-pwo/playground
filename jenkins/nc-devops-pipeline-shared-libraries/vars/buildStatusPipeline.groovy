import com.cloudbees.groovy.cps.NonCPS
import groovy.text.SimpleTemplateEngine
import hudson.model.Job
import jenkins.model.Jenkins
import nc.devops.shared.library.utils.TimeUtils
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import java.time.ZoneId

def call(body) {

    def config = [
            reportRecipients: []
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    final String DEFAULT_EMAIL_TEMPLATE_PATH = "template/multi-status.template"
    final String EMAIL_TITLE = "Scheduled build report"
    final String KUBERNETES_MODE_ENABLED = 'KUBERNETES_MODE_ENABLED'
    final String KUBERNETES_AGENT_LABEL = 'KUBERNETES_AGENT_LABEL'
    final String DEFAULT_AGENT_LABEL = 'master'

    List<String> jobs = []
    Map<String, RunWrapper> buildResultMap = [:].asSynchronized()
    String report, emailTemplatePath

    def agentLabel
    if (System.getenv(KUBERNETES_MODE_ENABLED) == 'true') {
        agentLabel = config.kubernetesPodTemplate ?: System.getenv(KUBERNETES_AGENT_LABEL)
    }
    agentLabel = config.agentLabel ?: (agentLabel ?: DEFAULT_AGENT_LABEL)

    String recipients = config.reportRecipients.join(",")

    pipeline {
        agent {
            label agentLabel
        }

        stages {
            stage('Validate parameters') {
                steps {
                    script {
                        if (config.regex == null) {
                            throw new IllegalArgumentException("Missing 'regex' parameter")
                        }
                        if (config.reportRecipients.isEmpty()) {
                            throw new IllegalArgumentException("Missing report recipient")
                        }
                        emailTemplatePath = config.emailTemplatePath ?: DEFAULT_EMAIL_TEMPLATE_PATH
                    }
                }
            }
            stage('Run Jobs') {
                steps {
                    script {
                        jobs = getJobs(config.regex)
                        parallelActions = jobs.collectEntries { name ->
                            [(name): {
                                def result = build job: name, wait: true, propagate: false
                                buildResultMap.put(name, result)
                            }
                            ]
                        }
                        parallel parallelActions
                    }
                }
            }
            stage('Generate report') {
                steps {
                    script {
                        Map bindings = [
                                "build"       : currentBuild,
                                "timestamp"   : TimeUtils.millisToDateTimeString(currentBuild.timeInMillis, ZoneId.systemDefault()),
                                "buildResults": buildResultMap.sort().values(),
                                "jobStability": getJobStabilityMap(jobs)
                        ]

                        String text = libraryResource emailTemplatePath
                        report = new SimpleTemplateEngine().createTemplate(text).make(bindings).toString()
                    }
                }
            }
        }
        post {
            success {
                script {
                    emailext mimeType: 'text/html', subject: "$EMAIL_TITLE ${TimeUtils.millisToDateTimeString(currentBuild.timeInMillis, ZoneId.systemDefault())}", body: report, to: recipients
                }
            }
            unsuccessful {
                emailext mimeType: 'text/html', subject: "$EMAIL_TITLE ${TimeUtils.millisToDateTimeString(currentBuild.timeInMillis, ZoneId.systemDefault())}", body: '''$SCRIPT''', to: recipients
            }
        }
    }
}

@NonCPS
List<String> getJobs(String regex) {
    Jenkins.get()
            .getAllItems(Job.class)
            .findAll { it.name.matches(regex) }
            .collect { it.name }
}

@NonCPS
Map<String, String> getJobStabilityMap(List<String> jobs) {
    Jenkins.get()
            .getAllItems(Job.class)
            .findAll {
                jobs.contains(it.name)
            }
            .collectEntries {
                job ->
                    [(job.getFullName()): "${job.getBuildHealth().getScore()}%"]
            }
}
