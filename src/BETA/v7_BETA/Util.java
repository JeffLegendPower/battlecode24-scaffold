package BETA.v7_BETA;

import battlecode.common.*;

public class Util {

    public static void storeLocationInSharedArray(RobotController rc, int index, MapLocation location) throws GameActionException {
        // TODO: more efficient bitpacking?
        rc.writeSharedArray(index, location.x + location.y * 1000);
    }

    public static MapLocation getLocationInSharedArray(RobotController rc, int index) throws GameActionException {
        int loc = rc.readSharedArray(index);
        return new MapLocation(loc % 1000, loc / 1000);
    }

    public static MapLocation getClosest(MapLocation[] locs, MapLocation curLoc) {
        MapLocation closest = locs[0];
        int closestDist = 999999;
        int dist;
        for (MapLocation loc : locs) {
            dist = curLoc.distanceSquaredTo(loc);
            if (dist < closestDist) {
                closest = loc;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static MapLocation getFurthest(MapLocation[] locs, MapLocation curLoc) {
        MapLocation furthest = locs[0];
        int furthestDist = 0;
        int dist;
        for (MapLocation loc : locs) {
            dist = curLoc.distanceSquaredTo(loc);
            if (dist > furthestDist) {
                furthest = loc;
                furthestDist = dist;
            }
        }
        return furthest;
    }

    public static RobotInfo getClosest(RobotInfo[] robots, MapLocation curLoc) {
        RobotInfo closest = robots[0];
        int closestDist = 999999;
        int dist;
        for (RobotInfo robot : robots) {
            dist = curLoc.distanceSquaredTo(robot.getLocation());
            if (dist < closestDist) {
                closest = robot;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static RobotInfo getFurthest(RobotInfo[] robots, MapLocation curLoc) {
        RobotInfo furthest = robots[0];
        int furthestDist = 0;
        int dist;
        for (RobotInfo robot : robots) {
            dist = curLoc.distanceSquaredTo(robot.getLocation());
            if (dist > furthestDist) {
                furthest = robot;
                furthestDist = dist;
            }
        }
        return furthest;
    }

    public static MapInfo getClosest(MapInfo[] maps, MapLocation curLoc) {
        MapInfo closest = maps[0];
        int closestDist = 999999;
        int dist;
        for (MapInfo map : maps) {
            dist = curLoc.distanceSquaredTo(map.getMapLocation());
            if (dist < closestDist) {
                closest = map;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static MapInfo getFurthest(MapInfo[] maps, MapLocation curLoc) {
        MapInfo furthest = maps[0];
        int furthestDist = 0;
        int dist;
        for (MapInfo map : maps) {
            dist = curLoc.distanceSquaredTo(map.getMapLocation());
            if (dist > furthestDist) {
                furthest = map;
                furthestDist = dist;
            }
        }
        return furthest;
    }

    public static FlagInfo getClosest(FlagInfo[] flags, MapLocation curLoc) {
        FlagInfo closest = flags[0];
        int closestDist = 999999;
        int dist;
        for (FlagInfo flag : flags) {
            dist = curLoc.distanceSquaredTo(flag.getLocation());
            if (dist < closestDist) {
                closest = flag;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static FlagInfo getFurthest(FlagInfo[] flags, MapLocation curLoc) {
        FlagInfo furthest = flags[0];
        int furthestDist = 0;
        int dist;
        for (FlagInfo flag : flags) {
            dist = curLoc.distanceSquaredTo(flag.getLocation());
            if (dist > furthestDist) {
                furthest = flag;
                furthestDist = dist;
            }
        }
        return furthest;
    }

    public static Direction dirFrom(int dx, int dy) {
        for (Direction dir : Direction.values()) {
            if (dir.dx == dx && dir.dy == dy)
                return dir;
        }
        return Direction.CENTER;
    }

    // Pair
    public static class Pair<T, U> {
        public final T a;
        public final U b;

        public Pair(T a, U b) {
            this.a = a;
            this.b = b;
        }
    }
}
