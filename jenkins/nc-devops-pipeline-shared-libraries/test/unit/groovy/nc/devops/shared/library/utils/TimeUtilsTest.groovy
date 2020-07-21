package nc.devops.shared.library.utils

import org.junit.jupiter.api.Test

import java.time.ZoneId

class TimeUtilsTest {

    @Test
    void millisToDateTimeStringTest() {
        long millis = 1580897770756
        assert TimeUtils.millisToDateTimeString(millis, ZoneId.of('Z')) == '05.02.2020 10:16'
    }
}
