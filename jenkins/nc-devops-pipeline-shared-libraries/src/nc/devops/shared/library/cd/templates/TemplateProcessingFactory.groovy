package nc.devops.shared.library.cd.templates

import nc.devops.shared.library.cd.kubernetes.Helm
import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.openshift.Openshift
import org.jenkinsci.plugins.workflow.cps.CpsScript

class TemplateProcessingFactory {
    private final Map<TemplateProcessingToolType, Closure<TemplateProcessingTool>> templateProcessors = new HashMap<>()

    TemplateProcessingFactory() {
        templateProcessors.put(TemplateProcessingToolType.HELM, { CpsScript cpsScript ->
            new Helm(cpsScript)
        })

        templateProcessors.put(TemplateProcessingToolType.KUBECTL, { CpsScript cpsScript ->
            new KubeCtl(cpsScript)
        })

        templateProcessors.put(TemplateProcessingToolType.OC, { CpsScript cpsScript ->
            new Openshift(cpsScript.openshift, cpsScript)
        })
    }

    TemplateProcessingTool createTemplateProcessingTool(TemplateProcessingToolType templateProcessingToolType, CpsScript cpsScript) {
        return templateProcessors.get(templateProcessingToolType).call(cpsScript)
    }
}