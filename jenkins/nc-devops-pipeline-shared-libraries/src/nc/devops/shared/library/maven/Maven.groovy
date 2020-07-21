package nc.devops.shared.library.maven

import nc.devops.shared.library.buildtool.BuildObject

interface Maven<V> extends BuildObject {

    V call(String tasks)
}