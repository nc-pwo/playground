package nc.devops.shared.library.xray.runner

import groovy.json.JsonSlurper
import nc.devops.shared.library.xray.dto.InfoDto
import nc.devops.shared.library.xray.dto.XrayJsonDto
import nc.devops.shared.library.xray.dto.mapper.TestMapper
import nc.devops.shared.library.xray.util.DateUtil

import java.util.regex.Matcher

class SpockTestRunnerStrategy implements TestRunnerStrategy {
    private final XrayClient xrayClient
    private final JiraClient jiraClient
    private final TestMapper testMapper
    private final String testsResultsPath

    SpockTestRunnerStrategy(XrayClient xrayClient, JiraClient jiraClient, TestMapper testMapper, String testsResultsPath) {
        this.xrayClient = xrayClient
        this.jiraClient = jiraClient
        this.testMapper = testMapper
        this.testsResultsPath = testsResultsPath
    }

    @Override
    def importExecutionResults(Map vars) {
        def script = vars.script
        script.echo 'Xray: importing spock execution results...'
        String spockReportContent = script.readFile "$vars.workspace/$testsResultsPath"
        def responses = []

        def testcasesFromJiraProject = jiraClient.getTestcasesFromProject(vars)

        getXrayJsonOfSpockReport(spockReportContent).each {
            script.echo it.toString()
            it.tests.each {
                def storyKeys = parseStoryKeys(it.testKey)
                def acceptanceCriteria = parseCriteriaNames(it.testKey)
                def testFromJira = getCurrentTestFromJira(testcasesFromJiraProject, it)

                setTestKey(it, testFromJira, acceptanceCriteria, vars)
                linkTestToStories(it, storyKeys, vars)
            }

            // import test executions to Jira
            def response = xrayClient.postExecutionResults(vars, it)
            script.echo 'Xray: spock execution results import success'

            responses << response
        }
        responses
    }

    private def getCurrentTestFromJira(testcasesFromJiraProject, spockReport) {
        return testcasesFromJiraProject?.data?.issues?.findAll {
            jiraTest -> spockReport.comment.equals(jiraTest.fields.summary.toString())
        }
    }

    private void setTestKey(spockReport, testFromJira, acceptanceCriteria, vars) {
        if (testFromJira?.empty) {
            def createTestCaseResponse = jiraClient.createTestCase(vars, spockReport.comment, acceptanceCriteria)
            spockReport.testKey = createTestCaseResponse?.data?.key
        } else {
            spockReport.testKey = testFromJira?.get(0)?.key
        }
    }

    private void linkTestToStories(spockReport, storyKeys, vars) {
        storyKeys.each { userStoryKey ->
            def findStoryResponse = jiraClient.checkIssueExistance(vars, userStoryKey)
            if (findStoryResponse?.data?.issues?.size == null || findStoryResponse.data?.issues?.size == 0) {
                vars.script.echo "ERROR: User story with key " + userStoryKey + " does not exist."
            } else {
                jiraClient.linkTestToStory(vars, spockReport.testKey, userStoryKey)
            }
        }
    }

    private List<XrayJsonDto> getXrayJsonOfSpockReport(String spockReportContent) {
        def spockSlurped = parseSpockJsonReport(spockReportContent)
        spockSlurped.collect {
            new XrayJsonDto(
                    info: new InfoDto(
                            summary: "Execution of the $it.name test",
                            startDate: DateUtil.millisToOffsetDateTime(Long.valueOf(it.start)).toString()
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
        }.unique()
    }

    private Set<String> parseCriteriaNames(def issueKey) {
        issueKey.split(",").collect { x ->
            def (_, criteria) = x.tokenize(":")
            criteria?.trim()
        }.unique() - null
    }

}
