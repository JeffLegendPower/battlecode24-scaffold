package v8.robots;

import battlecode.common.*;
import com.sun.tools.internal.jxc.ap.Const;
import scala.collection.immutable.Stream;

import static v7.RobotPlayer.rng;
import static v8.Pathfinding.moveTowards;
import static v8.RobotPlayer.directions;
import v8.RobotPlayer;
import v8.Pathfinding;
import v8.Utils;

import v8.Constants;

public class Attacker extends AbstractRobot{
    public static int attackerID = -1;
    public static MapLocation currentTarget = null;
    public static int spawnLocsSearched = 0;
    public static int timeSinceLastAttacked = 0;
    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        attackerID = rc.readSharedArray(Constants.SharedArray.numberAttackers);
        rc.writeSharedArray(Constants.SharedArray.numberAttackers, rc.readSharedArray(Constants.SharedArray.numberAttackers) + 1);
        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {

        if (attackerID == rc.readSharedArray(Constants.SharedArray.currentAttackLeader)) {
            // orchestrate
            timeSinceLastAttacked = rc.readSharedArray(Constants.SharedArray.timeSinceLastAttack);
            if (rc.readSharedArray(Constants.SharedArray.attackersHaveFlag) != 0) {
                currentTarget = Utils.getClosest(rc.getAllySpawnLocations(), curLoc);
                spawnLocsSearched += 1;
            } else if (timeSinceLastAttacked > 100){
                if (spawnLocsSearched < 3) {
                    MapLocation myspawn = rc.getAllySpawnLocations()[spawnLocsSearched * 9 + 4];
                    currentTarget = new MapLocation(rc.getMapWidth() - myspawn.x, rc.getMapHeight() - myspawn.y);
                }
            } else {
                currentTarget = rc.getAllySpawnLocations()[rng.nextInt(3) * 9 + 4];
            }
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.currentAttackerTarget, currentTarget);
            System.out.println("Attacker current: " + currentTarget + " sdf se " + timeSinceLastAttacked + " asdf " + attackerID);
            timeSinceLastAttacked += 1;
            if (timeSinceLastAttacked > 200) {
                timeSinceLastAttacked = 0;
            }
            rc.writeSharedArray(Constants.SharedArray.timeSinceLastAttack, timeSinceLastAttacked);
        }
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (rc.hasFlag()) {
            System.out.println("I AHVE A FLAG!!" + attackerID);
            rc.writeSharedArray(Constants.SharedArray.attackersHaveFlag, attackerID);
            moveTowards(rc, curLoc, currentTarget);
            return;
        } else if (rc.readSharedArray(Constants.SharedArray.attackersHaveFlag) == attackerID) {
            // we no longer have the flag
            rc.writeSharedArray(Constants.SharedArray.attackersHaveFlag, 0);
        }

        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if (nearbyFlags.length > 0) {
            MapLocation flagLoc = nearbyFlags[0].getLocation();
            System.out.println("I got a flag!!! " + flagLoc+" i am at: " + curLoc);
            rc.setIndicatorDot(flagLoc, 225, 0, 0);
            if (rc.canPickupFlag(flagLoc)) {
                rc.pickupFlag(flagLoc);
                System.out.println("I picke dup flag " + flagLoc);
            }
        }

        if (enemyRobots.length > 0) {
            // Enemies nearby, deal with this first
            // Find the nearest enemy robot

            RobotInfo nearestEnemy = Utils.getClosest(enemyRobots, curLoc);

            if (rc.getHealth() < 300)
                moveTowards(rc, curLoc, curLoc.add(curLoc.directionTo(nearestEnemy.getLocation()).opposite()));

            int dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
            if (dist < 9 || dist > 16) // If we move forward to attack they will get the first hit
                moveTowards(rc, curLoc, nearestEnemy.getLocation()); // Try to move towards the nearest enemy

            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }
        }

        // If we are not holding an enemy flag, let's go to the nearest one
        //flagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if (nearbyFlags.length > 0) {
            FlagInfo firstFlag = nearbyFlags[0];
            MapLocation flagLoc = firstFlag.getLocation();
            moveTowards(rc, curLoc, flagLoc);
        } else {
            moveTowards(rc, curLoc, Utils.getLocationInSharedArray(rc, Constants.SharedArray.currentAttackerTarget));
        }

        RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo flagHolder = null;
        for (RobotInfo ally : allyRobots) {
            if (ally.hasFlag) {
                flagHolder = ally;
                moveTowards(rc, curLoc, flagHolder.getLocation().add(directions[rng.nextInt(8)]).add(directions[rng.nextInt(8)]));
            }
        }

        if (flagHolder != null && rc.canHeal(flagHolder.getLocation()))
            rc.heal(flagHolder.getLocation());

    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
