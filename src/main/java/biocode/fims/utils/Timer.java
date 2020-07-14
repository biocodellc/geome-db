package biocode.fims.utils;

/**
 * Utility class to help time events easily
 */
public class Timer {
    private static Timer instance;
    long begin;

    public Timer() {
        reset();
    }

    public static Timer getInstance() {
        if (instance == null) {
            instance = new Timer();
        }
        return instance;
    }

    public void reset() {
        begin = System.currentTimeMillis();
    }

    public void lap(String message) {
        long end = System.currentTimeMillis();
        long executionTime = end - begin;
        System.out.println("" + executionTime + " ms : " + message);
    }


}
