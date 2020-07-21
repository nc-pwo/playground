package dslScripts

int NUMBER_TO_KEEP = 20

multibranchPipelineJob(name) {
    branchSources {
        git {
            id(UUID.randomUUID().toString())
            remote(gitUrl)
            credentialsId(creds)
            includes("${branches} feature/*")
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(NUMBER_TO_KEEP)
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }

}