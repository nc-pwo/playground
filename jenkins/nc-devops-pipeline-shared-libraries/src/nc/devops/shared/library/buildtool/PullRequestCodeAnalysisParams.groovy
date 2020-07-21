package nc.devops.shared.library.buildtool

class PullRequestCodeAnalysisParams extends StaticCodeAnalysisParams {
    String pullRequestNumber
    String pullRequestSourceBranchName
    String pullRequestBaseBranchName
    String repoUrl
}
