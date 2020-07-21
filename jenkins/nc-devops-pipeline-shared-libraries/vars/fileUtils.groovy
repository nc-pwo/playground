/*
    vars/fileUtils.readFile: Read fileName (full path) and return properties file
    E.g.: fileName = "${WORKSPACE}/src/jobs/nc/devops/hello-world-application/pipeline.properties"
*/
def readFile(fileName) {
    properties =  readProperties file: fileName
    properties
}

String getDirectory(String fullFileName) {
    def file = new File(fullFileName)
    return file.isDirectory() ? fullFileName : file.parent ?: '.'
}

String getFileNameWithoutExtension(String fileName) {
    String cleanedFileName = new File(fileName.replace("\\", "/")).name

    int dotIndex = cleanedFileName.lastIndexOf('.')
    return dotIndex != -1 ? cleanedFileName.substring(0, dotIndex) : cleanedFileName
}