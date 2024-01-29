package v11_1.robots;

import battlecode.common.*;
import v11_1.Constants;
import v11_1.RobotPlayer;
import v11_1.Utils;

public abstract class AbstractRobot {

    protected Integer[] enemyFlagIDs = new Integer[3];
    protected MapLocation[] enemyFlagLocs = new MapLocation[3];
    protected MapLocation spawn;

    // Returns whether the robot should be this type or not
    public abstract boolean setup(RobotController rc) throws GameActionException;

    public abstract void tick(RobotController rc, MapLocation curLoc) throws GameActionException;

    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) {
            spawn = rc.getLocation();
            return;
        }

        int tries = 0;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        while (!rc.isSpawned()) {
            if (tries > 15) break;
            int rand = RobotPlayer.rng.nextInt(spawnLocs.length);
            if (rc.canSpawn(spawnLocs[rand])) {
                rc.spawn(spawnLocs[rand]);
                spawn = spawnLocs[rand];
                break;
            }
            tries++;
        }
    }

    public void tickJailed(RobotController rc) throws GameActionException {
        spawn(rc);
    }

    public void setupTick(RobotController rc, MapLocation curLoc) throws GameActionException {
        tick(rc, curLoc);
    }

    public abstract boolean completedTask();

    public abstract String name();

    public void detectAndPickupFlags(RobotController rc, FlagInfo[] nearbyFlags) throws GameActionException {

        // If it sees a flag, add it to the global tracking array
        for (int i = 0; i < 3; i++) {
            enemyFlagIDs[i] = rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1;

            // If this guessed location doesn't have a flag, get rid of it
            // Ignore flags which have been properly indexed
            if (enemyFlagIDs[i] == -1) {
                MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flagLoc != null && flagLoc.isWithinDistanceSquared(rc.getLocation(), 4) && nearbyFlags.length == 0) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], null);
                }
            }
        }

        for (FlagInfo info : nearbyFlags) {
            if (info.isPickedUp())
                continue;



            boolean ignore = false;
            for (int i = 0; i < 3; i++) {
                if (info.getID() == rc.readSharedArray(Constants.SharedArray.carriedFlagIDs[i]) >> 1) {
                    ignore = true;
                    break;
                }
            }

            if (ignore) {
                if (rc.canPickupFlag(info.getLocation()) && rc.getRoundNum() > RobotPlayer.flagChainDropTurn + 1)
                    rc.pickupFlag(info.getLocation());
                continue;
            }

            int index = Utils.indexOf(enemyFlagIDs, info.getID());
            int lastNotSeenFlag = Utils.indexOf(enemyFlagIDs, -1);
//            int enemyFlagLocIndex = Utils.indexOf(enemyFlagLocs, info.getLocation());
            int enemyFlagLocIndex = -1;

            // Sometimes the map symmetry is not reflectional (what we predict) and it leads to overwriting flag locations
            // This should fix that
            if (index == -1) {
                int closest = 9999999;
                for (int i = 0; i < 3; i++) {
                    MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                    if (flagLoc == null) continue;
                    int dist = flagLoc.distanceSquaredTo(info.getLocation());
                    if (dist < closest) {
                        closest = dist;
                        enemyFlagLocIndex = i;
                    }
                }
            }

            if (enemyFlagLocIndex != -1)
                lastNotSeenFlag = enemyFlagLocIndex;

            if (index == -1) {
                rc.writeSharedArray(Constants.SharedArray.enemyFlagIDs[lastNotSeenFlag], info.getID() + 1);
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[lastNotSeenFlag], info.getLocation());
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[lastNotSeenFlag], info.getLocation());
                index = lastNotSeenFlag;
            } else {
                if (!ignore)
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], info.getLocation());
                // Since we default flag locations to their spawn zones and if the flags are not there then this is just making sure that
                // we not putting the flag origins in the wrong place
                if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[index]) == null)
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[index], info.getLocation());
            }

            // Prevents the flag dropper from picking up the flag again so we can do a flag chain
            if (rc.canPickupFlag(info.getLocation()) && rc.getRoundNum() > RobotPlayer.flagChainDropTurn + 1) {
                // Transition into FlagCarrier robotType
                rc.pickupFlag(info.getLocation());
            }
        }


        for (int i = 0; i < 3; i++) {
            enemyFlagLocs[i] = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
        }

        // delete flags that aren't there anymore
        for (int i = 0; i < 3; i++) {
            MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
            if (flagLoc != null && rc.canSenseLocation(flagLoc)) {
//                System.out.println(flagLoc + " e");
                FlagInfo flag = null;
                for (FlagInfo info : nearbyFlags) {
                    if (info.getLocation().equals(flagLoc)) {
                        flag = info;
                        break;
                    }
                }
                if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i]) == null
                        || flag == null && flagLoc.equals(Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i])))
                    continue;
                if (flag == null) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i],
                            Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i]));
                }
            }
        }
    }
}
