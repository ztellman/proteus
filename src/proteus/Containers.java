package proteus;

public class Containers {
    
    public static class B {
        public boolean x;
        public B(boolean y) { x = y; };
        public void set(boolean y) { x = y; };
    }

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
