package nc.devops.shared.library.cd.kubernetes

class HelmProcessedModel implements Serializable {
    String applicationName
    String templatePath
    Map additionalParameters
}
