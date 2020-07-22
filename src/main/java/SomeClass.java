public class SomeClass {

    public static int randomBehaviour(int a, int b) {
        if (a > 20) throw new RuntimeException();
        else if (a<-100000000) return 21;
        else if (a < 10) return a;
        else if (b > 20) return 20;
        else if (a + b + 2 < 0) return 0;
        else return a + b;
    }
}
