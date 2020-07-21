package nc.devops.shared.library.cd.openshift

class SelectorMock {
    def valueToReturn

    SelectorMock(def valueToReturn) {
        this.valueToReturn = valueToReturn
    }

    def rollout() {
        new Object() {
            def status() {
                if (Exception.class.isAssignableFrom(valueToReturn.class)) {
                    throw valueToReturn
                } else {
                    return valueToReturn
                }
            }

            def latest() {
                if (Exception.class.isAssignableFrom(valueToReturn.class)) {
                    throw valueToReturn
                } else {
                    return valueToReturn
                }
            }
        }
    }

    def count() {
        return valueToReturn
    }

    def object() {
        new Object() {
            def status = new Object() {
                boolean succeeded = valueToReturn
                boolean latestVersion = valueToReturn
            }

            def getAt(String key) {
                new Object() {
                    String host = valueToReturn
                }
            }
        }
    }

    def watch(Closure c) {
        c.call(new Object() {
            def count() {
                return 0
            }
        })
    }

    boolean withEach(Closure closure) {
        return valueToReturn
    }
}


