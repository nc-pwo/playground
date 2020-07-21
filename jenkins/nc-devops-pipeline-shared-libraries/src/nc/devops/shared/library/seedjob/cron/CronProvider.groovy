package nc.devops.shared.library.seedjob.cron


import java.util.regex.Matcher

class CronProvider implements Serializable {
    private final def script
    private final String repository

    CronProvider(def script, String repository) {
        this.script = Objects.requireNonNull(script, 'Script must not be null')
        this.repository = Objects.requireNonNull(repository,
                'Repository must not be null. ' +
                        'If you want to do not pass repository name, invoke constructor without repository parameter')
    }


    String findPollScmCronExpression(Closure<String> contentProvider) {
        return findPollScmCronExpressionForContent(contentProvider())
    }

    private String findPollScmCronExpressionForContent(String content) {
        Matcher pollSCMMatcher = (content =~ /('?pollSCM'?\s?\(["']?)(.*?)(?=\s*["']?\))/)
        if (pollSCMMatcher) {
            return pollSCMMatcher[0][2]
        } else {
            return getPollScmValueFromPredefinedPipeline(content)
        }
    }

    private String getPollScmValueFromPredefinedPipeline(String content) {
        Matcher libraryNameMatcher = content =~ /(@Library\(['"]?nc-devops-pipeline-shared-libraries['"]?\)\s\w)/
        if (libraryNameMatcher) {
            return getPollScmFromSharedPipeline(content)
        } else {
            return null
        }
    }

    private String getPollScmFromSharedPipeline(String content) {
        Matcher pipelineNameMatcher = content =~ /(.*Pipeline)/
        if (pipelineNameMatcher) {
            String pipelineName = pipelineNameMatcher[0][1]
            script.echo "Pipeline name: ${pipelineName}"
            return getPollScmValueFromSharedPipeline(pipelineName)
        } else {
            return null
        }
    }

    private String getPollScmValueFromSharedPipeline(String pipelineName) {
        try {
            return script["${pipelineName}"].pollSCMStrategy
        } catch (MissingPropertyException e) {
            script.echo "WARNING! No pollSCMStrategy defined for pipeline type: ${pipelineName} in repository: ${repository}. PollSCM not set. Error message: \"${e.getMessage()}\""
            return null
        }
    }
}
