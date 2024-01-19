package v9.robots;

import battlecode.common.*;
import v9.Constants;
import v9.Pathfinding;
import v9.Utils;

import java.util.ArrayList;
import java.util.Random;

import static v9.RobotPlayer.*;

public class Defender extends AbstractRobot {

    public static int flagNumber;
    public static MapLocation target = null;
    public static int turnsSinceLastTrap = 0;
    public static boolean builder = false;
    public static MapLocation buildTarget = null;
    public static MapLocation spamTarget = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (rc.readSharedArray(Constants.SharedArray.numberDefenders) < 6) {
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
        target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagNumber]);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (target == null) {
            target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagNumber]);
            if (rc.getRoundNum() > 200 && target == null) {
                //retarget
                Random rng = new Random();
                int rand = rng.nextInt(3);
                while (rand == flagNumber)
                    rand = rng.nextInt(3);
                target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[rand]);
                flagNumber = rand;
            } else if (target == null) {
                //move randomly
                Direction dir = directions[rng.nextInt(8)];
                if (rc.canMove(dir))
                    rc.move(dir);
                return;
            }
        }

        for (RobotInfo enemy : enemies)
            if (enemy.hasFlag)
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.capturedFlagLocs[flagNumber], enemy.getLocation());

        MapLocation capturedFlag = Utils.getLocationInSharedArray(rc, Constants.SharedArray.capturedFlagLocs[flagNumber]);
        if (capturedFlag != null) {
            Pathfinding.moveTowards(rc, curLoc, capturedFlag, true);
            if (rc.canAttack(capturedFlag))
                rc.attack(capturedFlag);
        } else if (!curLoc.isWithinDistanceSquared(target, 20)) {
            Pathfinding.moveTowards(rc, curLoc, target, false);
        } else if (!builder) {
            RobotInfo lowest = Utils.lowestHealth(enemies);
            RobotInfo closest = Utils.getClosest(enemies, curLoc);
            if (lowest == null) {
                if (closest != null && rc.canAttack(closest.getLocation()))
                    rc.attack(closest.getLocation());
                else if (rc.canAttack(lowest.getLocation()))
                    rc.attack(lowest.getLocation());
            }
            for (int i = 0; i < 4; i++) {
                Direction dir = directions[i * 2];
                MapLocation digTarget = curLoc.add(dir);
                if ((digTarget.x + digTarget.y) % 2 == 0 && rc.getCrumbs() > 1400 && rc.canDig(digTarget)) {
                    rc.dig(digTarget);
                    break;
                }
            }


            Direction dir = directions[rng.nextInt(4) * 2 + ((curLoc.x + curLoc.y) % 2 == 0 ? 0 : 1)];
            if (rc.canMove(dir))
                rc.move(dir);
        } else if (rc.getRoundNum() < 150) {
            if (spamTarget == null) {
                for(Direction dir : directions) {
                    if (rc.canDig(curLoc.add(dir))) {
                        spamTarget = curLoc.add(dir);
                        break;
                    }
                }
            } else {
                if (rc.canDig(spamTarget))
                    rc.dig(spamTarget);
                else if (rc.canFill(spamTarget))
                    rc.fill(spamTarget);
            }
        } else {
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
                if ((curLoc.x + curLoc.y) % 2 == 1
                        && (turnsSinceLastTrap > 15 || (rc.readSharedArray(Constants.SharedArray.lastFlagTrapPlaced) != flagNumber && turnsSinceLastTrap > 5))
                        && rc.getCrumbs() > 1500) {
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
                        }
                    }
                    turnsSinceLastTrap = 0;
                    buildTarget = null;
                }
                turnsSinceLastTrap++;

//                for (Direction dir : directions) {
//                    MapLocation digTarget = curLoc.add(dir);
//                    if ((digTarget.x + digTarget.y) % 2 == 0 && rc.getCrumbs() > 1000 && rc.canDig(digTarget)) {
//                        rc.dig(digTarget);
//                        break;
//                    }
//                }
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