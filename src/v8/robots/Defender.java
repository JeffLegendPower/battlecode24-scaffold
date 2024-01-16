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
            if (target == null) {
                // move randomly
                Direction dir = directions[rng.nextInt(8)];
                if (rc.canMove(dir))
                    rc.move(dir);
                return;
            }
            if (flagNumber == 1)
                target = target.translate(0, (target.y == rc.getMapHeight() - 1) ? -10 : 10);
            else if (flagNumber == 2)
                target = target.translate((target.x == rc.getMapWidth() - 1) ? -10 : 10, 0);
            System.out.println("Flag number " + flagNumber + " at " + target);
        }
        if (!rc.canSenseLocation(target)) {
            Pathfinding.moveTowards(rc, curLoc, target, true);
        }
        else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo lowest = Utils.lowestHealth(enemies);
            RobotInfo closest = Utils.getClosest(enemies, curLoc);
            if (lowest == null)
                if (closest != null && rc.canAttack(closest.getLocation()))
                    rc.attack(closest.getLocation());

            if (rc.readSharedArray(Constants.SharedArray.lastFlagTrapPlaced) != flagNumber && turnsSinceLastTrap > 5) {
                int rand = rng.nextInt(40);
                if (rand >= 32) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, curLoc)) {
                        rc.build(TrapType.EXPLOSIVE, curLoc);
                        rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
                        turnsSinceLastTrap = 0;
//                        System.out.println("Trap placed for flag " + flagNumber);
                    }
                } else if (rand >= 15) {
                    if (rc.canBuild(TrapType.STUN, curLoc)) {
                        rc.build(TrapType.STUN, curLoc);
                        rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
                        turnsSinceLastTrap = 0;
//                        System.out.println("Trap placed for flag " + flagNumber);
                    }
                } else if (rand == 14) {
                    if (rc.canBuild(TrapType.WATER, curLoc)) {
                        rc.build(TrapType.WATER, curLoc);
                        rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
                        turnsSinceLastTrap = 0;
                    }
                } else {
                    if (rc.canDig(curLoc)) {
                        rc.dig(curLoc);
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