package nc.devops.shared.library.buildtool


import nc.devops.shared.library.tests.model.ProcessedTestProperty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension)
class TestPropertiesConverterTest {

    private final List testParameters = [
            [
                    type : 't',
                    name : 'n',
                    value: 'v'
            ]
            ,
            [
                    type : 't2',
                    name : 'n2',
                    value: 'v2'
            ]
    ]

    @Test
    void conversionTest() {
        assert new TestPropertiesConverterImpl().convert(testParameters as List<ProcessedTestProperty>) == "-tn=v -t2n2=v2"
    }

}
