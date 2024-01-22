package v10_3.robots;

import battlecode.common.*;
import v10_3.Constants;
import v10_3.Utils;

import static v10_3.Utils.sort;
import static v10_3.Evaluators.Action;
import static v10_3.Evaluators.staticLocEval;
import static v10_3.Pathfinding.moveTowards;
import static v10_3.RobotPlayer.*;

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

//        MapLocation globalDefenseTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalDefenseTarget);
//        int numNeededDefense = rc.readSharedArray(Constants.SharedArray.numNeededDefense);
//        if (globalDefenseTarget != null && numNeededDefense > (attackerGroup + 1) * 4) {
//            return globalDefenseTarget;
//        }

        for (int i = 0; i < 3; i++) {
            MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
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
    static Direction[] getIdealMovementDirections(MapLocation start, MapLocation goal) {
        int sx = start.x;
        int sy = start.y;
        int gx = goal.x;
        int gy = goal.y;

        // 13 cases
        if (sx < gx) {  // rightwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // right > up
                    return new Direction[]{Direction.NORTHEAST, Direction.EAST, Direction.NORTH, Direction.SOUTHEAST, Direction.NORTHWEST};
                } else {  // up > right
                    return new Direction[]{Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHWEST, Direction.SOUTHEAST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTH, Direction.SOUTH};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // right > down
                    return new Direction[]{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHWEST};
                } else {  // down > right
                    return new Direction[]{Direction.SOUTHEAST, Direction.SOUTH, Direction.EAST, Direction.SOUTHWEST, Direction.NORTHEAST};
                }
            }
        } else if (sx == gx) {  // already vertically centered
            if (sy < gy) {  // upwards
                return new Direction[]{Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.EAST, Direction.WEST};
            } else if (sy == gy) {  // dont go anywhere
                return new Direction[]{Direction.CENTER};
            } else {  // downwards
                return new Direction[]{Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.WEST, Direction.EAST};
            }
        } else {  // leftwards
            if (sy < gy) {  // upwards
                if ((gx-sx) > (gy-sy)) {  // left > up
                    return new Direction[]{Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.SOUTHWEST, Direction.NORTHEAST};
                } else {  // up > left
                    return new Direction[]{Direction.NORTHWEST, Direction.NORTH, Direction.WEST, Direction.NORTHEAST, Direction.SOUTHWEST};
                }
            } else if (sy == gy) {  // already horizontally centered
                return new Direction[]{Direction.WEST, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.NORTH, Direction.SOUTH};
            } else {  // downwards
                if ((gx-sx) > (sy-gy)) {  // left > down
                    return new Direction[]{Direction.SOUTHWEST, Direction.WEST, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHEAST};
                } else {  // down > left
                    return new Direction[]{Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.SOUTHEAST, Direction.NORTHWEST};
                }
            }
        }
    }


    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (attackerGroup == -1) {
            attackerGroup = (rc.readSharedArray(Constants.SharedArray.numAttackers) - 1) % 3;
            rc.writeSharedArray(Constants.SharedArray.numAttackers, rc.readSharedArray(Constants.SharedArray.numAttackers) + 1);
        }

        if (rc.getRoundNum() < 160) {
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

//                    if (dist <= 4 && rc.canBuild(TrapType.STUN, curLoc) && rng.nextInt(10) == 0 && !built) {
//                        rc.build(TrapType.STUN, curLoc);
//                    }

                    if (dist < closest) {
                        closest = dist;
                        dam = info;
                    }
                }
            }

            if (dam != null) {
                MapLocation target = dam.getMapLocation();
                if (rc.getRoundNum() >= 180) {
                    Direction dirToSpawn = curLoc.directionTo(Utils.getClosest(rc.getAllySpawnLocations(), curLoc));
                    target.translate(dirToSpawn.dx * 3, dirToSpawn.dy * 3);
                }
                moveTowards(rc, curLoc, target, false);
            } else {
                moveTowards(rc, curLoc, getTarget(rc, curLoc), false);
            }
        } else {
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

            if (enemies.length > 0) {

                double maxScore = -9999999;
                Direction bestDir = null;

                for (Direction direction : Direction.values()) {
                    if (direction != Direction.CENTER && !rc.canMove(direction))
                        continue;
                    MapLocation loc = curLoc.add(direction);
                    double eval = staticLocEval(rc, enemies, allies, loc);
                    if (eval > maxScore) {
                        maxScore = eval;
                        bestDir = direction;
                    }
                }
                if (bestDir != null && rc.canMove(bestDir))
                    rc.move(bestDir);

                bestAction = Action.getBest(rc);
                switch (bestAction.type) {
                    case 0:
                        if (rc.canAttack(bestAction.target))
                            rc.attack(bestAction.target);
                        break;
                    case 1:
                        if (rc.canHeal(bestAction.target))
                            rc.heal(bestAction.target);
                        break;
                    case 2:
                        if (rc.canBuild(TrapType.STUN, bestAction.target))
                            rc.build(TrapType.STUN, bestAction.target);
                        break;
                    default:
                        break;
                }

//                if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
//                    for (Direction direction : Direction.allDirections()) {
//                        MapLocation newLoc = curLoc.add(direction);
//                        if ((newLoc.x + newLoc.y) % 2 == 0) {
//                            if (rc.canBuild(TrapType.STUN, curLoc.add(direction))) {
//                                System.out.println("J");
//                                rc.build(TrapType.STUN, curLoc.add(direction));
//                                break;
//                            }
//                        }
//                    }
//                }

            } else {
                if (allies.length > 0 && closestAlly.getHealth() <= 1000 - rc.getHealAmount()) {
                    for (Direction d : sort(
                            getIdealMovementDirections(curLoc, target),
                            (d) -> curLoc.add(d).distanceSquaredTo(closestAlly.getLocation()))
                    ) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            rc.setIndicatorString("moved towards goal & ally");
                            if (rc.canHeal(closestAlly.getLocation())) {
                                rc.heal(closestAlly.getLocation());
                                rc.setIndicatorString("healed a guy");
                            }
                            return;
                        } else {
                            if (rc.canFill(curLoc.add(d))) {
                                rc.fill(curLoc.add(d));
                            }
                        }
                    }
                    if (rc.canHeal(closestAlly.getLocation())) {
                        rc.heal(closestAlly.getLocation());
                    }
                } else {
                    moveTowards(rc, curLoc, target, true);
                }
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }

    @Override
    public void spawn(RobotController rc, RobotType type) throws GameActionException {
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
                super.spawn(rc, type);
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
