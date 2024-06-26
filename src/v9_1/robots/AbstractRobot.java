package v9_1.robots;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v9_1.Constants;
import v9_1.RobotPlayer;
import v9_1.Utils;

public abstract class AbstractRobot {

    protected Integer[] enemyFlagIDs = new Integer[3];
    protected MapLocation spawn;

    // Returns whether the robot should be this type or not
    public abstract boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException;

    public abstract void tick(RobotController rc, MapLocation curLoc) throws GameActionException;

    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) return;
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                spawn = spawnLoc;
                return;
            }
        }
    }

    public void tickJailed(RobotController rc) throws GameActionException {}

    public abstract boolean completedTask();

    public void detectAndPickupFlags(RobotController rc, FlagInfo[] nearbyFlags) throws GameActionException {

        // If it sees a flag, add it to the global tracking array
        for (int i = 0; i < 3; i++)
            enemyFlagIDs[i] = rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1;

        for (FlagInfo info : nearbyFlags) {
            if (info.isPickedUp())
                continue;
            int index = Utils.indexOf(enemyFlagIDs, info.getID());
            int lastNotSeenFlag = Utils.indexOf(enemyFlagIDs, -1);
            if (index == -1) {
                System.out.println("v9.1-" + RobotPlayer.type.name() + " found flag " + info.getID() + " at " + info.getLocation());
                rc.writeSharedArray(Constants.SharedArray.enemyFlagIDs[lastNotSeenFlag], info.getID() + 1);
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[lastNotSeenFlag], info.getLocation());
                index = lastNotSeenFlag;
            } else {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], info.getLocation());
            }


            if (rc.canPickupFlag(info.getLocation())) {
                // Transition into FlagCarrier robotType
                rc.pickupFlag(info.getLocation());
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], null);
            }
        }

        // delete flags that aren't there anymore
        for (int i = 0; i < 3; i++) {
            MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
            if (flagLoc != null && rc.canSenseLocation(flagLoc)) {
                FlagInfo flag = null;
                for (FlagInfo info : nearbyFlags) {
                    if (info.getLocation().equals(flagLoc)) {
                        flag = info;
                        break;
                    }
                }
                if (flag == null && flagLoc.equals(Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i])))
                    continue;
                if (flag == null)
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i],
                            Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i]));
            }
        }
    }
}
