package nc.devops.shared.library.xray.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class DateUtil {
    static OffsetDateTime millisToOffsetDateTime(long millis) {
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()) // TODO find better algorithm
    }
}
