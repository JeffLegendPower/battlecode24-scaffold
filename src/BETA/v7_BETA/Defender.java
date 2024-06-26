package BETA.v7_BETA;

import battlecode.common.*;

public class Defender {

    private static int movesSinceLastEnemy = 0;
    private static int turnsSinceLastTrap = 0;

    public static void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        // Look and attack nearby enemies
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // No enemies nearby for a while, supporters can continue attacking
        if (movesSinceLastEnemy > 10 && (rc.readSharedArray(0) - 1) / 2 == RobotPlayer.spawnFlag) {
            rc.writeSharedArray(0, 0);
            rc.writeSharedArray(1, 0);
        }

        turnsSinceLastTrap++;

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
            if (RobotPlayer.spawn.isWithinDistanceSquared(curLoc, 25))
                Pathfinding.moveTowards(rc, curLoc, nearestEnemy.getLocation(), false);
            // Now attack the nearest enemy
            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
            }

            // Way too many enemies, call backup
            if (enemyRobots.length >= Math.max(5, rc.readSharedArray(1)) && RobotPlayer.spawnFlag != -1) {
                rc.writeSharedArray(0, (RobotPlayer.spawnFlag * 2) + (enemyRobots.length >= 7 ? 2 : 1));
                rc.writeSharedArray(1, enemyRobots.length);
            }
            movesSinceLastEnemy = 0;


        } else movesSinceLastEnemy++;

//        rc.setIndicatorString(String.valueOf(rc.getCrumbs()));

        FlagInfo[] nearbyAllyFlags = rc.senseNearbyFlags(-1, rc.getTeam());

        // Don't go too far from the nearest flag
        if (nearbyAllyFlags.length > 0 && !RobotPlayer.spawn.isWithinDistanceSquared(curLoc, 9)) {
            Pathfinding.moveTowards(rc, curLoc, RobotPlayer.spawn, false);

        }

        boolean trapBuilt = false;
        int numTraps = 0;

        for (int i = -2; i <= 2; i++)
            for (int j = -2; j <= 2; j++) {
                int x = Math.max(0, Math.min(rc.getMapWidth() - 1, RobotPlayer.spawn.x + i));
                int y = Math.max(0, Math.min(rc.getMapHeight() - 1, RobotPlayer.spawn.y + j));
                if (RobotPlayer.map[y][x].getTrapType() != TrapType.NONE)
                    numTraps++;
            }

        if (turnsSinceLastTrap > 5) {
            for(MapLocation spawnLoc : rc.getAllySpawnLocations()) {
                if(curLoc.distanceSquaredTo(spawnLoc) < 4) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, curLoc) && rc.canBuild(TrapType.STUN, curLoc)) {
                        if ((rc.getRoundNum() < GameConstants.SETUP_ROUNDS ? RobotPlayer.rng.nextInt(25) >= numTraps : RobotPlayer.rng.nextInt(10) >= 6))
                            rc.build(TrapType.EXPLOSIVE, curLoc);
                        else
                            rc.build(TrapType.STUN, curLoc);
                        trapBuilt = true;
                    }
//                    else if (rc.canBuild(TrapType.EXPLOSIVE, curLoc)) {
//                        rc.build(TrapType.EXPLOSIVE, curLoc);
//                        trapBuilt = true;
//                    }
//                    else if(rc.canBuild(TrapType.STUN, curLoc)) {
//                        rc.build(TrapType.STUN, curLoc);
//                        trapBuilt = true;
//                    }
                }
                if(trapBuilt) {
                    turnsSinceLastTrap = 0;
                    break;
                }
            }
        }

        /*if (spawn.isWithinDistanceSquared(curLoc, 2))
            onetileradius:
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (rc.canDig(curLoc.add(dirFrom(i, j))) && !map[curLoc.y + j][curLoc.x + i].isSpawnZone() && rng.nextInt(5) == 1) {
                        rc.dig(curLoc.add(dirFrom(i, j)));
                        break onetileradius;
                    }
                }
            }*/

        // Hover around the area if nothing to do
        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        Pathfinding.moveTowards(rc, curLoc, nextLoc, false);
    }
}
