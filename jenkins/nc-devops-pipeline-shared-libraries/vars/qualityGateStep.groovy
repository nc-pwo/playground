def call(Boolean maybeSonarqubeDisableWebhook) {
    boolean sonarqubeDisableWebhook = maybeSonarqubeDisableWebhook == null ? true : maybeSonarqubeDisableWebhook
    timeout(time: 30, unit: 'MINUTES') {
        def qg = sonarqubeDisableWebhook ? pollForQualityGate() : waitForQualityGate()
        echo "Quality Gate reported status ${qg.status}"
        if (qg.status != 'OK') {
            error "Quality Gate status not OK"
        }
    }
}