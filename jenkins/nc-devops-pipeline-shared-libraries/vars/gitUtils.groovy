import java.util.regex.Matcher

@Deprecated
// Remove this method at some point (cause: leaky abstraction)
def cloneRepo(url, credentialsId, branch = 'master') {
    // FIXME remove this if:
    if (url.startsWith("https:")) url = url.replace("https://source.netcompany.com", "ssh://source.netcompany.com:22")
    git url: url, credentialsId: credentialsId, branch: branch
}

String getRepositoryName(String gitUrl) {
    final Matcher matcher = gitUrl =~ /.*\/(.*?)(\.git)?$/
    final List<String> firstMatchWithGroups = matcher[0]
    return firstMatchWithGroups[1]
}

def getGitRepositoryOwner(String gitUrl) {
    final Matcher matcher = gitUrl =~ /.+(:|\/)(.*)\/.*/
    final List<String> firstMatchWithGroups = matcher[0]
    return firstMatchWithGroups[2]
}

String getPullRequestNumber(def changeID, def sourceBranch) {
    return changeID ?: getPullRequestIDFromSourceBranch(sourceBranch)
}

private String getPullRequestIDFromSourceBranch(String sourceBranch) {
        return (sourceBranch.split('/'))[2]
}

String prepareProjectName(String jobName) {
    String projectName = jobName.replaceFirst("(multibranch_)|(build_)|(local-build_)", "").replaceAll("(?i)(%2F)|[_/]", "-").toLowerCase()
    return projectName.length() <= 41 ? projectName : (projectName.substring(0, 40) + "xxx")
}