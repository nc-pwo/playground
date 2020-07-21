package dms.devops.shared.library.version

import org.junit.jupiter.api.Test

class VersionTest {

    @Test
    void testGetLatestVersion(){
        assert VersionUtil.getLatestVersion('2.5.4', '3.1.2') == '3.1.2'
        assert VersionUtil.getLatestVersion('1.11.0', '11.0.0') == '11.0.0'
        assert VersionUtil.getLatestVersion('2.1.1', '2.1.0') == '2.1.1'
        assert VersionUtil.getLatestVersion('2.0.1', '2.1.0') == '2.1.0'
    }
}
