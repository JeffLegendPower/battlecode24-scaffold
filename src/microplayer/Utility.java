package microplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import java.util.*;
import java.util.function.Function;

import static microplayer.General.*;

public class Utility {
    static int stepDistance(MapLocation a, MapLocation b) {
        // distance in how many steps to get there
        return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    static int manhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x-b.x) + Math.abs(a.y-b.y);
    }

    static <T> T[] shuffleInPlace(T[] list) {
        for (int i1=0; i1<list.length; i1++) {
            int i2 = rng.nextInt(list.length);
            T o1 = list[i1];
            list[i1] = list[i2];
            list[i2] = o1;
        }
        return list;
    }

    static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
        return list;
    }

    static MapLocation[] getAdjacents(MapLocation loc) {
        return new MapLocation[]{
                loc.add(directions[0]), loc.add(directions[1]), loc.add(directions[2]), loc.add(directions[3]),
                loc.add(directions[4]), loc.add(directions[5]), loc.add(directions[6]), loc.add(directions[7])
        };
    }

    static MapLocation[] getAttackableLocations(MapLocation robotLoc) {
        MapLocation[] locations = new MapLocation[12];
        locations[8] = robotLoc.translate(2, 0);
        locations[9] = robotLoc.translate(-2, 0);
        locations[10] = robotLoc.translate(0, 2);
        locations[11] = robotLoc.translate(0, -2);
        for (int i=0; i<8; i++) {
            locations[i] = robotLoc.add(directions[i]);
        }
        return locations;
    }

    public static <K,V> Function<K,V> cache(Function<K,V> f, Map<K,V> cache) {
        return k->cache.computeIfAbsent(k, f);
    }

    public static <K,V> Function<K,V> cache(Function<K,V> f) {
        return cache(f, new IdentityHashMap<>());
    }

    static <T> T[] sortWithCache(T[] list, Function<T, Integer> valueFn) {
        Arrays.sort(list, Comparator.comparing(cache(valueFn)));
        return list;
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

    static MapLocation locationInOtherDirection(MapLocation pivot, MapLocation location) {
        return new MapLocation(2 * pivot.x - location.x, 2 * pivot.y - location.y);
    }

    static MapLocation mirroredAcrossMapLocation(MapLocation loc) {
        if (!symmetryWasDetermined) {
            return new MapLocation((mapWidth-loc.x-1+mapWidth/2)/2, (mapHeight-loc.y-1+mapHeight/2)/2);
        }
        if (possibleSymmetries[0]) {  // rotational
            return new MapLocation(mapWidth-loc.x-1, mapHeight-loc.y-1);
        }
        if (possibleSymmetries[1]) {  // up/down
            return new MapLocation(loc.x, mapHeight-loc.y-1);
        }
        if (possibleSymmetries[2]) {  // left/right
            return new MapLocation(mapWidth-loc.x-1, loc.y);
        }
        return centerOfMap;
    }

    static int locToInt(MapLocation ml, int flag1, int flag2) {
        return (1 << 15) | (flag1 << 14) | (flag2 << 13) | (ml.x << 6) | ml.y;
    }

    static MapLocation intToLoc(int v) {
        return new MapLocation((v >> 6) & 0x3f, v & 0x3f);
    }

    static boolean flag2(int v) {
        return (v & 0x2000) != 0;
    }

    static boolean flag1(int v) {
        return (v & 0x4000) != 0;
    }

    static boolean intIsLoc(int v) {
        return (v & 0x8000) != 0;
    }

    static MapLocation readLocationFromShared(int index) throws GameActionException {
        int v = rc.readSharedArray(index);
        return intIsLoc(v) ? intToLoc(v) : null;
    }

    static void writeLocationToShared(int index, MapLocation loc, int flag1, int flag2) throws GameActionException {
        rc.writeSharedArray(index, locToInt(loc, flag1, flag2));
    }

    static <T> boolean contains(T[] list, T item) {
        for (T itemCheck : list) {
            if (item.equals(itemCheck)) {
                return true;
            }
        }
        return false;
    }

    static boolean nextToWall(MapLocation l) {
        return l.x == 0 || l.y == 0 || l.x == mapWidth - 1 || l.y == mapHeight - 1;
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

    static Direction[] getTrulyIdealMovementDirections(MapLocation start, MapLocation goal) {
        // like the above but the duck moves closer 100% of the time
        int sx = start.x;
        int sy = start.y;
        int gx = goal.x;
        int gy = goal.y;

        // 13 cases
        if (sx < gx) {  // rightwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // right > up
                    return new Direction[]{Direction.NORTHEAST, Direction.EAST, Direction.NORTH};
                } else {  // up > right
                    return new Direction[]{Direction.NORTHEAST, Direction.NORTH, Direction.EAST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // right > down
                    return new Direction[]{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH};
                } else {  // down > right
                    return new Direction[]{Direction.SOUTHEAST, Direction.SOUTH, Direction.EAST};
                }
            }
        } else if (sx == gx) {  // already vertically centered
            if (sy < gy) {  // upwards
                return new Direction[]{Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST};
            } else if (sy == gy) {  // dont go anywhere
                return new Direction[]{Direction.CENTER};
            } else {  // downwards
                return new Direction[]{Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHEAST};
            }
        } else {  // leftwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // left > up
                    return new Direction[]{Direction.NORTHWEST, Direction.WEST, Direction.NORTH};
                } else {  // up > left
                    return new Direction[]{Direction.NORTHWEST, Direction.NORTH, Direction.WEST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.WEST, Direction.NORTHWEST, Direction.SOUTHWEST};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // left > down
                    return new Direction[]{Direction.SOUTHWEST, Direction.WEST, Direction.SOUTH};
                } else {  // down > left
                    return new Direction[]{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST};
                }
            }
        }
    }

    public static MapLocation[] get2ndSquareRingAroundLocation(MapLocation l) {
        int x = l.x;
        int y = l.y;
        return new MapLocation[]{
                new MapLocation(x-2, y), new MapLocation(x-2, y+1), new MapLocation(x-2, y+2),
                new MapLocation(x-1, y+2), new MapLocation(x, y+2), new MapLocation(x+1, y+2),
                new MapLocation(x+2, y+2), new MapLocation(x+2, y+1), new MapLocation(x+2, y),
                new MapLocation(x+2, y-1), new MapLocation(x+2, y-2), new MapLocation(x+1, y-2),
                new MapLocation(x, y-2), new MapLocation(x-1, y-2), new MapLocation(x-2, y-2),
                new MapLocation(x-2, y-1)
        };
    }

    public static MapLocation[] genBugNavAroundPath(MapLocation robotStart, MapLocation wallStart, MapLocation goal) {
        ArrayDeque<MapLocation> vertices = new ArrayDeque<>();
        ArrayDeque<MapLocation> allPlaces = new ArrayDeque<>();
        Direction dirLeft = robotStart.directionTo(wallStart);
        for (int i=0; i<8; i++) {
            MapLocation added = robotStart.add(dirLeft);
            int nx = added.x;
            int ny = added.y;
            if ((!rc.onTheMap(added)) || (mapped[nx][ny] & 0b10) == 0) { // not on the map, or is a wall
                dirLeft = dirLeft.rotateLeft();
                continue;
            }
            break;
        }
        Direction dirRight = robotStart.directionTo(wallStart);
        for (int i=0; i<8; i++) {
            MapLocation added = robotStart.add(dirRight);
            int nx = added.x;
            int ny = added.y;
            if ((!rc.onTheMap(added)) || (mapped[nx][ny] & 0b10) == 0) { // not on the map, or is a wall
                dirRight = dirRight.rotateRight();
                continue;
            }
            break;
        }
        Direction d;
        if (bugNavGoingClockwise == null) {
            d = robotStart.add(dirLeft).distanceSquaredTo(goal) > robotStart.add(dirRight).distanceSquaredTo(goal) ? dirRight : dirLeft;
            bugNavGoingClockwise = d == dirLeft;
        } else {
            d = bugNavGoingClockwise ? dirLeft : dirRight;
        }
        MapLocation robotCurrent = robotStart.add(d);
        Direction prevDirection = d;
        if (bugNavGoingClockwise) {  // clockwise
            for (int i = 0; i < 200; i++) {
                for (int j = 0; j < 8; j++) {  // rotate right until is a wall
                    MapLocation added = robotCurrent.add(d);
                    int nx = added.x;
                    int ny = added.y;
                    if (rc.onTheMap(added) && (mapped[nx][ny] & 0b11) != 0b01) { // on the map, and not a wall
                        d = d.rotateRight();
                        continue;
                    }
                    d = d.rotateLeft();
                    break;
                }
                for (int j = 0; j < 8; j++) {  // rotate left until is not a wall
                    MapLocation added = robotCurrent.add(d);
                    int nx = added.x;
                    int ny = added.y;
                    if (!rc.onTheMap(added) || (mapped[nx][ny] & 0b11) == 0b01) { // off the map or is a wall
                        d = d.rotateLeft();
                        continue;
                    }
                    break;
                }
                allPlaces.add(robotCurrent);
                if (d != prevDirection) {
                    vertices.add(robotCurrent);
                    prevDirection = d;
                }
                robotCurrent = robotCurrent.add(d);
                if (robotCurrent.equals(robotStart)) {
                    break;
                }
            }
        } else {  // counter clockwise
            for (int i = 0; i < 200; i++) {
                for (int j = 0; j < 8; j++) {  // rotate left until is a wall
                    MapLocation added = robotCurrent.add(d);
                    int nx = added.x;
                    int ny = added.y;
                    if (rc.onTheMap(added) && (mapped[nx][ny] & 0b11) != 0b01) { // on the map, and not a wall
                        d = d.rotateLeft();
                        continue;
                    }
                    d = d.rotateRight();
                    break;
                }
                for (int j = 0; j < 8; j++) {  // rotate right until is not a wall
                    MapLocation added = robotCurrent.add(d);
                    int nx = added.x;
                    int ny = added.y;
                    if (!rc.onTheMap(added) || (mapped[nx][ny] & 0b11) == 0b01) { // off the map or is a wall
                        d = d.rotateRight();
                        continue;
                    }
                    break;
                }
                allPlaces.add(robotCurrent);
                if (d != prevDirection) {
                    vertices.add(robotCurrent);
                    prevDirection = d;
                }
                robotCurrent = robotCurrent.add(d);
                if (robotCurrent.equals(robotStart)) {
                    break;
                }
            }
        }
        bugNavAllPlaces = allPlaces.toArray(new MapLocation[0]);
        return vertices.toArray(new MapLocation[0]);
    }
}
