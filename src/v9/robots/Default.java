package v9.robots;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static v9.Pathfinding.moveTowards;
import static v9.RobotPlayer.*;
import v9.Utils;
import v9.Constants;
public class Default extends AbstractRobot {

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearbyCrumbs.length > 0)
            moveTowards(rc, curLoc, nearbyCrumbs[0], false);
        else
            moveTowards(rc, curLoc, curLoc.add(directions[rng.nextInt(8)]), false);
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
