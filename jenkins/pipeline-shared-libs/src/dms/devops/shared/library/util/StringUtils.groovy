package dms.devops.shared.library.util

class StringUtils {
    static boolean nullOrEmpty(String value) {
        return value == null || value == ''
    }

    static String getEmptyIfNull(String value) {
        return value == null ? '' : value
    }
}
