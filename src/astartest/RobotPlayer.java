package astartest;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
    static RobotController rc;
    static MapLocation[] allySpawnLocations;
    static Integer id = null;

    static int[][] mapped;
    /*

    LSB usage for each int in mapped
    0 | 0=unseen 1=seen
    1 | 0=not wall 1=wall

    */

    static Integer mapWidth = null;
    static Integer mapHeight = null;

    static int rngSeed = 12839;
    static Random rng = new Random(rngSeed);
    static MapLocation robotLoc = null;
    static MapLocation previousLocation = null;

    public static void run(RobotController rc) {
        RobotPlayer.rc = rc;
        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (!trySpawnDuck()) {
                    continue;
                }
                onTurn();
                randomizeRng();
            } catch (GameActionException gae) {
                System.out.println("GAMEACTIONEXCEPTION =========");
                gae.printStackTrace();
            } catch (Exception e) {
                System.out.println("EXCEPTION ===================");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void randomizeRng() {
        rngSeed = (rng.nextInt( robotLoc.x + 142891) + robotLoc.y) % 66500;
        rng.setSeed(rngSeed);
    }

    public static boolean trySpawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            robotLoc = rc.getLocation();
            return true;
        } else {
            previousLocation = null;
            robotLoc = null;
        }

        if (rc.readSharedArray(0) == 1) {  // one duck has already been spawned in
            return false;
        }

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        mapped = new int[mapWidth][mapHeight];

        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }

        for (MapLocation allySpawn : allySpawnLocations) {
            if (rc.canSpawn(allySpawn)) {
                rc.spawn(allySpawn);
                id = rc.readSharedArray(0);
                rc.writeSharedArray(0, id+1);
                robotLoc = rc.getLocation();
                return true;
            }
        }
        return false;
    }

    public static void onTurn() {
        // todo: optimize to only check newly seen locations by knowing the move offset from last turn
        MapInfo[] nearbyInfos;
        if (previousLocation == null) {
            nearbyInfos = rc.senseNearbyMapInfos();
        } else {
            nearbyInfos = getFreshInVisionInfos();
        }
        for (MapInfo info : nearbyInfos) {
            MapLocation loc = info.getMapLocation();
            if (0 <= loc.x && loc.x < mapWidth && 0 <= loc.y & loc.y < mapHeight) {
                mapped[loc.x][loc.y] = info.isWall() ? 0b11 : 0b01;
            }
        }
        previousLocation = robotLoc;

        // move randomly


        // draw the seen locations (uses up like 13000+ bytecode)
        for (int x=0; x<mapWidth; x++) {
            for (int y=0; y<mapHeight; y++) {
                int v = mapped[x][y];
                if (v == 0) {
                    continue;
                }
                rc.setIndicatorDot(new MapLocation(x, y), (v & 0b10) << 6, 0, 0);
            }
        }
    }

    public static MapLocation[] getFreshInVisionLocations() {
        int x = robotLoc.x;
        int y = robotLoc.y;
        switch (previousLocation.directionTo(robotLoc)) {
            case NORTH:
                return new MapLocation[]{
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4),
                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+4)
                };
        }
        System.out.println("big issue 1 !!!");
        return new MapLocation[0];
    }

    public static MapInfo[] getFreshInVisionInfos() {
        if (previousLocation.equals(robotLoc)) {  // did not move
            return new MapInfo[0];
        }
        MapInfo[] infosWithNulls = new MapInfo[9];
        int nullAmount = 0;
        for (MapLocation l : getFreshInVisionLocations()) {
            if (0 <= l.x && l.x < mapWidth && 0 <= l.y) {

            }
// todo finish this
        }

        MapInfo[] infosWithoutNulls = new MapInfo[99999999];
    }
}
