package nc.devops.shared.library.changelog

class ChangelogGenerator {
    def pipeline
    def changelogConfig
    final String PR_EXPRESSION = '(\\w+\\s\\w+\\s[0-9]+:\\s?)'
    final String CHANGELOG_PATH = 'template/changelog.mustache'
    final String NO_ISSUE_EXPRESSION = '(##No issue)'
    final String IGNORE_COMMIT_EXPRESSION = "(###Merge )(remote|branch)(.*)"
    final String DEFAULT_CREDENTIALS_ID = "nc-devops-credentials"
    final String DEFAULT_PUBLISHING_REPOSITORY = "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-release-notes"

    ChangelogGenerator(def pipeline, def changelogConfig) {
        this.pipeline = pipeline
        this.changelogConfig = changelogConfig
    }

    def parseConfig() throws MissingPropertyException {
        String changelogPublishingCredentialsId = changelogConfig.changelogPublishingCredentialsId ?: DEFAULT_CREDENTIALS_ID
        String changelogPublishingRepository = changelogConfig.changelogPublishingRepository ?: DEFAULT_PUBLISHING_REPOSITORY
        String changelogPublishingBranch = changelogConfig.changelogPublishingBranch ?: "master"
        String changelogCompareRepository = changelogConfig.changelogCompareRepository
        String changelogOutputDirectory = changelogConfig.changelogOutputDirectory ?: "release-notes/${changelogCompareRepository.split("/")[-1]}"
        String compareBranch = changelogConfig.compareBranch ?: "master"
        return [changelogPublishingCredentialsId, changelogPublishingRepository, changelogPublishingBranch, changelogCompareRepository, changelogOutputDirectory, compareBranch]
    }

    String generateChangelogAsString(String branchToCompare) {
        String changelogAsString = pipeline.gitChangelog to: [type : 'REF',
                                                              value: "$branchToCompare"],
                template: pipeline.libraryResource(CHANGELOG_PATH),
                returnType: 'STRING'
        return changelogAsString.replaceAll(PR_EXPRESSION, '').replaceAll(NO_ISSUE_EXPRESSION, "").replaceAll(IGNORE_COMMIT_EXPRESSION, "")
    }

    void checkoutAndPull(String compareBranch, String changelogPublishingCredentialsId, String changelogCompareRepository) {
        pipeline.checkout([$class: 'GitSCM', branches: [[name: "*/$compareBranch"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$changelogPublishingCredentialsId", url: "$changelogCompareRepository"]]])
        pipeline.sh script: """git checkout $compareBranch 
                                      git pull origin $compareBranch"""

    }

    void publishChanges(String changelog, String changelogPublishingRepository, String changelogPublishingBranch, String changelogOutputDirectory, String changelogPublishingCredentialsId) {
        pipeline.checkout([$class: 'GitSCM', branches: [[name: "*/$changelogPublishingBranch"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$changelogPublishingCredentialsId", url: "$changelogPublishingRepository"]]])
        pipeline.sh label: '', script: "git pull origin $changelogPublishingBranch"
        pipeline.writeFile file: "$changelogOutputDirectory/Changelog.md", text: changelog
        pipeline.sh label: "Push changelog to $changelogPublishingBranch",
                script: """git add \'$changelogOutputDirectory/Changelog.md\'
                                       git commit -m \'Changelog update\'
                                       git push origin HEAD:$changelogPublishingBranch"""
    }

    void generateAndPublish() {
        def (changelogPublishingCredentialsId, changelogPublishingRepository, changelogPublishingBranch,
             changelogCompareRepository, changelogOutputDirectory, compareBranch) = parseConfig()
        checkoutAndPull(compareBranch, changelogPublishingCredentialsId, changelogCompareRepository)
        String changelogMd = generateChangelogAsString(compareBranch)
        publishChanges(changelogMd, changelogPublishingRepository, changelogPublishingBranch, changelogOutputDirectory, changelogPublishingCredentialsId)
    }
}
