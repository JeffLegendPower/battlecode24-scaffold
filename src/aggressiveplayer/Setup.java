package aggressiveplayer;

import battlecode.common.*;

import java.util.ArrayList;

import static aggressiveplayer.Util.*;

public class Setup {
    public static void setupFlag(RobotController rc) throws GameActionException {
        MapLocation robotPos = rc.getLocation();
        Direction dirToMove;

        if (rc.getMapWidth() - robotPos.x > robotPos.x) {
            if (rc.getMapHeight() - robotPos.y > robotPos.y) {
                dirToMove = Direction.SOUTHWEST;
            } else {
                dirToMove = Direction.NORTHWEST;
            }
        } else {
            if (rc.getMapHeight() - robotPos.y > robotPos.y) {
                dirToMove = Direction.SOUTHEAST;
            } else {
                dirToMove = Direction.NORTHEAST;
            }
        }
        if (rc.canMove(dirToMove)) {
            rc.move(dirToMove);
        }

        // place traps and stuff
        if (robotPos.x == 0 || robotPos.x == rc.getMapWidth()-1 || robotPos.y == 0 || robotPos.y == rc.getMapHeight()-1) {
            if (rc.hasFlag()) {
                rc.dropFlag(robotPos);
                int i;
                for (i=1;i<4;i++) {
                    if (flagDefaultLocations[i-1] == null) {
                        flagDefaultLocations[i-1] = robotPos;
                        break;
                    }
                }
                rc.writeSharedArray(i, locToInt(robotPos, 0));
            }
            protectAdjacents(rc);
        }
    }

    public static void protectAdjacents(RobotController rc) throws GameActionException {
        MapLocation robotPos = rc.getLocation();
        for (int i=0; i<8; i++) {
            MapLocation adjacent = robotPos.add(directions[i]);
            if (rc.canDig(adjacent)) {
                rc.dig(adjacent);
                return;
            } else {
                rc.setIndicatorDot(adjacent, 0, 0, 255);
            }
            if (rc.canBuild(TrapType.EXPLOSIVE, adjacent)) {
                rc.build(TrapType.EXPLOSIVE, adjacent);
                return;
            } else {
                rc.setIndicatorDot(adjacent, 255, 0, 0);
            }
        }
    }

    public static void run(RobotController rc) throws GameActionException {
        MapLocation robotPos = rc.getLocation();

        // know the default flag locations
        for (int i=0; i<3; i++) {
            if (flagDefaultLocations[i] == null) {
                int v = rc.readSharedArray(i + 1);
                if (intIsLoc(v)) {
                    MapLocation m = intToLoc(v);
                    flagDefaultLocations[i] = m;
                }
            } else {
                rc.setIndicatorDot(flagDefaultLocations[i], 0, 255, 0);
            }
        }

        // flag setup
        if (rc.canPickupFlag(robotPos)) {
            if (!(robotPos.x == 0 || robotPos.x == rc.getMapWidth()-1 || robotPos.y == 0 || robotPos.y == rc.getMapHeight()-1)) {
                rc.pickupFlag(robotPos);
                liableForFlag = true;
            }
        }
        if (liableForFlag) {
            setupFlag(rc);
            return;
        }

        if (rc.getRoundNum() >= 140) {  // 140-200
            // get the crumbs and fill in the water
            for (int i=0; i<8; i++) {
                MapLocation adjacent = robotPos.add(directions[i]);
                if (!rc.canSenseLocation(adjacent)) {
                    continue;
                }
                if (rc.canFill(adjacent)) {
                    rc.fill(adjacent);
                    return;
                }
                if (rc.senseMapInfo(adjacent).getCrumbs() > 0) {
                    if (rc.canMove(directions[i])) {
                        rc.move(directions[i]);
                        return;
                    }
                }
            }

            // place traps
            MapLocation centerLocation = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
            if (rc.getLocation().isWithinDistanceSquared(centerLocation, 16)) {
                if (rc.canBuild(TrapType.EXPLOSIVE, robotPos)) {
                    rc.build(TrapType.EXPLOSIVE, robotPos);
                }
            }
        }

        // moving
        MapLocation centerLocation = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        MapLocation[] movePriorities;
        if (rc.getRoundNum() <= 50) {  // 1-50
            movePriorities = sortAdjacents(robotPos, (a) -> spawnLocation.distanceSquaredTo(a));
        } else if (rc.getRoundNum() <= 192) {  // 51-192
            movePriorities = sortAdjacents(robotPos, (a) -> centerLocation.distanceSquaredTo(a));
        } else {  // 193-200
            movePriorities = sortAdjacents(robotPos, (a) -> spawnLocation.distanceSquaredTo(a), true);
        }
        for (int i=0; i<8; i++) {
            Direction dirToMove = robotPos.directionTo(movePriorities[i]);
            if (rc.canMove(dirToMove)) {
                rc.move(dirToMove);
                return;
            }
        }
    }
}
