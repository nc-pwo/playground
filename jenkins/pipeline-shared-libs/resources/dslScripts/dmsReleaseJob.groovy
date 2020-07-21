package dslScripts

pipelineJob(name) {
    description("Repo: ${gitUrl} \nPath: ${path} \nTask: ${task}")
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
                        credentials(creds)
                    }
                    branch(branch)
                    extensions {
                        submoduleOptions {
                            recursive(true)
                            parentCredentials(true)
                        }
                        pruneStaleBranch()
                    }
                }
            }
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }
    parameters {
        choiceParam('releaseType', ['MINOR', 'PATCH', 'MAJOR'], 'Release type')
    }
}