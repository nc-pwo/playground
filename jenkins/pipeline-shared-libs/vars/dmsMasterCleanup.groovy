import hudson.FilePath

def call() {

    String JOBS_LIBS_DIR = "${env.JENKINS_HOME}/jobs/${env.JOB_NAME}/builds/${env.BUILD_NUMBER}/libs"
    String JOBS_WORKFLOW_DIR = "${env.JENKINS_HOME}/jobs/${env.JOB_NAME}/builds/${env.BUILD_NUMBER}/workflow"
    String WORKSPACE_LIBS_DIR = "${env.WORKSPACE}@libs"
    String WORKSPACE_TMP_DIR = "${env.WORKSPACE}@tmp"
    String WORKSPACE_SCRIPTS_DIR = "${env.WORKSPACE}@scripts"

    echo "Cleanup files on master"
    deleteDir(JOBS_LIBS_DIR)
    deleteDir(JOBS_WORKFLOW_DIR)
    deleteDir(WORKSPACE_LIBS_DIR)
    deleteDir(WORKSPACE_TMP_DIR)
    deleteDir(WORKSPACE_SCRIPTS_DIR)
}

void deleteDir(String path) {
    FilePath f = new FilePath(new File(path))
    if (f.exists()) {
        echo "Removing ${f.getRemote()}"
        f.deleteRecursive()
    }
}