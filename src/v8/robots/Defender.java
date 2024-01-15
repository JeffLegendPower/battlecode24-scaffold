package v8.robots;

import battlecode.common.*;
import v8.Constants;
import v8.Pathfinding;
import v8.Utils;
import static v8.RobotPlayer.rng;
import static v8.RobotPlayer.directions;

public class Defender extends AbstractRobot {

    public static int flagNumber;
    public static MapLocation target = null;
    public static int turnsSinceLastTrap = 0;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        if(rc.readSharedArray(Constants.SharedArray.numberDefenders) < 9) {
            flagNumber = rc.readSharedArray(Constants.SharedArray.numberDefenders) % 3;
            rc.writeSharedArray(Constants.SharedArray.numberDefenders, rc.readSharedArray(Constants.SharedArray.numberDefenders) + 1);
            return true;
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (target == null) {
            target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLoc);
            if (flagNumber == 1)
                target = target.translate(0, (target.y == rc.getMapHeight() - 1) ? -10 : 10);
            else if (flagNumber == 2)
                target = target.translate((target.x == rc.getMapWidth() - 1) ? -10 : 10, 0);
        }
        if (!rc.canSenseLocation(target)) {
            Pathfinding.moveTowards(rc, curLoc, target);
        }
        else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    rc.attack(enemy.getLocation());
                    break;
                }
            }
            if (turnsSinceLastTrap > 10) {
                if (rng.nextInt(10) >= 7) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, curLoc)) {
                        rc.build(TrapType.EXPLOSIVE, curLoc);
                        turnsSinceLastTrap = 0;
                    }
                }
                else {
                    if (rc.canBuild(TrapType.STUN, curLoc)) {
                        rc.build(TrapType.STUN, curLoc);
                        turnsSinceLastTrap = 0;
                    }
                }
            }
            Direction dir = directions[rng.nextInt(8)];
            if (rc.canMove(dir))
                rc.move(dir);
        }
        turnsSinceLastTrap++;
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}