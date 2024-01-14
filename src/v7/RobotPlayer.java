package v7;

import battlecode.common.*;

import java.util.Random;

import static v7.Pathfinding.moveTowards;

// Shared array indices used: 0, 1, 2, 3
public strictfp class RobotPlayer {

    public static int turnCount = 0;

    public static final Random rng = new Random();

    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static MapInfo[][] map;

    public static MapLocation spawn = null;

    public static int spawnFlag = -1;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        int rand = rng.nextInt();
        int numDefendersFlag1 = rc.readSharedArray(1);
        int numDefendersFlag2 = rc.readSharedArray(2);
        int numDefendersFlag3 = rc.readSharedArray(3);
        boolean defender = false; // Will stay at the spawn location at all times
        if (numDefendersFlag1 < 2) {
            defender = true;
            rc.writeSharedArray(1, numDefendersFlag1 + 1);
            spawnFlag = 0;
        } else if (numDefendersFlag2 < 2) {
            defender = true;
            rc.writeSharedArray(2, numDefendersFlag2 + 1);
            spawnFlag = 1;
        } else if (numDefendersFlag3 < 2) {
            defender = true;
            rc.writeSharedArray(3, numDefendersFlag3 + 1);
            spawnFlag = 2;
        }

//        map = new byte[rc.getMapHeight()][rc.getMapWidth()];
        map = new MapInfo[rc.getMapHeight()][rc.getMapWidth()];

        boolean supporter1 = !defender && rand % 7 == 1; // Will attack but will return to spawn if being rushed
        boolean supporter2 = !defender && !supporter1 && rand % 5 == 1; // Last resort backup
        int movesSinceLastEnemy = 0;
        MapLocation target;
        float mapFilled = 0;

        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                rc.setIndicatorString((defender ? "Defender" : supporter1 ? "Supporter1" : supporter2 ? "Supporter2" : "Attacker")
                        + " " + (mapFilled / (rc.getMapHeight() * rc.getMapWidth()) * 100) + "% filled");
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                MapLocation curLoc = rc.getLocation();

                if (!rc.isSpawned()) {
                    spawn(rc);
                } else if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS / 2) {
                    // RUNS DURING FIRST HALF OF SETUP PHASE
                    // Move around randomly to collect crumbs
                    Direction dir = directions[rng.nextInt(directions.length)];
                    MapLocation nextLoc = curLoc.add(dir);
                    moveTowards(rc, curLoc, nextLoc);

                } else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 5) {
                    // RUNS AFTER SETUP PHASE (prepares for action 5 rounds before)

                    if (rc.canPickupFlag(curLoc))
                        rc.pickupFlag(curLoc);

                    if (supporter1)
                        defender = rc.readSharedArray(0) >= 1;
                    if (supporter2)
                        defender = rc.readSharedArray(0) % 2 == 0;

                    // Robot assigned to be defender, hold spawn location
                    if (rc.isSpawned()) {
                        if (defender)
                            Defender.tick(rc, curLoc);
                        else
                            Attacker.tick(rc, curLoc);
                    }

                    // Heal teammate if possible
                    for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam()))
                        if (rc.canHeal(ally.getLocation()))
                            rc.heal(ally.getLocation());

                } else if (defender) {
                    Defender.tick(rc, curLoc);
                } else {
                    // RUNS DURING SECOND HALF OF SETUP PHASE
                    // Move towards the center of the map
                    moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
//                        moveTowardsFar(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                    // Last 20 moves start placing traps
                    if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 20 &&
                            rng.nextInt() % 9 == 1) {
                        // Check if movement is restricted in any one direction
                        // if yes then the robot is touching the wall and should place a trap
                        for (Direction d : directions)
                            if (!rc.canMove(d) && rc.canBuild(TrapType.EXPLOSIVE, curLoc))
                                rc.build(TrapType.EXPLOSIVE, curLoc);
                    }
                }

                if (rc.isSpawned()) {
                    MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                    for (MapInfo info : nearbyMapInfo) {
                        MapLocation loc = info.getMapLocation();
                        if (map[loc.y][loc.x] == null)
                            mapFilled++;
                        map[loc.y][loc.x] = info;
                    }
                }

                // Go towards the other side of the map if doing nothing else
//                moveTowards(rc, new MapLocation(rc.getMapWidth() - spawn.x, rc.getMapHeight() - spawn.y));

            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void spawn(RobotController rc) throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        // Pick a random spawn location to attempt spawning in.
        MapLocation randomLoc;
        int alert = rc.readSharedArray(0);
        if (spawnFlag != -1) {
            randomLoc = spawnLocs[spawnFlag * 9 + 4]; // Spawns on center flag
        } else if (alert > 0) {
            randomLoc = spawnLocs[(alert - 1) / 2 * 9 + 4];
        } else {
//            spawnFlag = rng.nextInt(spawnLocs.length);
            randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
        }
        rc.writeSharedArray(20, rc.getID());
        if (rc.canSpawn(randomLoc)) {
            rc.spawn(randomLoc);
            spawn = randomLoc;
        }
    }
}



