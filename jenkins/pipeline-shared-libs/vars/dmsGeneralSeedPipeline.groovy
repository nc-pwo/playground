import com.cloudbees.groovy.cps.NonCPS
import dms.devops.shared.library.seedjob.DmsJobCreator
import groovy.transform.Field
import nc.devops.shared.library.seedjob.JobData
import nc.devops.shared.library.seedjob.cron.CronProvider
import nc.devops.shared.library.seedjob.dsl.JobDslExecutor
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

@Field List<String> errors = []

def call(body) {
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    def defaultPipelineConfig = [
            cleaningStrategy: [
                    daysToKeep: '7',
                    numToKeep : '10'
            ]
    ]
    pipelineConfig = defaultPipelineConfig + pipelineConfig

    def AGENT_LABEL
    if (System.getenv('KUBERNETES_MODE_ENABLED') == 'true') {
        AGENT_LABEL = pipelineConfig.kubernetesPodTemplate ?: System.getenv('KUBERNETES_AGENT_LABEL')
    }

    AGENT_LABEL = pipelineConfig.agentLabel ?: (AGENT_LABEL ?: 'master')

    pipeline {
        agent { label "${AGENT_LABEL}" }

        parameters {
            text(name: 'YAML', defaultValue: "${params.YAML}" /* previously provided param value */, description: 'Repositories')
        }
        environment {
            PATH_DSL_SCRIPTS = 'dslScripts'
        }
        options {
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: '14'),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: '30'))
            )
        }
        stages {
            stage('Validate parameters') {
                steps {
                    script {
                        if (parametersAreEmpty()) {
                            error("Empty parameters are not valid.")
                        }

                        String yamlString = params.YAML
                        parsedYamlParams = new Yaml().load(yamlString)

                        def descriptionError = parametersNotValid(parsedYamlParams)
                        if (descriptionError) {
                            currentBuild.result = 'ABORTED'
                            error("Parameters are not valid.\nDescription: ${descriptionError}")
                        }

                        parameters = buildJobConfigurations(parsedYamlParams)
                        localModeEnabled = env.LOCAL_MODE_ENABLED == 'true'
                    }
                }
            }

            stage('Checkout repositories') {
                steps {
                    script {
                        parameters.each {
                            def (repository, credentials, path, branch, bitbucketSSHCredentials) = it
                            String repositoryName = gitUtils.getRepositoryName(repository)
                            catchCheckoutError(repository, credentials, path, branch, bitbucketSSHCredentials) {
                                dir("${repositoryName}") {
                                    if (localModeEnabled) {
                                        localCheckout(credentials, path, repositoryName, branch, bitbucketSSHCredentials)
                                    } else {
                                        echo 'Chosen branch: ' + branch
                                        remoteCheckout(repository, branch, credentials)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            stage('Checking jenkinsfiles') {
                steps {
                    script {
                        List<JobData> listOfPaths = getListOfPaths(parameters)
                        Map<String, List<JobData>> pathsByJobName = groupByJobName(listOfPaths)

                        // The next two variables are global and should be used in the next stage
                        duplicatedJobs = findAllDuplicatedJobs(pathsByJobName)
                        jobsData = findAllUniqueJobs(pathsByJobName)

                        showWarningWhenDuplicated(duplicatedJobs)
                    }
                }
            }

            stage('Run Job DSL') {
                steps {
                    script {

                        createAllJobs(jobsData)
                        createAllViews()
                    }
                }
            }
        }

        post {
            failure {
                informAboutErrorsAndFailJob(errors)
            }
            cleanup {
                dmsMasterCleanup()
            }
        }
    }
}

private List buildJobConfigurations(parsedYaml) {
    def parameters = new ArrayList()
    parsedYaml.each {
        jobConfig = it
        def branches = []
        if (!jobConfig.branch && !jobConfig.branches) {
            branches = ['master']
        }
        if (jobConfig.branch) {
            branches = [jobConfig.branch]
        }
        if (jobConfig.branches) {
            branches = jobConfig.branches
        }
        branches.each {
            parameters.add([jobConfig.repository, jobConfig.credentials, jobConfig.path, it, jobConfig.bitbucketSSHCredentials, jobConfig.branches?.join(' ')])
        }
    }
    return parameters
}

private boolean parametersAreEmpty() {
    return "$params.YAML".isEmpty()
}

private List<String> getListFromTextParameter(param) {
    return param.split('\n').findAll { (it as String).trim().length() != 0 }
}

private List<String> getListFromPathsParameter(param) {
    def pathsWithLinuxFormat = getListFromTextParameter(param).collect {
        it.trim().replaceAll('\\\\+', '/')
                .replaceAll('/{2,}', '/')
                .replaceFirst('^/', '')
    }
    return pathsWithLinuxFormat
}


private String parametersNotValid(parsedYaml) {
    String descriptionError = ''

    def repositories = parsedYaml.repository.size()
    def credentials = parsedYaml.credentials.size()
    def paths = parsedYaml.path.size()

    if (repositories != credentials || credentials != paths) {
        descriptionError = 'REPOSITORIES, CREDENTIALS, PATHS and BRANCHES should have the same number of elements:'
        def repositoriesError = (repositories.size() == 0) ?
                "\n- REPOSITORIES is empty" : "\n- REPOSITORIES has ${repositories.size()} element(s):\n" + repositories.join('\n')
        def credentialsError = (credentials.size() == 0) ?
                "\n- CREDENTIALS is empty" : "\n- CREDENTIALS has ${credentials.size()} element(s):\n" + credentials.join('\n')
        def pathsError = (paths.size() == 0) ?
                "\n- PATHS is empty" : "\n- PATHS has ${paths.size()} element(s) are:\n" + paths.join('\n')

        descriptionError = "${descriptionError}${repositoriesError}${credentialsError}${pathsError}${branchesError}"
    }

    descriptionError += validateBranches(parsedYaml)

    return descriptionError
}

private String validateBranches(parsedYaml) {
    String descriptionError = ''
    parsedYaml.each {
        if (it.branch && it.branches) {
            descriptionError += "Parameters 'branch' and 'branches' set for ${it}. Use only 'branches' instead."
        }
        if (it.branch) {
            echo "Parameter 'branch' is deprecated. Use 'branches' instead. (${it})"
        }
        if (it.branch && !(it.branch instanceof String)) {
            descriptionError += "Wrong type of 'branch' parameter for ${it}. Should be String."
        }
        if (it.branches && !(it.branches instanceof List<String>)) {
            descriptionError += "Wrong type of 'branches' parameter for ${it}. Should be List<String>."
        }
    }
    return descriptionError
}

private List<JobData> getListOfPaths(parameters) {
    return parameters.collect {
        def (repository, credential, path, branch, bitbucketSSHCredentials, branches) = it
        def repositoryName = gitUtils.getRepositoryName(repository)
        List<FileWrapper> jenkinfilesFileList = findFiles(glob: "**/${repositoryName}/${branch}/${path}/**/*.jenkinsfile")
        return jenkinfilesFileList
                .collect { normalizeSlashesInPath(it) }
                .collect { relativizePaths(it, repositoryName as String) }
                .collect {
                    createJobData(it, repositoryName, branch, repository, credential, bitbucketSSHCredentials, branches)
                }
    }.flatten() as List<JobData>
}

private JobData createJobData(FileWrapper fileWrapper, repositoryName, branch, repository, credential, bitbucketSSHCredentials, branches) {
    def (parentFolder, jenkinsfileName, taskName) = getDataFromJenkinsfile(fileWrapper)
    String jobName = buildJobName(parentFolder, repositoryName, taskName, branch)
    String pathToJenkinsFile = new File(fileWrapper.getPath()).getParent()
    String pathToJenkinsfileInWorkspace = relativizePathToJenkinsFileWithoutFileName(fileWrapper.getPath(), branch as String)
    return new JobData(jobName: jobName,
            pathToJenkinsfile: pathToJenkinsFile,
            pathToJenkinsfileInWorkspace: pathToJenkinsfileInWorkspace,
            jenkinsfileName: jenkinsfileName,
            taskName: taskName,
            repositoryName: repositoryName,
            repository: repository,
            credential: credential,
            branch: branch,
            bitbucketSSHCredentials: bitbucketSSHCredentials,
            branches: branches)
}

private void remoteCheckout(repository, branch, credentials) {
    checkout([$class           : 'GitSCM',
              branches         : [[name: "*/${branch}"]],
              extensions       : [[$class           : 'RelativeTargetDirectory',
                                   relativeTargetDir: branch]],
              userRemoteConfigs: [[credentialsId: credentials,
                                   url          : repository]]])
}


private void localCheckout(credentials, path, repositoryName, branch, bitbucketSSHCredentials) {
    dir(branch) {
        repositoryName = "/projects/${repositoryName}"
        if (new File(repositoryName).exists() && new File("$repositoryName/.git").exists() && new File("${repositoryName}/${path}").exists()) {
            checkout filesystem(clearWorkspace: true,
                    copyHidden: false,
                    filterSettings: [includeFilter: true, selectors: [[wildcard: "**/$path/**"]]],
                    path: repositoryName)
        } else {
            currentBuild.result = "FAILED"
            errors.add """Repository: ${repositoryName}
Credentials: ${credentials}
Path: ${path}
Bitbucket SSH Credentials: ${bitbucketSSHCredentials}
Cause : $repositoryName/$path does not exist or $repositoryName is not git repository."""
        }
    }
}

private FileWrapper normalizeSlashesInPath(FileWrapper fw) {
    return new FileWrapper(fw.name, fw.path.replaceAll('\\\\', '/'), fw.directory, fw.length, fw.lastModified)
}

private FileWrapper relativizePaths(FileWrapper fw, String repoName) {
    return new FileWrapper(fw.name, relativizePathToJenkinsFile(fw.path, repoName), fw.directory, fw.length, fw.lastModified)
}

private String getDataFromJenkinsfile(FileWrapper fileWrapper) {
    def parentFolder = new File(fileWrapper.path).parentFile.name
    def jenkinsfileName = fileWrapper.name
    def taskName = jenkinsfileName.split('\\.')[0]
    return [parentFolder, jenkinsfileName, taskName]
}

private String buildJobName(parentFolder, repositoryName, taskName, branchName) {
    branchName = removeUnsafeChars(branchName)
    return (["${parentFolder == "jenkins" ? '' : parentFolder}",
             "${taskName}",
             "${repositoryName}",
             "${branchName == "master" ? '' : branchName}"] - '').join('_')
}

@NonCPS
String relativizePathToJenkinsFile(String jenkinsFilepath, String repoPath) {
    return relativizePath(repoPath, jenkinsFilepath).toString()
}

@NonCPS
private Path relativizePath(String repoPath, String jenkinsFilepath) {
    Paths.get(repoPath).relativize(Paths.get(jenkinsFilepath))
}

@NonCPS
String relativizePathToJenkinsFileWithoutFileName(String jenkinsFilepath, String repoPath) {
    return relativizePath(repoPath, jenkinsFilepath).parent.toString()
}

private Map<String, List<JobData>> groupByJobName(ArrayList<JobData> listOfPaths) {
    listOfPaths.groupBy { it.jobName }
}

private Map<String, List<JobData>> findAllDuplicatedJobs(Map<String, List<JobData>> pathsByJobName) {
    pathsByJobName.findAll { it.value.size() > 1 }
}

private List<JobData> findAllUniqueJobs(Map<String, List<JobData>> pathsByJobName) {
    pathsByJobName.findAll { it.value.size() == 1 }.collect { it.value }.flatten() as List<JobData>
}

private void showWarningWhenDuplicated(duplicatedJobs) {
    if (duplicatedJobs) {
        currentBuild.result = 'UNSTABLE'
        println(buildErrorMessage())
    }
}

private String buildErrorMessage() {
    String errorDescription = '''
WARNING: Jenkinsfiles have duplicates
Description: We can not have two or more jenkinsfiles with same name and same parent folder name.
Please, correct the issue and try it again to generate them. The next jobs were not generated:
'''
    String allJenkinsfilePaths
    String errorDetails = duplicatedJobs.collect { jobName, listOfPaths ->
        allJenkinsfilePaths = listOfPaths.collect {
            " ${it.repository}/${it.pathToJenkinsfile}/${it.jenkinsfileName}\n"
        }
                .join('\n')
        return "- '${jobName}' is resulting from the following jenkinsfiles:\n" + allJenkinsfilePaths
    }.join('\n')
    return "$errorDescription\n$errorDetails"
}

private void createAllJobs(List<JobData> jobsData) {
    DmsJobCreator jobCreator = new DmsJobCreator(this, localModeEnabled, PATH_DSL_SCRIPTS, env.WORKSPACE, { ->
        new JobDslExecutor(this)
    }, { script, repo -> new CronProvider(script, repo)
    })

    jobsData.each { it ->
        try {
            jobCreator.create(it)
        } catch (Exception e) {
            currentBuild.result = "FAILED"
            errors.add """Repository: ${it.repositoryName}
Credentials: ${it.credential}
Path: ${it.pathToJenkinsfile}
Branch: ${it.branch}
Bitbucket SSH Credentials: ${it.bitbucketSSHCredentials}
Exception: $e
Stack trace: ${e.getStackTrace().each { err -> "${err.toString()}" }.join("\n")})"""
        }
    }
}


private void createAllViews() {
    createViewsForJobsGenerated()
    createViewForSeedJob()
    createViewForTotalFailedJob()
}

private void createViewsForJobsGenerated() {
    jobsData.collect { it.repositoryName }
            .unique()
            .each { repositoryName -> createViewsForRepository(repositoryName) }
}

private void createViewsForRepository(repositoryName) {
    try {
        new JobDslExecutor(this).createJob(libraryResource("${PATH_DSL_SCRIPTS}/jobsGeneratedViews.groovy"), [repositoryName: repositoryName])

    } catch (Exception e) {
        currentBuild.result = "Failed"
        errors.add """Repository: ${repositoryName}
Exception: $e
Stack trace: ${e.getStackTrace().each { it -> "${it.toString()}" }.join("\n")}"""
    }
}

private void createViewForSeedJob() {
    new JobDslExecutor(this).createJob(libraryResource("${PATH_DSL_SCRIPTS}/seedJobView.groovy"))
}

private void createViewForTotalFailedJob() {
    new JobDslExecutor(this).createJob(libraryResource("${PATH_DSL_SCRIPTS}/totalFailedView.groovy"))
}

private void informAboutErrorsAndFailJob(List<String> errors) {
    echo "The following parameters were not valid:"
    errors.eachWithIndex { item, index ->
        echo "${index + 1}. ${item}"
    }
}

private void catchCheckoutError(repository, credentials, path, branch, bitbucketSSHCredentials, Closure call) {
    try {
        call()
    } catch (Exception e) {
        currentBuild.result = "FAILED"
        errors.add """Repository: ${gitUtils.getRepositoryName(repository)}
Credentials: ${credentials}
Path: ${path}
Branch: ${branch}
Bitbucket SSH Credentials: ${bitbucketSSHCredentials}
Exception: $e
Stack trace: ${e.getStackTrace().each { err -> "${err.toString()}" }.join("\n")}"""
    }
}

private String removeUnsafeChars(String branchName) {
    return branchName.replaceAll(/[\\/&%$@{;]/, '-')
}
