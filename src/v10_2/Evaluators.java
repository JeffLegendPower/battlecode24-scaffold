package v10_2;

import battlecode.common.*;

import java.util.Random;

import static v10_2.RobotPlayer.directions;
import static v10_2.RobotPlayer.map;

public class Evaluators {

    public static int staticLocEval(RobotController current, RobotInfo[] enemies, RobotInfo[] allies, MapLocation loc) {
        int score = 0;
        int maxDist = 25;

        RobotInfo target = bestTarget(current, enemies, loc);

        int avgEnemyX = 0;
        int avgEnemyY = 0;

        for (RobotInfo enemy : enemies) {
            avgEnemyX += enemy.getLocation().x;
            avgEnemyY += enemy.getLocation().y;
            if (enemy.getID() == target.getID()) continue;
            score -= (maxDist - enemy.getLocation().distanceSquaredTo(loc)) * 1000 / current.getHealth();
        }

        for (RobotInfo ally : allies) {
            score += maxDist - ally.getLocation().distanceSquaredTo(loc); //* (rc.getHealth() / 500);
        }

        int closestDist = target.getLocation().distanceSquaredTo(loc);

        if (enemies.length < allies.length * 3 + 3 && allies.length > 1)
            score += (maxDist - closestDist) * 10;

//        if (closestDist >= 5 && closestDist <= 10)
//            score -= 20;

        MapLocation avgEnemyLoc = new MapLocation(avgEnemyX / enemies.length, avgEnemyY / enemies.length);
        Direction avgEnemyDir = loc.directionTo(avgEnemyLoc);
        // Look at all 3 adjacent directions to the average enemy direction
        // If we have atleast 1 trap, we are a lot safer and this should be rewarded
//        boolean allyTrap = Utils.isInMap(loc, current) && map[loc.y][loc.x].getTrapType() != TrapType.NONE;
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
            score += 20;
        }

        return score;
    }

    public static RobotInfo bestTarget(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        RobotInfo best = null;
        int bestScore = -9999999;
        for (RobotInfo enemy : enemies) {
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

//        return -(target.getLocation().distanceSquaredTo(loc) * 1000 / (target.health - dmg)); //* target.attackLevel / 2;
        return (4 - target.getLocation().distanceSquaredTo(loc)) + 1000 / (target.health - dmg);
    }

    public static int staticTrapEval(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        return 0;
    }

    public static class Action {
        public int score;
        public int type; // -1 = none, 0 = attack, 1 = heal, 2 = build
        public MapLocation target;

        public Action(int score, int type, MapLocation target) {
            this.score = score;
            this.type = type;
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
                }
            }

            int bestActionScore = -9999999;
            Action bestAction = new Action(-9999999, -1, null);
            if (bestAttackTarget != null && bestAttackScore > bestActionScore) {
                bestActionScore = bestAttackScore;
                bestAction = new Action(bestActionScore, 0, bestAttackTarget);
            }

            return bestAction;
        }
    }
}
