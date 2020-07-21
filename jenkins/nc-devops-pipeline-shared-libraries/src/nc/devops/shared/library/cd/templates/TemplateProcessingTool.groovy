package nc.devops.shared.library.cd.templates

interface TemplateProcessingTool<V> {
    V processTemplate(ApplicationParameters parameters)
}