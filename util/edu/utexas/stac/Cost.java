package edu.utexas.stac;

public class Cost {
    private static long value;
    static {
        value = 0;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // Print the cost when program terminates
                System.out.println("[COST] " + value);
            }
        });
    }

    public static void inc() {
        ++value;
    }

    public static void inc(int c) {
        value += c;
    }

    public long get() { return value; }
}