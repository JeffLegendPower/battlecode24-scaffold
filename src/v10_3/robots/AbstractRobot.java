package v10_3.robots;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v10_3.Constants;
import v10_3.RobotPlayer;
import v10_3.Utils;

public abstract class AbstractRobot {

    protected Integer[] enemyFlagIDs = new Integer[3];
    protected MapLocation[] enemyFlagLocs = new MapLocation[3];
    protected MapLocation spawn;

    // Returns whether the robot should be this type or not
    public abstract boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException;

    public abstract void tick(RobotController rc, MapLocation curLoc) throws GameActionException;

    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) return;

//        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
//            if (rc.canSpawn(spawnLoc)) {
//                rc.spawn(spawnLoc);
//                spawn = spawnLoc;
//                return;
//            }
//        }

        int tries = 0;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        while (!rc.isSpawned()) {
            if (tries > 10) break;
            int rand = RobotPlayer.rng.nextInt(spawnLocs.length);
            if (rc.canSpawn(spawnLocs[rand])) {
                rc.spawn(spawnLocs[rand]);
                spawn = spawnLocs[rand];
                break;
            }
            tries++;
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
                System.out.println("v10.2-" + RobotPlayer.type.name() + " found flag " + info.getID() + " at " + info.getLocation());
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


        for (int i = 0; i < 3; i++)
            enemyFlagLocs[i] = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);

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
