package v10_1.robots;

import battlecode.common.*;
import v10_1.Constants;
import v10_1.Utils;

import static v10_1.Pathfinding.*;
import static v10_1.RobotPlayer.*;
import static v10_1.Evaluators.*;

public class AttackerTwo extends AbstractRobot {

    private int attackerGroup = -1;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        spawn = rc.getAllySpawnLocations()[0];
        attackerGroup = (rc.readSharedArray(Constants.SharedArray.numAttackers) - 1) % 3;
        rc.writeSharedArray(Constants.SharedArray.numAttackers, rc.readSharedArray(Constants.SharedArray.numAttackers) + 1);

        return true;
    }

    public MapLocation getTarget(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation globalTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackerTargets[attackerGroup]);
        if (globalTarget != null) return globalTarget;

        MapLocation flagLoc;
        for (int i = 0; i < 3; i++) {
            flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
            if (flagLoc != null) {
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

                int bestFirstAttackScore = -9999999;
                MapLocation bestFirstAttackTarget = null;
                for (RobotInfo enemy : enemies) {
                    if (rc.canAttack(enemy.getLocation())) {
                        int score = staticAttackEval(rc, enemy, curLoc);
                        if (score > bestFirstAttackScore) {
                            bestFirstAttackScore = score;
                            bestFirstAttackTarget = enemy.getLocation();
                        }
                    }
                }
                if (bestFirstAttackTarget != null)
                    rc.attack(bestFirstAttackTarget);

                int maxScore = -9999999;
                Direction bestDir = null;

                for (Direction direction : Direction.values()) {
                    if (!rc.canMove(direction))
                        continue;
                    MapLocation loc = curLoc.add(direction);
                    int eval = staticLocEval(rc, enemies, allies, closestEnemy, loc);
                    if (eval > maxScore) {
                        maxScore = eval;
                        bestDir = direction;
                    }
                }
                if (bestDir != null)
                    rc.move(bestDir);

                int bestSecondAttackScore = -9999999;
                MapLocation bestSecondAttackTarget = null;
                for (RobotInfo enemy : enemies) {
                    if (rc.canAttack(enemy.getLocation())) {
                        int score = staticAttackEval(rc, enemy, curLoc);
                        if (score > bestSecondAttackScore) {
                            bestSecondAttackScore = score;
                            bestSecondAttackTarget = enemy.getLocation();
                        }
                    }
                }
                if (bestSecondAttackTarget != null)
                    rc.attack(bestSecondAttackTarget);

            } else {
                boolean allHealed = true;
                for (RobotInfo ally : allies) {
                    if (rc.canHeal(ally.getLocation())) {
                        rc.heal(ally.getLocation());
                        allHealed = false;
                    }
                }
                if (allHealed)
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
            return;
        }
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
//        Arrays.sort(spawnLocs, Comparator.comparingInt(a -> a.distanceSquaredTo(target)));
        MapLocation bestSpawn = null;
        int shortestDist = 999999999;
        for (MapLocation spawnLoc : spawnLocs) {
            int dist = spawnLoc.distanceSquaredTo(target);
            if (rc.canSpawn(spawnLoc) && dist < shortestDist) {
//                Utils.incrementSharedArray(rc, Constants.SharedArray.numAttackersRespawned);
//                releasedFromSpawn = false;
                bestSpawn = spawnLoc;
                shortestDist = dist;
                return;
            }
        }

        if (bestSpawn != null) {
            rc.spawn(bestSpawn);
            spawn = bestSpawn;
        }
    }
}
