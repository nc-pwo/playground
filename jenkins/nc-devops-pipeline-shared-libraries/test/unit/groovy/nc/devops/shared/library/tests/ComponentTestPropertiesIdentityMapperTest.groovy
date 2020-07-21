package nc.devops.shared.library.tests

import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class ComponentTestPropertiesIdentityMapperTest {

    @Test
    void test() {
        List<TestProperty> properties = [
                [
                        propertyType   : 'D',
                        propertyName   : 'a',
                        propertyValue  : 'b',
                        appHostForValue: true
                ] as TestProperty
        ]
        def mappedProperties = new ComponentTestPropertiesIdentityMapper().map(properties)

        assert mappedProperties == [
                [
                        type : 'D',
                        name : 'a',
                        value: 'b'
                ] as ProcessedTestProperty
        ]
    }
}
