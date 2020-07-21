package dms.devops.shared.library.xray.runner

import dms.devops.shared.library.xray.dto.dmsXrayJsonDto
import groovy.json.JsonSlurper
import dms.devops.shared.library.xray.dto.dmsInfoDto
import dms.devops.shared.library.xray.dto.mapper.dmsTestMapper

import java.util.regex.Matcher

class dmsSpockTestRunnerStrategy implements dmsTestRunnerStrategy {
    protected final dmsXrayClient xrayClient
    protected final dmsJiraClient jiraClient
    protected final dmsTestMapper testMapper
    protected final String testsResultsPath

    dmsSpockTestRunnerStrategy(dmsXrayClient xrayClient, dmsJiraClient jiraClient, dmsTestMapper testMapper, String testsResultsPath) {
        this.xrayClient = xrayClient
        this.jiraClient = jiraClient
        this.testMapper = testMapper
        this.testsResultsPath = testsResultsPath
    }

    @Override
    def importExecutionResults(Map vars, boolean isBPPR) {
        def script = vars.script
        def filenames = listReportFiles(script, "$vars.workspace", testsResultsPath)
        if (filenames != null && filenames.length > 0) {
            script.echo "Xray: importing spock execution results from:\n$filenames"
            def responses = []
            filenames.each { filename ->
                String spockReportContent = script.readFile "$filename"
                def xrayJsonDtos = getXrayJsonOfSpockReport(vars.testExecutionProjectKey, spockReportContent)
                if (xrayJsonDtos.tests.size() != 0) {
                    createTestsAndLinkTestsToStories(vars, xrayJsonDtos, responses, isBPPR)
                }
                else {
                    script.echo "No test implementation found. Skip publishing results of $xrayJsonDtos.info.description test."
                }
            }
            responses
        }
        else {
            script.echo "No report files found. Skip publishing report result to JIRA."
        }
    }

    private void processTestIssues(def test, def vars, String className) {
        def storyKeys = parseStoryKeys(test.testKey)

        def indexOfSplitter = test.comment.indexOf("\n;\n")
        def testName = test.comment.substring(0,indexOfSplitter).replaceAll(" ", "-")
        def testCode = test.comment.substring(indexOfSplitter+3)

        test.comment = testName

        def description = "Test Code:\n" + testCode
        def labels = ["$vars.repoName","$className", "$testName"]
        def testFromJira = jiraClient.getTestcaseFromProject(vars, vars.repoName, className, testName)

        if (testFromJira) {
            setTestKey(test, testFromJira, description, labels, vars)

            Set newIssueIDs = storyKeys.toSet()
            def oldIssueLinks
            if (testFromJira?.data?.issues?.size() > 0) {
                oldIssueLinks = testFromJira?.data?.issues?.get(0)?.fields?.issuelinks
                newIssueIDs = issueLinksHandler(vars, newIssueIDs, oldIssueLinks)
                if (newIssueIDs.size() > 0) {
                    linkTestToStories(test, newIssueIDs, vars)
                }
            }
            else {
                linkTestToStories(test, newIssueIDs, vars)
            }
        }
    }

    private void createTestsAndLinkTestsToStories(vars, def xrayJsonDtos, def responses, def isBPPR){
        def script = vars.script
        xrayJsonDtos.each {
            def className = it.info.description
            it.tests.each {
                processTestIssues(it, vars, className)
            }
            // import test executions to Jira
            def response
            if (!isBPPR) {
                response = xrayClient.postExecutionResults(vars, it)
            }
            script.echo 'Xray: spock execution results import success'
            responses << response
        }
        responses
    }

    private void setTestKey(spockReport, testFromJira, String testCode, testLabels, vars) {
        def totalIssue = testFromJira.data?.total
        switch (totalIssue) {
            case 0:
                def createTestCaseResponse = jiraClient.createTestCase(vars, spockReport.comment, testCode, testLabels)
                spockReport.testKey = createTestCaseResponse?.data?.key
                break
            case 1:
                spockReport.testKey = testFromJira?.data?.issues?.get(0).key
                jiraClient.updateTestCase(vars, spockReport.comment, testCode, testLabels, spockReport.testKey)
                break
            default:
                throw new IllegalArgumentException('More than one test case was found. Pipeline failed.')
        }
    }

    private void linkTestToStories(spockReport, issueIDsSet, vars) {
        issueIDsSet.each { issueId ->
            def findStoryResponse = jiraClient.checkIssueExistance(vars, issueId)
            if (findStoryResponse?.data?.issues?.size == null || findStoryResponse.data?.issues?.size == 0) {
                vars.script.echo "ERROR: User story with key " + issueId + " does not exist."
            } else {
                jiraClient.linkTestToStory(vars, spockReport.testKey, issueId)
            }
        }
    }

    private List<dmsXrayJsonDto> getXrayJsonOfSpockReport(String executionProjectKey, spockReportContent) {
        def spockSlurped = parseSpockJsonReport(spockReportContent)
        spockSlurped.collect {
            new dmsXrayJsonDto(
                    info: new dmsInfoDto(
                            project: "$executionProjectKey",
                            summary: "Execution of the $it.name test",
                            description: it.name
                    ),
                    tests: it.features.collect { feature -> testMapper.map(feature) }
            )
        }
    }

    private static Object parseSpockJsonReport(String spockReportContent) {
        Matcher regexMatcher = (spockReportContent =~ ~/(?s)loadLogFile\((.*)\)/)
        new JsonSlurper().parseText(regexMatcher[0][1])
    }

    private def parseStoryKeys(def issueKey) {
        issueKey.split(",").collect { x ->
            def (story, _) = x.tokenize(":")
            story?.trim()
        }.unique()-null
    }

    private String[] listReportFiles(def script, String workspacePath, String testsResultsPath) {
        try {
            String fileList = script.sh(script: "ls | find $workspacePath -regex .*/$testsResultsPath/[^/]*.json", returnStdout: true)
            if (fileList != "") {
                return fileList.split("\n")
            }
        }
        catch (Exception e) {
            script.echo e.toString()
        }
        return null
    }

    private Set issueLinksHandler(Map vars, def newIssueIDs, def oldIssueLinks) {
        oldIssueLinks.each { issueLink ->
            if (issueLink.type.name == "Test") {
                newIssueIDs.contains(issueLink.outwardIssue.key) ? newIssueIDs.remove(issueLink.outwardIssue.key) : jiraClient.deleteIssueLink(vars, issueLink.id)
            }
        }
        newIssueIDs
    }
}
