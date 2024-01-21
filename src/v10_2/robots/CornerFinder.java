package v10_2.robots;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import v10_2.Constants;
import v10_2.Pathfinding;
import v10_2.Utils;

public class CornerFinder extends AbstractRobot {

    public static int findingCornerTurns = 0;
    public static int findingCorner = -1;
    public static boolean completed = false;

    private MapLocation[] corners;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        corners = new MapLocation[] {
                new MapLocation(0, 0),
                new MapLocation(rc.getMapWidth()-1, 0),
                new MapLocation(0, rc.getMapHeight()-1),
                new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1)
        };

        if (findingCorner == -1){
            findingCorner = rc.readSharedArray(Constants.SharedArray.numberCornerFinder);
            if (findingCorner > 3) {
                // Not possible
                findingCorner = -1;
                return false;
            }
            rc.writeSharedArray(Constants.SharedArray.numberCornerFinder, rc.readSharedArray(Constants.SharedArray.numberCornerFinder)+1);
        }

        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS) {
            // start: move flags to corner
            if (findingCornerTurns < 30) {
                Pathfinding.moveTowards(rc, curLoc, corners[findingCorner], true);
            } else {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[findingCorner], curLoc);
                Utils.storeBitInSharedArray(rc, Constants.SharedArray.cornerPosessions, findingCorner, rc.canSenseLocation(corners[findingCorner]) ? 1 : 0);
                if (findingCorner != 0) {
                    completed = true;
                }
            }

            if (findingCornerTurns > 31 && findingCorner == 0){
                // have one of them orchestrate it
                MapLocation[] locs = {Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[0]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[1]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[2]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[3])};

                int smallestDist = 999;
                int smallestIdx = -1;
                int fclIdx = 0;
                int dist;

                for (int i = 0; i<4; i++){
                    dist = locs[i].distanceSquaredTo(corners[i]);
                    System.out.println("i: " + i + " dist: " + dist);
                    if (dist < smallestDist){
                        smallestDist = dist;
                        smallestIdx = i;
                    }
                    if (dist < 4) {
                        //Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[fclIdx], corners[i]);
                        fclIdx++;
                    }
                }
                System.out.println("Best corner: " + corners[smallestIdx]);
                if (fclIdx == 0 || true)
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0], corners[smallestIdx]);
                rc.writeSharedArray(Constants.SharedArray.numberCornerFinder, 100); // simply signify that the corner is located
                completed = true;
            }

            findingCornerTurns++;
        }
    }

    @Override
    public boolean completedTask() {
        return completed;
    }
}
