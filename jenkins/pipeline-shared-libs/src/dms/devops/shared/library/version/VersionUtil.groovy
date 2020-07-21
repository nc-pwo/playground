package dms.devops.shared.library.version

class VersionUtil {

    static final String TAG_PATTERN = "(release)-(\\d.\\d.\\d)"
    static final def CONCRETE_VERSION_PATTERN = ~/\d+\.\d+\.\d+/

    static boolean isSnapshotVersion(String version) {
        return version.contains("SNAPSHOT")
    }

    static boolean isConcreteVersion(String version) {
        return CONCRETE_VERSION_PATTERN.matcher(version).matches()
    }

    static String getLatestRelease(def script) {
        String version = ""
        try {
            String out = script.sh script: "git describe --abbrev=0", returnStdout: true
            def matcher = (out =~ TAG_PATTERN)
            version = matcher.find() ? matcher[0][2] : ""
        }
        catch (Exception ex) {
            //ignore
        }
        return version
    }

    static String getLatestVersion(String currentVersion, newVersion){
        Version currentSemanticVersion = parseStringToSemanticVesion(currentVersion)
        Version newSemanticVersion = parseStringToSemanticVesion(newVersion)
        if (currentSemanticVersion.major > newSemanticVersion.major) {
            return currentVersion
        }
        boolean isSameMajor = currentSemanticVersion.major == newSemanticVersion.major
        if(isSameMajor && currentSemanticVersion.minor > newSemanticVersion.minor) {
            return currentVersion
        }
        boolean isSameMinor = currentSemanticVersion.minor == newSemanticVersion.minor
        if (isSameMajor & isSameMinor & currentSemanticVersion.patch > newSemanticVersion.patch) {
            return currentVersion
        }
        return newVersion
    }

    private static Version parseStringToSemanticVesion(String version){
        return new Version(version)
    }

}
