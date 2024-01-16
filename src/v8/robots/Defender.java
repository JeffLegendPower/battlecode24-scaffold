package v8.robots;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.List;
import v8.Constants;
import v8.Pathfinding;
import v8.Utils;
import static v8.RobotPlayer.rng;
import static v8.RobotPlayer.directions;

public class Defender extends AbstractRobot {

    public static int flagNumber;
    public static MapLocation target = null;
    public static int turnsSinceLastTrap = 0;
    public static boolean builder = false;
    public static MapLocation buildTarget = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        if(rc.readSharedArray(Constants.SharedArray.numberDefenders) < 6) {
            flagNumber = rc.readSharedArray(Constants.SharedArray.numberDefenders);
            if (flagNumber < 3) {
                builder = true;
            }
            flagNumber %= 3;
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
//            System.out.println("Flag number " + flagNumber + " at " + target);
        }
        if (!rc.canSenseLocation(target)) {
            Pathfinding.moveTowards(rc, curLoc, target, false);
        }
        else if (!builder) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo lowest = Utils.lowestHealth(enemies);
            RobotInfo closest = Utils.getClosest(enemies, curLoc);
            if (lowest == null) {
                if (closest != null && rc.canAttack(closest.getLocation()))
                    rc.attack(closest.getLocation());
            } else {
                if (rc.canAttack(lowest.getLocation()))
                    rc.attack(lowest.getLocation());
            }
            Direction dir = directions[rng.nextInt(4) * 2 + 1];
            if (rc.getRoundNum() < 300) {
                if (rc.getCrumbs() > 1000 && rc.canDig(curLoc.add(dir)))
                    rc.dig(curLoc.add(dir));
                dir = directions[rng.nextInt(4) * 2 + 1];
            }
            if (rc.canMove(dir))
                rc.move(dir);
        }
        else {
            if (rc.senseMapInfo(curLoc).getTrapType() != TrapType.NONE) {
                if (buildTarget == null) {
                    ArrayList<MapInfo> infos = new ArrayList<>();
                    for (MapInfo info : rc.senseNearbyMapInfos()) {
                        if (info.getTrapType() == TrapType.NONE) {
                            infos.add(info);
                        }
                    }
                    buildTarget = Utils.getClosest(infos, target).getMapLocation();
                }
                Pathfinding.moveTowards(rc, curLoc, buildTarget, false);
            }
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemies.length > 0) {
                RobotInfo lowest = Utils.lowestHealth(enemies);
                RobotInfo closest = Utils.getClosest(enemies, curLoc);
                if (lowest == null) {
                    if (closest != null && rc.canAttack(closest.getLocation()))
                        rc.attack(closest.getLocation());
                }
                else {
                    if (rc.canAttack(lowest.getLocation()))
                        rc.attack(lowest.getLocation());
                }
            }
            else {
                if ((turnsSinceLastTrap > 15 || (rc.readSharedArray(Constants.SharedArray.lastFlagTrapPlaced) != flagNumber && turnsSinceLastTrap > 5)) && rc.getRoundNum() > 200 && rc.getCrumbs() > 1000) {
                    int rand = rng.nextInt(10);
                    if (rand >= 7) {
                        if (rc.canBuild(TrapType.EXPLOSIVE, curLoc)) {
                            rc.build(TrapType.EXPLOSIVE, curLoc);
                            rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
//                        System.out.println("Trap placed for flag " + flagNumber);
                        }
                    }
                    else if (rand >= 3) {
                        if (rc.canBuild(TrapType.STUN, curLoc)) {
                            rc.build(TrapType.STUN, curLoc);
                            rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
//                        System.out.println("Trap placed for flag " + flagNumber);
                        }
                    }
                    else {
                        if (rc.canDig(curLoc)) {
                            rc.dig(curLoc);
                            rc.writeSharedArray(Constants.SharedArray.lastFlagTrapPlaced, flagNumber);
                        }
                    }
                    turnsSinceLastTrap = 0;
                    buildTarget = null;
                }
                turnsSinceLastTrap++;
            }

            Direction dir = directions[rng.nextInt(8)];
            if (rc.canMove(dir))
                rc.move(dir);
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}