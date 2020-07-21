package nc.devops.shared.library.test

import hudson.model.Build

class ChangelogPipelineMock extends PipelineMock{
    String changelogMock

    ChangelogPipelineMock(Build rawBuildMock) {
        super(rawBuildMock)
    }

    def sh(Closure c){
        c.call()
    }

    def gitChangelog(Map m){
        return changelogMock
    }

    String libraryResource(String path){
        return path
    }
}
