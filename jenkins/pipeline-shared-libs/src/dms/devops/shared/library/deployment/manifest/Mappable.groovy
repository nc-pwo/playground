package dms.devops.shared.library.deployment.manifest

class Mappable {

    Map<String, Object> toMap() {
        this.metaClass.properties.findAll { 'class' != it.name }.collectEntries {
            if (Mappable.isAssignableFrom(it.type)) {
                [(it.name): this."$it.name".toMap()]
            } else {
                [(it.name): "${this."$it.name"}"]
            }
        }.sort()
    }
}