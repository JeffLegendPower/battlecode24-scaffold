package microplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import java.util.*;
import java.util.function.Function;

import static microplayer.General.*;

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

    static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
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

    public static MapLocation[] genBugNavAroundPath(MapLocation robotStart, MapLocation wallStart, MapLocation goal) {
        ArrayDeque<MapLocation> seen = new ArrayDeque<>();
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
        Direction d = robotStart.add(dirLeft).distanceSquaredTo(goal) > robotStart.add(dirRight).distanceSquaredTo(goal) ? dirRight : dirLeft;
        MapLocation robotCurrent = robotStart.add(d);
        Direction prevDirection = d;
        if (d == dirLeft) {  // clockwise
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
                    if (rc.onTheMap(added) && (mapped[nx][ny] & 0b11) == 0b01) { // on the map, and is a wall
                        d = d.rotateLeft();
                        continue;
                    }
                    break;
                }
                if (d != prevDirection) {
                    seen.add(robotCurrent);
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
                    if (rc.onTheMap(added) && (mapped[nx][ny] & 0b11) == 0b01) { // on the map, and is a wall
                        d = d.rotateRight();
                        continue;
                    }
                    break;
                }
                if (d != prevDirection) {
                    seen.add(robotCurrent);
                    prevDirection = d;
                }
                robotCurrent = robotCurrent.add(d);
                if (robotCurrent.equals(robotStart)) {
                    break;
                }
            }
        }
        return seen.toArray(new MapLocation[0]);
    }
}
