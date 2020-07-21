package dslScripts

int PERIOD_IN_MINUTES = 2

multibranchPipelineJob(name) {
    description("Repo: ${repositoryName} \nPath: ${path} \nTask: ${task}")
    branchSources {
        github {
            id(UUID.randomUUID().toString())
            scanCredentialsId(creds)
            buildForkPRMerge(false)
            buildOriginBranch(false)
            buildOriginBranchWithPR(false)
            buildOriginPRMerge(true)
            repoOwner(repositoryOwner)
            repository(repositoryName)
        }
    }
    triggers {
        periodic(PERIOD_IN_MINUTES)
    }
    orphanedItemStrategy {
        discardOldItems {
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }
}