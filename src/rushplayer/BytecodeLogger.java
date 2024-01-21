package rushplayer;

import battlecode.common.Clock;

import java.util.HashMap;

import static rushplayer.General.rc;

public class BytecodeLogger {
    static int prev = 0;
    static HashMap<String, Integer> byteCodeCounts = new HashMap<>();

    public static void log(String name) {
        int now = Clock.getBytecodeNum();
        byteCodeCounts.put(name, now-prev);
        prev = now;
    }

    public static void print() {
        for (String s : byteCodeCounts.keySet()) {
            System.out.printf("%s=%d", s, byteCodeCounts.get(s));
        }
    }

    public static void showIndicator() {
        StringBuilder total = new StringBuilder();
        for (String s : byteCodeCounts.keySet()) {
            total.append(String.format("%s=%d", s, byteCodeCounts.get(s)));
            total.append("\n");
        }
        rc.setIndicatorString(total.toString());
    }

    public static void clear() {
        prev = 0;
        byteCodeCounts.clear();
    }
}
