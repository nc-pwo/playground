package dslScripts

listView("seed-job") {
    jobs {
        regex(".+-seed-job")
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