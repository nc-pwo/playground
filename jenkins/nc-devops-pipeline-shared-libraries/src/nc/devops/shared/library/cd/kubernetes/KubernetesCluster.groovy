package nc.devops.shared.library.cd.kubernetes

import com.cloudbees.groovy.cps.NonCPS

class KubernetesCluster implements Serializable {
    private final def cpsScript
    private Map kubectlConfig

    KubernetesCluster(def cpsScript) {
        this.cpsScript = cpsScript
        this.kubectlConfig = new LinkedHashMap()
    }

    def withKubeConfig(String projectName, Closure codeWithKubeConfig) {
        def config = kubectlConfig + [namespace: projectName]
        execWithKubeConfig(config, codeWithKubeConfig)
    }

    def withKubeConfig(Closure codeWithKubeConfig) {
        execWithKubeConfig(kubectlConfig, codeWithKubeConfig)
    }

    private def execWithKubeConfig(Map config, Closure codeWithKubeConfig) {
        cpsScript.withKubeConfig(config, codeWithKubeConfig)
    }

    @NonCPS
    void setKubectlConfig(Map config) {
        this.kubectlConfig = config
    }
}
