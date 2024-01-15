package v8.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class AbstractRobot {

    // Returns whether the robot should be this type or not
    public abstract boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException;

    public abstract void tick(RobotController rc, MapLocation curLoc) throws GameActionException;
}
