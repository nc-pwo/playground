package nc.devops.shared.library.tests.model

class IntegrationTests implements Serializable {
    boolean legacyMode
    IntegrationTest component
    IntegrationTest publicApi
    IntegrationTest internalApi
}
