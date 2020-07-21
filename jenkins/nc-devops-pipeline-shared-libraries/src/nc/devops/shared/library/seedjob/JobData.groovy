package nc.devops.shared.library.seedjob

import com.cloudbees.groovy.cps.NonCPS

class JobData implements Serializable {
    String jobName
    String pathToJenkinsfile
    String pathToJenkinsfileInWorkspace
    String jenkinsfileName
    String taskName
    String repositoryName
    String repository
    String credential
    String branch
    String bitbucketSSHCredentials
    String branches

    @NonCPS
    @Override
    String toString() {
        return """\
JobData{
    jobName='$jobName', 
    pathToJenkinsfile='$pathToJenkinsfile', 
    pathToJenkinsfileInWorkspace='$pathToJenkinsfileInWorkspace', 
    jenkinsfileName='$jenkinsfileName', 
    taskName='$taskName', 
    repositoryName='$repositoryName', 
    repository='$repository', 
    credential='$credential', 
    branch='$branch', 
    bitbucketSSHCredentials='$bitbucketSSHCredentials', 
    branches='$branches'
}"""
    }
}
