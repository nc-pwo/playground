package nc.devops.shared.library.utils

import com.cloudbees.groovy.cps.NonCPS

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimeUtils {
    @NonCPS
    static String millisToDateTimeString(long epochMillis, ZoneId zoneId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern('dd.MM.yyyy HH:mm')
        Instant.ofEpochMilli(epochMillis)
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
        formatter.format(zdt)
    }
}
