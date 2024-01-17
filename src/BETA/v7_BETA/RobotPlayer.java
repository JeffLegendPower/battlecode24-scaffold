package BETA.v7_BETA;

import battlecode.common.*;

import java.util.Random;

import static BETA.v7_BETA.Pathfinding.moveTowards;

// Shared array indices used: 0, 1, 2, 3, 4, 5, 6, 7, 8
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
    public static float mapFilled = 0;

    public static int spawnFlag = -1;
    public static int currentFlagHeld = -1;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        int rand = rng.nextInt();
        int numDefendersFlag1 = rc.readSharedArray(2);
        int numDefendersFlag2 = rc.readSharedArray(3);
        int numDefendersFlag3 = rc.readSharedArray(4);
        int numCaptains = rc.readSharedArray(5);
        boolean defender = false; // Will stay at the spawn location at all times

        if (numDefendersFlag1 < 4) {
            spawnFlag = 0;
            defender = true;
            rc.writeSharedArray(2, numDefendersFlag1 + 1);
        } else if (numDefendersFlag2 < 4) {
            spawnFlag = 1;
            defender = true;
            rc.writeSharedArray(3, numDefendersFlag2 + 1);
        } else if (numDefendersFlag3 < 4) {
            spawnFlag = 2;
            defender = true;
            rc.writeSharedArray(4, numDefendersFlag3 + 1);
        }

        map = new MapInfo[rc.getMapHeight()][rc.getMapWidth()];

        boolean supporter1 = !defender && rand % 7 == 1; // Will attack but will return to spawn if being rushed
        boolean supporter2 = !defender && !supporter1 && rand % 5 == 1; // Last resort backup


        int movesSinceLastEnemy = 0;
        MapLocation target;

        MapLocation[] allSpawnLocations = rc.getAllySpawnLocations();
        // TODO: what is shared array initialize to?
        for (int i = 0; i < 3; i++) {
            MapLocation loc = allSpawnLocations[i * 9 + 4];
            MapLocation opposite = new MapLocation(rc.getMapWidth() - loc.x - 1, rc.getMapHeight() - loc.y - 1);
            Util.storeLocationInSharedArray(rc, 9+i, opposite);
        }

//        NavigableSet<MapLocation> seenNotVisited = new TreeSet<>();
//        MapLocation setupTarget = null;

        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                String indicatorString = (defender ? "Defender" : supporter1 ? "Supporter1" : supporter2 ? "Supporter2" : "Attacker")
                        + " " + ((mapFilled / (rc.getMapHeight() * rc.getMapWidth()) * 100) + "% filled");
                if (spawn != null)
                    indicatorString += " spawn: " + spawn.x + ", " + spawn.y;
                rc.setIndicatorString(indicatorString);
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                MapLocation curLoc = rc.getLocation();
//                if (rc.isSpawned())
//                    seenNotVisited.remove(curLoc);

                if (!rc.isSpawned()) {

                    spawn(rc);
                } else if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS / 2) {
                    // RUNS DURING FIRST HALF OF SETUP PHASE
                    // Move around randomly to collect crumbs
                    MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
                    if (nearbyCrumbs.length > 0) {
                        Pathfinding.moveTowards(rc, curLoc, nearbyCrumbs[0], true);
                    } else {
                        Direction dir = directions[rng.nextInt(directions.length)];
                        MapLocation nextLoc = curLoc.add(dir);
                        Pathfinding.moveTowards(rc, curLoc, nextLoc, true);
                    }

                } else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 5) {
                    // RUNS AFTER SETUP PHASE (prepares for action 5 rounds before)
                    FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    if (nearbyFlags.length > 0) {
                        MapLocation flagLoc = nearbyFlags[0].getLocation();
                        if (rc.canPickupFlag(flagLoc) && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                            rc.pickupFlag(flagLoc);
                            if (flagLoc.equals(Util.getLocationInSharedArray(rc, 9))) {
                                currentFlagHeld = 0;
                            } else if (flagLoc.equals(Util.getLocationInSharedArray(rc, 10))) {
                                currentFlagHeld = 1;
                            } else {
                                currentFlagHeld = 2;
                            }
                        }
                    }

                    if (rc.hasFlag()) { // if it has a flag, broadcast its position
                        Util.storeLocationInSharedArray(rc, 9 + currentFlagHeld, curLoc);
                    }

                    for (int i=0; i<3; i++) {
                        MapLocation flagLoc = Util.getLocationInSharedArray(rc, 9+i);
                        if (rc.canSenseLocation(flagLoc) && !rc.canPickupFlag(flagLoc) && rc.senseMapInfo(flagLoc).isPassable()) {
                            // its no longer there
                            //System.out.println("WTF??????");
                            MapLocation loc = allSpawnLocations[i * 9 + 4];
                            MapLocation opposite = new MapLocation(rc.getMapWidth() - loc.x - 1, rc.getMapHeight() - loc.y - 1);
                            Util.storeLocationInSharedArray(rc, 9+i, opposite);
                        }
                    }

                    if (supporter1)
                        defender = rc.readSharedArray(0) >= 1;
                    if (supporter2)
                        defender = rc.readSharedArray(0) % 2 == 0;

                    // Robot assigned to be defender, hold spawn location
                    if (rc.isSpawned()) {
                        if (defender)
                            Defender.tick(rc, curLoc);
                        else {
                            Attacker.tick(rc, curLoc);
                        }
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
                    Pathfinding.moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), true);
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
                        if (map[loc.y][loc.x] == null) {
                            mapFilled++;
                        }
                        map[loc.y][loc.x] = info;
                    }
                }

            } catch (GameActionException e) {
                System.out.println("GameActionException " + spawnFlag);
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                rc.setIndicatorDot(Util.getLocationInSharedArray(rc, 9), 255, 0, 0);
                rc.setIndicatorDot(Util.getLocationInSharedArray(rc, 10), 0, 255, 0);
                rc.setIndicatorDot(Util.getLocationInSharedArray(rc, 11), 0, 0, 255);

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



