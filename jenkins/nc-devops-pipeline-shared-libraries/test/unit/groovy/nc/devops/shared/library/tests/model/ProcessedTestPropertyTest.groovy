package nc.devops.shared.library.tests.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class ProcessedTestPropertyTest {

    @Test
    void twoObjectsWithSameValuesAreEqual() {
        ProcessedTestProperty first = new ProcessedTestProperty(type: 'D', name: 'a', value: 'b')
        ProcessedTestProperty second = new ProcessedTestProperty(type: 'D', name: 'a', value: 'b')

        assert first.hashCode() == second.hashCode()
        assert first.equals(second)

    }

    @Test
    void twoObjectsWithDifferentValuesAreNotEqual() {
        ProcessedTestProperty first = new ProcessedTestProperty(type: 'D', name: 'a', value: 'b')
        ProcessedTestProperty second = new ProcessedTestProperty(type: 'D', name: 'c', value: 'd')

        assert !first.equals(second)
    }
}
