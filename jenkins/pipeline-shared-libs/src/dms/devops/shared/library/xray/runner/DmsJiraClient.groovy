package dms.devops.shared.library.xray.runner

import groovy.json.JsonOutput
import groovyx.net.http.HttpResponseException
import nc.devops.shared.library.xray.runner.JiraClient

class dmsJiraClient extends JiraClient {
    def getTestcaseFromProject(Map vars, String repoName, className, testName) {
        def script = vars.script
        testName = testName.replaceAll("\"","\\\\\"")
        def jqlQuery = "project = " + vars.testIssueProjectKey + " AND type IN (test)" + " AND labels = $repoName AND labels = $className AND labels = " + "\"$testName\""

        def response
        try {
            response = script.jiraJqlSearch jql: jqlQuery
        } catch (Exception exception) {
            script.echo "Fail to get test case with test name $testName from JIRA project."
            script.echo exception.toString()
        }

        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    def createTestCase(Map vars, def testName, def testCode, def testLabel) {
        def script = vars.script
        def testIssue = constructTestIssue(vars, testName, testCode, testLabel)
        log vars.loggingEnabled, script, "ProjectKey:\n$vars.jiraProjectKey\nRequest body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(testIssue))}"

        def response
        try {
            response = script.jiraNewIssue issue: testIssue, auditLog   : false
        } catch (Exception exception) {
            script.echo "Fail to create new test case $testName."
            script.echo exception.toString()
        }

        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    def updateTestCase(Map vars, def testName, def testCode, def testLabel, def testKey) {
        def script = vars.script
        def testIssue = constructTestIssue(vars, testName, testCode, testLabel)

        log vars.loggingEnabled, script, "ProjectKey:\n$vars.jiraProjectKey\nRequest body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(testIssue))}"
        def response
        try {
            response = script.jiraEditIssue  idOrKey: testKey, issue: testIssue
        } catch (Exception exception) {
            script.echo "Fail to update test case with ID $testKey."
            script.echo exception.toString()
        }

        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    @Override
    def checkIssueExistance(Map vars, def issueId){
        def script = vars.script
        def jqlQuery = "issueKey = " + issueId

        def response
        try {
            response = script.jiraJqlSearch jql: jqlQuery
        } catch (Exception exception) {
            script.echo "Fail to search for issue with ID $issueId from JIRA project."
            script.echo exception.toString()
        }
        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    def deleteIssueLink(Map vars, String issueLinkID) {
        def script = vars.script
        def response
        try {
            response = script.jiraDeleteIssueLink id: issueLinkID
        } catch (Exception exception) {
            script.echo "Fail to delete issue link with ID $issueLinkID from JIRA project."
            script.echo exception.toString()
        }
        response
    }

    private def constructTestIssue(Map vars, def testName, def testCode, def testLabels) {
        def testIssue = [fields: [project    : [key: "${vars.testIssueProjectKey}"],
                                  summary    : testName,
                                  description: testCode,
                                  labels     : testLabels,
                                  issuetype  : [name: 'Test']]]
        testIssue
    }
}