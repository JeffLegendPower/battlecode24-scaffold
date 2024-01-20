package v9_1.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v9_1.Constants;
import v9_1.Utils;

import static v9_1.Pathfinding.*;
public class FlagCarrier extends AbstractRobot {
    public MapLocation[] spawns;
    public MapLocation closestSpawn;
    private boolean aboutToCapture = false;
    private int flagID = -1;

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
        if (flagID != -1) {
            for (int i = 0; i < 3; i++) {
                if (rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) == flagID + 1) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i],
                            Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[i]));
                    break;
                }
            }
        }
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (closestSpawn == null)
             closestSpawn = Utils.getClosest(spawns, curLoc);
//        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
//        if (nearestEnemies.length > 0) {
//            RobotInfo closestEnemy = Utils.getClosest(nearestEnemies, curLoc);
//            moveAway(rc, curLoc, closestEnemy.getLocation(), false);
//        }


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
