package ALPHA.v5_ALPHA;

import battlecode.common.*;

public class Attacker {

    public static void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if(!rc.isSpawned()) return;

        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
//        Arrays.sort(
//                enemyRobots,
//                Comparator.comparingInt(a -> a.getLocation().distanceSquaredTo(curLoc)));
        // If we are holding an enemy flag, singularly focus on moving towards
        // an ally spawn zone to capture it!
        if (rc.hasFlag()) {
            Pathfinding.moveTowards(rc, curLoc, RobotPlayer.spawn);
        }
        else if (enemyRobots.length > 0) {
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

            //int flagX = rc.readSharedArray(1);
            //int flagY = rc.readSharedArray(2);
            //if (flagX > 0 || flagY > 0)
            //    moveTowards(rc, new MapLocation(flagX, flagY));

            if (rc.getHealth() < 300)
                Pathfinding.moveTowards(rc, curLoc, curLoc.directionTo(nearestEnemy.getLocation()).opposite());

            dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
            if (dist < 9 || dist > 16) // If we move forward to attack they will get the first hit
                Pathfinding.moveTowards(rc, curLoc, nearestEnemy.getLocation()); // Try to move towards the nearest enemy
            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }
        }

        // If we are not holding an enemy flag, let's go find one!
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if (nearbyFlags.length > 0) {
            FlagInfo firstFlag = nearbyFlags[0];
            MapLocation flagLoc = firstFlag.getLocation();
            Pathfinding.moveTowards(rc, curLoc, flagLoc);
        } else {
            Pathfinding.moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() - RobotPlayer.spawn.x, rc.getMapHeight() - RobotPlayer.spawn.y));
        }
    }
}
