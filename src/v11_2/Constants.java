package v11_2;

public class Constants {
    public static int maxCornerFinders = 4;

    public static class SharedArray {

        public static final int[] enemyFlagIDs = {0, 1, 2};
        public static final int[] enemyFlagLocs = {3, 4, 5};
        public static final int[] flagOrigins = {6, 7, 8};

        public static final int numDefenders = 9;
        public static final int defenderAlert = 10;
        public static final int[] coordinatedAttacks = {11, 12, 13, 14, 15, 16};
        public static final int[] carriedFlagIDs = {17, 18, 19};

        // 0: Unknown
        // 1: Symmetric by rotation
        // 2: Symmetric by up/down reflection
        // 3: Symmetric by left/right reflection
        public static final int mapSymmetry = 20;
    }
}