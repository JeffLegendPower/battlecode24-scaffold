package v8.robots;

import battlecode.common.*;
import battlecode.world.Trap;
import scala.collection.immutable.Stream;
import v8.Utils;
import v8.Constants;
import v8.Pathfinding;
import static v8.RobotPlayer.directions;

public class FlagPlacer extends AbstractRobot{

    public static boolean flagPlaced = false;
    public static int flagPlacerNum = 0;
    public static MapInfo[] nearby;
    public static MapLocation target = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        //System.out.println("Loc: " + curLoc);
        if (rc.readSharedArray(Constants.SharedArray.numberFlagPlacer) < 3 && rc.canPickupFlag(curLoc)) {
            rc.pickupFlag(curLoc);
            flagPlacerNum = rc.readSharedArray(Constants.SharedArray.numberFlagPlacer);
            rc.writeSharedArray(Constants.SharedArray.numberFlagPlacer, rc.readSharedArray(Constants.SharedArray.numberFlagPlacer) + 1);
            return true;
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        //if (rc.getRoundNum() <=  GameConstants.SETUP_ROUNDS) return;
        //if (rc.getRoundNum() == GameConstants.SETUP_ROUNDS+1) {rc.pickupFlag(curLoc);}
        if (!flagPlaced) {
            if (rc.readSharedArray(Constants.SharedArray.numberCornerFinder) == 100) {
                if (target == null) {
                    target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLoc);
                    if (flagPlacerNum == 1)
                        target = target.translate(0, (target.y == rc.getMapHeight() - 1) ? -10 : 10);
                    else if (flagPlacerNum == 2)
                        target = target.translate((target.x == rc.getMapWidth() - 1) ? -10 : 10, 0);
                }
                //System.out.println("Im goin to flag: " + target + " cuz im id " + rc.getID() + " on team " + rc.getTeam());
                if (curLoc.equals(target)) {
                    //System.out.println("Droppin flag: " + curLoc + " cuz im id " + rc.getID());
                    rc.dropFlag(curLoc);
                    flagPlaced = true;
                }
                Pathfinding.moveTowards(rc, curLoc, target, false);
            }
        }
        else {
            if (!curLoc.equals(target)) {
                Pathfinding.moveTowards(rc, curLoc, target, true);
            }
            else {
                nearby = rc.senseNearbyMapInfos(2);
                for (MapInfo info : nearby) {
                    if (rc.canDig(info.getMapLocation())) {
                        rc.dig(info.getMapLocation());
                        break;
                    }
                    else if (rc.canBuild(TrapType.EXPLOSIVE, info.getMapLocation())) {
                        rc.build(TrapType.EXPLOSIVE, info.getMapLocation());
                        break;
                    }
                }
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                RobotInfo closest = Utils.getClosest(enemies, curLoc);
                if (closest != null && rc.canAttack(closest.getLocation()))
                    rc.attack(closest.getLocation());
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
