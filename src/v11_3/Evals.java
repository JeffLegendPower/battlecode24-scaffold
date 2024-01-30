package v11_3;

import battlecode.common.*;
import v11_3.Utils;

import static v11_3.RobotPlayer.directions;

public class Evals {

    public static int staticLocEval(RobotController current, RobotInfo[] enemies, RobotInfo[] allies, MapLocation loc) {
        int score = 0;
        int maxDist = 25;

        RobotInfo target = bestTarget(current, enemies, loc);

//        int avgEnemyX = 0;
//        int avgEnemyY = 0;

        boolean isOnCooldown = current.getActionCooldownTurns() >= 10;

        for (RobotInfo enemy : enemies) {
//            avgEnemyX += enemy.getLocation().x;
//            avgEnemyY += enemy.getLocation().y;
            if (enemy.getID() == target.getID()) continue;
            score -= (maxDist - enemy.getLocation().distanceSquaredTo(loc))
                    * 1000 / current.getHealth()
                    * (isOnCooldown ? 2 : 1) / (Math.max(allies.length * allies.length - enemies.length * enemies.length * 4, 1));
        }

        MapLocation nearestSpawn = Utils.getClosest(current.getAllySpawnLocations(), loc);
        int nearestSpawnDist = nearestSpawn.distanceSquaredTo(loc);

        if (nearestSpawnDist < 50 && score != 0) {
            score += 50;
        }

        for (RobotInfo ally : allies) {
            score += maxDist - ally.getLocation().distanceSquaredTo(loc); //* (rc.getHealth() / 500);
        }


        int closestDist = target.getLocation().distanceSquaredTo(loc);

        if (enemies.length < allies.length * 3 + 3 && allies.length > 1)
            score += (maxDist - closestDist) * 20;

//        if (closestDist >= 5 && closestDist <= 10)
//            score -= 20;

//        MapLocation avgEnemyLoc = new MapLocation(avgEnemyX / enemies.length, avgEnemyY / enemies.length);
//        Direction avgEnemyDir = loc.directionTo(avgEnemyLoc);
        // Look at all 3 adjacent directions to the average enemy direction
        // If we have atleast 1 trap, we are a lot safer and this location should be rewarded
//        boolean allyTrap = false;
//        for (int i = -1; i <= 1; i++) {
//            // The +8 is to prevent negatives
//            Direction dir = directions[(Utils.indexOf(directions, avgEnemyDir) + i + 8) % 8];
//            MapLocation ahead = loc.add(dir);
//            if (Utils.isInMap(ahead, current) && map[ahead.y][ahead.x].getTrapType() != TrapType.NONE) {
//                allyTrap = true;
//                break;
//            }
//        }
//
//        if (allyTrap) {
//            score += 20;
//        }

        return score;
    }

    public static int staticEnemyLocEval(RobotInfo me, RobotInfo[] enemies, RobotInfo[] allies, MapLocation curLoc) {
        int score = 0;
        int maxDist = 25;

        if (me.hasFlag()) {
            return 9999999;
        }

        int closestDistToAlly = -1;
        int dist;
        for (RobotInfo ally : allies) {
            dist = ally.getLocation().distanceSquaredTo(curLoc);
            if (dist > closestDistToAlly) {
                closestDistToAlly = dist;
            }
        }

        int closestDistToEnemy = -1;
        for (RobotInfo enemy : enemies) {
            dist = enemy.getLocation().distanceSquaredTo(curLoc);
            if (dist > closestDistToEnemy) {
                closestDistToEnemy = dist;
            }
        }

        return 100 - (closestDistToEnemy + closestDistToAlly) + me.getHealth() / 10 + me.getAttackLevel()*3 + me.getHealLevel();
    }

    public static RobotInfo bestTarget(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        RobotInfo best = null;
        int bestScore = -9999999;
        for (RobotInfo enemy : enemies) {
            if (!rc.canAttack(enemy.getLocation())) continue;
            int score = staticAttackEval(rc, enemy, loc);
            if (score > bestScore) {
                bestScore = score;
                best = enemy;
            }
        }
        return best;
    }

    public static int staticAttackEval(RobotController rc, RobotInfo target, MapLocation loc) {
        int dmg = rc.getAttackDamage();

        if (target.health <= dmg) return 999999;

        if (target.hasFlag) return 9999999;

//        return -(target.getLocation().distanceSquaredTo(loc) * 1000 / (target.health - dmg)); //* target.attackLevel / 2;
        return (4 - target.getLocation().distanceSquaredTo(loc)) + 1000 / (target.health - dmg);
    }

    public static int staticTrapEval(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        if (enemies.length == 0) return -99999999;
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
            // The +8 is to prevent negatives
            Direction dir = directions[(Utils.indexOf(directions, avgEnemyDir) + i + 8) % 8];
            MapLocation ahead = curLoc.add(dir);
            if (ahead.equals(loc)) {
                goodPlace = true;
                break;
            }
        }

        if (goodPlace) {
            return 0;
        }
        return -10;
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

        public static MapLocation getBestTarget(RobotController rc) throws GameActionException {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
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
                }
            }
            return bestAttackTarget;
        }
    }
}
