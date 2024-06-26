package ALPHA.v6_ALPHA;

import battlecode.common.*;

public class Defender {

    private static int movesSinceLastEnemy = 0;

    public static void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        // Look and attack nearby enemies
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // No enemies nearby for a while, supporters can continue attacking
        if (movesSinceLastEnemy > 10)
            rc.writeSharedArray(0, 0);

        if (enemyRobots.length > 0) {
            // Enemies nearby, deal with this first
            // Find the nearest enemy robot

            RobotInfo nearestEnemy = enemyRobots[0];
            int nearestDistance = 99999;
            int dist;
            for (int i = 1; i < enemyRobots.length; i++) {
                dist = Pathfinding.calculateDistance(rc.getLocation(), enemyRobots[i].getLocation());
                if (dist < nearestDistance) {
                    nearestEnemy = enemyRobots[i];
                    nearestDistance = dist;
                }
            }

            // Try to move towards the nearest enemy
            Pathfinding.moveTowards(rc, curLoc, nearestEnemy.getLocation());
            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }

//            if (rc.canBuild(TrapType.EXPLOSIVE, curLoc) && rng.nextInt() % 12 == 1)
//                rc.build(TrapType.EXPLOSIVE, curLoc);

            // Way too many enemies, call backup
            if (enemyRobots.length >= 3) {
                rc.writeSharedArray(0, enemyRobots.length >= 5 ? 2 : 1);
            }
            movesSinceLastEnemy = 0;


        } else movesSinceLastEnemy++;

        FlagInfo[] nearbyAllyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
//        Arrays.sort(
//                nearbyAllyFlags,
//                Comparator.comparingInt(a -> a.getLocation().distanceSquaredTo(curLoc)));

        // Don't go too far from the nearest flag
        if (nearbyAllyFlags.length > 0 && !RobotPlayer.spawn.isWithinDistanceSquared(curLoc, 9)) {
            Pathfinding.moveTowards(rc, curLoc, RobotPlayer.spawn);

        }
//        else if (nearbyAllyFlags.length == 0 && !spawn.isWithinDistanceSquared(curLoc, 9)) {
//            moveTowards(rc, curLoc, spawn);
//        }

        if (rc.canBuild(TrapType.EXPLOSIVE, curLoc))
            rc.build(TrapType.EXPLOSIVE, curLoc);

        // Hover around the area if nothing to do
        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        Pathfinding.moveTowards(rc, curLoc, nextLoc);
    }
}
