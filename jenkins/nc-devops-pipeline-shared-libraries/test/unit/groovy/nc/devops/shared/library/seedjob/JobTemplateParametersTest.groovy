package nc.devops.shared.library.seedjob

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

@CompileStatic
class JobTemplateParametersTest {

    private final JobTemplateParameters templateParameters = new JobTemplateParameters(new JobData(), true, 'testOwner', '*')
    private final Set<String> expectedParamNames = ['name',
                                                    'task',
                                                    'creds',
                                                    'gitUrl',
                                                    'path',
                                                    'localPath',
                                                    'branch',
                                                    'repositoryOwner',
                                                    'repositoryName',
                                                    'cronFormat',
                                                    'localMode',
                                                    'bitbucketSSHCredentials',
                                                    'branches'] as Set

    @Test
    void paramsMapContainsExactlyAllRequiredKeys() {
        Map map = templateParameters.asMapForJobTemplate()
        assert map.keySet() == expectedParamNames
    }

}
