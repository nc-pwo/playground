package nc.devops.shared.library.cd.kubernetes

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
class KubernetesWithHelmProviderTest {

    @Mock
    private TemplateProcessingTool templateProcessingTool
    @Mock
    private KubernetesCluster cluster
    @Mock
    private KubeCtl kubeCtl
    @Mock
    private Helm helm
    @Mock
    private ProjectManagementProvider projectManagementProvider

    private KubernetesWithHelmProvider provider
    private DeploymentProviderParameters parameters

    private String projectName = "test-project"
    private String appName = "test-app"
    private HelmProcessedModel processedModel

    void setup() {
        parameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                templateProcessingTool: templateProcessingTool,
                parallel: { Map m ->
                    m.each { k, v -> println("Calling $k"); v() }
                },
                projectName: projectName,
                deliveryCluster: "test-cluster",
                defaultTimeout: 5,
                kubernetesCluster: [credentialsId: "test-id"],
                templatePath: "some/template/path",
                deploymentParameters: ["param1=value1", "param2=value2"]
        )

        processedModel = new HelmProcessedModel(applicationName: appName, templatePath: parameters.templatePath, additionalParameters: [param1: "value1", param2: "value2"])
        provider = new KubernetesWithHelmProvider(cluster, kubeCtl, helm, parameters)
    }

    @Test
    void testDeployApplication() {
        setup()
        when(cluster.withKubeConfig(anyString(), any())).then({ i -> i.getArgument(1).call() })
        when(projectManagementProvider.lockResource(eq(parameters))).thenReturn(parameters.projectName)
        when(templateProcessingTool.processTemplate(any())).thenReturn(processedModel)
        provider.useProject(projectManagementProvider)
        provider.deployApplication()
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(eq(parameters.projectName), any())
        verifyNoMoreInteractions(cluster)

        verify(helm).upgrade(appName, parameters.templatePath, [param1: "value1", param2: "value2"], 5)
        verifyNoMoreInteractions(helm)
        verifyZeroInteractions(kubeCtl)

    }

    @Test
    void testDeployApplicationWithRequiredComponents() {
        String requiredAppName = "required-app"

        parameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                templateProcessingTool: templateProcessingTool,
                parallel: { Map m ->
                    m.each { k, v -> println("Calling $k"); v() }
                },
                projectName: projectName,
                deliveryCluster: "test-cluster",
                defaultTimeout: 5,
                kubernetesCluster: [credentialsId: "test-id"],
                templatePath: "some/template/path",
                deploymentParameters: ["param1=value1", "param2=value2"],
                requiredComponents: [
                        [
                                templatePath        : "some/other/path",
                                isBppr              : true,
                                deploymentParameters: ["p1=v1", "p2=v2"]
                        ]
                ]
        )
        provider = new KubernetesWithHelmProvider(cluster, kubeCtl, helm, parameters)
        when(cluster.withKubeConfig(anyString(), any())).then({ i -> i.getArgument(1).call() })

        when(templateProcessingTool.processTemplate(any()))
                .thenReturn(new HelmProcessedModel(
                        applicationName: appName,
                        templatePath: parameters.templatePath,
                        additionalParameters: [param1: "value1", param2: "value2"]
                ))
                .thenReturn(new HelmProcessedModel(
                        applicationName: requiredAppName,
                        templatePath: parameters.requiredComponents[0].templatePath,
                        additionalParameters: [p1: "v1", p2: "v2"]
                ))
        when(projectManagementProvider.lockResource(eq(parameters))).thenReturn(parameters.projectName)

        provider.useProject(projectManagementProvider)
        provider.deployApplication()
        verify(cluster).setKubectlConfig(anyMap())
        verify(cluster).withKubeConfig(eq(parameters.projectName), any())
        verifyNoMoreInteractions(cluster)

        verify(helm).upgrade(requiredAppName, "some/other/path", [p1: "v1", p2: "v2"], 5)
        verify(helm).upgrade(appName, parameters.templatePath, [param1: "value1", param2: "value2"], 5)
        verifyNoMoreInteractions(helm)
        verifyZeroInteractions(kubeCtl)

    }

}
