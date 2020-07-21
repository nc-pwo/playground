package dslScripts

listView("${repositoryName}") {
    jobs {
        regex(".+_${repositoryName}([_].*)*")
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}