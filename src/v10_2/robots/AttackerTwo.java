package v10_2.robots;

import battlecode.common.*;
import v10_2.Constants;
import v10_2.Utils;

import static v10_2.Evaluators.*;
import static v10_2.Pathfinding.*;
import static v10_2.RobotPlayer.*;

public class AttackerTwo extends AbstractRobot {

    private int attackerGroup = -1;

    private MapLocation lastTarget = null;

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

        MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);
        int numNeededDefense = rc.readSharedArray(Constants.SharedArray.numNeededDefense);
        if (globalDefenseTarget != null && numNeededDefense > (attackerGroup + 1) * 4) {
            return globalDefenseTarget;
        }

        MapLocation flagLoc;
        for (int i = 0; i < 3; i++) {
            flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
            if (flagLoc != null) {
                return flagLoc;
            }
        }

//        MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);
//        if (globalDefenseTarget != null
//                && rc.readSharedArray(Constants.SharedArray.numNeededDefense) > (attackerGroup + 1) * 4)
//            return globalDefenseTarget;

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

                    if (dist <= 4 && rc.canBuild(TrapType.STUN, curLoc) && rng.nextInt(10) == 0 && !built) {
                        rc.build(TrapType.STUN, curLoc);
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
            lastTarget = target;

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

            RobotInfo closestAlly = Utils.getClosest(allies, curLoc);
            if (enemies.length > 0) {

                Action bestAction = Action.getBest(rc);
                if (bestAction.type == 1)
                    System.out.println(bestAction.score);
                switch (bestAction.type) {
                    case 0:
                        rc.attack(bestAction.target);
                        break;
                    case 1:
                        rc.heal(bestAction.target);
                        break;
                    case 2:
                        rc.build(TrapType.STUN, bestAction.target);
                        break;
                    default:
                        break;
                }

                int maxScore = -9999999;
                Direction bestDir = null;

                for (Direction direction : Direction.values()) {
                    if (!rc.canMove(direction))
                        continue;
                    MapLocation loc = curLoc.add(direction);
                    int eval = staticLocEval(rc, enemies, allies, loc);
                    if (eval > maxScore) {
                        maxScore = eval;
                        bestDir = direction;
                    }
                }
                if (bestDir != null)
                    rc.move(bestDir);

                bestAction = Action.getBest(rc);
                switch (bestAction.type) {
                    case 0:
                        rc.attack(bestAction.target);
                        break;
                    case 1:
                        rc.heal(bestAction.target);
                        break;
                    case 2:
                        rc.build(TrapType.STUN, bestAction.target);
                        break;
                    default:
                        break;
                }

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
//        int numDiedLastTurn = rc.readSharedArray(Constants.SharedArray.deathsInLastTurn);
//        System.out.println("Deaths last turn: " + numDiedLastTurn);
//        if (numDiedLastTurn > 10) {
            MapLocation target = lastTarget;

            MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);
            int numNeededDefense = rc.readSharedArray(Constants.SharedArray.numNeededDefense);
            if (globalDefenseTarget != null && numNeededDefense > (attackerGroup + 1) * 4) {
                target = globalDefenseTarget;
            }

            if (target == null) {
                super.spawn(rc);
                return;
            }

//            if (spawn != null) target = spawn;

            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation bestSpawn = null;
            int shortestDist = 999999999;
            for (MapLocation spawnLoc : spawnLocs) {
                int dist = spawnLoc.distanceSquaredTo(target);
                if (rc.canSpawn(spawnLoc) && dist < shortestDist) {
                    bestSpawn = spawnLoc;
                    shortestDist = dist;
                }
            }

            if (bestSpawn != null) {
                rc.spawn(bestSpawn);
                spawn = bestSpawn;
            }
//        } else
//            super.spawn(rc);
    }
}
