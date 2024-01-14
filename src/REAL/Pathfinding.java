package REAL;

import battlecode.common.*;

import java.util.ArrayList;

import static REAL.RobotPlayer.rng;

public class Pathfinding {

    private static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static ArrayList<MapLocation> prevLocs = new ArrayList<>();


    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target) throws GameActionException {
        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;

        int x = Math.max(1, Math.min(rc.getMapWidth() - 1, target.x));
        int y = Math.max(1, Math.min(rc.getMapHeight() - 1, target.y));
        target = new MapLocation(x, y);

//        rc.setIndicatorString("x: " + target.x + " y: " + target.y);
        // TODO: THIS IS PROBABLY INEFFICIENT!!! FIND BETTER WAY OF CALCING CLOSEST POINT
        int closestPointDist = 999999;
        int dist;
        Direction bestDir = Direction.CENTER;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            /*if (newLoc.x < 0 || newLoc.x > rc.getMapWidth() || newLoc.y < 0 || newLoc.y > rc.getMapHeight())
                continue;*/
            dist = calculateDistance(newLoc, target);
            if (dist < closestPointDist || (dist == closestPointDist && rng.nextBoolean())) {
                bestDir = dir;
                closestPointDist = dist;
            }
        }

        Direction rightDir = bestDir;

        while (!rc.canMove(rightDir) || (rng.nextInt(10) > 7 && inPrevLocs(rc.getLocation().add(rightDir)))) {
            rightDir = rightDir.rotateRight();
            if (rightDir.equals(bestDir)) {
                prevLocs = new ArrayList<>();
                break;
            }
        }


        prevLocs.add(rc.getLocation());
        if (prevLocs.size() > 7) {
            prevLocs.remove(0);
        }
    }

    public static void moveTowards(RobotController rc, MapLocation curLoc, Direction target) throws GameActionException {
        moveTowards(rc, curLoc, curLoc.add(target));
    }

    public static int calculateDistance(MapLocation ml1, MapLocation ml2) {
        // Use Manhatten distance for now
        return (Math.abs(ml1.x-ml2.x) + Math.abs(ml1.y-ml2.y));
    }

    public static boolean inPrevLocs(MapLocation loc) {
        for (MapLocation prevLoc : prevLocs) {
            if (loc.equals(prevLoc))
                return true;
        }
        return false;
    }
}