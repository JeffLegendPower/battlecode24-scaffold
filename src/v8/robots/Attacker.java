package v8.robots;

import battlecode.common.*;
import com.sun.tools.internal.jxc.ap.Const;
import scala.collection.immutable.Stream;

import static v8.RobotPlayer.rng;
import static v8.Pathfinding.*;
import static v8.RobotPlayer.directions;
import v8.RobotPlayer;
import v8.Pathfinding;
import v8.Utils;

import v8.Constants;

public class Attacker extends AbstractRobot{

    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {

        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearestFriends = rc.senseNearbyRobots(-1, rc.getTeam());

        FlagInfo[] nearestFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        if (rc.hasFlag()) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, curLoc);
            moveTowards(rc, curLoc, Utils.getClosest(rc.getAllySpawnLocations(), curLoc));
        }

        MapLocation flagHolderLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc);
        if (flagHolderLoc.x != 0 && flagHolderLoc.y != 0) {
            moveTowards(rc, curLoc, flagHolderLoc);
            if (rc.canHeal(flagHolderLoc))
                rc.heal(flagHolderLoc);
        }

        if (nearestFlags.length > 0) {
            if (rc.canPickupFlag(nearestFlags[0].getLocation())) {
                rc.pickupFlag(nearestFlags[0].getLocation());
            }

            moveTowards(rc, curLoc, nearestFlags[0].getLocation(), 10);
        }
        else if (nearestEnemies.length > 0) {
            if (rc.canAttack(nearestEnemies[0].getLocation())) {
                rc.attack(nearestEnemies[0].getLocation());
                moveAway(rc, curLoc, nearestEnemies[0].getLocation());
            } else {
                moveTowards(rc, curLoc, nearestEnemies[0].getLocation(), 10);
            }
        } else {
            if (nearestFriends.length > 0) {
                if (rc.canHeal(nearestFriends[0].getLocation()))
                    rc.heal(nearestFriends[0].getLocation());
            }
            if (nearestFriends.length > 0 && rng.nextInt(5) == 1) {
                moveTowards(rc, curLoc, nearestFriends[0].getLocation(), 10);
            } else {
                MapLocation furthestSpawn = Utils.getFurthest(rc.getAllySpawnLocations(), curLoc);
                moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() - furthestSpawn.x, rc.getMapHeight() - furthestSpawn.y), 10);
            }
        }

    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
