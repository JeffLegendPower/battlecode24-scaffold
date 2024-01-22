package v10_3.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v10_3.Constants;
import v10_3.Utils;

import static v10_3.Pathfinding.moveTowardsAfraid;

public class FlagCarrier extends AbstractRobot {
    public MapLocation[] spawns;
    public MapLocation closestSpawn;
    private boolean aboutToCapture = false;
    private int flagID = -1;
    private int turnsDied = 0;
    private MapLocation lastLoc = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation[] mySpawns = rc.getAllySpawnLocations();
        spawns = new MapLocation[] {
                new MapLocation(mySpawns[0].x, mySpawns[0].y),
                new MapLocation(mySpawns[1*9+4].x, mySpawns[1*9+4].y),
                new MapLocation(mySpawns[2*9+4].x, mySpawns[2*9+4].y),
        };
        closestSpawn = null;

        flagID = rc.senseNearbyFlags(0, rc.getTeam().opponent())[0].getID();

        for (int i = 0; i < 3; i++) {
            if (rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) == flagID + 1) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], null);
                break;
            }
        }
        return true;
    }

    @Override
    public void tickJailed(RobotController rc) throws GameActionException {
        turnsDied++;
        if (turnsDied <= 4 && flagID != -1 && lastLoc != null) {
            for (int i = 0; i < 3; i++) {
                if (rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) == flagID + 1) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], lastLoc);
                    break;
                }
            }
            lastLoc = null;
        } else if (flagID != -1) {
            for (int i = 0; i < 3; i++) {
                if (rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) == flagID + 1) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i],
                            Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i]));
                    break;
                }
            }
            flagID = -1;
        }
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        lastLoc = curLoc;
        turnsDied = 0;
        closestSpawn = Utils.getClosest(spawns, curLoc);

        moveTowardsAfraid(rc, curLoc, closestSpawn, false);

        if (aboutToCapture && !rc.hasFlag()) {
            aboutToCapture = false;
            for (int i = 0; i < 3; i++) {
                if (rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) == flagID + 1) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], null);
                    break;
                }
            }
        }

        for (MapLocation spawn : rc.getAllySpawnLocations()) {
            if (rc.getLocation().isAdjacentTo(spawn)) {
                aboutToCapture = true;
                break;
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
