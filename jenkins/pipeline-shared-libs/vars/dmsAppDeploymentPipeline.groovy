import dms.devops.shared.library.deployment.helm.DeploymentConfiguration
import dms.devops.shared.library.deployment.helm.HelmExecutor
import dms.devops.shared.library.deployment.manifest.DeploymentManifest
import dms.devops.shared.library.version.VersionUtil

import static dms.devops.shared.library.util.StringUtils.nullOrEmpty

def call(body) {
    def pipelineConfig = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineConfig
    body()

    /*
     The default is set for Spring-boot applications.
     Remaining deployment pipelines are expected to define the chartVersion
     */
    def defaultPipelineConfig = [
            chartVersion    : '0.0.1-SNAPSHOT',
            cleaningStrategy: [
                    daysToKeep: '7',
                    numToKeep : '8'
            ],
            envType         : 'app',
            mailRecipients: [
                    'alko@netcompany.com',
                    'krba@netcompany.com',
                    'lnt@netcompany.com',
                    'mvcu@netcompany.com',
                    'nhp@netcompany.com',
                    'nvmt@netcompany.com',
                    'ptp@netcompany.com',
                    'pklo@netcompany.com',
                    'pwil@netcompany.com',
                    'vtcl@netcompany.com',
            ]
    ]
    pipelineConfig = defaultPipelineConfig + pipelineConfig

    HelmExecutor helm
    DeploymentConfiguration deploymentConfiguration
    boolean appSpecificValuesFileExists

    final String HELM_REPOSITORY = "ufstdms-dev-helm"
    final String HELM_REMOTE_REPOSITORY = "https://artifactory.nchosting.dk/ufstdms-dev-helm"
    final String CONFIG_PACKAGE_NAME = "deployment-values"
    final String CONFIG_PACKAGE_FORMAT = "zip"
    final String OCAD_JOB_NAME = "deployment_deploy_env_devops"
    final String CONFIG_FILE_DESTINATION = "config"
    final int FIRST_UPSTREAM_BUILD_INDEX = 0

    final Map<String, String> ENV_NAME_TO_CONFIG_TYPE_MAP = [
            'dev01'    : 'dev01',
            'dev02'    : 'dev02',
            'dev03'    : 'dev03',
            'test01'   : 'test',
            'udd01'    : 'udd',
            'preprod01': 'preprod'
    ]

    boolean uninstall

    pipeline {
        agent {
            label 'kubernetes'
        }
        parameters {
            string(name: 'applicationVersion', description: 'Docker image version of the application (applications\' image tag)' +
                    '\nTags like \'latest\' can be used as well. \'latest-release\' causes deployment with latest released version')
            string(name: 'configurationVersion', description: 'Version of the configuration package')
            string(name: 'chartVersion', defaultValue: '0.0.1-SNAPSHOT', description: 'Helm chart version. If empty, 0.0.1-SNAPSHOT will be used')
            string(name: 'envName', description: 'Exact name of the kubernetes environment e.g. dev01')
            string(name: 'replicaCount', defaultValue: '1', description: 'Number of replicas for the deployment')
            string(name: 'imageName', defaultValue: '', description: 'OPTIONAL Docker image name - optional parameter to overwrite default value in jenkinsfile')
            string(name: 'chartName', defaultValue: '', description: 'OPTIONAL Helm chart name - optional parameter to overwrite default value in jenkinsfile')
            booleanParam(name: 'uninstall', defaultValue: false, description: 'Remove release history before installing.' +
                    '\nThis option should only be used for specific case, when first ever installation failed, to remove deployment history stored in k8s' +
                    '\nIn any other scenario leave it blank')
            booleanParam(name: 'cleanDb', defaultValue: false, description: 'Clean the database for the application before deployment?')
            booleanParam(name: 'cleanEs', defaultValue: false, description: 'Clean the Elasticsearch indices for the application before deployment?')
            string(name: 'mailRecipients', defaultValue: '', description: 'OPTIONAL. Given recipients (use semicolon to separate each recipient). It will be used when failing build')
        }
        environment {
            ARTIFACTORY_CREDENTIALS = credentials('artifactory-reader-token')
        }

        options {
            timestamps()
            buildDiscarder(logRotator(
                    artifactDaysToKeepStr: pipelineConfig.cleaningStrategy?.artifactDayToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_DAY_TO_KEEP') ?: ''),
                    artifactNumToKeepStr: pipelineConfig.cleaningStrategy?.artifactNumToKeep ?: (System.getenv('LOG_ROTATE_ARTIFACT_NUM_TO_KEEP') ?: ''),
                    daysToKeepStr: pipelineConfig.cleaningStrategy?.daysToKeep ?: (System.getenv('LOG_ROTATE_DAY_TO_KEEP') ?: '14'),
                    numToKeepStr: pipelineConfig.cleaningStrategy?.numToKeep ?: (System.getenv('LOG_ROTATE_NUM_TO_KEEP') ?: '30'))
            )
            copyArtifactPermission(OCAD_JOB_NAME)
        }

        stages {
            stage('Validate parameters') {
                steps {
                    script {
                        helm = new HelmExecutor({ String cmd -> sh cmd })
                        deploymentConfiguration = getDeploymentConfiguration(pipelineConfig, params, ENV_NAME_TO_CONFIG_TYPE_MAP)
                        currentBuild.displayName = "${env.BUILD_DISPLAY_NAME} ${deploymentConfiguration.namespace} version $deploymentConfiguration.imageVersion"
                        uninstall = params.uninstall ? true : firstReleaseFailed(deploymentConfiguration.applicationName, deploymentConfiguration.namespace)
                    }
                }
            }

            stage('Clean database') {
                when {
                    expression {
                        params.cleanDb
                    }
                }
                steps {
                    build job: "deploy_clean-database_dms-Ops", parameters: [
                            [$class: 'StringParameterValue', name: "environment", value: params.envName],
                            [$class: 'StringParameterValue', name: "applicationName", value: pipelineConfig.applicationName],
                    ], propagate: true, wait: true
                }
            }

            stage('Clean Elasticsearch') {
                when {
                    expression {
                        params.cleanEs
                    }
                }
                steps {
                    build job: "deploy_clean-elasticsearch_dms-Ops", parameters: [
                            [$class: 'StringParameterValue', name: "environment", value: params.envName],
                            [$class: 'StringParameterValue', name: "applicationName", value: pipelineConfig.applicationName],
                    ], propagate: true, wait: true
                }
            }

            stage('Download configuration package') {
                steps {
                    script {
                        String configPackage = "$CONFIG_PACKAGE_NAME-${deploymentConfiguration.configurationVersion}.$CONFIG_PACKAGE_FORMAT"

                        echo "Downloading the configuration package version ${deploymentConfiguration.configurationVersion}..."
                        sh "curl -su $ARTIFACTORY_CREDENTIALS -O https://artifactory.nchosting.dk/ufstdms-generic-dev-local/deployment-values/$configPackage"
                        sh "unzip -o -q $configPackage"
                        sh "rm $configPackage"
                    }
                }
            }

            stage('Update repository') {
                steps {
                    script {
                        helm.updateRepository(HELM_REPOSITORY, HELM_REMOTE_REPOSITORY, "$ARTIFACTORY_CREDENTIALS_USR", "$ARTIFACTORY_CREDENTIALS_PSW")
                        helm.search(HELM_REPOSITORY, pipelineConfig.chartName as String)
                    }
                }
            }

            stage('Download the chart') {
                steps {
                    script {
                        helm.pull(HELM_REPOSITORY, deploymentConfiguration)
                        sh "cp -r ${deploymentConfiguration.configurationType} ${pipelineConfig.chartName}/$CONFIG_FILE_DESTINATION"

                        helm.createPackage(deploymentConfiguration)
                        sh "rm -r ${pipelineConfig.chartName}"
                    }
                }
            }

            stage('Validate the chart') {
                steps {
                    script {
                        appSpecificValuesFileExists = fileExists("${env.WORKSPACE}/${deploymentConfiguration.appValuesFile}")
                        helm.template(deploymentConfiguration, appSpecificValuesFileExists)
                    }
                }
            }

            stage('Uninstall previous release') {
                when {
                    expression {
                        uninstall
                    }
                }
                steps {
                    script {
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            helm.uninstall(deploymentConfiguration)
                        }
                    }
                }
            }

            stage('Deploy the chart') {
                steps {
                    script {
                        helm.upgrade(deploymentConfiguration, appSpecificValuesFileExists)
                    }
                }
            }
        }

        post {
            failure {
                script {
                    if(params.mailRecipients){
                        def defaultRecipients = pipelineConfig.mailRecipients
                        defaultRecipients.addAll(params.mailRecipients.split(';'))
                        echo 'Sending emails to given recipients...'
                        notifyViaEmailAboutFailure(defaultRecipients)
                    }
                }
            }
            always {
                script {
                    if (deploymentConfiguration != null) {
                        String fileName = 'deploymentResult.yaml'
                        DeploymentManifest manifest = new DeploymentManifest(deploymentConfiguration, currentBuild.result)
                        echo "${manifest.toMap()}"
                        writeYaml file: fileName, data: manifest.toMap()
                        archiveArtifacts artifacts: fileName
                    }
                }
            }
            cleanup {
                dmsMasterCleanup()
                cleanWs()
            }
        }
    }
}

