package microplayer;

import battlecode.common.*;

import static microplayer.General.*;
import static microplayer.General.rc;
import static microplayer.Utility.sort;

public class Protector {
    static int lastRoundNumSinceFlagWasThere = 0;

    public static void onProtectorGameTurn() throws GameActionException {
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // update lastRoundNumSinceFlagWasThere
        if (rc.senseNearbyFlags(-1, rc.getTeam()).length == 0) {
            lastRoundNumSinceFlagWasThere = rc.getRoundNum();
        }

        // get (harmonic) mean distance to enemy
        double denominator = 0;
        double distanceToClosestEnemy = 999;
        for (RobotInfo enemyInfo : enemyInfos) {
            double dist = Math.sqrt(enemyInfo.getLocation().distanceSquaredTo(robotLoc));
            denominator += 1 / dist;
            if (distanceToClosestEnemy > dist) {
                distanceToClosestEnemy = dist;
            }
        }
        double harmonicMeanDist = enemyInfos.length == 0 ? 999 : enemyInfos.length/denominator;

        // read the spawn suggestion array and write the danger level in the allowed bits
        int v = rc.readSharedArray(7);
        int enemiesSeen = Math.min(enemyInfos.length, 19);
        if (lastRoundNumSinceFlagWasThere + 40 < rc.getRoundNum()) {  // no flag there in a while
            enemiesSeen = Math.max(enemiesSeen / 3, 0);
        }

        // calculate safety level
        int dangerLevel = enemiesSeen;
        if (harmonicMeanDist <= 3.8) {
            dangerLevel += 2;
        }
        if (harmonicMeanDist <= 3) {
            dangerLevel += 3;
        }
        if (distanceToClosestEnemy < 4) {
            dangerLevel += 2;
        }
        if (distanceToClosestEnemy < 2) {
            dangerLevel += 5;
        }

        // write safety level
        int dangerLevelBits = dangerLevel << (5*protectedFlagIndex);
        int newV = (v ^ (v & (0b11111 << (5*protectedFlagIndex)))) | dangerLevelBits;
        rc.setIndicatorString("enemies: " + enemiesSeen + " avgdist: " + harmonicMeanDist + " danger: " + dangerLevel);
        rc.writeSharedArray(7, newV);

        if (myProtectedFlagLocation != rc.getLocation()) {
            System.out.println("big error 1 !!!");
            return;
        }

        // attack the enemy
        MapLocation enemyToAttackLocation = null;
        for (RobotInfo enemy : enemyInfos) {
            MapLocation enemyLocation = enemy.getLocation();
            if (enemy.hasFlag) {
                if (rc.canAttack(enemyLocation)) {
                    enemyToAttackLocation = enemyLocation;
                }
                break;
            }
            if (enemyToAttackLocation == null) {
                if (rc.canAttack(enemyLocation)) {
                    enemyToAttackLocation = enemyLocation;
                }
                continue;
            }
            if (enemyToAttackLocation.distanceSquaredTo(robotLoc) > enemyLocation.distanceSquaredTo(robotLoc)) {
                if (rc.canAttack(enemyLocation)) {
                    enemyToAttackLocation = enemyLocation;
                }
            }
        }
        if (enemyToAttackLocation != null) {
            rc.attack(enemyToAttackLocation);
        }

        // place stun traps in the diagonals if no enemies to attack
        MapLocation[] diagonalsToRobotLoc = new MapLocation[]{
                robotLoc.add(Direction.NORTHWEST), robotLoc.add(Direction.SOUTHEAST),
                robotLoc.add(Direction.SOUTHWEST), robotLoc.add(Direction.NORTHEAST)
        };
        sort(diagonalsToRobotLoc, (loc) -> {
            int total=0;
            for (RobotInfo enemy : enemyInfos) {
                total += enemy.getLocation().distanceSquaredTo(robotLoc);
            }
            return total;
        });
        for (MapLocation diag : diagonalsToRobotLoc) {
            if (rc.canBuild(TrapType.STUN, diag)) {
                rc.build(TrapType.STUN, diag);
                return;
            }
        }

        // place bomb traps in the orthogonals if rich enough
        if (rc.getCrumbs() > 2400) {
            MapLocation[] orthogonalsToRobotLoc = new MapLocation[]{
                    robotLoc.add(Direction.WEST), robotLoc.add(Direction.SOUTH),
                    robotLoc.add(Direction.NORTH), robotLoc.add(Direction.EAST)
            };
            for (MapLocation orthog : orthogonalsToRobotLoc) {
                if (rc.canBuild(TrapType.EXPLOSIVE, orthog)) {
                    rc.build(TrapType.EXPLOSIVE, orthog);
                    return;
                }
            }
        }

    }

    public static void onProtectorSetupTurn() throws GameActionException {
        for (MapLocation diag : new MapLocation[]{
                robotLoc.add(Direction.NORTHWEST), robotLoc.add(Direction.SOUTHEAST),
                robotLoc.add(Direction.SOUTHWEST), robotLoc.add(Direction.NORTHEAST)
        }) {
            if (rc.canBuild(TrapType.STUN, diag)) {
                rc.build(TrapType.STUN, diag);
                return;
            }
        }
    }
}
