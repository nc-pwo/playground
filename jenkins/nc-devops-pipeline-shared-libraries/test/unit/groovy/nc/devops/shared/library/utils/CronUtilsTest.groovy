package nc.devops.shared.library.utils

import hudson.model.Build
import nc.devops.shared.library.test.PipelineMock
import nc.devops.shared.library.utils.CronUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension.class)
class CronUtilsTest {

    @Mock
    private Build rawBuild
    private static PipelineMock pipelineMock

    @BeforeEach
    void setup() {
        pipelineMock = new PipelineMock(rawBuild)
    }

    @Test
    void testCronWithLocalModeEnabledAndEmptyConfig() {
        assert CronUtils.calculateCronExpression(pipelineMock, []) == ''
    }

    @Test
    void testCronWithLocalModeEnabledAndMockedConfig() {
        assert CronUtils.calculateCronExpression(pipelineMock, [cronExpression: '0 17 * * *']) == '0 17 * * *'
    }
}
