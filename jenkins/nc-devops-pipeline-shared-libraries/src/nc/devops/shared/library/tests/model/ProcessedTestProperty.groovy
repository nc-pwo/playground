package nc.devops.shared.library.tests.model

class ProcessedTestProperty implements Serializable {
    String type
    String name
    String value

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ProcessedTestProperty that = (ProcessedTestProperty) o

        if (name != that.name) return false
        if (type != that.type) return false
        if (value != that.value) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (type != null ? type.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (value != null ? value.hashCode() : 0)
        return result
    }
}
