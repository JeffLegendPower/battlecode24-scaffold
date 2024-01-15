package v8;

import battlecode.common.RobotController;

public class Constants {
    public static int maxCornerFinders = 4;

    public static class SharedArray {

        public static int numberCornerFinder = 0; // Index 0
        public static int[] cornerLocations = {1, 2, 3, 4};
        public static int flagCornerLoc = 5;
        public static int numberFlagPlacer = 6;
        public static int numberDefenders = 7;
        public static int numberAttackers = 8;
        public static int currentAttackerTarget = 9;
        public static int attackersHaveFlag = 10;
        public static int currentAttackLeader = 11;
        public static int timeSinceLastAttack = 12;
    }
}