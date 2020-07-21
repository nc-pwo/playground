package dms.devops.shared.library.version

import java.util.regex.Pattern

class Version {
    private static final Pattern NUMBER_PATTERN = ~/(\d+)/
    private static final Pattern VERSION_REGEX = ~/(?<major>$NUMBER_PATTERN).(?<minor>$NUMBER_PATTERN).(?<patch>$NUMBER_PATTERN)/

    private final int major, minor, patch

    Version(String version) {
        def matcher = VERSION_REGEX.matcher(version)
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version")
        }
        this.major = Integer.parseInt(matcher.group("major"))
        this.minor = Integer.parseInt(matcher.group("minor"))
        this.patch = Integer.parseInt(matcher.group("patch"))
    }
}
