import org.junit.jupiter.api.Test

class LocalModeTest {

    @Test
    void localModeEnabledIsAlwaysFalseForMissingEnvVariable() {
        assert System.getenv('LOCAL_MODE_ENABLED') == null
        assert !new localMode().isEnabled()
    }

    @Test
    void branchNameForLocalModeIsANonEmptyString() {
        assert !new localMode().branchNameForLocalMode().isEmpty()
    }
}
