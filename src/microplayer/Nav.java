package microplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static microplayer.General.*;
import static microplayer.Utility.getIdealMovementDirections;
import static microplayer.Utility.sort;
import static microplayercopy.General.rng;

public class Nav {

    /**
     returns true if the duck sees crumbs and is going towards them
     */
    public static boolean goToCrumbs() throws GameActionException {
        // todo: rework to use bugnav
        // see crumbs, go to the nearest one
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length > 0) {
            sort(crumbLocs, (crumbLoc) -> crumbLoc.distanceSquaredTo(robotLoc));
            MapLocation closestCrumbLoc = crumbLocs[0];
            if (continueInThisDirection != null) {
                if (rc.canMove(continueInThisDirection)) {
                    rc.move(continueInThisDirection);
                }
                if (rng.nextInt(7) == 0) {
                    continueInThisDirection = null;
                }
            }
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, closestCrumbLoc);
            int i=0;
            // try to move in the ideal directions
            for (Direction idealMoveDir : idealMovementDirections) {
                i++;
                if (rc.canMove(idealMoveDir)) {
                    if (i >= 3) {
                        continueInThisDirection = idealMoveDir;
                    }
                    rc.move(idealMoveDir);
                    return true;
                } else {
                    MapLocation newRobotLoc = robotLoc.add(idealMoveDir);
                    if (rc.canFill(newRobotLoc)) {
                        rc.fill(robotLoc.add(idealMoveDir));
                        return true;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
