package v10.robots;

import battlecode.common.*;
import v10.Constants;
import v10.Pathfinding;
import v10.Utils;

import java.util.ArrayList;

import static v10.RobotPlayer.*;
import static v10.Evaluators.*;

public class Defender extends AbstractRobot {

    public int flagNumber;
    public MapLocation target = null;
    public int turnsSinceLastTrap = 0;
    public boolean builder = false;
    public MapLocation buildTarget = null;
    public MapLocation spamTarget = null;
    public int moveRadius = 0;

    private int numTurnsWithoutEnemies = 0;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (rc.readSharedArray(Constants.SharedArray.numberDefenders) < 6) {
            flagNumber = rc.readSharedArray(Constants.SharedArray.numberDefenders);
            if (flagNumber < 3) {
                builder = true;
            }
            flagNumber %= 3;
            rc.writeSharedArray(Constants.SharedArray.numberDefenders, rc.readSharedArray(Constants.SharedArray.numberDefenders) + 1);
            if (builder)
                moveRadius = 9;
            else
                moveRadius = 20;
            return true;
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagNumber]);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (target == null) {
            if (rc.getRoundNum() > 200) {
                //retarget
                for (int i = 0; i < 3; i++) {
                    target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[i]);

                    if (target != null) {
                        flagNumber = i;
                        break;
                    }
                }
            }
        }

        if (target == null) {
            // move randomly
            Direction dir = directions[rng.nextInt(8)];
            if (rc.canMove(dir))
                rc.move(dir);
            return;
        }

        MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);

        if (enemies.length > 4 && globalDefenseTarget == null) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget, target);
            rc.writeSharedArray(Constants.SharedArray.numNeededDefense, enemies.length);
        }

        if (enemies.length == 0) {
            numTurnsWithoutEnemies++;
            if (numTurnsWithoutEnemies > 10 && globalDefenseTarget != null && globalDefenseTarget.equals(target)) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget, null);
                rc.writeSharedArray(Constants.SharedArray.numNeededDefense, 0);
            }
        }
        else
            numTurnsWithoutEnemies = 0;

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
//            RobotInfo lowest = Utils.lowestHealth(enemies);

//            if (enemies.length > 0 && rc.canAttack(lowest.getLocation()))
//                rc.attack(lowest.getLocation());
            if (enemies.length > 0) {
                MapLocation bestAttack = null;
                int bestScore = -9999999;
                for (RobotInfo enemy : enemies) {
                    int score = staticActionEval(rc, enemy, curLoc);
                    if (rc.canAttack(enemy.getLocation()) && score > bestScore) {
                        bestScore = score;
                        bestAttack = enemy.getLocation();
                    }
                }
                if (bestAttack != null)
                    rc.attack(bestAttack);
            }

            for (int i = 3; --i >= 0;) {
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
            if (enemies.length > 0) {
                MapLocation bestAttack = null;
                int bestScore = -9999999;
                for (RobotInfo enemy : enemies) {
                    int score = staticActionEval(rc, enemy, curLoc);
                    if (rc.canAttack(enemy.getLocation()) && score > bestScore) {
                        bestScore = score;
                        bestAttack = enemy.getLocation();
                    }
                }
                if (bestAttack != null)
                    rc.attack(bestAttack);
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