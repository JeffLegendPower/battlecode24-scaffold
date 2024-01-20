package v10;

import battlecode.common.*;

import java.util.ArrayList;

public class Utils {

    public static void storeLocationInSharedArray(RobotController rc, int index, MapLocation location) throws GameActionException {
        rc.writeSharedArray(index, location == null ? 0 : (1 << 15) // 16th bit to 1 to indicate that it's a location and not a default value (0)
                | (location.x << 6) // Bits 7-12 to x
                | location.y); // Bits 1-6 to y
    }

    public static MapLocation getLocationInSharedArray(RobotController rc, int index) throws GameActionException {
        int loc = rc.readSharedArray(index);
        return (loc & (1 << 15)) == 0 ? null : new MapLocation((loc >> 6) & 63, loc & 63);
    }

    public static void storeInfoInSharedArray(RobotController rc, int index, MapInfo info) throws GameActionException {
        if (info == null) {
            rc.writeSharedArray(index, 0);
            return;
        }

        rc.writeSharedArray(index, (1 << 15) // 16th bit to 1 to indicate that it's a location and not a default value (0)
                | (1 << 14) // 15th bit to 1 to indicate that it's a map info and not a map location (0)
                | (info.getTeamTerritory().isPlayer() ? 1 << 13 : 0) // 14th bit to 1 if it's our territory
                | (info.isWall() || info.isDam() ? 1 << 12 : 0) // 13th bit to 1 if it's a wall or dam
                | (info.getMapLocation().x << 6) // Bits 7-12 to x
                | info.getMapLocation().y); // Bits 1-6 to y
    }

    public static void storeBitInSharedArray(RobotController rc, int index, int bit /*0 is right*/, int val) throws GameActionException {
        if (val == 0)
            rc.writeSharedArray(index, rc.readSharedArray(index) | 1 << bit);
        else
            rc.writeSharedArray(index, rc.readSharedArray(index) & ~(1 << bit));
    }

    public static int getBitInSharedArray(RobotController rc, int index, int bit /*0 is first*/) throws GameActionException {
        return (rc.readSharedArray(index) >> bit) & 1;
    }

    public static MapInfo getInfoInSharedArray(RobotController rc, int index) throws GameActionException {
        int info = rc.readSharedArray(index);
        MapLocation loc = (info & (1 << 15)) == 0 ? null : new MapLocation((info >> 6) & 63, info & 63);
        if (loc == null || (info & (1 << 14)) == 0) return null;
        boolean isWallOrDam = (info & (1 << 12)) != 0;
        boolean isOurTerritory = (info & (1 << 13)) != 0;
        return new MapInfo(
                loc,
                isWallOrDam,
                isWallOrDam,
                false,
                0,
                false,
                0,
                TrapType.NONE,
                isOurTerritory ? rc.getTeam() : rc.getTeam().opponent()
        );
    }

    public static MapLocation getClosest(MapLocation[] locs, MapLocation curLoc) {
        MapLocation closest = null;
        int closestDist = 999999;
        int dist;
        for (MapLocation loc : locs) {
            if (loc == null)
                continue;
            dist = curLoc.distanceSquaredTo(loc);
            if (dist < closestDist) {
                closest = loc;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static MapLocation getClosest(ArrayList<MapLocation> locs, MapLocation curLoc, int useless) {
        MapLocation closest = null;
        int closestDist = 999999;
        int dist;
        for (MapLocation loc : locs) {
            if (loc == null)
                continue;
            dist = curLoc.distanceSquaredTo(loc);
            if (dist < closestDist) {
                closest = loc;
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

    public static void incrementSharedArray(RobotController rc, int index) throws GameActionException {
        rc.writeSharedArray(index, rc.readSharedArray(index) + 1);
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

    public static MapLocation clamp(MapLocation loc, RobotController rc) {
        return new MapLocation(Math.max(1, Math.min(rc.getMapWidth() - 1, loc.x)), Math.max(1, Math.min(rc.getMapHeight() - 1, loc.y)));
    }

    public static <T> int indexOf(T[] array, T element) {
        // Create a new array and manually copy elements from the original array
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(element)) return i;
        }
        return -1;
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
