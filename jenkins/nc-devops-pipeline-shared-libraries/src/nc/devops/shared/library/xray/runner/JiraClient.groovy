package nc.devops.shared.library.xray.runner

import groovy.json.JsonOutput
import groovyx.net.http.HttpResponseException

class JiraClient {

    void log(boolean loggingEnabled, def script, String message) {
        if (loggingEnabled)
            script.echo message
    }

    def getTestcasesFromProject(Map vars) {
        def script = vars.script
        def jqlQuery = "project = " + vars.jiraProjectKey + " AND type IN (test)"

        def response = script.jiraJqlSearch jql: jqlQuery

        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    def createTestCase(Map vars, def testName, def acceptanceCriteriaList) {
        def script = vars.script
        def testIssue = [fields: [project    : [key: "${vars.jiraProjectKey}"],
                                  summary    : testName,
                                  description: acceptanceCriteriaToComment(acceptanceCriteriaList),
                                  issuetype  : [name: 'Test']]]
        log vars.loggingEnabled, script, "ProjectKey:\n$vars.jiraProjectKey\nRequest body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(testIssue))}"

        def response = script.jiraNewIssue issue: testIssue

        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    def linkTestToStory(Map vars, def testId, def storyId) {
        def script = vars.script

        def response = script.jiraLinkIssues type: 'Test', inwardKey: testId, outwardKey: storyId
        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }


    def checkIssueExistance(Map vars, def issueId){
        def script = vars.script
        def jqlQuery = "project = '" + vars.jiraProjectKey + "' AND type IN (story) AND issueKey = " + issueId

        def response
        try {
            response = script.jiraJqlSearch jql: jqlQuery
        } catch (HttpResponseException excetpion1) {
            response = excetpion1.response
        }
        log vars.loggingEnabled, script, "Response body:\n${JsonOutput.prettyPrint(JsonOutput.toJson(response?.data))}"
        response
    }

    protected def acceptanceCriteriaToComment(def acceptanceCriteriaList) {
        acceptanceCriteriaList.collect {x -> "Acceptance criteria '${x}'"}.join(" and\n")
    }
}