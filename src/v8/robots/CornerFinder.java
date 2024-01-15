package v8.robots;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import scala.collection.immutable.Stream;
import v8.Constants;
import v8.Pathfinding;
import v8.Utils;

public class CornerFinder extends AbstractRobot {

    public static int findingCornerTurns = 0;
    public static int findingCorner = -1;

    private MapLocation[] corners;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        corners = new MapLocation[] {
                new MapLocation(0, 0),
                new MapLocation(rc.getMapWidth(), 0),
                new MapLocation(0, rc.getMapHeight()),
                new MapLocation(rc.getMapWidth(), rc.getMapHeight())
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
                Pathfinding.moveTowards(rc, curLoc, corners[findingCorner], 10);
            } else {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[findingCorner], curLoc);
            }

            if (findingCornerTurns >= 15 && findingCorner == 0){
                // have one of them orchestrate it
                MapLocation[] locs = {Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[0]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[1]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[2]),
                        Utils.getLocationInSharedArray(rc, Constants.SharedArray.cornerLocations[3])};

                int smallestDist = 99;
                int smallestIdx = 0;
                int dist;

                for (int i = 0; i<4; i++){
                    dist = locs[i].distanceSquaredTo(corners[i]);
                    if (dist < smallestDist){
                        smallestDist = dist;
                        smallestIdx = i;
                    }
                }
                System.out.println("Best corner: " + corners[smallestIdx]);
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLoc, corners[smallestIdx]);
            }

            findingCornerTurns++;
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
