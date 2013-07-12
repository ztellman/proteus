package proteus;

public class Containers {
    public static class O {
        public Object x;
        public O(Object y) { x = y; };
        public void set(Object y) { x = y; };
    }

    public static class D {
        public double x;
        public D(double y) { x = y; };
        public void set(double y) { x = y; };
    }

    public static class L {
        public long x;
        public L(long y) { x = y; };
        public void set(long y) { x = y; };
    }
}
