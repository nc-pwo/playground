package nc.devops.shared.library.buildtool

import com.cloudbees.groovy.cps.NonCPS
import nc.devops.shared.library.buildtool.logging.LoggingLevel

class BuildToolParameters implements Serializable {
    String serverId, binaryRepoUsername, binaryRepoPassword,
           pushRegistryUsername, pushRegistryPassword, prefixBuildInfoName,
           mavenSettingsConfig, sonarProfile, buildToolCustomClass,
           releaseRepo, snapshotRepo
    LoggingLevel loggingLevel
    boolean useWrapper

    @Override
    @NonCPS
    String toString() {
        return """\
BuildToolParameters{
    serverId='$serverId', 
    prefixBuildInfoName='$prefixBuildInfoName', 
    mavenSettingsConfig='$mavenSettingsConfig', 
    sonarProfile='$sonarProfile', 
    buildToolCustomClass='$buildToolCustomClass', 
    releaseRepo='$releaseRepo', 
    snapshotRepo='$snapshotRepo', 
    useWrapper='$useWrapper',
    loggingLevel='$loggingLevel'
}"""
    }
}
