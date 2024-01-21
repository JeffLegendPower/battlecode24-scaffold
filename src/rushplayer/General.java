package rushplayer;

import battlecode.common.*;
import rushplayer.map.BaseMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class General {

    public static RobotController rc;

    public static boolean protectingFlag = false;
    public static int timeSinceLastMoveForFlagProtector = 0;
    public static int timeSinceLastMoveForRunBack = 0;

    public static int rngSeed = 238;
    public static Random rng = new Random(rngSeed);

    public static MapLocation[] protectedFlagLocations = new MapLocation[3];
    public static Integer protectorLocationIndex = null;

    public static MapLocation[] allySpawnLocations = null;
    public static MapLocation whereShouldSpawn = null;
    public static Set<MapLocation> wallLocations = new HashSet<>();
    public static MapLocation spawnPosition = null;
    public static MapLocation defaultExplorationPoint = null;

    public static boolean hasHealingFocus = false;
    public static MapLocation[] enemyFlagLocations = new MapLocation[3];
    public static MapLocation[] enemyFlagCarrierLocations = new MapLocation[3];
    public static boolean[] enemyFlagIsPickedUp = new boolean[3];
    public static Integer carryBackFlagIndex = null;
    public static MapLocation lastCarryLocationWhenAlive = null;
    public static ArrayList<MapLocation> visitedForRunBack = new ArrayList<>();

    public static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };

    public static BaseMap map;

    /*
        Shared array
        0  | found enemy flag location 1 | 0=there 1=not there
        1  | found enemy flag location 2 | 0=there 1=not there
        2  | found enemy flag location 3 | 0=there 1=not there

        3  | carried enemy flag location 1 | 0=being carried 1=captured
        4  | carried enemy flag location 2 | 0=being carried 1=captured
        5  | carried enemy flag location 3 | 0=being carried 1=captured

        60 | ideal spawn location | 1=urgent need of defense

        61 | protected flag location 1 | 0=safe 1=undecided
        62 | protected flag location 2 | 0=safe 1=undecided
        63 | protected flag location 3 | 0=safe 1=undecided
     */

}
