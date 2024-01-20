package v10;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Evaluators {

    public static int staticLocEval(RobotController rc, RobotInfo[] enemies, RobotInfo[] allies, RobotInfo closest, MapLocation loc) {
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

    public static int staticAttackEval(RobotController rc, RobotInfo target, MapLocation loc) {
//        if (!rc.canAttack(target.getLocation())) return -999999;
        int dmg = rc.getAttackDamage();

        if (target.health <= dmg) return 999999;

        return -target.getLocation().distanceSquaredTo(loc); //- target.health / 2; // TODO try if target.health / 2 works
    }
}
