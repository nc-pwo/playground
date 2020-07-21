package nc.devops.shared.library


import nc.devops.shared.library.xray.runner.JiraClient
import org.junit.Before
import org.junit.Test

class JiraClientTest {

    private JiraClient jiraClient
    private static final def mockJiraResponse = [data:
                                                         [["id"    : "10094",
                                                           "key"   : "AC-1",
                                                           "fields": ["summary": "Successful test"]]]]

    @Before
    void setUp() {
        jiraClient = new JiraClient()
    }

    @Test
    void createTestCaseIsSuccessful() {
        def testName = "Successful test"
        def acceptanceCriteriaList = ['Acceptance 1', 'Second']
        def responses = jiraClient.createTestCase([loggingEnabled: true, script: this], testName, acceptanceCriteriaList)

        assert responses.size() > 0
    }

    private echo(String message) {
        println message
    }

    private jiraNewIssue(def anyMap) {
        return mockJiraResponse
    }
}
