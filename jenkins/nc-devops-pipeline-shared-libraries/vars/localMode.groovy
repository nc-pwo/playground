
boolean isEnabled() {
    def envVar = System.getenv('LOCAL_MODE_ENABLED')
    return envVar == null ? false : envVar == 'true'
}

String branchNameForLocalMode() {
    return 'local_mode_dummy_branch'
}