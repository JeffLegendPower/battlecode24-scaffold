package v10;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Evaluators {

    public static int staticLocEval(RobotInfo current, RobotInfo[] enemies, RobotInfo[] allies, RobotInfo closest, MapLocation loc) {
        int score = 0;
        for (RobotInfo enemy : enemies) {
            if (closest.getID() == enemy.getID()) continue;
            score += enemy.getLocation().distanceSquaredTo(loc) * 1000 / current.getHealth();
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

    public static int staticActionEval(RobotController rc, RobotInfo target, MapLocation loc) {
        int dmg = rc.getAttackDamage();

        if (target.health <= dmg) return 999999;

        return -target.getLocation().distanceSquaredTo(loc) * 100;
    }

    public static int staticTrapEval(RobotController rc, RobotInfo[] enemies, MapLocation loc) {
        return 0;
    }
}
