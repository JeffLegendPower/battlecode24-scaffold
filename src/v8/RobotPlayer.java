package v8;

import battlecode.common.*;
import v8.robots.*;

import java.util.Random;


/* 1) move all flags to nearest corner
 * 2) Bombard the guys will our huge army
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

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        map = new MapInfo[rc.getMapHeight()][rc.getMapWidth()];

        while (true) {

            if (!rc.isSpawned()) {
                spawn(rc);

            } else {
                MapLocation curLoc = rc.getLocation();

                if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
                    if (!setupBeforeSetup) {
                        type = RobotType.CornerFinder;
                        if (!type.getRobot().setup(rc, rc.getLocation()))
                            type = null;
                        setupBeforeSetup = true;
                    }
                    if (type != null && type.getRobot().completedTask()){
                        type = null;
                    }
                } else {
                    if (!setupAfterSetup && type != RobotType.FlagPlacer) {
                        type = RobotType.Attacker;
                        type.getRobot().setup(rc, curLoc);
                        if (rc.readSharedArray(Constants.SharedArray.currentAttackLeader) == 0)
                            rc.writeSharedArray(Constants.SharedArray.currentAttackLeader, Attacker.attackerID);
                        setupAfterSetup = true;
                    }
                }



                if (type != null) {
                    rc.setIndicatorString(type.name());
                    //System.out.println(" I AM A: " + type.name());
                    type.getRobot().tick(rc, curLoc);
                } else {
                    //type = RobotType.Attacker;
                    Pathfinding.moveTowards(rc, curLoc, new MapLocation(0, 30));
                }

            }

            if (rc.isSpawned()) {
                MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    map[loc.y][loc.x] = info;
                }
            }

            Clock.yield();
        }
    }



    private static void spawn(RobotController rc) throws GameActionException {
        int i = 0;
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);

                    type = RobotType.FlagPlacer;
                    if (!type.getRobot().setup(rc, rc.getLocation())) {
                        type = null;
                    } else {
                        setupBeforeSetup = true;
                    }

            }
            i += 1;
        }
    }
}
