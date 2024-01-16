package v2.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v2.robots.AbstractRobot;

import static v2.Pathfinding.moveTowards;
import static v2.RobotPlayer.*;
public class Default extends AbstractRobot {

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearbyCrumbs.length > 0)
            moveTowards(rc, curLoc, nearbyCrumbs[0], true);
        else
            moveTowards(rc, curLoc, curLoc.add(directions[rng.nextInt(8)]), true);
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
