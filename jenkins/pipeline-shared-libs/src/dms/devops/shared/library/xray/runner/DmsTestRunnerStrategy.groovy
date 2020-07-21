package dms.devops.shared.library.xray.runner

interface dmsTestRunnerStrategy {
    def importExecutionResults(Map vars, boolean isBPPR)
}