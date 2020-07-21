package nc.devops.shared.library.time

import org.junit.Test
import org.junit.jupiter.api.Assertions

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException

class TimedExecutionTest {

    @Test
    void TimedExecutionCanBeCreatedWithLongTimeout() {
        TimedExecution timedExecution = new TimedExecution({ sleepInMilis -> Thread.sleep(sleepInMilis)}, { String message -> println(message) }, 10, ChronoUnit.SECONDS, 1)
        assert true
    }

    @Test
    void executionFinishesCorrectly() {

        TimedExecution timedExecution = new TimedExecution({ sleepInMilis -> Thread.sleep(sleepInMilis)}, { String message -> println(message) },10, ChronoUnit.SECONDS, 1)
        int counter = 3
        timedExecution.doUntilTrue {
            counter--
            return counter <= 0
        }
        assert counter == 0
    }

    @Test
    void afterTimeoutExpiredExceptionIsThrown() {

        TimedExecution timedExecution = new TimedExecution({ sleepInMilis -> Thread.sleep(sleepInMilis)}, { String message -> println(message) }, 0, ChronoUnit.SECONDS, 0)
        Assertions.assertThrows(TimeoutException.class) {
            timedExecution.doUntilTrue {
                return false
            }
        }
    }

}
