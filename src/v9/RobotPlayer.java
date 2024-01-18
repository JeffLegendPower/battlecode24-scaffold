package v9;

import battlecode.common.*;
import v9.robots.RobotType;

import java.util.Random;

import static v9.Pathfinding.moveTowards;


/* 1) move all flags to nearest corner
 * 2) Bombard the guys with our huge army
 *
 */
public strictfp class RobotPlayer {

    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static MapInfo[][] map;
    public static RobotType type = null;
    private static boolean setupBeforeSetup = false;
    private static boolean setupAfterSetup = false;
    public static Random rng = new Random();

    public static int numMapped = 0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        map = new MapInfo[rc.getMapHeight()][rc.getMapWidth()];

        while (true) {

            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK))
                rc.buyGlobal(GlobalUpgrade.ATTACK);
            else if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
                rc.buyGlobal(GlobalUpgrade.HEALING);
            else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
                rc.buyGlobal(GlobalUpgrade.CAPTURING);

            if (!rc.isSpawned()) {
                spawn(rc);

            } else {

                MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    if (map[loc.y][loc.x] == null) numMapped++;
                    map[loc.y][loc.x] = info;
                }

                for (int i = 0; i < 12; i++) {
                    MapInfo info = Utils.getInfoInSharedArray(rc, Constants.SharedArray.scoutInfoChannels[i]);
                    if (info != null) {
                        MapLocation loc = info.getMapLocation();
                        if (map[loc.y][loc.x] == null) {
                            map[loc.y][loc.x] = info;
                            numMapped++;
                        }
                    }
                }

                MapLocation curLoc = rc.getLocation();

                if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS - 20) {
                    if (!setupBeforeSetup) {
                        type = RobotType.CornerFinder;
                        if (!type.getRobot().setup(rc, rc.getLocation()))
                            type = RobotType.Default;
                        setupBeforeSetup = true;
                    }
                    if (type != RobotType.Default && type.getRobot().completedTask())
                        type = RobotType.Default;
                } else {
                    if (!setupAfterSetup && (type == RobotType.Default || type == RobotType.CornerFinder)) {
                       type = RobotType.Attacker;
                       type.getRobot().setup(rc, curLoc);
                       setupAfterSetup = true;
                    }
                }

                if (rc.getRoundNum() % 20 == 0) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, null);
                }

                if (type != null) {
                    rc.setIndicatorString(type.name() + " " + (numMapped * 100) / (rc.getMapWidth() * rc.getMapHeight()) + "% mapped");
                    type.getRobot().tick(rc, curLoc);
                    if (type == RobotType.Scouter) {
                        rc.setIndicatorDot(curLoc, 0, 0, 255);
                    }
//                    if (type == RobotType.CornerFinder) System.out.println(rc.getLocation());
                } else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 50) {
                     type = RobotType.Attacker;
                     type.getRobot().setup(rc, curLoc);
                     setupAfterSetup = true;
                } else {
                    MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
                    if (nearbyCrumbs.length > 0)
                        moveTowards(rc, curLoc, nearbyCrumbs[0], true);
                    else
                        moveTowards(rc, curLoc, curLoc.add(directions[rng.nextInt(8)]), true);
                }
            }

            Clock.yield();
        }
    }


    private static boolean assigned = false;

    private static void spawn(RobotController rc) throws GameActionException {
        if (!assigned) {
            type = RobotType.FlagPlacer;
            type.getRobot().spawn(rc);
            if (!type.getRobot().setup(rc, rc.getLocation()))
                type = RobotType.Default;
            else
                setupBeforeSetup = true;

            if (type == RobotType.Default) {
                type = RobotType.Defender;
                if (!type.getRobot().setup(rc, rc.getLocation())) {
                    type = RobotType.Scouter;
                    if (!type.getRobot().setup(rc, rc.getLocation()))
                        type = RobotType.Default;
                }
                setupBeforeSetup = type != RobotType.Default;
            }
            assigned = true;
        }

        type.getRobot().spawn(rc);

//        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
//            if (rc.canSpawn(spawnLoc)) {
//                rc.spawn(spawnLoc);
//            }
//        }
    }
}
