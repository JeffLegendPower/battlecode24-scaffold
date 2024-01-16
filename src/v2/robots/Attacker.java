package v2.robots;

import battlecode.common.*;
import v2.Constants;
import v2.Utils;
import v2.robots.AbstractRobot;

import java.util.Arrays;
import java.util.Comparator;

import static v2.Pathfinding.*;

public class Attacker extends AbstractRobot {

    public static MapLocation lastAttackerTarget = null;
    public static MapLocation[] attackerTargets;
    public static boolean hasRetrievedFlag = false;
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLoc);
        MapLocation[] mySpawns = rc.getAllySpawnLocations();

        if (corner == null) {
            System.out.println("An attacker was created without corner being found");
            return false; // it needs the corner to organize targets
        }

        // TODO: this could be bugged in some scenarios when they choose the accessible corner that too close to the dam
        attackerTargets = new MapLocation[] {
                new MapLocation(rc.getMapWidth() - mySpawns[0].x, rc.getMapHeight() - mySpawns[0].y),
                new MapLocation(rc.getMapWidth() - mySpawns[1*9+4].x, rc.getMapHeight() - mySpawns[1*9+4].y),
                new MapLocation(rc.getMapWidth() - mySpawns[2*9+4].x, rc.getMapHeight() - mySpawns[2*9+4].y),
        };

        Arrays.sort(attackerTargets, Comparator.comparingInt(a -> a.distanceSquaredTo(corner)));

        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearestAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo nearestEnemy = Utils.getClosest(nearestEnemies, curLoc);
        RobotInfo nearestAlly = Utils.getClosest(nearestAllies, curLoc);
        FlagInfo[] nearestFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        MapLocation currentTarget = attackerTargets[rc.readSharedArray(Constants.SharedArray.globalAttackTarget)];

        if (rc.hasFlag()) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, curLoc);
            for (MapLocation spawn : rc.getAllySpawnLocations()) {
                if (curLoc.isAdjacentTo(spawn) && !hasRetrievedFlag) {
                    rc.writeSharedArray(Constants.SharedArray.globalAttackTarget, Math.min(2, rc.readSharedArray(Constants.SharedArray.globalAttackTarget) + 1));
                    System.out.println("I retrieved a flag! " + rc.getID());
                    hasRetrievedFlag = true;
                }
            }
            moveTowards(rc, curLoc, Utils.getClosest(rc.getAllySpawnLocations(), curLoc), true);
        } else {
            hasRetrievedFlag = false;
        }

        MapLocation flagHolderLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc);
        if (flagHolderLoc != null) {
            moveTowards(rc, curLoc, flagHolderLoc.add(flagHolderLoc.directionTo(curLoc)).add(flagHolderLoc.directionTo(curLoc)), true);

            if (nearestEnemy != null && rc.canAttack(nearestEnemy.getLocation()))
                rc.attack(nearestEnemy.getLocation());
            else if (rc.canHeal(flagHolderLoc))
                rc.heal(flagHolderLoc);
        }

        else if (nearestFlags.length > 0) {
            if (rc.canPickupFlag(nearestFlags[0].getLocation()))
                rc.pickupFlag(nearestFlags[0].getLocation());
            moveTowards(rc, curLoc, nearestFlags[0].getLocation(), true);
        } else if (nearestEnemies.length > 0) {
//            if (rc.canAttack(nearestEnemy.getLocation())) {
//                rc.attack(nearestEnemy.getLocation());
//                //moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
//            } else {
//                int dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
//                if (dist < 9 || dist > 16) // If we move forward to attack we will get the first hit
//                    moveTowards(rc, curLoc, nearestEnemy.getLocation(), true);
//            }
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
                // Micro 1: Run in to attack and then run away right after
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
            } else if (rc.getHealth() >= 600 || nearestAlly == null) { // Micro 2: Only run in if you have enough health
//                int dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
                moveTowards(rc, curLoc, nearestEnemy.getLocation(), true);
            } else {
                moveTowards(rc, curLoc, nearestAlly.getLocation(), true); // Micro 3: Run towards nearest ally if low
            }
        } else {
            if (nearestAllies.length > 0) {
                if (rc.canHeal(nearestAllies[0].getLocation()))
                    rc.heal(nearestAllies[0].getLocation());
            }

//            if (nearestFriends.length > 0 && rng.nextInt(5) == 1) {
//                moveTowards(rc, curLoc, nearestFriends[0].getLocation(), true);
            if (rc.getRoundNum() < 200) {
                moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), true);
            } else {
                moveTowards(rc, curLoc, currentTarget, true);
                lastAttackerTarget = currentTarget;
            }
        }
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) return;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation target = attackerTargets[rc.readSharedArray(Constants.SharedArray.globalAttackTarget)];
        if (target == null) {
            //System.out.println("USING DEFAULT SPAWN");
            super.spawn(rc);
            return;
        }
        //System.out.println("USING ATACKER SPAWN");
        Arrays.sort(spawnLocs, Comparator.comparingInt(a -> a.distanceSquaredTo(target)));
        for (MapLocation spawnLoc : spawnLocs) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                break;
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
