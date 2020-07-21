package nc.devops.shared.library.buildtool

import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.jenkinsci.plugins.workflow.cps.CpsScript

class CustomBuildToolMock implements BuildTool {
    @Override
    void clean() {

    }

    @Override
    void unitTest() {

    }

    @Override
    void buildArtifacts() {

    }

    @Override
    void staticCodeAnalysis(StaticCodeAnalysisParams codeAnalysisParams) {

    }

    @Override
    void publishArtifacts(List<String> additionalTagsList) {

    }

    @Override
    void publishImages(String registryURL, String openshiftProjectName) {

    }

    @Override
    void integrationTest(List<ProcessedTestProperty> properties) {

    }

    @Override
    void integrationTestPublicApi(List<ProcessedTestProperty> properties) {

    }

    @Override
    void integrationTestInternalApi(List<ProcessedTestProperty> properties) {

    }

    @Override
    void componentTest(List<ProcessedTestProperty> properties) {

    }

    @Override
    String getTestResultPath() {
        return null
    }

    @Override
    String getIntegrationTestResultPath() {
        return null
    }

    @Override
    String getComponentTestResultPath() {
        return null
    }

    @Override
    String getPublicApiTestResultPath() {
        return null
    }

    @Override
    String getInternalApiTestResultPath() {
        return null
    }

    @Override
    void setCpsScript(CpsScript cpsScript) {

    }
}
