package v10_3.robots;

import battlecode.common.*;
import v10_3.Constants;
import v10_3.Utils;

import static v10_3.Pathfinding.moveTowards;

public class FlagPlacer extends AbstractRobot {

    public static boolean flagPlaced = false;
    public static int flagPlacerNum = 0;
    public static MapInfo[] nearby;
    public static MapLocation target = null;
    public static boolean flagGone = false;
    public static int flagGoneFor = 0;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (curLoc == null)
            return false;
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

        if (target == null) {
            target = curLoc;
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagPlacerNum], curLoc);
        }

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

            if (flagGone && flagGoneFor > 30) {
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
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo closestEnemy = Utils.getClosest(enemies, curLoc);
            RobotInfo closestAlly = Utils.getClosest(allies, curLoc);
            if (closestEnemy != null && rc.canAttack(closestEnemy.getLocation()))
                rc.attack(closestEnemy.getLocation());
            else if (closestAlly != null && rc.canHeal(closestAlly.getLocation()))
                rc.heal(closestAlly.getLocation());

            nearby = rc.senseNearbyMapInfos(2);
            for (MapInfo info : nearby) {
                MapLocation infoLoc = info.getMapLocation();
                if (rc.canBuild(TrapType.STUN, infoLoc) && ((infoLoc.x % 2 == 0 && infoLoc.y % 2 == 0) || (infoLoc.x % 2 == 1 && infoLoc.y % 2 == 1))) {
                    rc.build(TrapType.STUN, info.getMapLocation());
                }
            }
        }


    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
