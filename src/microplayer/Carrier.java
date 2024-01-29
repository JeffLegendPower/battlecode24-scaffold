package microplayer;

import battlecode.common.*;

import java.util.ArrayList;

import static microplayer.General.*;
import static microplayer.General.enemyFlagLocations;
import static microplayer.Utility.*;
import static microplayer.Utility.writeLocationToShared;

public class Carrier {
    public static ArrayList<MapLocation> visited = new ArrayList<>();

    /**
     returns true if a flag was picked up (the robot cannot do any more actions anyways)
     */
    public static boolean senseAndPickupFlags() throws GameActionException {
        for (FlagInfo flagInfo : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
            MapLocation flagLoc = flagInfo.getLocation();
            if (flagInfo.isPickedUp()) {
                continue;
            }
            // find new flags that might not have been previously seen
            findNewFlags: for (int i=0; i<3; i++) {
                if (enemyFlagLocations[i] == null) {
                    for (MapLocation carrierLoc : carrierLocations) {
                        if (carrierLoc == null) {
                            continue;
                        }
                        if (carrierLoc.distanceSquaredTo(flagLoc) <= 2) {
                            break findNewFlags;
                        }
                    }
                    enemyFlagLocations[i] = flagInfo.getLocation();
                    int v = locToInt(flagInfo.getLocation(), 0, 0);
                    if (rc.canWriteSharedArray(i+1, v)) {
                        rc.writeSharedArray(i+1, v);
                        System.out.println("saw flag " + (i+1) + " at " + intToLoc(v));
                    }
                    break;
                } else if (enemyFlagLocations[i].equals(flagInfo.getLocation())) {
                    break;
                }
            }

            // attempt to grab the flag
            if (flagInfo.getLocation().distanceSquaredTo(robotLoc) <= 2) {
                if (flagTransferCooldown > 0) {
                    continue;
                }
                if (rc.canPickupFlag(flagInfo.getLocation())) {
                    rc.pickupFlag(flagInfo.getLocation());
                    isCarrier = true;
                    carrierDestination = sortableCenterSpawnLocations[0];
                    visited.add(robotLoc);
                    for (int i=0; i<3; i++) {
                        if (flagInfo.getLocation().equals(enemyFlagLocations[i])) {
                            flagCarrierIndex = i;
                            System.out.println("flag " + (i+1) + " picked up at " + flagInfo.getLocation());
                            writeLocationToShared(4+i, robotLoc, 0, 0);
                            writeLocationToShared(1+i, enemyFlagLocations[i], 1, 0);
                            return true;
                        }
                        if (carrierLocations[i] == null) {
                            continue;
                        }
                        if (carrierLocations[i].equals(flagInfo.getLocation())) {  // picked up dead carrier's flag
                            flagCarrierIndex = i;
                            System.out.println("flag " + (i+1) + " reacquired at " + flagInfo.getLocation());
                            writeLocationToShared(4+i, robotLoc, 0, 0);
                            writeLocationToShared(1+i, enemyFlagLocations[i], 1, 0);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void onCarrierGameTurn() throws GameActionException {
        MapLocation closestSpawnLoc = sortableCenterSpawnLocations[0];
        MapLocation closestEnemySpawnLoc = mirroredAcrossMapLocation(sortableCenterSpawnLocations[0]);
        rc.setIndicatorDot(closestSpawnLoc, 0, 255, 0);
        rc.setIndicatorString("carrying flag " + (flagCarrierIndex+1));

        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation[] enemyLocations = new MapLocation[enemyInfos.length];

        for (int i=0; i<enemyInfos.length; i++) {
            enemyLocations[i] = enemyInfos[i].getLocation();
        }

        // get which directions to move in
        MapLocation[] sortedMovementDirections;
        if (robotLoc.distanceSquaredTo(closestSpawnLoc) <= 30) {
            sortedMovementDirections = sort(getAdjacents(robotLoc), (loc) -> loc.distanceSquaredTo(carrierDestination));
        } else {
            sortedMovementDirections = sort(getAdjacents(robotLoc), (loc) -> {
                if (enemyLocations.length == 0) {
                    return loc.distanceSquaredTo(carrierDestination);
                }
                int avgEnemyDistance = 0;
                // avoid enemies
                for (MapLocation enemyLocation : enemyLocations) {
                    avgEnemyDistance += enemyLocation.distanceSquaredTo(loc);
                }
                return loc.distanceSquaredTo(carrierDestination) - avgEnemyDistance / enemyLocations.length - loc.distanceSquaredTo(closestEnemySpawnLoc);
            });
        }

        // debug movement weights
        for (int i=0; i<8; i++) {
            rc.setIndicatorDot(sortedMovementDirections[i], 255-31*i, 0, 0);
        }

        // move the flag carrier
        MapLocation newRobotLoc = null;
        for (MapLocation adjacent : sortedMovementDirections) {
            if (visited.contains(adjacent)) {
                continue;
            }
            Direction d = robotLoc.directionTo(adjacent);
            if (rc.canMove(d)) {
                rc.move(d);
                newRobotLoc = robotLoc.add(d);
                lastTimeSinceFlagCarrierMoved = 0;
                if (!rc.hasFlag()) {  // flag deposited
                    System.out.println("deposited flag " + (flagCarrierIndex+1));
                    writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 1);
                    isCarrier = false;
                    flagCarrierIndex = -1;
                    break;
                }
                visited.add(adjacent);
                writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 0);
                break;
            }
        }

        // pass the flag if an ally is in the 2nd ring
        // todo dont pass if stuck near wall just do bugnav
        if (rc.getActionCooldownTurns() < 10 && newRobotLoc != null) {
            if (!robotLoc.isWithinDistanceSquared(closestSpawnLoc, 16)) {
                MapLocation bestDropLocation = null;
                int bestDropDistance = 9999;
                for (MapLocation ringLoc : get2ndSquareRingAroundLocation(newRobotLoc)) {
                    if (ringLoc.distanceSquaredTo(closestSpawnLoc) >= newRobotLoc.distanceSquaredTo(closestSpawnLoc)) {  // further away
                        continue;
                    }
                    if (!rc.onTheMap(ringLoc)) {  // not on the map
                        continue;
                    }
                    if ((mapped[ringLoc.x][ringLoc.y] & 0b10) == 0) {  // wall is there
                        continue;
                    }
                    RobotInfo other = rc.senseRobotAtLocation(ringLoc);
                    if (other == null) {  // no robot there
                        continue;
                    }
                    if (other.team != rc.getTeam()) {  // robot is an enemy
                        continue;
                    }
                    if (other.hasFlag) {  // the other guy has a flag
                        continue;
                    }
                    if (other.health <= rc.getHealth()+75 && other.health < 700) {  // other guy's health is lower
                        continue;
                    }
                    MapLocation locToDrop = newRobotLoc.add(newRobotLoc.directionTo(other.location));
                    if (!rc.canDropFlag(locToDrop)) {
                        break;
                    }
                    System.out.println("passing flag");
                    int dist = closestSpawnLoc.distanceSquaredTo(locToDrop);
                    if (bestDropDistance > dist) {
                        bestDropDistance = dist;
                        bestDropLocation = locToDrop;
                    }
                }
                if (bestDropLocation != null && flagCarrierIndex != -1) {
                    rc.dropFlag(bestDropLocation);
                    isCarrier = false;
                    flagTransferCooldown = 4;
                    rc.writeSharedArray(flagCarrierIndex + 4, locToInt(bestDropLocation, 1, 0));
                    hasCarrierDroppedFlag[flagCarrierIndex] = true;
                    lastDroppedFlagValue = -1;
                    flagCarrierIndex = -1;
                }
            }
        }

        // pass the flag if stuck
        if (rc.isSpawned()) {
            if (rc.getMovementCooldownTurns() < 10 && rc.getActionCooldownTurns() < 10) {
                for (MapLocation d : sort(getAdjacents(robotLoc), (loc) -> loc.distanceSquaredTo(closestSpawnLoc))) {
                    if (!rc.onTheMap(d)) {
                        continue;
                    }
                    RobotInfo ally = rc.senseRobotAtLocation(d);
                    if (ally == null) {
                        continue;
                    }
                    if (!ally.getTeam().equals(rc.getTeam())) {
                        continue;
                    }
                    if (ally.hasFlag()) {
                        continue;
                    }
                    if (!rc.canDropFlag(ally.getLocation())) {
                        break;
                    }
                    rc.dropFlag(ally.getLocation());
                    isCarrier = false;
                    flagTransferCooldown = 4;
                    rc.writeSharedArray(flagCarrierIndex+4, locToInt(ally.getLocation(), 1, 0));
                    hasCarrierDroppedFlag[flagCarrierIndex] = true;
                    lastDroppedFlagValue = -1;
                    flagCarrierIndex = -1;
                    break;
                }
            }
        }

        // this is part of checking if the flag carrier moved recently and passing the flag if it hasn't
        lastTimeSinceFlagCarrierMoved++;
        if (lastTimeSinceFlagCarrierMoved > 5) {
            lastTimeSinceFlagCarrierMoved = 0;
            visited.clear();
        }
    }
}
