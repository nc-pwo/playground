package dms.devops.shared.library.buildtool

import dms.devops.shared.library.version.VersionType
import nc.devops.shared.library.buildtool.BuildTool

interface dmsBuildTool extends BuildTool {
    void compile()

    void functionalTest()

    void integrationTest()

    void buildArtifacts(String extraArgs)

    void buildImage()

    String currentVersion(def script)

    void createRelease(VersionType type)

    void pushRelease()

    void publishArtifacts()
}