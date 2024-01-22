package v10_3;

import battlecode.common.*;

import static v10_3.RobotPlayer.*;

public class Evaluators {

    private static int attackEvalMax = -1;
    private static int trapEvalMax = -1;

    public static double staticLocEval(RobotController current, RobotInfo[] enemies, RobotInfo[] allies, MapLocation loc) {
        double score = 0;
        int maxDist = 25;
        double enemyScore = 0;
        double allyScore = 0;

        RobotInfo target = bestTarget(current, enemies, loc);

        int avgEnemyX = 0;
        int avgEnemyY = 0;

        boolean isOnCooldown = current.getActionCooldownTurns() >= 10;

        MapLocation trapExplosionLoc = new MapLocation(-100, -100);

        for (RobotInfo enemy : enemies) {
            MapLocation enemyLoc = enemy.getLocation();
            if (!enemyLoc.isWithinDistanceSquared(loc, 4)) continue;
            if (!current.onTheMap(enemyLoc)) continue;
            MapInfo lastTurnInfo = lastTurnMap[enemyLoc.y][enemyLoc.x];
            if (lastTurnInfo != null && lastTurnInfo.getTrapType() == TrapType.STUN) {
                trapExplosionLoc = enemyLoc;
                break;
            }
        }

        for (RobotInfo enemy : enemies) {
            avgEnemyX += enemy.getLocation().x;
            avgEnemyY += enemy.getLocation().y;
            if (enemy.getID() == target.getID()) continue;
            MapLocation enemyLoc = enemy.getLocation();
            int dist = enemyLoc.distanceSquaredTo(loc);
            int curScore = Math.max(maxDist - dist, 0)
                    * 700 / Math.max(current.getHealth(), 1) * (dist <= 4 ? 2 : 1);
            // + (enemy.attackLevel + enemy.healLevel + 1) / 4;

            if (!enemyLoc.isWithinDistanceSquared(trapExplosionLoc, 13))
                enemyScore += curScore;

        }

        enemyScore *= isOnCooldown ? 2 : 1;

        MapLocation nearestSpawn = Utils.getClosest(current.getAllySpawnLocations(), loc);
        int nearestSpawnDist = nearestSpawn.distanceSquaredTo(loc);

        if (nearestSpawnDist < 50 && enemyScore != 0) {
            enemyScore *= .5;
        } else if (nearestSpawnDist < 20) {
            return loc.distanceSquaredTo(target.getLocation());
        } else if (nearestSpawnDist < 10) {
            return nearestSpawn.distanceSquaredTo(target.getLocation());
        }

        for (RobotInfo ally : allies) {
            allyScore += (maxDist - ally.getLocation().distanceSquaredTo(loc)); // + (ally.attackLevel + ally.healLevel + 1) / 4;
        }

        if (nearestSpawnDist > 100)
            enemyScore *= 1.1;


        score += allyScore - enemyScore;

        int targestDist = target.getLocation().distanceSquaredTo(loc);

//        if (allies.length - 2 > enemies.length)
//            score += (maxDist - targestDist) * 10;// * 5000 / target.health;

//        if (closestDist >= 5 && closestDist <= 10 && nearestSpawnDist > 100)
//            score -= 20;

        MapLocation avgEnemyLoc = new MapLocation(avgEnemyX / enemies.length, avgEnemyY / enemies.length);
        Direction avgEnemyDir = loc.directionTo(avgEnemyLoc);
        // Look at all 3 adjacent directions to the average enemy direction
        // If we have atleast 1 trap, we are a lot safer and this location should be rewarded
        boolean allyTrap = false;
        for (int i = -1; i <= 1; i++) {
            // The +8 is to prevent negatives
            Direction dir = directions[(Utils.indexOf(directions, avgEnemyDir) + i + 8) % 8];
            MapLocation ahead = loc.add(dir);
            if (Utils.isInMap(ahead, current) && map[ahead.y][ahead.x].getTrapType() != TrapType.NONE) {
                allyTrap = true;
                break;
            }
        }

        if (allyTrap) {
            score += 50;
        }

        return score;
    }

    public static RobotInfo bestTarget(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
//        RobotInfo best = null;
//        int bestScore = -9999999;
//        for (RobotInfo enemy : enemies) {
//            int score = staticAttackEval(rc, enemy, loc);
//            if (score > bestScore) {
//                bestScore = score;
//                best = enemy;
//            }
//        }
//        return best;
        return Utils.getClosest(enemies, loc);
    }

    public static int staticAttackEval(RobotController rc, RobotInfo target, MapLocation loc) {
        int dmg = rc.getAttackDamage();

        if (target.health <= dmg) return 999999;

        if (target.hasFlag) return 999999;

//        return -(target.getLocation().distanceSquaredTo(loc) * 1000 / (target.health - dmg)); //* target.attackLevel / 2;
        return (4 - target.getLocation().distanceSquaredTo(loc)) + 2000 / (target.health - dmg);
    }

