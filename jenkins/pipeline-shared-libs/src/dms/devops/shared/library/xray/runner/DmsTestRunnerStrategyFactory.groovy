package dms.devops.shared.library.xray.runner

import dms.devops.shared.library.xray.dto.mapper.dmsTestMapper
import nc.devops.shared.library.xray.runner.TestRunnerType

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.XML

class dmsTestRunnerStrategyFactory {
    dmsTestRunnerStrategy create(TestRunnerType type, Closure restClientFactory, String testsResultsPath) {
        switch (type) {
            case TestRunnerType.SPOCK:
                return new dmsSpockTestRunnerStrategy(
                        new dmsXrayClient(restClientFactory, '/import/execution', JSON),
                        new dmsJiraClient(),
                        new dmsTestMapper(),
                        testsResultsPath)
                break
            case TestRunnerType.JUNIT:
                return new dmsJUnitTestRunnerStrategy(new dmsXrayClient(restClientFactory, '/import/execution/junit', XML), testsResultsPath)
                break
            default:
                throw new RuntimeException("Wrong test runner type: $type")
        }
    }
}