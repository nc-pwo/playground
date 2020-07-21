package dms.devops.shared.library.seedjob

import nc.devops.shared.library.seedjob.JobCreator
import nc.devops.shared.library.seedjob.cron.CronProvider
import nc.devops.shared.library.seedjob.dsl.JobDslExecutor

class DmsJobCreator extends JobCreator {

    DmsJobCreator(Object script, boolean localMode, String defaultDslScriptsPath, String workDir, Closure<JobDslExecutor> dslExecutorFactory, Closure<CronProvider> cronProviderFactory) {
        super(script, localMode, defaultDslScriptsPath, workDir, dslExecutorFactory, cronProviderFactory)
    }

    @Override
    protected String textFromTemplate(String repository, String taskName) {
        String file
        if (isBitBucketBppr(repository, taskName)) {
            file = "dmsBitBucketBPPRJob.groovy"
        } else if (isGitHubBppr(taskName, repository)) {
            file = "gitHubBPPRJob.groovy"
        } else if (isBpprJob(taskName)) {
            file = "tfsBPPRJob.groovy"
        } else if (isReleaseJob(taskName)) {
            file = "dmsReleaseJob.groovy"
        } else {
            def scriptToRun = localMode ? 'localModeEnabled' : 'dmsLocalModeDisabled'
            file = "${scriptToRun}.groovy"
        }
        return script.libraryResource("${defaultDslScriptsPath}/${file}")
    }

    protected boolean isReleaseJob(String taskName){
        return  taskName.startsWith('release')
    }

}
