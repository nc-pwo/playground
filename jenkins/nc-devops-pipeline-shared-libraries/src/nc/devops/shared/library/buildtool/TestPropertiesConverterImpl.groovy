package nc.devops.shared.library.buildtool

import nc.devops.shared.library.tests.model.ProcessedTestProperty

class TestPropertiesConverterImpl implements TestPropertiesConverter {

    String convert(List<ProcessedTestProperty> properties) {
        return properties
                .collect { "-${it.type}${it.name}=${it.value}" }
                .join(' ')
    }
}
