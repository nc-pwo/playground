package nc.devops.shared.library.tests.model

import org.apache.commons.lang3.StringUtils

class IntegrationTest implements Serializable {
    String agentLabel
    String agentInheritFrom
    String agentServiceAccount
    boolean runTest
    List<TestProperty> testProperties
    SourceRepository externalRepository

    String getAgentLabelOrDefault(String defaultLabel) {
        return StringUtils.isBlank(agentLabel) ? defaultLabel : agentLabel
    }
}
