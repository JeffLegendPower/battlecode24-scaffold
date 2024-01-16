package v8.robots;

import battlecode.common.*;
import v8.Constants;
import v8.Utils;

import static v8.Pathfinding.moveAway;
import static v8.Pathfinding.moveTowards;
import static v8.RobotPlayer.rng;

public class Attacker extends AbstractRobot{

    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {

        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearestFriends = rc.senseNearbyRobots(-1, rc.getTeam());

        RobotInfo nearestEnemy = Utils.getClosest(nearestEnemies, curLoc);

        FlagInfo[] nearestFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        if (rc.hasFlag()) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, curLoc);
            moveTowards(rc, curLoc, Utils.getClosest(rc.getAllySpawnLocations(), curLoc), true);
        }

        MapLocation flagHolderLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc);
        if (flagHolderLoc != null) {
            moveTowards(rc, curLoc, flagHolderLoc.add(flagHolderLoc.directionTo(curLoc)).add(flagHolderLoc.directionTo(curLoc)), true);

            if (nearestEnemy != null && rc.canAttack(nearestEnemy.getLocation()))
                rc.attack(nearestEnemy.getLocation());
            else if (rc.canHeal(flagHolderLoc))
                rc.heal(flagHolderLoc);
        }

        if (nearestFlags.length > 0) {
            if (rc.canPickupFlag(nearestFlags[0].getLocation())) {
                rc.pickupFlag(nearestFlags[0].getLocation());
            }
            moveTowards(rc, curLoc, nearestFlags[0].getLocation(), true);
        }
        else if (nearestEnemies.length > 0) {
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
            } else {
                int dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
                if (dist < 9 || dist > 16) // If we move forward to attack we will get the first hit
                    moveTowards(rc, curLoc, nearestEnemy.getLocation(), true);
            }
        } else {
            if (nearestFriends.length > 0) {
                if (rc.canHeal(nearestFriends[0].getLocation()))
                    rc.heal(nearestFriends[0].getLocation());
            }
            if (nearestFriends.length > 0 && rng.nextInt(5) == 1) {
                moveTowards(rc, curLoc, nearestFriends[0].getLocation(), true);
            } else {
                MapLocation furthestSpawn = Utils.getFurthest(rc.getAllySpawnLocations(), curLoc);
                moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() - furthestSpawn.x, rc.getMapHeight() - furthestSpawn.y), true);
            }
        }

    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