    public static int staticHealEval(RobotController rc, RobotInfo target, MapLocation loc, RobotInfo[] enemies, RobotInfo[] allies) {
        int heal = rc.getHealAmount();

        // Don't heal if it won't fully be utilized
        if (1000 - target.health < heal) return -999999;

        int closestDist = Utils.getClosest(enemies, loc).getLocation().distanceSquaredTo(loc);
        // Don't heal if we can be attacked
        if (closestDist <= 9) return -999999;

        return heal * 20 / target.health - 5;
    }

    public static int staticTrapEval(RobotController rc, RobotInfo[] enemies, RobotInfo[] allies, MapLocation loc) {
        if (enemies.length == 0) return -99999999;
        if (loc.x % 2 != loc.y % 2) return -99999999;
        int avgEnemyX = 0;
        int avgEnemyY = 0;
        for (RobotInfo enemy : enemies) {
            avgEnemyX += enemy.getLocation().x;
            avgEnemyY += enemy.getLocation().y;
        }
        MapLocation avgEnemyLoc = new MapLocation(avgEnemyX / enemies.length, avgEnemyY / enemies.length);
        Direction avgEnemyDir = loc.directionTo(avgEnemyLoc);
        // Look at all 3 adjacent directions to the average enemy direction

        MapLocation curLoc = rc.getLocation();

        boolean goodPlace = false;
        for (int i = -1; i <= 1; i++) {
            // The +8 is to prevent negative array indices
            Direction dir = directions[(Utils.indexOf(directions, avgEnemyDir) + i + 8) % 8];
            MapLocation ahead = curLoc.add(dir);
            if (ahead.equals(loc)) {
                goodPlace = true;
                break;
            }
        }

        int score = goodPlace ? 50 : 0;

        if (enemies.length - 3 >= allies.length) {
            score += 40;
        } else if (allies.length - 2 > enemies.length) {
            score -= 20;
        }

        return score;
    }

    public static class Action {
        public int score;
        public int type; // -1 = none, 0 = attack, 1 = heal, 2 = build
        public MapLocation target;

        public Action(int score, int type, MapLocation target) {
            this.score = score;
            this.type = type;
            this.target = target;
        }

        public static Action getBest(RobotController rc) throws GameActionException {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            MapLocation curLoc = rc.getLocation();

            int bestAttackScore = -9999999;
            MapLocation bestAttackTarget = null;
            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    int score = staticAttackEval(rc, enemy, curLoc);
                    if (score > bestAttackScore) {
                        bestAttackScore = score;
                        bestAttackTarget = enemy.getLocation();
                    }
                    if (score > attackEvalMax) {
                        attackEvalMax = score;
                    }
                }
            }

//            int bestHealScore = -9999999;
//            MapLocation bestHealTarget = null;
//            for (RobotInfo ally : allies) {
//                if (rc.canHeal(ally.getLocation())) {
//                    int score = staticHealEval(rc, ally, curLoc, enemies, allies);
//                    if (score > bestHealScore) {
//                        bestHealScore = score;
//                        bestHealTarget = ally.getLocation();
//                    }
//                }
//            }

            int bestTrapScore = -9999999;
            MapLocation bestTrapTarget = null;
            for (Direction direction : Direction.values()) {
                MapLocation newLoc = curLoc.add(direction);
                if (!rc.canBuild(TrapType.EXPLOSIVE, newLoc) || !rc.canBuild(TrapType.STUN, newLoc)) continue;
                int score = staticTrapEval(rc, enemies, allies, newLoc);
                if (score > bestTrapScore) {
                    bestTrapScore = score;
                    bestTrapTarget = newLoc;
                }
                if (score > trapEvalMax) {
                    trapEvalMax = score;
                }
            }

            if (attackEvalMax > 0)
                bestAttackScore /= attackEvalMax;
            if (trapEvalMax > 0)
                bestTrapScore /= trapEvalMax;

            int bestActionScore = -9999999;
            Action bestAction = new Action(-9999999, -1, null);
            if (bestAttackTarget != null && bestAttackScore > bestActionScore) {
                bestActionScore = bestAttackScore;
                bestAction = new Action(bestActionScore, 0, bestAttackTarget);
            }
//            if (bestHealTarget != null && bestHealScore > bestActionScore) {
//                bestActionScore = bestHealScore;
//                bestAction = new Action(bestActionScore, 1, bestHealTarget);
//            }
            if (bestTrapTarget != null && bestTrapScore > bestActionScore) {
                bestActionScore = bestTrapScore;
                bestAction = new Action(bestActionScore, 2, bestTrapTarget);
            }

            return bestAction;
        }
    }
}
