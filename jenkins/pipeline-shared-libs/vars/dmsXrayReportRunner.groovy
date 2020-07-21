import dms.devops.shared.library.xray.runner.dmsPreReportRunner

def call(def stepConfig) {
    if (stepConfig) {
        def script = stepConfig.script ?: this
        String commitId = stepConfig.commitId
        String jobName = stepConfig.jobName
        String buildNumber = stepConfig.buildNumber
        dmsPreReportRunner.storeCommitInfo(script, commitId)
        dmsPreReportRunner.archiveArtifact(script)
        dmsPreReportRunner.runXrayReport(script, jobName, buildNumber)
    }
}