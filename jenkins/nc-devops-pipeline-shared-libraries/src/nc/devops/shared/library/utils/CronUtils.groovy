package nc.devops.shared.library.utils

class CronUtils {

    static String calculatePollSCMExpression(def script) {
        /* Note env is not yet properly initialized - that is why System.getenv is used.
         */
        if (System.getenv('LOCAL_MODE_ENABLED') == 'true') {
            script.echo 'pollSCM trigger disabled when LOCAL_MODE_ENABLED=true'
            return ''
        } else {
            script.echo "Using pollSCM trigger with cron expression = '${script.pollSCMStrategy}'"
            return script.pollSCMStrategy
        }

    }

    static String calculateCronExpression(def script, def pipelineConfig) {
        if (System.getenv('LOCAL_MODE_ENABLED') == 'true') {
            script.echo 'cron trigger disabled when LOCAL_MODE_ENABLED=true'
            return ''
        } else {
            def cronExpression = pipelineConfig.cronExpression ?: ''
            script.echo "Using cron trigger with expression = '$cronExpression'"
            return cronExpression
        }
    }
}
