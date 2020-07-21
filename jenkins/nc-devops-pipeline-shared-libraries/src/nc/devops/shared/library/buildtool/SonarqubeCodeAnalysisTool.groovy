package nc.devops.shared.library.buildtool

class SonarqubeCodeAnalysisTool {
    protected static final int PROJECT_INDEX = 3

    String convertParams(StaticCodeAnalysisParams codeAnalysisParams) {
        switch (codeAnalysisParams.class) {
            case PullRequestCodeAnalysisParams:
                def prParams = codeAnalysisParams as PullRequestCodeAnalysisParams
                return "-Dsonar.pullrequest.branch=${prParams.pullRequestSourceBranchName} " +
                        "-Dsonar.pullrequest.key=${prParams.pullRequestNumber} " +
                        "-Dsonar.pullrequest.base=${prParams.pullRequestBaseBranchName} " +
                        "${additionalPropertiesForPrDecoration(prParams.repoUrl)}"
            case StaticCodeAnalysisParams:
                return "-Dsonar.branch.name=${codeAnalysisParams.branchName}"
        }
        throw new IllegalArgumentException("unsupported class: ${codeAnalysisParams.class}")
    }

    protected String additionalPropertiesForPrDecoration(String repoUrl) {
        def uri = new URI(repoUrl)
        if (uri.host == 'source.netcompany.com') {
            def path = Arrays.asList(uri.path.split('/'))
            return "-Dsonar.pullrequest.provider=vsts " +
                    "-Dsonar.pullrequest.vsts.instanceUrl=https://source.netcompany.com/tfs/Netcompany " +
                    "-Dsonar.pullrequest.vsts.project=${path[PROJECT_INDEX]} " +
                    "-Dsonar.pullrequest.vsts.repository=${path.last()}"
        } else {
            return ''
        }
    }
}
