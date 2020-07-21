package nc.devops.shared.library.cd

import nc.devops.shared.library.cd.templates.TemplateProcessingTool
import nc.devops.shared.library.credentials.AbstractCredentials
import org.jenkinsci.plugins.workflow.cps.CpsScript

class DeploymentProviderParameters implements Serializable {
    String projectName, deliveryCluster, sourceBranch, projectNameSuffix, projectNameDelimiter, templatePath, gitCommitId, pullRequestId
    int defaultTimeout
    boolean bppr
    List<String> deploymentParameters
    List<? extends AbstractCredentials> credentialParameters
    List<Map> requiredComponents
    Map kubernetesCluster
    Closure logger, parallel, timeout, sleep
    CpsScript cpsScript
    TemplateProcessingTool templateProcessingTool
}