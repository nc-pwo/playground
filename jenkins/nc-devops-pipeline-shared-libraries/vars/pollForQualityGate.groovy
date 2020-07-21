import nc.devops.shared.library.sonar.QualityGatePoller
import nc.devops.shared.library.sonar.SonarPluginAdapter

def call() {
    return new QualityGatePoller(new SonarPluginAdapter()).pollForQualityGate(this)
}