package nc.devops.shared.library.buildtool

import org.jenkinsci.plugins.workflow.cps.CpsScript

interface BuildObject {

    void setCpsScript(CpsScript script)
}