package v11_2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

import static v11_2.General.*;

public class Utils {
    public static <T> T[] shuffleInPlace(T[] list) {
        for (int i1=0; i1<list.length; i1++) {
            int i2 = rng.nextInt(list.length);
            T o1 = list[i1];
            list[i1] = list[i2];
            list[i2] = o1;
        }
        return list;
    }

    public static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
        return list;
    }

    public static MapLocation[] getAdjacents(MapLocation loc) {
        MapLocation[] locations = new MapLocation[8];
        for (int i=0; i<8; i++) {
            locations[i] = loc.add(directions[i]);
        }
        return locations;
    }

    public static MapLocation averageLocation(MapLocation[] locs) {
        int x = 0;
        int y = 0;
        for (MapLocation loc : locs) {
            x += loc.x;
            y += loc.y;
        }
        return new MapLocation(x/locs.length, y/locs.length);
    }

    public static MapLocation[] robotInfosToMapLocations(RobotInfo[] ris) {
        MapLocation[] locs = new MapLocation[ris.length];
        for (int i=0; i<ris.length; i++) {
            locs[i] = ris[i].getLocation();
        }
        return locs;
    }

    public static MapLocation locationInOtherDirection(MapLocation center, MapLocation location) {
        return new MapLocation(2*center.x-location.x, 2*center.y-location.y);
    }

    public static int locToInt(MapLocation ml, int flag1, int flag2) {
        return (1 << 15) | (flag1 << 14) | (flag2 << 13) | (ml.x << 6) | ml.y;
    }

    public static MapLocation intToLoc(int v) {
        return new MapLocation((v >> 6) & 0x3f, v & 0x3f);
    }

    public static boolean flag2(int v) {
        return (v & 0x2000) != 0;
    }

    public static boolean flag1(int v) {
        return (v & 0x4000) != 0;
    }

    public static boolean intIsLoc(int v) {
        return (v & 0x8000) != 0;
    }

    public static MapLocation readLocationFromShared(int index) throws GameActionException {
        int v = rc.readSharedArray(index);
        return intIsLoc(v) ? intToLoc(v) : null;
    }

    public static void writeLocationToShared(int index, MapLocation loc, int flag1, int flag2) throws GameActionException {
        rc.writeSharedArray(index, locToInt(loc, flag1, flag2));
    }

    public static <T> boolean contains(T[] list, T item) {
        for (T itemCheck : list) {
            if (item.equals(itemCheck)) {
                return true;
            }
        }
        return false;
    }

    public static boolean nextToWall(MapLocation l) {
        return l.x == 0 || l.y == 0 || l.x == mapWidth - 1 || l.y == mapHeight - 1;
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
}
