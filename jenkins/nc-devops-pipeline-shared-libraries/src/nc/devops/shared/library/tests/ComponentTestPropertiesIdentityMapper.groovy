package nc.devops.shared.library.tests

import nc.devops.shared.library.tests.model.ProcessedTestProperty
import nc.devops.shared.library.tests.model.TestProperty

class ComponentTestPropertiesIdentityMapper {
    List<ProcessedTestProperty> map(List<TestProperty> properties) {
        properties.collect { property ->
            new ProcessedTestProperty(
                    type: property.propertyType,
                    name: property.propertyName,
                    value: property.propertyValue
            )
        }
    }
}
