package REAL;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;

import static REAL.Pathfinding.*;
import static REAL.RobotPlayer.*;

public class Defender {

    private static int movesSinceLastEnemy = 0;

    public static void tick(RobotController rc) throws GameActionException {
        // Look and attack nearby enemies
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // No enemies nearby for a while, supporters can continue attacking
        if (movesSinceLastEnemy > 10)
            rc.writeSharedArray(0, 0);

        if (enemyRobots.length > 0) {
            // Enemies nearby, deal with this first
            // Find the nearest enemy robot
            RobotInfo nearestEnemy = enemyRobots[0];
            for (int i = 1; i < enemyRobots.length; i++)
                if (rc.getLocation().distanceSquaredTo(enemyRobots[i].getLocation()) <
                        rc.getLocation().distanceSquaredTo(nearestEnemy.getLocation()))
                    nearestEnemy = enemyRobots[i];

            // Try to move towards the nearest enemy
            moveTowards(rc, nearestEnemy.getLocation());
            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }

            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()) && rng.nextInt() % 12 == 1)
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());

            // Way too many enemies, call backup
            if (enemyRobots.length >= 3) {
                rc.writeSharedArray(0, enemyRobots.length >= 5 ? 2 : 1);
            }
            movesSinceLastEnemy = 0;
        } else movesSinceLastEnemy++;

        FlagInfo[] nearbyAllyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        Arrays.sort(
                nearbyAllyFlags,
                Comparator.comparingInt(a -> a.getLocation().distanceSquaredTo(rc.getLocation())));

        // Don't go too far from the nearest flag
        if (nearbyAllyFlags.length > 0 && !nearbyAllyFlags[0].getLocation().isWithinDistanceSquared(rc.getLocation(), 25)) {
            moveTowards(rc, nearbyAllyFlags[0].getLocation());

        } else if (nearbyAllyFlags.length == 0 && !spawn.isWithinDistanceSquared(rc.getLocation(), 25)) {
            moveTowards(rc, spawn);
        }
        // Hover around the area if nothing to do
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        moveTowards(rc, nextLoc);
    }
}
