package microplayer;

import battlecode.common.*;

import static microplayer.General.*;
import static microplayer.Utility.*;

public class MicroAttacker {

    public static MapLocation pathfindGoalLocForMicroAttacker;

    static int distToNearestEnemyFlag = 1000000;

    static double[] DPS = new double[]{150, 162, 169, 178, 220, 260, 365};  // modified slightly to account for cooldown

    static double currentDPS = 0;
    static double currentRangeExtended = 20;
    static double currentActionRadius = 4;
    static boolean canAttack;

    public static void doMicro(RobotInfo[] allies, RobotInfo[] enemies) throws GameActionException {
        if (!rc.isMovementReady()) return;
        canAttack = rc.isActionReady();

        if (!canAttack) {  // cant attack, move back a little
            rc.setIndicatorString("cant attack or low health");
            Direction[] sortedDirections = sort(directions, (dir) -> {
                MapLocation newRobotLoc = robotLoc.add(dir);

                int numEnemiesThatCanAttackThisLocation = 0;
                int distToNearestEnemy = 999;
                for (RobotInfo enemy : enemies) {
                    MapLocation enemyLocation = enemy.getLocation();
                    int dist = enemyLocation.distanceSquaredTo(newRobotLoc);
                    if (dist <= 4) {
                        numEnemiesThatCanAttackThisLocation += 1;
                    }
                    if (distToNearestEnemy > dist) {
                        distToNearestEnemy = dist;
                    }
                }

                int numAlliesThatCanHealMe = 0;
                for (RobotInfo ally : allies) {
                    int dist = ally.getLocation().distanceSquaredTo(newRobotLoc);
                    if (dist <= 4) {
                        numAlliesThatCanHealMe += 1;
                    }
                }

                return numEnemiesThatCanAttackThisLocation * 60 - numAlliesThatCanHealMe * 30 + distToNearestEnemy
                        + pathfindGoalLocForMicroAttacker.distanceSquaredTo(newRobotLoc);
            });

            for (int i=0; i<3; i++) {
                if (rc.canMove(sortedDirections[i])) {
                    rc.move(sortedDirections[i]);
                    return;
                }
            }

            for (int i=0; i<5; i++) {
                if (rc.canFill(robotLoc.add(sortedDirections[i]))) {
                    rc.fill(robotLoc.add(sortedDirections[i]));
                }
            }
        } else {  // can attack
            rc.setIndicatorString("can attack");
            Direction[] sortedDirections = sort(directions, (dir) -> {
                MapLocation newRobotLoc = robotLoc.add(dir);

                int numEnemiesThatCanAttackThisLocation = 0;
                int distToNearestEnemy = 999;
                for (RobotInfo enemy : enemies) {
                    MapLocation enemyLocation = enemy.getLocation();
                    int dist = enemyLocation.distanceSquaredTo(newRobotLoc);
                    if (dist <= 4) {
                        numEnemiesThatCanAttackThisLocation += 1;
                    }
                    if (distToNearestEnemy > dist) {
                        distToNearestEnemy = dist;
                    }
                }

                int numAlliesThatCanHealMe = 0;
                for (RobotInfo ally : allies) {
                    int dist = ally.getLocation().distanceSquaredTo(newRobotLoc);
                    if (dist <= 4) {
                        numAlliesThatCanHealMe += 1;
                    }
                }

                return numEnemiesThatCanAttackThisLocation - numAlliesThatCanHealMe * 10
                        + distToNearestEnemy * 60 + pathfindGoalLocForMicroAttacker.distanceSquaredTo(newRobotLoc);
            });

            for (int i=0; i<2; i++) {
                if (rc.canMove(sortedDirections[i])) {
                    rc.move(sortedDirections[i]);
                    return;
                }
            }

            for (int i=0; i<5; i++) {
                if (rc.canFill(robotLoc.add(sortedDirections[i]))) {
                    rc.fill(robotLoc.add(sortedDirections[i]));
                    return;
                }
            }
        }

    }

}