package aggressiveplayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import static aggressiveplayer.Util.*;
import static aggressiveplayer.Util.directions;

public class Game {
    static ArrayList<MapLocation> visitedForRunback = new ArrayList<>();
    static int lastMoveSuggestion = -1;
    static int suggestionTimer = 0;
    static int turnsSinceLastMoveRunningBack = 0;

    public static void respawn() {
        visitedForRunback.clear();
        turnsSinceLastMoveRunningBack = 0;
        suggestionTimer = 0;
        liableForFlag = false;
    }

    public static void globalUpgrades(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 750) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
            }
        }
        if (rc.getRoundNum() == 1500) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }
        }
    }

    public static void runBackWithFlag(RobotController rc) throws GameActionException {
        MapLocation robotPos = rc.getLocation();
        while (visitedForRunback.size() > 200) {
            visitedForRunback.remove(200);
        }
        MapLocation closestSpawn = sortPositions(allySpawnLocs, (pos) -> pos.distanceSquaredTo(robotPos))[0];
        for (MapLocation visitedLoc : visitedForRunback) {
            rc.setIndicatorDot(visitedLoc, 255, 255, 0);
        }

        MapLocation[] adjacents = sortAdjacents(robotPos, (a) -> a.distanceSquaredTo(closestSpawn));
        for (int i=0; i<8; i++) {
            MapLocation adjacent = adjacents[i];
            Direction dirToMove = robotPos.directionTo(adjacent);
            if (!rc.canMove(dirToMove)) {
                rc.setIndicatorDot(adjacent, 255, 0, 0);
                continue;
            }
            if (visitedForRunback.contains(adjacent)) {
                continue;
            }
            visitedForRunback.add(0, robotPos);
            rc.move(dirToMove);
            turnsSinceLastMoveRunningBack = 0;
            rc.setIndicatorLine(robotPos, closestSpawn, 255, 0, 0);
        }
        if (turnsSinceLastMoveRunningBack > 5) {
            visitedForRunback.clear();
            visitedForRunback.add(robotPos);
            turnsSinceLastMoveRunningBack = 0;
        }
        turnsSinceLastMoveRunningBack += 1;
    }

    public static void run(RobotController rc) throws GameActionException {
        MapLocation robotPos = rc.getLocation();
        globalUpgrades(rc);

        // you are a liability...
        if (liableForFlag) {
            Setup.protectAdjacents(rc);
            return;
        }

        // run back!!
        if (rc.hasFlag()) {
            runBackWithFlag(rc);
            return;
        }

        // move suggestedly
        int v = rc.readSharedArray(0);
        if (intIsLoc(v)) {
            MapLocation suggestedPos = intToLoc(v);
            Direction moveDir = robotPos.directionTo(suggestedPos);
            if (rng.nextInt(5)+2 > suggestionTimer) {
                if (rc.canMove(moveDir)) {
                    rc.move(moveDir);
                    robotPos = robotPos.add(moveDir);
                } else {
                    if (rc.canFill(robotPos)) {
                        rc.fill(robotPos);
                    }
                }
            }
            if (v != lastMoveSuggestion) {
                suggestionTimer = 0;
                lastMoveSuggestion = v;
            }
            suggestionTimer += 1;
        }

        // see the flag
        FlagInfo[] nearbyEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        for (FlagInfo enemyFlag : nearbyEnemyFlags) {
            if (Objects.equals(enemyFlag.getLocation(), intToLoc(lastMoveSuggestion))) {
                continue;
            }
            rc.writeSharedArray(0, locToInt(enemyFlag.getLocation(), 0));
            break;
        }

        // grab the flag
        for (Direction dir : directions) {
            MapLocation adjacent = robotPos.add(dir);
            if (rc.canPickupFlag(adjacent)) {
                rc.pickupFlag(adjacent);
            }
        }

        // heal others
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(2, rc.getTeam());
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
        Arrays.sort(friendlyRobots, Comparator.comparingInt((a) -> a.getHealth()));
        if (enemyRobots.length == 0) {
            for (RobotInfo ri : friendlyRobots) {
                if (rc.canHeal(ri.getLocation())) {
                    rc.heal(ri.getLocation());
                }
            }
        } else {
            // attack
            for (RobotInfo enemyRI : enemyRobots) {
                if (rc.canAttack(enemyRI.getLocation())) {
                    rc.attack(enemyRI.getLocation());
                    if (rc.canMove(robotPos.directionTo(enemyRI.getLocation()))) {
                        rc.move(robotPos.directionTo(enemyRI.getLocation()));
                    }
                    break;
                }
            }
        }

        // go far away from the spawn to try to find the bread
        MapLocation[] adjacents = sortAdjacents(robotPos, (a) -> spawnLocation.distanceSquaredTo(a), true);
        for (int i=0; i<8; i++) {
            MapLocation adjacent = adjacents[i];
            if (rng.nextInt(5) == 0) {
                continue;
            }
            Direction dirToMove = robotPos.directionTo(adjacent);
            if (rc.canMove(dirToMove)) {
                rc.move(dirToMove);
                return;
            } else {
                for (MapLocation loc : flagDefaultLocations) {
                    if (loc != null) {
                        if (adjacent.isWithinDistanceSquared(loc, 3)) {
                            return;
                        }
                    }
                }
                if (rc.canFill(adjacent)) {
                    rc.fill(adjacent);
                }
            }
        }
    }
}
