package nc.devops.shared.library.seedjob

import nc.devops.shared.library.seedjob.cron.CronProvider
import nc.devops.shared.library.seedjob.dsl.JobDslExecutor

class JobCreator {
    protected def script
    protected final boolean localMode
    protected final String defaultDslScriptsPath
    protected final String workDir
    protected final Closure<JobDslExecutor> dslExecutorFactory
    protected final Closure<CronProvider> cronProviderFactory

    JobCreator(script, boolean localMode, String defaultDslScriptsPath, String workDir, Closure<JobDslExecutor> dslExecutorFactory, Closure<CronProvider> cronProviderFactory) {
        this.script = script
        this.localMode = localMode
        this.defaultDslScriptsPath = defaultDslScriptsPath
        this.workDir = workDir
        this.dslExecutorFactory = dslExecutorFactory
        this.cronProviderFactory = cronProviderFactory
    }

    def create(JobData jobData) {
        String jenkinsfilePath = "${workDir}/${jobData.repositoryName}/${jobData.pathToJenkinsfile}/${jobData.taskName}"
        JobTemplateParameters param = new JobTemplateParameters(jobData, localMode, getRepositoryOwner(jobData.repository),
                initCronFormat(jobData.repository, { -> script.readFile "${jenkinsfilePath}.jenkinsfile" }))
        script.echo "Using ${param} for job generation"

        JobDslExecutor executor = dslExecutorFactory.call()
        executor.createJob(getScriptText(jobData, jenkinsfilePath), param.asMapForJobTemplate())
    }

    protected String getScriptText(JobData jobData, String jenkinsfilePath) {

        if (isCustomDslPresent("${jenkinsfilePath}.jobdsl")) {
            String text = script.readFile "${jenkinsfilePath}.jobdsl"
            return text
        }
        return textFromTemplate(jobData.repository, jobData.taskName)
    }

    protected String textFromTemplate(String repository, String taskName) {
        String file
        if (isBitBucketBppr(repository, taskName)) {
            file = "bitBucketBPPRJob.groovy"
        } else if (isGitHubBppr(taskName, repository)) {
            file = "gitHubBPPRJob.groovy"
        } else if (isBpprJob(taskName)) {
            file = "tfsBPPRJob.groovy"
        } else if (isMultibranch(taskName)) {
            file = "gitMultibranchPipelineJob.groovy"
        } else {
            def scriptToRun = localMode ? 'localModeEnabled' : 'localModeDisabled'
            file = "${scriptToRun}.groovy"
        }
        return script.libraryResource("${defaultDslScriptsPath}/${file}")
    }

    protected boolean isCustomDslPresent(String path) {
        script.fileExists(path)
    }

    protected boolean isBitBucketBppr(String repository, String taskName) {
        repository.contains('bitbucket.org') && isBpprJob(taskName)
    }

    protected boolean isGitHubBppr(String taskName, String repository) {
        repository.contains('github.com') && isBpprJob(taskName)
    }

    protected boolean isMultibranch(String taskName){
        return taskName.startsWith("multibranch")
    }

    protected boolean isBpprJob(String taskName) {
        return taskName.startsWith('bppr') || taskName.startsWith('mavenBppr')
    }

    protected String getRepositoryOwner(String repository) {
        if (repository.contains('github.com') || repository.contains('git@bitbucket.org')) {
            return script.gitUtils.getGitRepositoryOwner(repository)
        }
        return null
    }

    protected String initCronFormat(String repository, Closure<String> scriptTextProvider) {
        cronProviderFactory.call(script, repository).findPollScmCronExpression(scriptTextProvider)
    }
}