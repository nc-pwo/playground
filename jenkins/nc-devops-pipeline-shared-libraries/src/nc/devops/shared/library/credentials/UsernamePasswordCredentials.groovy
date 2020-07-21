package nc.devops.shared.library.credentials

import groovy.transform.CompileStatic

@CompileStatic
class UsernamePasswordCredentials extends AbstractCredentials {
    String usernameParameter, passwordParameter
}
