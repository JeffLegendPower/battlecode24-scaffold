package old_microplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.Random;

public class General {
    public static RobotController rc;
    public static int mapWidth = -1;
    public static int mapHeight = -1;

    public static int rngSeed = 1;
    public static int id = -1;
    public static Random rng = new Random(rngSeed);

    public static Direction lastMovedSetupExplorationDirection = null;
    public static MapLocation[] allySpawnLocations = null;
    public static MapLocation[] centerSpawnLocations = null;
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
    public static int lastAliveRound = -1;
    public static int lastDroppedFlagValue = -1;
    public static MapLocation[] carrierLocations = new MapLocation[3];
    public static boolean[] hasCarrierDroppedFlag = new boolean[3];
    public static boolean[] isEnemyFlagDeposited = new boolean[3];
    public static int transferCooldown = 0;
    public static ArrayList<MapLocation> visited = new ArrayList<>();

    public static boolean isProtector = false;
    public static MapLocation myProtectedFlagLocation = null;
    public static Integer protectedFlagIndex = null;

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
    4 | carried flag location 1 | 0=carried 1=dropped 2,3=deposited
    5 | carried flag location 2 | 0=carried 1=dropped 2,3=deposited
    6 | carried flag location 3 | 0=carried 1=dropped 2,3=deposited
    7 | 1 bit empty, 5 bits protector index 3, 5 bits protector index 2, 5 bits protector index 1
    */

}
