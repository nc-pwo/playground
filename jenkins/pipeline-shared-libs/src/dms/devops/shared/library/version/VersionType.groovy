package dms.devops.shared.library.version

enum VersionType {
    PATCH('incrementPatch'),
    MINOR('incrementMinor'),
    MAJOR('incrementMajor')


    String incrementPolicy

    VersionType(String incrementPolicy) {
        this.incrementPolicy = incrementPolicy
    }

}
