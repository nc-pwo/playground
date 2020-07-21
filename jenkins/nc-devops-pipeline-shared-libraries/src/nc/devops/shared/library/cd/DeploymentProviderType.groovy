package nc.devops.shared.library.cd


import nc.devops.shared.library.cd.templates.TemplateProcessingToolType

enum DeploymentProviderType {
    OPENSHIFT([TemplateProcessingToolType.OC] as Set),
    KUBERNETES([TemplateProcessingToolType.KUBECTL] as Set),
    KUBERNETES_WITH_HELM([TemplateProcessingToolType.HELM] as Set)

    private final Set<TemplateProcessingToolType> processingToolTypeSet


    DeploymentProviderType(Set processingToolTypeSet) {
        this.processingToolTypeSet = processingToolTypeSet
    }

    Set<TemplateProcessingToolType> getAvailableProcessingTools() {
        return processingToolTypeSet
    }

}