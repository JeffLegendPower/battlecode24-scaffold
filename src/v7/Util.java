package v7;

import battlecode.common.FlagInfo;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Util {

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
}
