package v8.robots;

import battlecode.common.*;
import scala.collection.immutable.Stream;
import v8.Utils;
import v8.Constants;
import v8.Pathfinding;
import static v8.RobotPlayer.directions;

public class FlagPlacer extends AbstractRobot{

    public static boolean flagPlaced = false;
    public static boolean waterDug = false;
    public static boolean trapsPlaced = false;
    public static int flagPlacerNum = 0;
    public static MapInfo[] nearby;
    private static int digIdx = 0;
    private static int trapIdx = 0;
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
                Pathfinding.moveTowards(rc, curLoc, target);
            }
        }
        else if (!waterDug) {
            nearby = rc.senseNearbyMapInfos(2);

            boolean done = true;

            for (MapInfo info : nearby) {
                if (rc.canDig(info.getMapLocation())) {
                    rc.dig(info.getMapLocation());
                    break;
                }
            }

            for (MapInfo info : nearby)
                if (!info.isWater() && !info.isWall() && !info.getMapLocation().equals(curLoc)) done = false;

            waterDug = done;
        }
        else if (!trapsPlaced) {
            nearby = rc.senseNearbyMapInfos(2);
            if (trapIdx < 2 * nearby.length) {
                if (rc.canBuild(TrapType.EXPLOSIVE, nearby[trapIdx % nearby.length].getMapLocation())) {
                    rc.build(TrapType.EXPLOSIVE, nearby[trapIdx % nearby.length].getMapLocation());
                }
                else if (rc.getCrumbs() < 250)
                    trapIdx--;
                trapIdx++;
            }
            else
                trapsPlaced = true;
        }
        else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    rc.attack(enemy.getLocation());
                    break;
                }
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
