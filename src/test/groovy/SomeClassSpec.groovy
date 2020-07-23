

import spock.lang.Specification

class SomeClassSpec extends Specification {

    def "Addition"() {
        given:
        def a = 15
        def b = 5
        expect:
        SomeClass.randomBehaviour(a, b) == 20
    }
    def "a less than 10"() {
        given:
        def a = 8
        def b = 5
        expect:
        SomeClass.randomBehaviour(a, b) == a
    }
    def "b greater than 20"() {
        given:
        def a = 11
        def b = 21
        expect:
        SomeClass.randomBehaviour(a, b) == 20
    }
    def "a+b less than -2"() {
        given:
        def a = 11
        def b = -14
        expect:
        SomeClass.randomBehaviour(a,b) == 0
    }
}
