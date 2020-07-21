package dms.devops.shared.library.publish
import com.cloudbees.groovy.cps.NonCPS

class ArtifactoryPublisher {
    Closure shellProvider
    String url

    ArtifactoryPublisher(Closure shellProvider, String url) {
        this.shellProvider = shellProvider
        this.url = getNormalizedUrl(url)
    }

    void publishArtifact(String filePath, String repoName, String destinationPath, String token) {
        shellProvider "curl -f -X PUT -su $token -T $filePath '$url/$repoName/$destinationPath'"
    }

    @NonCPS
    private String getNormalizedUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Artifactory url cannot be null or empty")
        }
        if (url.charAt(url.length() - 1) == '/') {
            return url.substring(0, url.length() - 1)
        }
        return url
    }
}
