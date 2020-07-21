package dms.devops.shared.library.xray.runner

class dmsPreReportRunner {
    static void storeCommitInfo(def script, String commitId) {
        def committerEmail = script.sh(
                script: 'git --no-pager show -s --format=\'%ae\'',
                returnStdout: true
        ).trim()
        script.sh "mkdir -p commit"
        script.sh "printf '{ \"id\": \"${commitId}\", \"committerEmail\": \"${committerEmail}\"}' > commit/commitInfo.json"
    }

    static void archiveArtifact(def script) {
        def path = 'build/test-results/test/*.json'
        script.archiveArtifacts artifacts: "**/${path}, commit/commitInfo.json", allowEmptyArchive: true
        script.sh "rm -r commit"
    }

    static void runXrayReport(def script, String jobName, String buildNumber) {
        script.build job: "xray_publish_report_devops", propagate: false, parameters: [
                [$class: 'StringParameterValue', name: "BUILD_JOB_NAME", value: jobName],
                [$class: 'StringParameterValue', name: "BUILD_NUMBER", value: buildNumber],
        ], wait: false
    }
}