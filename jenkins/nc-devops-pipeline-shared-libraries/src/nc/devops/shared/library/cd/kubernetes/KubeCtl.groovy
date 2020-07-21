package nc.devops.shared.library.cd.kubernetes

import nc.devops.shared.library.cd.templates.ApplicationParameters
import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import org.jenkinsci.plugins.workflow.cps.CpsScript

class KubeCtl implements TemplateProcessingTool<Object>, Serializable {

    private final CpsScript cpsScript

    KubeCtl(CpsScript cpsScript) {
        this.cpsScript = cpsScript
    }

    String getIngressHost(String ingressName) {
        return kubeCtlWithStdOut("get ingress/$ingressName -o jsonpath={.spec.rules[0].host}")
    }

    List<String> getNamespaces() {
        kubeCtlWithStdOut("get namespaces --no-headers -o custom-columns=\":metadata.name\"").split("\n")
    }

    def deleteAllFromNamespace(String namespace, int timeoutInMinutes) {
        kubectl("delete --all all --namespace=$namespace --timeout=${timeoutInMinutes}m")
    }

    def createNamespace(String namespace) {
        kubectl("create namespace $namespace")
    }

    def deleteNamespace(String namespace, int timeoutInMinutes) {
        kubectl("delete namespace $namespace --timeout=${timeoutInMinutes}m")
    }

    private String kubectl(String parameters, boolean withStdOutput = false) {
        cpsScript.sh script: "kubectl $parameters", returnStdout: withStdOutput
    }

    private String kubeCtlWithStdOut(String params) {
        return kubectl(params, true)
    }

    @Override
    Object processTemplate(ApplicationParameters applicationParameters) {
        throw new UnsupportedOperationException()
    }

}
