package v9.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Scouter extends AbstractRobot {

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {

    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
