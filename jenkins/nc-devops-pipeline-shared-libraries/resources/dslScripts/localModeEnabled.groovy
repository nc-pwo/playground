package dslScripts

pipelineJob(name) {
    description("Local Mode enabled \nRepo: /projects/${localPath} \nPath: ${path} \nTask: ${task}")
    definition {
        cpsScm {
            scm {
                filesystem {
                    path("/projects/${localPath}")
                    clearWorkspace(false)
                    copyHidden(false)
                    filterSettings {
                        includeFilter(false)
                        selectors {
                            filterSelector { wildcard("**/build/**") }
                            filterSelector { wildcard("**/target/**") }
                            filterSelector { wildcard("**/out/**") }
                            filterSelector { wildcard("**/node_modules/**") }
                        }
                    }
                }
            }
            scriptPath("${path}/${task}.jenkinsfile")
        }
    }
    triggers {
         if (cronFormat) {
             scm(cronFormat)
         }
    }
}