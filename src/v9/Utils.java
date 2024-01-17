package v9;

import battlecode.common.*;

import java.util.ArrayList;

public class Utils {

    public static void storeLocationInSharedArray(RobotController rc, int index, MapLocation location) throws GameActionException {
        // 13th bit to 1 to indicate that it's a location and not a default value (0)
        rc.writeSharedArray(index, location == null ? 0 : (1 << 12) | (location.x << 6) | location.y);
    }

    public static MapLocation getLocationInSharedArray(RobotController rc, int index) throws GameActionException {
        int loc = rc.readSharedArray(index);
        return (loc & (1 << 12)) == 0 ? null : new MapLocation((loc >> 6) & 63, loc & 63);
    }

    public static MapLocation getClosest(MapLocation[] locs, MapLocation curLoc) {
        MapLocation closest = null;
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
        MapLocation furthest = null;
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
        RobotInfo closest = null;
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
        RobotInfo furthest = null;
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
        MapInfo closest = null;
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

    public static MapInfo getClosest(ArrayList<MapInfo> maps, MapLocation curLoc) {
        MapInfo closest = null;
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
        MapInfo furthest = null;
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
        FlagInfo closest = null;
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
        FlagInfo furthest = null;
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

    public static RobotInfo lowestHealth(RobotInfo[] robots) {
        int min = 99999;
        RobotInfo minRobot = null;
        for (RobotInfo robot : robots) {
            if (robot.getHealth() < min) {
                min = robot.getHealth();
                minRobot = robot;
            }
        }
        return minRobot;
    }

    public static boolean canBeFilled(RobotController rc, MapLocation loc) throws GameActionException {
        boolean canBeFilled = true;
        for (int i = 0; i < 3; i++) {
            MapLocation flagCornerLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[i]);
            if (flagCornerLoc == null) continue;
            if (loc.isAdjacentTo(flagCornerLoc) || loc.equals(flagCornerLoc)) {
                canBeFilled = false;
                break;
            }
        }
        return canBeFilled;
//        MapLocation flag = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
//        if (flag == null) return false;
//        return loc.distanceSquaredTo(flag) > 2
//                && loc.distanceSquaredTo(flag.translate(0, (flag.y == rc.getMapHeight() - 1) ? -10 : 10)) > 2
//                && loc.distanceSquaredTo(flag.translate((flag.x == rc.getMapWidth() - 1) ? -10 : 10, 0)) > 2;
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
