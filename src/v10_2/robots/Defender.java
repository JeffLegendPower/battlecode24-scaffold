package v10_2.robots;

import battlecode.common.*;
import v10_2.Constants;
import v10_2.Pathfinding;
import v10_2.Utils;

import static v10_2.Evaluators.staticAttackEval;
import static v10_2.RobotPlayer.directions;
import static v10_2.RobotPlayer.rng;

public class Defender extends AbstractRobot {

    public int flagNumber;
    public MapLocation target = null;
    public MapLocation[] spawns;
    private int numTurnsWithoutEnemies = 0;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {

        MapLocation[] mySpawns = rc.getAllySpawnLocations();
        spawns = new MapLocation[] {
                new MapLocation(mySpawns[0].x, mySpawns[0].y),
                new MapLocation(mySpawns[1*9+4].x, mySpawns[1*9+4].y),
                new MapLocation(mySpawns[2*9+4].x, mySpawns[2*9+4].y),
        };

        if (rc.readSharedArray(Constants.SharedArray.numberDefenders) < 6) {
            flagNumber = rc.readSharedArray(Constants.SharedArray.numberDefenders);
            flagNumber %= 3;
            rc.writeSharedArray(Constants.SharedArray.numberDefenders, rc.readSharedArray(Constants.SharedArray.numberDefenders) + 1);
            return true;
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[flagNumber]);

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

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

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
        } else if (!curLoc.isWithinDistanceSquared(target, 10)) {
            Pathfinding.moveTowards(rc, curLoc, target, false);
        } else {
            if (enemies.length > 0) {
                MapLocation bestAttack = null;
                int bestScore = -9999999;
                for (RobotInfo enemy : enemies) {
                    int score = staticAttackEval(rc, enemy, curLoc);
                    if (rc.canAttack(enemy.getLocation()) && score > bestScore) {
                        bestScore = score;
                        bestAttack = enemy.getLocation();
                    }
                }
                if (bestAttack != null)
                    rc.attack(bestAttack);
            }

//            for (int i = 3; --i >= 0;) {
//                Direction dir = directions[i * 2];
//                MapLocation digTarget = curLoc.add(dir);
//                if ((digTarget.x + digTarget.y) % 2 == 0 && rc.getCrumbs() > 1400 && rc.canDig(digTarget)) {
//                    rc.dig(digTarget);
//                    break;
//                }
//            }

            if (curLoc.isWithinDistanceSquared(target, 4)) {
                for (MapInfo info : rc.senseNearbyMapInfos(2)) {
                    MapLocation infoLoc = info.getMapLocation();
                    if (rc.canBuild(TrapType.STUN, infoLoc) && ((infoLoc.x % 2 == 0 && infoLoc.y % 2 == 0) || (infoLoc.x % 2 == 1 && infoLoc.y % 2 == 1))) {
                        rc.build(TrapType.STUN, info.getMapLocation());
                    }
                }
            }

            Direction dir = directions[rng.nextInt(4) * 2 + ((curLoc.x + curLoc.y) % 2 == 0 ? 0 : 1)];
            if (rc.canMove(dir))
                rc.move(dir);
        }


        Direction dir = directions[rng.nextInt(8)];
        if (rc.canMove(dir))
            rc.move(dir);
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}