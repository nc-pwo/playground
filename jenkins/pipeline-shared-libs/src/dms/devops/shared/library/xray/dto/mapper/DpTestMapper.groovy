package dms.devops.shared.library.xray.dto.mapper

import dms.devops.shared.library.xray.dto.dmsTestDto

class dmsTestMapper {
    dmsTestDto map(def testJson) {
        def test = new dmsTestDto()
        test.status = testJson.result.toUpperCase()
        test.comment = testJson.name + "\n;\n" + testJson.testCode
        test.testKey = ""
        if(testJson.tags) {
            testJson.tags?.each { it ->
                test.testKey += it.name + ","
            }
            test.testKey = test.testKey.substring(0,test.testKey.length()-1)
        }
        test
    }
}