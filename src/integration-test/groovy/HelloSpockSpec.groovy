

import spock.lang.Specification

class HelloSpockSpec extends Specification {

    def "length of Spock's and his friends' names"() {
        expect:
        name.size() == length

        // Some random change
        where:
        name     | length
        "Spock"  | 5
        "Kirk"   | 4
        "Scotty" | 6
    }
}  