static boolean pipelineConfigContainsNull(Map pipelineConfig) {
    pipelineConfig.applicationName == null || pipelineConfig.chartName == null || pipelineConfig.applicationImage == null
}

static boolean parametersContainNull(def params) {
    return params.applicationVersion.isEmpty() || params.configurationVersion.isEmpty() || params.envName.isEmpty()
}

void validateNotNull(String value, String errorMsg) {
    if (nullOrEmpty(value)) {
        error(errorMsg)
    }
}

private String getImageVersion(applicationVersion) {
    final String LATEST_RELEASE = "latest-release"
    if (applicationVersion == LATEST_RELEASE) {
        return VersionUtil.getLatestRelease(this)
    } else {
        return applicationVersion
    }
}

private DeploymentConfiguration getDeploymentConfiguration(Map pipelineConfig, def params, Map<String, String> configurationTypeDict) {

    String envType = pipelineConfig.envType
    String envName = params.envName
    String namespace = "${envName}-${envType}"
    String applicationName = pipelineConfig.applicationName
    String imageName = params.imageName != '' ? params.imageName : pipelineConfig.applicationImage
    String imageVersion = getImageVersion(params.applicationVersion)
    String chartName = params.chartName != '' ? params.chartName : pipelineConfig.chartName
    String chartVersion = params.chartVersion != '' ? params.chartVersion : pipelineConfig.chartVersion
    String configurationType = getConfigurationType(envName, configurationTypeDict)
    String configurationVersion = params.configurationVersion
    String replicaCount = params.replicaCount

    validateNotNull(applicationName, "Required parameter applicationName was not specified in jenkinsfile")
    validateNotNull(imageVersion, "Required parameter applicationVersion was not specified or latest release has not been found")
    validateNotNull(configurationVersion, "Required parameter configurationVersion was not specified in job parameters")
    validateNotNull(chartVersion, "Required parameter chartVersion was not specified neither in job parameters nor in jenkinsfile")
    validateNotNull(envType, "Required parameter envType was not specified")
    validateNotNull(envName, "Required parameter envName was not specified")
    validateNotNull(imageName, "Required parameter imageName was not specified neither in job parameters nor in jenkinsfile")
    validateNotNull(chartName, "Required parameter chartName was not specified neither in job parameters nor in jenkinsfile")
    validateNotNull(replicaCount, "Required parameter replicaCount were not specified in job parameters")

    return new DeploymentConfiguration(
            applicationName,
            imageName,
            imageVersion,
            chartName,
            chartVersion,
            namespace,
            configurationType,
            configurationVersion,
            replicaCount
    )
}

String getConfigurationType(String envType, Map<String, String> dict) {
    if (dict.containsKey(envType)) {
        return dict.get(envType)
    } else {
        error("There is no configuration type specified for environment $envType. Possible options are $dict")
    }
}

boolean firstReleaseFailed(String appName, String namespace) {
    String releaseStatus
    try {
        releaseStatus = sh script: "helm status ${appName} -n ${namespace} -o yaml", returnStdout: true
    }
    catch (Exception ex) {
        return false
    }
    Map releaseDescription = readYaml text: releaseStatus
    return releaseDescription.info.status == 'failed' && releaseDescription.version == 1
}