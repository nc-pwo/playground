package nc.devops.shared.library.cd.project

enum ProjectManagementProviderType {
    //todo: use enum with constructor for less complicated names eg: BPPR(OPENSHIFT, JENKINS_LOCK), or sthg like this
    OPENSHIFT_BPPR_WITH_JENKINS_LOCK,
    OPENSHIFT_BPPR_WITHOUT_JENKINS_LOCK,
    OPENSHIFT_BUILD_WITHOUT_JENKINS_LOCK,
    KUBERNETES_BPPR_WITH_JENKINS_LOCK,
    KUBERNETES_BPPR_WITHOUT_JENKINS_LOCK,
    KUBERNETES_BUILD_WITHOUT_JENKINS_LOCK
}
