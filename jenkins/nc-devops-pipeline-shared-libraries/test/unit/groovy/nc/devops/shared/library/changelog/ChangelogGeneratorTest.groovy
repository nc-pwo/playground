package nc.devops.shared.library.changelog

import nc.devops.shared.library.changelog.ChangelogGenerator
import nc.devops.shared.library.test.ChangelogPipelineMock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension.class)
class ChangelogGeneratorTest {
    private ChangelogGenerator changelogGenerator
    private ChangelogPipelineMock pipeline


    private changelogConfig = [changelogPublishingCredentialsId: "nc-devops-credentials",
                               changelogPublishingRepository   : "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-release-notes",
                               changelogPublishingBranch       : "master",
                               compareBranch                   : "master",
                               changelogCompareRepository      : "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-pipeline-shared-libraries"]

    @BeforeEach
    void setup() {
        pipeline = new ChangelogPipelineMock()
        changelogGenerator = new ChangelogGenerator(pipeline, changelogConfig)
    }

    @Test
    void parseConfigCorrectTest() {
        def (changelogPublishingCredentialsId, changelogPublishingRepository, changelogPublishingBranch, changelogCompareRepository, changelogOutputDirectory, compareBranch) = changelogGenerator.parseConfig()
        assert changelogPublishingCredentialsId == "nc-devops-credentials"
        assert changelogPublishingRepository == "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-release-notes"
        assert changelogPublishingBranch == "master"
        assert changelogCompareRepository == "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-pipeline-shared-libraries"
        assert changelogOutputDirectory == "release-notes/nc-devops-pipeline-shared-libraries"
        assert compareBranch == "master"
    }

    @Test
    void parseMissingProperty() {
        def invalidMap = changelogGenerator.changelogConfig.findAll { it.key != "changelogCompareRepository" }
        changelogGenerator.changelogConfig = invalidMap
        Assertions.assertThrows(NullPointerException.class, {
            changelogGenerator.parseConfig()
        })
    }

    @Test
    void removeMergedPRMessageFromCommitTest() {
        pipeline.setChangelogMock("""###Merge remote-tracking branch 'origin/develop' into HEAD
###Merged PR 35833: Use-kubernertes-slave
- Related work items: #12972
###Merged PR 35773: 15328 Remove updateBaseImage step
- 15328 Remove updateBaseImage step
- Related work items: #15328
###Merged PR 36212: TASK 15554 - general-seed-job-parameters-yaml-fromat
- removal of &#39;initParametersFromConfigFile&#39; + parse parameters from yaml
- Related work items: #15554""")
        String parsedChangelog = changelogGenerator.generateChangelogAsString("")
        assert !parsedChangelog.contains(changelogGenerator.PR_EXPRESSION)
    }
}
