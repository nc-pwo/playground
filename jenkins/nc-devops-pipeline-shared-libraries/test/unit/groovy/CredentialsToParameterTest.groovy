import nc.devops.shared.library.credentials.CredentialsToDeploymentParametersAdapter
import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.Test

class CredentialsToParameterTest {

    private script = new PipelineMock()
    String credentialId = "my-credential"
    String usernameParam = "USERNAME"
    String passwordParam = "PASSWORD"
    String stringParam = "STRING"
    String fileParam = "FILE_PATH"

    @Test
    void usernamePasswordCredentialToParameterTest() {
        def usernamePasswordCredential = new credentials().usernamePassword(credentialId, usernameParam, passwordParam)
        def expected = ["USERNAME=username", "PASSWORD=password"]
        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)
        assert expected == adapter.convert([usernamePasswordCredential])
    }

    @Test
    void stringCredentialToParameterTest() {
        def stringCredential = new credentials().string(credentialId, stringParam)
        def expected = ["STRING=actual string"]
        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)
        assert expected == adapter.convert([stringCredential])
    }

    @Test
    void fileCredentialToParameterTest() {
        def fileCredential = new credentials().file(credentialId, fileParam)
        def expected = ["FILE_PATH=actual string"]
        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)
        assert expected == adapter.convert([fileCredential])
    }
}
