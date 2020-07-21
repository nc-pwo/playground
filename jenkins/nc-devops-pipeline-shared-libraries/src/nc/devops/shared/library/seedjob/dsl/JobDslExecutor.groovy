package nc.devops.shared.library.seedjob.dsl



class JobDslExecutor {
    private def script

    JobDslExecutor(def script) {
        this.script = Objects.requireNonNull(script, "Script must not be null")
    }

    def createJob(String scriptText, Map parametersMap = [:]) {
        script.jobDsl scriptText: scriptText,
                additionalParameters: parametersMap
    }

}
