package v10_1.robots;

import battlecode.common.*;
import v10_1.Constants;
import v10_1.Utils;

import static v10_1.Pathfinding.moveAway;
import static v10_1.Pathfinding.moveTowards;

public class FlagPlacer extends AbstractRobot {

    public static boolean flagPlaced = false;
    public static int flagPlacerNum = 0;
    public static MapInfo[] nearby;
    public static MapLocation target = null;
    public static boolean flagGone = false;
    public static int flagGoneFor = 0;

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
                    MapLocation possibleCorner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagPlacerNum]);
                    if (flagPlacerNum == 1)
                        if (possibleCorner == null)
                            target = target.translate(0, (target.y == rc.getMapHeight() - 1) ? -10 : 10);
                        else
                            target = possibleCorner;
                    else if (flagPlacerNum == 2)
                        if (possibleCorner == null)
                            target = target.translate((target.x == rc.getMapWidth() - 1) ? -10 : 10, 0);
                        else
                            target = possibleCorner;

                }
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

                if (rc.getRoundNum() >= 150 && closestDistance < 36 && !rc.hasFlag())
                    moveAway(rc, curLoc, closestFlag, true);

                moveTowards(rc, curLoc, target, true);
            }
        }
        else if (target != null) {
            if (!flagGone && !curLoc.equals(target)) {
                moveTowards(rc, curLoc, target, true);
            } else if (flagGone && !rc.canSenseLocation(target)) {
                moveTowards(rc, curLoc, target, false);
            } else {
                FlagInfo[] nearbyFlags = rc.senseNearbyFlags(1);
                if (nearbyFlags.length == 0) {
                    flagGoneFor++;
                    flagGone = true;
                }
                else {
                    flagGone = false;
                    flagGoneFor = 0;
                }
                if (flagGone && flagGoneFor > 20) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagPlacerNum], null);

                    for (int i = 0; i < 3; i++) {
                        target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[i]);
                        if (target != null) {
                            flagPlacerNum = i;
                            break;
                        }
                    }
                }
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
            if (!flagGone) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.capturedFlagLocs[flagPlacerNum], null);
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
