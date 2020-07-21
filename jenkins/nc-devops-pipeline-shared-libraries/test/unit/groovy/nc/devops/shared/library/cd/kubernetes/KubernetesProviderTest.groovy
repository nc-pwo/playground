package nc.devops.shared.library.cd.kubernetes

import nc.devops.shared.library.cd.DeploymentProviderParameters
import nc.devops.shared.library.cd.project.ProjectManagementProvider
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@ExtendWith(MockitoExtension)
class KubernetesProviderTest {

    @Mock
    private TemplateProcessingTool templateProcessingTool

    @Mock
    private KubernetesCluster cluster

    @Mock
    private KubeCtl kubeCtl

    @Mock
    private ProjectManagementProvider projectManagementProvider

    private KubernetesProvider kubernetesProvider
    private DeploymentProviderParameters parameters

    private static final String BASE_NAMESPACE = "test-namespace"

    @BeforeEach
    void setup() {
        parameters = new DeploymentProviderParameters(
                logger: { String message -> println(message) },
                sourceBranch: "refs/pull/60946/merge",
                projectName: BASE_NAMESPACE,
                projectNameSuffix: "bppr",
                projectNameDelimiter: "-",
                pullRequestId: "1337",
                deliveryCluster: "test-cluster",
                defaultTimeout: 5,
                kubernetesCluster: [credentialsId: "test-id"])

        kubernetesProvider = new KubernetesProvider(cluster, kubeCtl, parameters)
    }

    @Test
    void testIntegrationParametersProcessingWithoutCluster() {
        def processedProperties = kubernetesProvider.processIntegrationTestParamsWithoutCluster([
                [propertyType : 'P',
                 propertyName : 'appUrl',
                 propertyValue: 'localhost',
                ],
                [propertyType : 'D',
                 propertyName : 'prop',
                 propertyValue: 'value',
                ]
        ] as List<TestProperty>)

        assert processedProperties == [
                [type : 'P',
                 name : 'appUrl',
                 value: 'localhost'
                ] as ProcessedTestProperty,
                [type : 'D',
                 name : 'prop',
                 value: 'value',
                ] as ProcessedTestProperty
        ]

        verify(cluster).setKubectlConfig(any())
        verifyNoMoreInteractions(cluster)
        verifyZeroInteractions(kubeCtl)
    }

    @Test
    void testIntegrationParametersProcessingWithCluster() {
        when(cluster.withKubeConfig(anyString(), any())).then({ i -> i.getArgument(1).call() })
        when(kubeCtl.getIngressHost(eq("routeName"))).thenReturn("host_for_spec_and_routes/routeName")
        when(projectManagementProvider.lockResource(eq(parameters))).thenReturn(parameters.projectName)
        kubernetesProvider.useProject(projectManagementProvider)

        List<ProcessedTestProperty> processedProperties = kubernetesProvider.processIntegrationTestParams([
                [propertyType   : 'P',
                 propertyName   : 'appUrl',
                 propertyValue  : 'routeName',
                 appHostForValue: true],
                [propertyType : 'D',
                 propertyName : 'prop',
                 propertyValue: 'value',
                ]
        ] as List<TestProperty>)

        assert processedProperties == [
                [type : 'P',
                 name : 'appUrl',
                 value: 'http://host_for_spec_and_routes/routeName'
                ] as ProcessedTestProperty,
                [type : 'D',
                 name : 'prop',
                 value: 'value',
                ] as ProcessedTestProperty
        ]

        verify(cluster).setKubectlConfig(any())
        verify(cluster).withKubeConfig(eq(BASE_NAMESPACE), any())
        verifyNoMoreInteractions(cluster)

        verify(kubeCtl).getIngressHost("routeName")
        verifyNoMoreInteractions(kubeCtl)

    }

    @Test
    void testDeployApplication() {
        //TBD
        assert null == kubernetesProvider.deployApplication()
    }
}