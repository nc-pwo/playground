package nc.devops.shared.library.cd.templates

import nc.devops.shared.library.credentials.AbstractCredentials

class ApplicationParameters implements Serializable {
    final String templatePath
    List<String> deploymentParameters
    final List<AbstractCredentials> credentials
    final boolean hasBpprImage

    ApplicationParameters(String templatePath, List<String> deploymentParameters, List<AbstractCredentials> credentials, boolean hasBpprImage) {
        this.templatePath = templatePath
        this.deploymentParameters = deploymentParameters
        this.credentials = credentials
        this.hasBpprImage = hasBpprImage
    }

    ApplicationParameters(Map paramMap) {

        this(
                paramMap.templatePath as String,
                paramMap.deploymentParameters as List<String>,
                paramMap.credentialParameters as List<AbstractCredentials>,
                paramMap.hasBpprImage as boolean
        )
    }

}
