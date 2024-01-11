package oldlearn;

import battlecode.common.*;

import java.util.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        MapLocation spawnLoc = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

        while (true) {

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                    spawnLoc = randomLoc;
                }
                else {
                    if (rc.canPickupFlag(rc.getLocation())) {
                        rc.pickupFlag(rc.getLocation());
                        rc.setIndicatorString("Holding a flag!");
                    }

                    // We use the check roundNum >= SETUP_ROUNDS to make sure setup phase has ended.
                    if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        // If we are holding an enemy flag, singularly focus on moving towards
                        // an ally spawn zone to capture it!
                        if (rc.hasFlag()) {
                            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                            MapLocation firstLoc = spawnLocs[0];
                            moveTowards(rc, firstLoc);
                        }
                        else if (enemyRobots.length > 0) {
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
                                rc.setIndicatorString("Attacked an enemy!");
                                rc.attack(nearestEnemy.getLocation());
                            }
                        }
//                        else {
//                            // If we are not holding an enemy flag, let's go find one!
//                            FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
//                            if (nearbyFlags.length > 0) {
//                                FlagInfo firstFlag = nearbyFlags[0];
//                                MapLocation flagLoc = firstFlag.getLocation();
//                                Direction dir = rc.getLocation().directionTo(flagLoc);
//                                if (rc.canMove(dir)) rc.move(dir);
//                            }
//                        }
                    }

                    // Go towards the other side of the map
                    moveTowards(rc, new MapLocation(rc.getMapWidth() - spawnLoc.x, rc.getMapHeight() - spawnLoc.y));

                    // Move and attack randomly if no objective.
                    Direction dir = directions[rng.nextInt(directions.length)];
                    MapLocation nextLoc = rc.getLocation().add(dir);
                    if (rc.canMove(dir)){
                        rc.move(dir);
                    }

                    // Rarely attempt placing traps behind the robot.
                    MapLocation prevLoc = rc.getLocation().subtract(dir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1)
                        rc.build(TrapType.EXPLOSIVE, prevLoc);
                    // We can also move our code into different methods or classes to better organize it!
//                    updateEnemyRobots(rc);
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
//        Direction dir = rc.getLocation().directionTo(target);
        if (rc.getLocation().equals(target)) return;
        int dir = Arrays.asList(directions).indexOf(rc.getLocation().directionTo(target));
        for (int i = 0; i < directions.length; i++) {
            if (rc.canMove(directions[(dir + i) % directions.length])) {
                rc.move(directions[(dir + i) % directions.length]);
                break;
            }
        }
//        if (rc.canMove(dir)) rc.move(dir);
//        else if ()
    }
}
