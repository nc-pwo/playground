package nc.devops.shared.library.buildtool

import nc.devops.shared.library.tests.model.ProcessedTestProperty

interface TestPropertiesConverter {
    String convert(List<ProcessedTestProperty> properties)
}