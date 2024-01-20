package v9_1.robots;

import battlecode.common.*;
import v9_1.Constants;
import v9_1.RobotPlayer;
import v9_1.Utils;

import java.util.Arrays;
import java.util.Comparator;

import static v9_1.RobotPlayer.*;
import static v9_1.Pathfinding.*;

public class AttackerTwo extends AbstractRobot {

    private int attackerGroup = -1;
    private boolean releasedFromSpawn = true;
    private MapLocation lastTarget = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        spawn = rc.getAllySpawnLocations()[0];
        lastTarget = spawn;
        attackerGroup = (rc.readSharedArray(Constants.SharedArray.numAttackers) - 1) % 3;
        rc.writeSharedArray(Constants.SharedArray.numAttackers, rc.readSharedArray(Constants.SharedArray.numAttackers) + 1);

//        for (int i = 0; i < 3; i++)
//            enemyFlagIDs[i] = rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1;

        return true;
    }

    public MapLocation getTarget(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation globalTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackerTargets[attackerGroup]);
        if (globalTarget != null) return globalTarget;

        MapLocation flagLoc;
        for (int i = 0; i < 3; i++) {
            flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
            if (flagLoc != null) {
//                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackerTargets[attackerGroup], flagLoc);
                return flagLoc;
            }
        }

        MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);
        if (globalDefenseTarget != null
                && rc.readSharedArray(Constants.SharedArray.numNeededDefense) > (attackerGroup + 1) * 4)
            return globalDefenseTarget;

        MapLocation[] enemyFlagsBroadcast = rc.senseBroadcastFlagLocations();
        if (enemyFlagsBroadcast.length > 0) {
            return Utils.getClosest(enemyFlagsBroadcast, curLoc);
        }

        return new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    }


    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (attackerGroup == -1) {
            attackerGroup = (rc.readSharedArray(Constants.SharedArray.numAttackers) - 1) % 3;
            rc.writeSharedArray(Constants.SharedArray.numAttackers, rc.readSharedArray(Constants.SharedArray.numAttackers) + 1);
        }

//        for (int i = 0; i < 3; i++) {
//            MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
//            System.out.println(flagLoc);
//        }
//        System.out.println();

        if (rc.getRoundNum() < 170) {
            Direction dir = directions[rng.nextInt(8)];
            if (rc.canMove(dir))
                rc.move(dir);
        } else if (rc.getRoundNum() >= 160 && rc.getRoundNum() < 200) {
            MapInfo[] nearby = rc.senseNearbyMapInfos();
            MapInfo dam = null;
            int closest = 9999999;
            boolean built = false;
            for (MapInfo info : nearby) {
                if (info.isDam()) {
                    int dist = info.getMapLocation().distanceSquaredTo(curLoc);

                    if (dist <= 4 && rc.canBuild(TrapType.EXPLOSIVE, curLoc) && rng.nextInt(10) == 0 && !built) {
                        rc.build(TrapType.EXPLOSIVE, curLoc);
                        //built = true;
                    }

                    if (dist < closest) {
                        closest = dist;
                        dam = info;
                    }
                }
            }

            if (dam != null) {
                moveTowards(rc, curLoc, dam.getMapLocation(), true);
            } else {
                moveTowards(rc, curLoc, getTarget(rc, curLoc), true);
            }
        } else {
//            if (!releasedFromSpawn) {
//                if (rc.readSharedArray(Constants.SharedArray.numAttackersRespawned) % 20 == 0) {
//                    releasedFromSpawn = true;
//                    rc.writeSharedArray(Constants.SharedArray.numAttackersRespawned, 0);
//                }
//                Direction dir = directions[rng.nextInt(8)];
//                if (rc.canMove(dir))
//                    rc.move(dir);
//                return;
//            }

            boolean fillWater = rc.senseNearbyFlags(-1, rc.getTeam()).length == 0;
            FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            MapLocation target = getTarget(rc, curLoc);

            if (flags.length > 0 && !flags[0].isPickedUp()) {
                moveTowards(rc, curLoc, flags[0].getLocation(), true);
                detectAndPickupFlags(rc, flags);
            }

            if (rc.getHealth() < 500) {
                Direction toSpawn = curLoc.directionTo(spawn);
                target = target.translate(toSpawn.dx * 3, toSpawn.dy * 3);
            }

            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

            RobotInfo closestEnemy = Utils.getClosest(enemies, curLoc);
            RobotInfo closestAlly = Utils.getClosest(allies, curLoc);
            if (closestEnemy != null) {

                int maxScore = -9999999;
                Direction bestDir = null;
                RobotInfo localClosest = closestEnemy;

                for (Direction direction : directions) {
                    if (!rc.canMove(direction))
                        continue;
                    MapLocation loc = curLoc.add(direction);
                    RobotInfo closest = Utils.getClosest(enemies, loc);
                    int eval = staticLocEval(rc, enemies, allies, closestEnemy, loc);
                    if (eval > maxScore) {
                        maxScore = eval;
                        bestDir = direction;
                        localClosest = closest;
                    }
                }
                if (bestDir != null)
                    rc.move(bestDir);

                if (rc.canAttack(localClosest.getLocation()))
                    rc.attack(localClosest.getLocation());
//                moveTowards(rc, curLoc, closestEnemy.getLocation(), fillWater);
            } else {
                moveTowards(rc, curLoc, target, fillWater);
            }

            if (curLoc.distanceSquaredTo(target) <= 64
                    && rc.canBuild(TrapType.EXPLOSIVE, curLoc)
                    && rng.nextInt(2500 - Math.min(2500, rc.getCrumbs()) + 1) == 0
                    && rc.getCrumbs() > 1000)
                rc.build(rng.nextInt(3) < 2 ? TrapType.STUN : TrapType.EXPLOSIVE, curLoc);

            if (closestAlly != null && rc.canHeal(closestAlly.getLocation())) {
                rc.heal(closestAlly.getLocation());
            }
//            rc.setIndicatorDot(target, 0, 127, 255);
            lastTarget = target;
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        MapLocation target = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackerTargets[attackerGroup]);
        if (target == null) {
            super.spawn(rc);
            Utils.incrementSharedArray(rc, Constants.SharedArray.numAttackersRespawned);
            releasedFromSpawn = false;
            lastTarget = spawn;
            return;
        }
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        Arrays.sort(spawnLocs, Comparator.comparingInt(a -> a.distanceSquaredTo(target)));
        for (MapLocation spawnLoc : spawnLocs) {
            if (rc.canSpawn(spawnLoc)) {
                Utils.incrementSharedArray(rc, Constants.SharedArray.numAttackersRespawned);
                releasedFromSpawn = false;
                rc.spawn(spawnLoc);
                spawn = spawnLoc;
                lastTarget = spawn;
                return;
            }
        }
    }

    private int staticLocEval(RobotController rc, RobotInfo[] enemies, RobotInfo[] allies, RobotInfo closest, MapLocation loc) {
        int score = 0;
        for (RobotInfo enemy : enemies) {
            if (closest.getID() == enemy.getID()) continue;
            score += enemy.getLocation().distanceSquaredTo(loc) * 1000 / rc.getHealth();
        }
        for (RobotInfo ally : allies) {
            score -= ally.getLocation().distanceSquaredTo(loc); // * (rc.getHealth() / 500);
        }

        int closestDist = closest.getLocation().distanceSquaredTo(loc);

        score = score - closestDist * 20;

        if (closestDist >= 5 && closestDist <= 10)
            score /= 10;

        return score;
    }
}
