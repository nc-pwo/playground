package dslScripts

int DAYS_TO_KEEP = 30
int NUM_TO_KEEP = -1    // necessary to specify - "-1" will set it as empty/default
String REF_SPEC_VALUE = "+refs/heads/*:refs/remotes/@{remote}/*"

multibranchPipelineJob(name) {
    description("Repo: ${repositoryName} \nPath: ${path} \nTask: ${task}")
    branchSources {
        branchSource {
            source {
                bitbucket {
                    id(UUID.randomUUID().toString())
                    serverUrl("https://bitbucket.org")
                    credentialsId(bitbucketSSHCredentials)
                    repoOwner(repositoryOwner)
                    repository(repositoryName)
                    traits {
                        refSpecsSCMSourceTrait {
                            templates {
                                refSpecTemplate {
                                    value("${REF_SPEC_VALUE}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    configure {
        def traits = it / sources / data / 'jenkins.branch.BranchSource' / source / traits
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.BranchDiscoveryTrait' {
            // 1 - Exclude branches that are also filed as PRs
            // 2 - Only branches that are also filled as PRs
            // 3 - detect all branches
            strategyId(2)
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.OriginPullRequestDiscoveryTrait' {
            strategyId(1)   // Merging the pull request with the current target branch revision
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.SSHCheckoutTrait' {
            credentialsId(creds)
        }
        traits << 'com.cloudbees.jenkins.plugins.bitbucket.WebhookRegistrationTrait' {
            mode(ITEM)
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }
    triggers {
        teamPRPushTrigger {}
    }
    orphanedItemStrategy {
        defaultOrphanedItemStrategy {
            daysToKeepStr("${DAYS_TO_KEEP}")
            numToKeepStr("${NUM_TO_KEEP}")
            pruneDeadBranches(true)
        }
    }
}