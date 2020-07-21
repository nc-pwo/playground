package nc.devops.shared.library.xray.dto.mapper

import nc.devops.shared.library.xray.dto.TestDto
import nc.devops.shared.library.xray.util.DateUtil

class TestMapper {
    TestDto map(def testJson) {
        def test = new TestDto()
        test.start = DateUtil.millisToOffsetDateTime(Long.valueOf(testJson.start)).toString()
        test.finish = DateUtil.millisToOffsetDateTime(Long.valueOf(testJson.end)).toString()
        test.status = testJson.result.toUpperCase()
        test.comment = testJson.name
        test.testKey = testJson.tags?.find()?.name
        test
    }
}
