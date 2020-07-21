package nc.devops.shared.library.seedjob

import com.cloudbees.groovy.cps.NonCPS

class JobTemplateParameters implements Serializable {

    final String name
    final String task
    final String creds
    final String gitUrl
    final String path
    final String localPath
    final String branch
    final String repositoryOwner
    final String repositoryName
    final String cronFormat
    final boolean localMode
    final String bitbucketSSHCredentials
    final String branches

    JobTemplateParameters(JobData jobData, boolean localMode, String repositoryOwner, String cronFormat) {
        name = jobData.jobName
        task = jobData.taskName
        creds = jobData.credential
        gitUrl = jobData.repository
        path = jobData.pathToJenkinsfileInWorkspace
        localPath = jobData.repositoryName
        branch = jobData.branch
        repositoryName = jobData.repositoryName
        bitbucketSSHCredentials = jobData.bitbucketSSHCredentials
        branches = jobData.branches
        this.localMode = localMode
        this.repositoryOwner = repositoryOwner
        this.cronFormat = cronFormat
    }

    @NonCPS
    @Override
    String toString() {
        return """\
DslJobParameters{
    name='$name', 
    task='$task', 
    creds='$creds', 
    gitUrl='$gitUrl', 
    path='$path', 
    localPath='$localPath', 
    branch='$branch', 
    repositoryOwner='$repositoryOwner', 
    repositoryName='$repositoryName', 
    cronFormat='$cronFormat', 
    localMode=$localMode, 
    bitbucketSSHCredentials='$bitbucketSSHCredentials', 
    branches='$branches'
}"""
    }

    Map asMapForJobTemplate() {
        ['name',
         'task',
         'creds',
         'gitUrl',
         'path',
         'localPath',
         'branch',
         'repositoryOwner',
         'repositoryName',
         'cronFormat',
         'localMode',
         'bitbucketSSHCredentials',
         'branches'].collectEntries { property -> [property, this[property]] }
    }
}


