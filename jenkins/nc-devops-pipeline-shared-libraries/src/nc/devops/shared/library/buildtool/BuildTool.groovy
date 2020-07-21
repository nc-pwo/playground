package nc.devops.shared.library.buildtool

import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.jenkinsci.plugins.workflow.cps.CpsScript

interface BuildTool {

    void clean()

    void unitTest()

    void buildArtifacts()

    void staticCodeAnalysis(StaticCodeAnalysisParams codeAnalysisParams)

    void publishArtifacts(List<String> additionalTags)

    void publishImages(String registryURL, String openshiftProjectName)

    @Deprecated
    void integrationTest(List<ProcessedTestProperty> properties)

    void integrationTestPublicApi(List<ProcessedTestProperty> properties)

    void integrationTestInternalApi(List<ProcessedTestProperty> properties)

    void componentTest(List<ProcessedTestProperty> properties)

    String getTestResultPath()

    String getIntegrationTestResultPath()

    String getComponentTestResultPath()

    String getPublicApiTestResultPath()

    String getInternalApiTestResultPath()

    void setCpsScript(CpsScript cpsScript)
}