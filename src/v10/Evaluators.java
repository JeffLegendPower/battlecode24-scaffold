package v10;

import battlecode.common.*;

import static v10.RobotPlayer.*;

public class Evaluators {

    public static int staticLocEval(RobotController current, RobotInfo[] enemies, RobotInfo[] allies, RobotInfo closest, MapLocation loc) {
        int score = 0;
        for (RobotInfo enemy : enemies) {
            if (closest.getID() == enemy.getID()) continue;
            score += enemy.getLocation().distanceSquaredTo(loc) * 1000 / current.getHealth();
        }

        for (RobotInfo ally : allies) {
            score -= ally.getLocation().distanceSquaredTo(loc); // * (rc.getHealth() / 500);
        }

        int closestDist = closest.getLocation().distanceSquaredTo(loc);

        score -= closestDist * 20;

        if (closestDist >= 5 && closestDist <= 10)
            score /= 10;

        return score;
    }

//    public static int staticLocEval(RobotController current, RobotInfo[] enemies, RobotInfo[] allies, RobotInfo closest, MapLocation loc) {
//        int score = 0;
//        System.out.println(current.getID());
//
//        int sumEnemyX = 0;
//        int sumEnemyY = 0;
//
//        int enemyScore = 0;
//        int allyScore = 0;
//
//        // If we are low on health, we want to avoid fighting too many enemies
//        score -= enemies.length * 1000 / current.getHealth();
//
//        // Let's try to avoid all enemies that are not the closest enemy
//        for (RobotInfo enemy : enemies) {
//            sumEnemyX += enemy.getLocation().x;
//            sumEnemyY += enemy.getLocation().y;
//            if (closest.getID() == enemy.getID()) continue;
//            enemyScore += (int) (7.0 / (enemy.getLocation().distanceSquaredTo(loc) - 0.9)) + 2;
//        }
//
////        System.out.println("a " + score);
//
//        // Let's stick to our allies
//        for (RobotInfo ally : allies) {
//            allyScore += (int) (5.0 / ally.getLocation().distanceSquaredTo(loc)) + 1;
//        }
////        System.out.println("b " + score);
//        System.out.println("a " + enemyScore);
//        System.out.println("b " + allyScore);
//        score += allyScore - enemyScore;
//
//        // We want to get closer to the closest enemy
//        // but not if there's way more enemies than allies
//        int closestDist = closest.getLocation().distanceSquaredTo(loc);
//        score += enemyScore - allyScore > 10
//                ? -(int) (5.0 / closestDist) - 1
//                : (int) (25.0 / closestDist);
////        System.out.println("c " + score);
//
//        // Enemies can get the first hit here, so we want to avoid this
//        if (closestDist >= 5 && closestDist <= 10)
//            score -= 50;
////        System.out.println("d " + score);
//
//        MapLocation avgEnemyLoc = new MapLocation(sumEnemyX / enemies.length, sumEnemyY / enemies.length);
//        Direction avgEnemyDir = loc.directionTo(avgEnemyLoc);
//        // Look 1 step ahead of us in the average enemy direction and 2 adjacent directions
//        // If there's an allied trap there, we are pretty safe
////        boolean allyTrap = false;
////        for (int i = -1; i <= 1; i++) {
////            // The 8 is to prevent negatives
////            Direction dir = directions[(Utils.indexOf(directions, avgEnemyDir) + i + 8) % 8];
////            MapLocation ahead = loc.add(dir);
////            if (Utils.isInMap(ahead, current) && map[ahead.y][ahead.x].getTrapType() != TrapType.NONE) {
////                allyTrap = true;
////                break;
////            }
////        }
////        if (allyTrap)
////            score += 20;
//
//        return score;
//    }

    public static int staticActionEval(RobotController rc, RobotInfo target, MapLocation loc) {
        int dmg = rc.getAttackDamage();

        if (target.health <= dmg) return 999999;

        return -target.getLocation().distanceSquaredTo(loc) * 100;
    }

    public static int staticTrapEval(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        return 0;
    }
}
