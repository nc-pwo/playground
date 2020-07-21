package dslScripts

pipelineJob(name) {
    parameters {
        stringParam('COMMIT_ID')
        stringParam('SOURCE_BRANCH')
        stringParam('PR_SOURCE_BRANCH')
        stringParam('PR_TARGET_BRANCH')
    }

    description("Repo: ${gitUrl} \nPath: ${path} \nTask: ${task}")
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
                        credentials(creds)
                        name('origin')
                        refspec('+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin-pull/*')
                    }
                    branch('${COMMIT_ID}')
                    lightweight(false)
                    extensions {
                        submoduleOptions {
                            recursive(true)
                            parentCredentials(true)
                        }
                    }
                }
            }
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }
}