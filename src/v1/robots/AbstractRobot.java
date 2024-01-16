package v1.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class AbstractRobot {

    // Returns whether the robot should be this type or not
    public abstract boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException;

    public abstract void tick(RobotController rc, MapLocation curLoc) throws GameActionException;

    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) return;
        for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                break;
            }
        }
    }

    public abstract boolean completedTask();
}
