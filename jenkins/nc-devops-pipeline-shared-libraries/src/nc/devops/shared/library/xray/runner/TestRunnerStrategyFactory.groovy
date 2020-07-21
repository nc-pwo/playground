package nc.devops.shared.library.xray.runner

import nc.devops.shared.library.xray.dto.mapper.TestMapper

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.XML

class TestRunnerStrategyFactory {
    TestRunnerStrategy create(TestRunnerType type, Closure restClientFactory, String testsResultsPath) {
        switch (type) {
            case TestRunnerType.SPOCK:
                return new SpockTestRunnerStrategy(
                        new XrayClient(restClientFactory, '/import/execution', JSON),
                        new JiraClient(),
                        new TestMapper(),
                        testsResultsPath)
                break
            case TestRunnerType.JUNIT:
                return new JUnitTestRunnerStrategy(new XrayClient(restClientFactory, '/import/execution/junit', XML), testsResultsPath)
                break
            default:
                throw new RuntimeException("Wrong test runner type: $type")
        }
    }
}

