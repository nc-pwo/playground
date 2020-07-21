package nc.devops.shared.library.gradle

import nc.devops.shared.library.buildtool.BuildObject;

interface Gradle<V> extends BuildObject {

    V call(String tasks)
    
}
