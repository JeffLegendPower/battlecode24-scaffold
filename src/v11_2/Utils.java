package v11_2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

import static v11_2.RobotPlayer.directions;
import static v11_2.RobotPlayer.rng;

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

    public static int numWithinRadius(RobotInfo[] robots, MapLocation loc, int radiusSquared) {
        int num = 0;
        for(RobotInfo robot : robots) {
            if (robot.getLocation().distanceSquaredTo(loc) <= radiusSquared)
                num++;
        }
        return num;
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
            if (closest == null || dist < closestDist) {
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
            if (furthest == null || dist > furthestDist) {
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

    public static MapLocation clamp(MapLocation loc, RobotController rc) {
        return new MapLocation(Math.max(1, Math.min(rc.getMapWidth() - 1, loc.x)), Math.max(1, Math.min(rc.getMapHeight() - 1, loc.y)));
    }

    public static <T> int indexOf(T[] array, T element) {
        // Create a new array and manually copy elements from the original array
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].equals(element)) return i;
        }
        return -1;
    }

    public static boolean canAttack(RobotController rc, MapLocation curLoc, MapLocation target) {
        return rc.getActionCooldownTurns() < 10 && curLoc.isWithinDistanceSquared(target, 4);
    }


    public static MapLocation[] getAdjacents(MapLocation loc) {
        return new MapLocation[] {
                loc.add(Direction.NORTH),
                loc.add(Direction.NORTHEAST),
                loc.add(Direction.EAST),
                loc.add(Direction.SOUTHEAST),
                loc.add(Direction.SOUTH),
                loc.add(Direction.SOUTHWEST),
                loc.add(Direction.WEST),
                loc.add(Direction.NORTHWEST)
        };
    }

    public static Direction[] generateDeviations(Direction dir) {
        // ex. if dir is NORTH, then return NORTH, NORTHWEST, NORTHEAST, EAST, WEST
        Direction[] dirs = new Direction[5];
        int directionIndex = indexOf(directions, dir);
        dirs[0] = dir;
        dirs[1] = directions[(directionIndex + 7) % 8];
        dirs[2] = directions[(directionIndex + 1) % 8];
        dirs[3] = directions[(directionIndex + 2) % 8];
        dirs[4] = directions[(directionIndex + 6) % 8];
        return dirs;
    }

    public static Direction[] getIdealMovementDirections(MapLocation start, MapLocation goal) {
        int sx = start.x;
        int sy = start.y;
        int gx = goal.x;
        int gy = goal.y;

        // 13 cases
        if (sx < gx) {  // rightwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // right > up
                    return new Direction[]{Direction.NORTHEAST, Direction.EAST, Direction.NORTH, Direction.SOUTHEAST, Direction.NORTHWEST};
                } else {  // up > right
                    return new Direction[]{Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHWEST, Direction.SOUTHEAST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTH, Direction.SOUTH};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // right > down
                    return new Direction[]{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHWEST};
                } else {  // down > right
                    return new Direction[]{Direction.SOUTHEAST, Direction.SOUTH, Direction.EAST, Direction.SOUTHWEST, Direction.NORTHEAST};
                }
            }
        } else if (sx == gx) {  // already vertically centered
            if (sy < gy) {  // upwards
                return new Direction[]{Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.EAST, Direction.WEST};
            } else if (sy == gy) {  // dont go anywhere
                return new Direction[]{Direction.CENTER};
            } else {  // downwards
                return new Direction[]{Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.WEST, Direction.EAST};
            }
        } else {  // leftwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // left > up
                    return new Direction[]{Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.SOUTHWEST, Direction.NORTHEAST};
                } else {  // up > left
                    return new Direction[]{Direction.NORTHWEST, Direction.NORTH, Direction.WEST, Direction.NORTHEAST, Direction.SOUTHWEST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.WEST, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.NORTH, Direction.SOUTH};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // left > down
                    return new Direction[]{Direction.SOUTHWEST, Direction.WEST, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHEAST};
                } else {  // down > left
                    return new Direction[]{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.SOUTHEAST, Direction.NORTHWEST};
                }
            }
        }
    }

    public static <T> T[] shuffleInPlace(T[] list) {
        for (int i1=0; i1<list.length; i1++) {
            int i2 = rng.nextInt(list.length);
            T o1 = list[i1];
            list[i1] = list[i2];
            list[i2] = o1;
        }
        return list;
    }

//    public static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
//        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
//        return list;
//    }
    public static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
        Pair<T, Integer>[] pairs = new Pair[list.length];
        for (int i = 0; i < list.length; i++) {
            pairs[i] = new Pair<>(list[i], valueFn.apply(list[i]));
        }
        Arrays.sort(pairs, Comparator.comparingInt(pair -> pair.b));
        for (int i = 0; i < list.length; i++) {
            list[i] = pairs[i].a;
        }
        return list;
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
