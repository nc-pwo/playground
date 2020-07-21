import nc.devops.shared.library.changelog.ChangelogGenerator

def call(def changelogConfig){
    new ChangelogGenerator(this, changelogConfig).generateAndPublish()
}

