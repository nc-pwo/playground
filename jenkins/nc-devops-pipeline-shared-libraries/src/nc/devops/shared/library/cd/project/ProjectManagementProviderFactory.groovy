package nc.devops.shared.library.cd.project

import nc.devops.shared.library.cd.kubernetes.KubeCtl
import nc.devops.shared.library.cd.kubernetes.KubernetesCluster
import nc.devops.shared.library.cd.openshift.Openshift
import nc.devops.shared.library.cd.project.bpprtype.kubernetes.KubernetesBpprWithLockProjectManagementProvider
import nc.devops.shared.library.cd.project.bpprtype.kubernetes.KubernetesBpprWithoutLockProjectManagementProvider
import nc.devops.shared.library.cd.project.bpprtype.openshift.OpenshiftBpprWithLockProjectManagementProvider
import nc.devops.shared.library.cd.project.bpprtype.openshift.OpenshiftBpprWithoutLockProjectManagementProvider
import nc.devops.shared.library.cd.project.buildtype.kubernetes.KubernetesBuildWithoutLockProjectManagementProvider
import nc.devops.shared.library.cd.project.buildtype.openshift.OpenshiftBuildWithoutLockProjectManagementProvider
import org.jenkinsci.plugins.workflow.cps.CpsScript

class ProjectManagementProviderFactory {
    private final Map<ProjectManagementProviderType, Closure<ProjectManagementProvider>> projectManagementProviders = new HashMap<>()

    ProjectManagementProviderFactory() {
        projectManagementProviders.put(ProjectManagementProviderType.OPENSHIFT_BPPR_WITH_JENKINS_LOCK, { pipelineConfig, cpsScript ->
            new OpenshiftBpprWithLockProjectManagementProvider(pipelineConfig, new Openshift(cpsScript.openshift, cpsScript))
        })
        projectManagementProviders.put(ProjectManagementProviderType.OPENSHIFT_BPPR_WITHOUT_JENKINS_LOCK, { pipelineConfig, cpsScript ->
            new OpenshiftBpprWithoutLockProjectManagementProvider(pipelineConfig, new Openshift(cpsScript.openshift, cpsScript))
        })
        projectManagementProviders.put(ProjectManagementProviderType.OPENSHIFT_BUILD_WITHOUT_JENKINS_LOCK, { pipelineConfig, cpsScript ->
            new OpenshiftBuildWithoutLockProjectManagementProvider(pipelineConfig, new Openshift(cpsScript.openshift, cpsScript))
        })
        projectManagementProviders.put(ProjectManagementProviderType.KUBERNETES_BPPR_WITH_JENKINS_LOCK, { pipelineConfig, CpsScript cpsScript ->
            new KubernetesBpprWithLockProjectManagementProvider(pipelineConfig, new KubernetesCluster(cpsScript), new KubeCtl(cpsScript))
        })
        projectManagementProviders.put(ProjectManagementProviderType.KUBERNETES_BPPR_WITHOUT_JENKINS_LOCK, { pipelineConfig, CpsScript cpsScript ->
            new KubernetesBpprWithoutLockProjectManagementProvider(pipelineConfig, new KubernetesCluster(cpsScript), new KubeCtl(cpsScript))
        })
        projectManagementProviders.put(ProjectManagementProviderType.KUBERNETES_BUILD_WITHOUT_JENKINS_LOCK, { pipelineConfig, CpsScript cpsScript ->
            new KubernetesBuildWithoutLockProjectManagementProvider(pipelineConfig, new KubernetesCluster(cpsScript), new KubeCtl(cpsScript))
        })
    }

    ProjectManagementProvider createProjectManagementProvider(ProjectManagementProviderType projectManagementProviderType, def pipelineConfig, CpsScript cpsScript) {
        return projectManagementProviders.get(projectManagementProviderType).call(pipelineConfig, cpsScript)
    }
}
