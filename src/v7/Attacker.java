package v7;

import battlecode.common.*;

import static v7.Pathfinding.*;
import static v7.Pathfinding.calculateDistance;
import static v7.RobotPlayer.*;
import static v7.Util.getClosest;
import static v7.Util.getFurthest;

import v7.Constants;

public class Attacker {

    public static void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if(!rc.isSpawned()) return;

        RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo flagHolder = null;
        for (RobotInfo ally : allyRobots) {
            if (ally.hasFlag) {
                flagHolder = ally;
                moveTowards(rc, curLoc, flagHolder.getLocation().add(directions[rng.nextInt(8)]).add(directions[rng.nextInt(8)]), true);
            }
         }

        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // If we are holding an enemy flag, singularly focus on moving towards
        // an ally spawn zone to capture it!

        if (rc.hasFlag()) {
//            moveTowards(rc, curLoc, getClosest(rc.getAllySpawnLocations(), curLoc), 3);
            moveTowards(rc, curLoc, getClosest(rc.getAllySpawnLocations(), curLoc), true);
        }

        else if (enemyRobots.length > 0) {
            // Enemies nearby, deal with this first
            // Find the nearest enemy robot

            RobotInfo nearestEnemy = enemyRobots[0];
            int nearestDistance = 99999;
            int dist;
            for (int i = 1; i < enemyRobots.length; i++) {
                if (enemyRobots[i].hasFlag) { // Target enemy flagholders
                    nearestEnemy = enemyRobots[i];
                    break;
                }
                dist = calculateDistance(rc.getLocation(), enemyRobots[i].getLocation());
                if (dist < nearestDistance) {
                    nearestEnemy = enemyRobots[i];
                    nearestDistance = dist;
                }
            }

            if (rc.getHealth() < 300)
                moveTowards(rc, curLoc, curLoc.add(curLoc.directionTo(nearestEnemy.getLocation()).opposite()), true);

            dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
            if (dist < 9 || dist > 16) // If we move forward to attack they will get the first hit
                moveTowards(rc, curLoc, nearestEnemy.getLocation(), true); // Try to move towards the nearest enemy
            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }
        }

        // If we are not holding an enemy flag, let's go to the nearest one
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if (nearbyFlags.length > 0) {
            FlagInfo firstFlag = nearbyFlags[0];
            MapLocation flagLoc = firstFlag.getLocation();
            moveTowards(rc, curLoc, flagLoc, true);

        } else {
            MapLocation[] locs = {
                    Util.getLocationInSharedArray(rc, 9), Util.getLocationInSharedArray(rc, 10), Util.getLocationInSharedArray(rc, 11)
            };
            MapLocation closestFlag = getClosest(locs, curLoc);
            moveTowards(rc, curLoc, closestFlag, true);
        }

        if (flagHolder != null && rc.canHeal(flagHolder.getLocation()))
            rc.heal(flagHolder.getLocation());
    }
}
