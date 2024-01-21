package rushplayer;

import battlecode.common.*;
import rushplayer.map.BaseMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

import static rushplayer.General.*;

public class Utility {

    static <T> T[] shuffleInPlace(T[] list) {
        for (int i1=0; i1<list.length; i1++) {
            int i2 = rng.nextInt(list.length);
            T o1 = list[i1];
            list[i1] = list[i2];
            list[i2] = o1;
        }
        return list;
    }

    static <T> T[] sort(T[] list, Function<T, Integer> valueFn, boolean reverse) {
        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
        if (reverse) {
            for (int i=0; i<list.length/2; i++) {
                T temp = list[i];
                list[i] = list[list.length-1];
                list[list.length-1] = temp;
            }
        }
        return list;
    }

    static MapLocation[] getAdjacents(MapLocation loc) {
        MapLocation[] locations = new MapLocation[8];
        for (int i=0; i<8; i++) {
            locations[i] = loc.add(directions[i]);
        }
        return locations;
    }

    static MapLocation averageLocation(MapLocation[] locs) {
        int x = 0;
        int y = 0;
        for (MapLocation loc : locs) {
            x += loc.x;
            y += loc.y;
        }
        return new MapLocation(x/locs.length, y/locs.length);
    }

    static MapLocation[] robotInfosToMapLocations(RobotInfo[] ris) {
        MapLocation[] locs = new MapLocation[ris.length];
        for (int i=0; i<ris.length; i++) {
            locs[i] = ris[i].getLocation();
        }
        return locs;
    }

    static MapLocation locationInOtherDirection(MapLocation center, MapLocation location) {
        return new MapLocation(2*center.x-location.x, 2*center.y-location.y);
    }

    static int locToInt(MapLocation ml, int bitflag) {
        return (1 << 15) + (bitflag << 14) + (ml.x << 7) + ml.y;
    }

    static MapLocation intToLoc(int v) {
        return new MapLocation((v >> 7) & 0x7f, v & 0x7f);
    }

    static boolean bitflag(int v) {
        return (v & 0x4000) != 0;
    }

    static boolean intIsLoc(int v) {
        return (v & 0x8000) != 0;
    }

    static MapLocation readLocationFromShared(int index) throws GameActionException {
        int v = rc.readSharedArray(index);
        return intIsLoc(v) ? intToLoc(v) : null;
    }

    static void writeLocationToShared(int index, MapLocation loc, int bitflag) throws GameActionException {
        rc.writeSharedArray(index, locToInt(loc, bitflag));
    }

    static <T> boolean contains(T[] list, T item) {
        for (T itemCheck : list) {
            if (itemCheck == item) {
                return true;
            }
        }
        return false;
    }

    static boolean nextToWall(MapLocation l) {
        return l.x == 0 || l.y == 0 || l.x == map.width - 1 || l.y == map.height - 1;
    }

    static Direction[] getIdealMovementDirections(MapLocation start, MapLocation goal) {
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

    static boolean shownSymmetryWarning = false;

    static MapLocation symmetricLocation(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        int w = map.width == null ? rc.getMapWidth() : map.width;
        int h = map.height == null ? rc.getMapWidth() : map.height;
        if (map.symmetry == BaseMap.MapSymmetry.ROTATIONAL) {
            return new MapLocation(w-1-x, h-1-y);
        } else if (map.symmetry == BaseMap.MapSymmetry.HORIZONTAL) {
            return new MapLocation(x, h-1-y);
        } else if (map.symmetry == BaseMap.MapSymmetry.VERTICAL) {
            return new MapLocation(w-1-x, y);
        } else {
            if (!shownSymmetryWarning) {
                shownSymmetryWarning = true;
                System.out.println("Could not detect map symmetry! " + map.width + " " + map.height);
            }
            return new MapLocation((w/2+(w-1-x))/2, (h/2+(h-1-y))/2);
        }
    }

    static String reprLocations(MapLocation[] locations) {
        StringBuilder b = new StringBuilder(locations.length*5);
        for (MapLocation l : locations) {
            b.append(l);
            b.append("\n");
        }
        return b.toString();
    }

}
