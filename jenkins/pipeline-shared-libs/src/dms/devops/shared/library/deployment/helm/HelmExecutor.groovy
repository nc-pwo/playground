package dms.devops.shared.library.deployment.helm

class HelmExecutor {

    Closure shellExecutor

    HelmExecutor(Closure shellExecutor) {
        this.shellExecutor = shellExecutor
    }

    void uninstall(DeploymentConfiguration config) {
        shellExecutor "helm uninstall $config.applicationName -n $config.namespace"
    }

    void upgrade(DeploymentConfiguration config, boolean includeAppValues) {
        shellExecutor "helm upgrade ${config.applicationName} ./${config.chartName}-${config.chartVersion}.tgz ${getEnvironmentArgs(config)} --install --wait ${getApplicationArgs(config, includeAppValues)}"
    }

    void template(DeploymentConfiguration config, boolean includeAppValues) {
        shellExecutor "helm template ${config.applicationName} ./${config.chartName}-${config.chartVersion}.tgz ${getEnvironmentArgs(config)} ${getApplicationArgs(config, includeAppValues)}"
    }

    void createPackage(DeploymentConfiguration config) {
        shellExecutor "helm package ${config.chartName} --app-version ${config.imageVersion}"
    }

    void pull(String repository, DeploymentConfiguration config) {
        shellExecutor "helm pull $repository/${config.chartName} --version ${config.chartVersion} --untar"
    }

    void updateRepository(String repo, String remote, String user, String pass) {
        shellExecutor "helm repo add $repo $remote --username $user --password $pass"
        shellExecutor "helm repo update"
    }

    void search(String repo, String chartName) {
        shellExecutor "helm search repo $repo/$chartName --devel --versions"
    }

    private String getEnvironmentArgs(DeploymentConfiguration config) {
        return "-n ${config.namespace} --values=${config.configurationType}/environment.yaml --set metadata.namespace=${config.namespace} --set metadata.timestampAnnotation=${config.timestamp} --set replicaCount=${config.replicas}"
    }

    private String getApplicationArgs(DeploymentConfiguration config, boolean includeAppValues) {
        String args = "--set metadata.name=${config.applicationName} --set metadata.version=${config.imageVersion} --set image.imageName=${config.imageName}"
        return includeAppValues ? "$args --values ${config.appValuesFile}" : args
    }

}
