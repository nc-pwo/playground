package dslScripts

listView('Total failed') {
  description('List of all failed job')
  jobs {
    regex(/.*/)
  }
  jobFilters {
    status {
      matchType(MatchType.EXCLUDE_UNMATCHED)
      status(Status.FAILED)
    }
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