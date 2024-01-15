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
    private static boolean setup;
    public static Random rng = new Random();

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        map = new MapInfo[rc.getMapWidth()][rc.getMapHeight()];

        while (true) {

            if (!rc.isSpawned()) {
                spawn(rc);

            } else {
                MapLocation curLoc = rc.getLocation();

                if (!setup) {
                    type = RobotType.CornerFinder;
                    if (!type.getRobot().setup(rc, curLoc)) {
                        type = null;
                    }
                    setup = true;
                }

                if (type != null) {
                    rc.setIndicatorString(type.name());
                    type.getRobot().tick(rc, curLoc);
                }

                //Pathfinding.moveTowards(rc, curLoc, curLoc.add(Direction.allDirections()[rng.nextInt(8)]));

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
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            if (rc.canSpawn(spawnLoc))
                rc.spawn(spawnLoc);
        }
    }
}
