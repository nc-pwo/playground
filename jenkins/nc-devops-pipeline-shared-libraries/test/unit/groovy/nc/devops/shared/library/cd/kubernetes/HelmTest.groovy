package nc.devops.shared.library.cd.kubernetes


import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@ExtendWith(MockitoExtension)
class HelmTest {

    @Mock
    PipelineMock cpsScript

    Helm helm

    private String appName = "some-name"

    @BeforeEach
    void init() {
        helm = new Helm(cpsScript)
    }

    private void verifySh(String expectedScript, boolean withOutput) {
        verify(cpsScript).sh(script: "$expectedScript", returnStdout: withOutput)
    }

    private void verifyWriteFile(String expectedFile, String expectedContent) {
        verify(cpsScript).writeFile(eq([file: expectedFile, text: expectedContent]))
    }

    @Test
    void upgradeWithParams() {
        helm.upgrade("appName", "some/template/path", [key1: "value1", key2: "value2"], 10)
        verifySh("helm upgrade --install appName --wait --timeout=10m some/template/path -f override_values.yaml", false)
        verifyWriteFile("override_values.yaml", "{key1: value1, key2: value2}\n")
    }

    @Test
    void upgradeWithEmptyParams() {
        helm.upgrade("appName", "some/template/path", [:], 10)
        verifySh("helm upgrade --install appName --wait --timeout=10m some/template/path ", false)
    }

    @Test
    void testTemplateProcessing() {
        String appName = "someName"
        def applicationParameters = [
                templatePath        : "some/template/path",
                deploymentParameters: [
                        "key1=value1",
                        "key2=value2",
                        "key3=value3"
                ],
                hasBpprImage        : true
        ]

        def expectedResult = new HelmProcessedModel(
                applicationName: appName,
                templatePath: "some/template/path",
                additionalParameters: [
                        key1: "value1",
                        key2: "value2",
                        key3: "value3"
                ]
        )
        when(cpsScript.readYaml(anyMap())).thenReturn([name: appName])
        def result = helm.processTemplate(new ApplicationParameters(applicationParameters))
        verifySh("helm lint some/template/path -f override_values.yaml", false)
        verifyWriteFile("override_values.yaml", "{key1: value1, key2: value2, key3: value3}\n")
        assert result.templatePath == expectedResult.templatePath
        assert result.applicationName == expectedResult.applicationName
        assert result.additionalParameters == expectedResult.additionalParameters
    }

    @Test
    void testTemplateProcessingWithEmptyParams() {
        String appName = "someName"
        def applicationParameters = [
                templatePath: "some/template/path",
                hasBpprImage: true
        ]

        def expectedResult = new HelmProcessedModel(
                applicationName: appName,
                templatePath: "some/template/path",
                additionalParameters: [:]
        )
        when(cpsScript.readYaml(anyMap())).thenReturn([name: appName])
        def result = helm.processTemplate(new ApplicationParameters(applicationParameters))
        verifySh("helm lint some/template/path ", false)
        assert result.templatePath == expectedResult.templatePath
        assert result.applicationName == expectedResult.applicationName
        assert result.additionalParameters == expectedResult.additionalParameters
    }

    @Test
    void testTemplateValidationWithoutAdditionalParameters() {
        Map appParams = [
                templatePath: "some/template/path"
        ]
        when(cpsScript.readYaml(anyMap())).thenReturn([name: appName])
        helm.processTemplate(new ApplicationParameters(appParams))
        verifySh("helm lint some/template/path ", false)
    }

}
