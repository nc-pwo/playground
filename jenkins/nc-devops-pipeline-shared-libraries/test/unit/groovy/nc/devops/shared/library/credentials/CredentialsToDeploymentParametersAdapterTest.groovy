package nc.devops.shared.library.credentials

import nc.devops.shared.library.test.PipelineMock
import org.junit.jupiter.api.Test;

class CredentialsToDeploymentParametersAdapterTest {
    private script = new PipelineMock()

    @Test
    void testUsernamePasswordCredentialAdapter() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials()
        credentials.passwordParameter = "PASSWORD"
        credentials.usernameParameter = "USERNAME"
        credentials.credentialsId = "my-credential"

        def expected = ["USERNAME=username", "PASSWORD=password"]

        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)

        assert expected == adapter.convert([credentials])
    }

    @Test
    void testTokenCredentialAdapter() {
        StringCredentials credentials = new StringCredentials()
        credentials.stringParameter = "STRING"
        credentials.credentialsId = "my-credential"

        def expected = ["STRING=actual string"]

        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)
        assert expected == adapter.convert([credentials])
    }

    @Test
    void testFileCredentialAdapter() {
        FileCredentials credentials = new FileCredentials()
        credentials.fileParameter = "FILE_PATH"
        credentials.credentialsId = "my-credential"

        def expected = ["FILE_PATH=actual string"]

        CredentialsToDeploymentParametersAdapter adapter = new CredentialsToDeploymentParametersAdapter(script)
        assert expected == adapter.convert([credentials])
    }
}
