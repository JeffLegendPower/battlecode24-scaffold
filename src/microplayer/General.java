package microplayer;

import battlecode.common.*;

import java.util.Random;

public class General {
    public static RobotController rc;
    public static int mapWidth = -1;
    public static int mapHeight = -1;
    public static MapLocation centerOfMap;

    public static MapLocation robotLoc = null;

    public static int rngSeed = 1;
    public static int id = -1;
    public static Random rng = new Random(rngSeed);
    public static int maxTimeThatBreadCanBeOnTheGround = 4;

    public static boolean[] possibleSymmetries = {true, true, true};  // [rotational, up/down, left/right]
    public static boolean symmetryWasDetermined = false;
    public static MapLocation previousLocationForMappingFreshLocations = null;
    public static int[][] mapped;
    /*
    bit usage for each int in mapped
    0 | 0=unseen 1=explored
    1 | 0=wall 1=not
    2 | 0=dam 1=not
    */

    public static boolean doingBugNav = false;
    public static MapLocation[] bugNavVertices = null;
    public static int bugNavVertexIndex = -1;
    public static Boolean bugNavGoingClockwise = null;
    public static MapLocation bugnavPathfindGoal = null;
    public static MapLocation[] bugNavAllPlaces = null;  // todo delete

    public static Direction lastMovedSetupExplorationDirection = null;
    public static MapLocation[] allySpawnLocations = null;
    public static MapLocation[] orderedCenterSpawnLocations = null;  // do NOT reorder !!!
    public static MapLocation[] sortableCenterSpawnLocations = null;  // it is ok to sort this one
    public static MapLocation[] visitedForSetupPathfinding = new MapLocation[30];
    public static int visitedForSetupPathfindingIndex = 0;

    public static MapLocation broadcastFlagPathfindLoc = null;
    public static MapLocation[] enemyFlagLocations = new MapLocation[3];
    public static boolean[] enemyFlagIsTaken = new boolean[3];
    public static Direction continueInThisDirection = null;

    public static boolean isCarrier = false;
    public static MapLocation carrierDestination = null;
    public static int lastTimeSinceFlagCarrierMoved = 0;
    public static int flagCarrierIndex = -1;
    public static int lastAliveRoundNumber = -1;
    public static int lastDroppedFlagValue = -1;
    public static MapLocation[] carrierLocations = new MapLocation[3];
    public static boolean[] hasCarrierDroppedFlag = new boolean[3];
    public static boolean[] isEnemyFlagDeposited = new boolean[3];
    public static int flagTransferCooldown = 0;

    public static int[] bodyguardCounts = new int[]{0, 0, 0};
    public static int bodyguardingIndex = -1;

    public static boolean isProtector = false;
    public static MapLocation myProtectedFlagLocation = null;
    public static Integer protectedFlagIndex = null;

    // attack, heal, capture
    public static boolean[] allyGlobalUpgrades = new boolean[]{false, false, false};
    public static boolean[] enemyGlobalUpgrades = new boolean[]{false, false, false};

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

    /*
    shared array
    =================
    0 | next duck id value
    1 | spotted flag location 1 | 0=there 1=taken
    2 | spotted flag location 2 | 0=there 1=taken
    3 | spotted flag location 3 | 0=there 1=taken
    4 | carried flag location 1 | 00=carried 10=dropped 01,11=deposited
    5 | carried flag location 2 | 00=carried 10=dropped 01,11=deposited
    6 | carried flag location 3 | 00=carried 10=dropped 01,11=deposited
    7 | 1 bit empty, 5 bits protector index 3, 5 bits protector index 2, 5 bits protector index 1
    8 | 1 bit rotational symmetry, 1 bit up/down symmetry, 1 bit left/right symmetry, 1 bit spare, 4 bits per bodyguard count for carried flag locations
    9 | number of ducks spawned in
    */

}
