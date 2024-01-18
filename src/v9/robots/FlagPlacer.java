package v9.robots;

import battlecode.common.*;
import v9.Constants;
import static v9.Pathfinding.*;
import v9.Utils;

public class FlagPlacer extends AbstractRobot {

    public static boolean flagPlaced = false;
    public static int flagPlacerNum = 0;
    public static MapInfo[] nearby;
    public static MapLocation target = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
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
                    target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
                    if (flagPlacerNum == 1)
                        target = target.translate(0, (target.y == rc.getMapHeight() - 1) ? -10 : 10);
                    else if (flagPlacerNum == 2)
                        target = target.translate((target.x == rc.getMapWidth() - 1) ? -10 : 10, 0);
                }
                //System.out.println("Im goin to flag: " + target + " cuz im id " + rc.getID() + " on team " + rc.getTeam());
                int closestDistance = 9999999;
                MapLocation closestFlag = null;
                for (int i = 0; i <= 2; i++) {
                    if (i == flagPlacerNum) continue;
                    MapLocation loc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[i]);
                    if (loc != null && (loc.distanceSquaredTo(curLoc) < closestDistance)) {
                        closestDistance = loc.distanceSquaredTo(curLoc);
                        closestFlag = loc;
                    }
                }
                if ((curLoc.equals(target) || (rc.getRoundNum() >= 170 && closestDistance > 36)) && rc.hasFlag()) {
                    //System.out.println("Droppin flag: " + curLoc + " cuz im id " + rc.getID());
                    target = curLoc;
                    rc.dropFlag(curLoc);
                    flagPlaced = true;
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagPlacerNum], curLoc);
                }
                if (rc.getRoundNum() >= 150 && !rc.hasFlag() && closestDistance < 36)
                    moveAway(rc, curLoc, closestFlag, false);

                moveTowards(rc, curLoc, target, false);
            }
        }
        else {
            if (!curLoc.equals(target)) {
                moveTowards(rc, curLoc, target, true);
            } else {
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                RobotInfo closest = Utils.getClosest(enemies, curLoc);
                if (closest != null && rc.canAttack(closest.getLocation()))
                    rc.attack(closest.getLocation());

                nearby = rc.senseNearbyMapInfos(2);
                for (MapInfo info : nearby) {
                    if (rc.canDig(info.getMapLocation())) {
                        rc.dig(info.getMapLocation());
                        break;
                    }
                    else if (rc.canBuild(TrapType.EXPLOSIVE, info.getMapLocation()) && rc.senseMapInfo(info.getMapLocation()).isWater()) {

//                        rc.build(TrapType.EXPLOSIVE, info.getMapLocation());
//                        rc.build(TrapType.STUN, info.getMapLocation());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
