package ALPHA.v6_ALPHA;

import battlecode.common.*;

import java.util.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    static int turnCount = 0;

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

    public static byte[][] map;

    public static MapLocation spawn = null;

    private static int spawnFlag = -1;

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

        map = new byte[rc.getMapHeight()][rc.getMapWidth()];

        boolean supporter1 = !defender && rand % 7 == 1; // Will attack but will return to spawn if being rushed
        boolean supporter2 = !defender && !supporter1 && rand % 5 == 1; // Last resort backup
        int movesSinceLastEnemy = 0;
        MapLocation target;

        while (true) {
            rc.setIndicatorString((defender ? "Defender" : supporter1 ? "Supporter1" : supporter2 ? "Supporter2" : "Attacker"));
            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
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
                    Pathfinding.moveTowards(rc, curLoc, nextLoc);

                } else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 5) {
                    // RUNS AFTER SETUP PHASE (prepares for action 5 rounds before)

                    if (rc.canPickupFlag(curLoc))
                        rc.pickupFlag(curLoc);

                    if (supporter1)
                        defender = rc.readSharedArray(0) >= 1;
                    if (supporter2)
                        defender = rc.readSharedArray(0) == 2;

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
                    Pathfinding.moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
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
                        if (map[loc.y][loc.x] != 0) continue;
                        byte status = 0b001;
                        // add bit at index 1 if is water
                        if (info.isWater()) status |= 0b010;
                        if (info.isDam() || info.isWall()) status |= 0b100;
                        map[loc.y][loc.x] = status;
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
        if (spawnFlag == -1) spawnFlag = rng.nextInt(spawnLocs.length);
        MapLocation randomLoc = spawnLocs[spawnFlag];
        rc.writeSharedArray(20, rc.getID());
        if (rc.canSpawn(randomLoc)) {
            rc.spawn(randomLoc);
            spawn = randomLoc;
        }
    }
}



