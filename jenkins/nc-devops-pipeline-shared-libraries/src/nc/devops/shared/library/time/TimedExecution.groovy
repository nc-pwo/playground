package nc.devops.shared.library.time


import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeoutException

class TimedExecution implements Serializable {
    private final long minLoopDurationInSeconds
    private LocalDateTime expireDate
    private final long timeoutDuration
    private final TemporalUnit unit
    private final Closure sleepClosure
    private final Closure logger

    TimedExecution(Closure sleepClosure, Closure logger, long timeoutDuration, TemporalUnit unit, long minLoopDurationInSeconds = 5) {
        this.sleepClosure = sleepClosure
        this.logger = logger
        this.timeoutDuration = timeoutDuration
        this.unit = unit
        this.minLoopDurationInSeconds = minLoopDurationInSeconds
    }

    void doUntilTrue(Closure<Boolean> body) throws TimeoutException {
        boolean result = false
        LocalDateTime expireDate
        expireDate = timeoutDuration > 0 ? LocalDateTime.now().plus(timeoutDuration, unit) : LocalDateTime.now().plus(100, ChronoUnit.MILLIS)

        logger.call("Timeout set to expire at ${expireDate.toString()}")
        logger.call("Executing...")

        while (!LocalDateTime.now().isAfter(expireDate) && !result) {
            LocalDateTime start = LocalDateTime.now()
            result = body.call()
            LocalDateTime end = LocalDateTime.now()
            long duration = start.until(end, ChronoUnit.SECONDS)
            long sleepTime = (minLoopDurationInSeconds - duration)
            if (sleepTime > 0)
                sleepClosure.call(sleepTime)
        }
        if (result) {
            logger.call("Execution ended ${LocalDateTime.now().until(expireDate, ChronoUnit.SECONDS)} seconds before timeout expired")
        } else {
            throw new TimeoutException("Execution time of ${timeoutDuration} ${unit.toString()} expired")

        }
    }

    LocalDateTime getExpireDate() {
        return expireDate
    }

}