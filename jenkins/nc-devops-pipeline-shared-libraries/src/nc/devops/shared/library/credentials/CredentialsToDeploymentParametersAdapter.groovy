package nc.devops.shared.library.credentials

import groovy.transform.CompileStatic

class CredentialsToDeploymentParametersAdapter {

    private final def script

    CredentialsToDeploymentParametersAdapter(script) {
        this.script = script
    }

    @CompileStatic
    List<String> convert(List<? extends AbstractCredentials> credentials) {
        def result = []
        for (param in credentials) {
            switch (param) {
                case UsernamePasswordCredentials:
                    handleUsernamePassword(param as UsernamePasswordCredentials, result)
                    break
                case StringCredentials:
                    handleString(param as StringCredentials, result)
                    break
                case FileCredentials:
                    handleFile(param as FileCredentials, result)
                    break
                default:
                    throw new IllegalArgumentException("credentials type unsupported: ${param}")
            }
        }
        result
    }

    private Object handleFile(FileCredentials param, result) {
        param.with {
            script.withCredentials([script.file(credentialsId: param.credentialsId, variable:
                    'FILE_PATH')]) {
                result.add(createTemplateParameter(param.fileParameter, 'FILE_PATH'))
            }
        }
    }

    private Object handleString(StringCredentials param, result) {
        param.with {
            script.withCredentials([script.string(credentialsId: param.credentialsId, variable: 'TOKEN')]) {
                result.add(createTemplateParameter(param.stringParameter, 'TOKEN'))
            }
        }
    }

    private Object handleUsernamePassword(UsernamePasswordCredentials param, result) {
        param.with {
            script.withCredentials([script.usernamePassword(credentialsId: param.credentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                result.add(createTemplateParameter(param.usernameParameter, 'USERNAME'))
                result.add(createTemplateParameter(param.passwordParameter, 'PASSWORD'))
            }
        }
    }

    private String createTemplateParameter(String parameterName, String environmentVariableName) {
        parameterName + "=" + script.env[environmentVariableName]
    }
}
