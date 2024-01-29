package turtleplayer;

import battlecode.common.*;

import static turtleplayer.General.*;
import static turtleplayer.Utility.*;
import static rushplayer.General.rc;

public class RobotPlayer {
    public static void run(RobotController rc2) {
        rc = rc2;

        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (spawnDuck()) {
                    continue;
                }
                randomizeRng();
                buyGlobalUpgrades();
                onTurn();
            } catch (GameActionException gameActionException) {
                System.out.println("GAMEACTIONEXCEPTION ==========");
                gameActionException.printStackTrace();
            } catch (Exception exception) {
                System.out.println("EXCEPTION ====================");
                exception.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void buyGlobalUpgrades() throws GameActionException {
        if (rc.getRoundNum() == 750) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
            }
        }
        if (rc.getRoundNum() == 1500) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }
        }
    }

    public static void randomizeRng() {
        MapLocation robotPos = rc.getLocation();
        rngSeed = (rngSeed * 6415828 + robotPos.x * robotPos.y * 582 + robotPos.x * 299) % 138149392;
        rng.setSeed(rngSeed);
    }

    public static boolean spawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            return false;
        }
        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }
        for (MapLocation spawnLoc : allySpawnLocations) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                return false;
            }
        }
        return true;
    }

    public static void onTurn() throws GameActionException {
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
            onSetupTurn();
        } else {
            onGameTurn();
        }
    }

    public static void onSetupTurn() throws GameActionException {

    }

    public static void onGameTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        if (robotLoc == null) {
            System.out.println("ERR!" + rc.getID());
            return;
        }

        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        sort(enemyInfos, (enemy) -> {
            int weight = 0;  // less is better
            if (enemy.hasFlag()) {
                return -22222222;
            }
            weight += enemy.getLocation().distanceSquaredTo(robotLoc) * 1000;
            weight += enemy.getHealth();
            return weight;
        }, false);

        RobotInfo[] allyInfos = sort(rc.senseNearbyRobots(-1, rc.getTeam()), (ally) -> ally.getHealth(), false);
        int nearbyRobots = 0;
        for (RobotInfo ignored : rc.senseNearbyRobots(2, rc.getTeam())) {
            nearbyRobots += 1;
        }
        if (rc.getHealth() < 1000) {
            if (enemyInfos.length > 1) {
                if (nearbyRobots == 3 || nearbyRobots == 2) {  // corners
                    if (rc.canBuild(TrapType.WATER, robotLoc)) {
                        rc.build(TrapType.WATER, robotLoc);
                        return;
                    }
                }
                if (nearbyRobots == 4 || nearbyRobots == 5) {
                    // not implemented
                }
            }
        }
        for (RobotInfo enemy : enemyInfos) {
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                return;
            }
        }
        for (RobotInfo ally : allyInfos) {
            if (rc.canHeal(ally.getLocation())) {
                rc.heal(ally.getLocation());
                return;
            }
        }
    }
}
