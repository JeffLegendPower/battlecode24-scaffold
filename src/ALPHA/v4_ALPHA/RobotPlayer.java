package ALPHA.v4_ALPHA;

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
    static final Random rng = new Random();

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

    static Direction lastDir = Direction.CENTER;

    static HashMap<MapLocation, MapInfo> field = new HashMap<>();
    static Stack<MapLocation> toMove = new Stack<>();
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
        int rand = rng.nextInt();
        boolean defender = rand % 10 == 1; // Will stay at the spawn location at all times
        boolean supporter1 = !defender && rand % 7 == 1; // Will attack but will return to spawn if being rushed
        boolean supporter2 = !defender && !supporter1 && rand % 5 == 1; // Last resort backup
        System.out.println((defender ? "Defender" : supporter1 ? "Supporter1" : supporter2 ? "Supporter2" : "Attacker") + " reporting for duty!");
        int movesSinceLastEnemy = 0;
        MapLocation target;
        MapLocation spawn = null;
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
                    if (rc.canSpawn(randomLoc)) {
                        rc.spawn(randomLoc);
                        spawn = randomLoc;
                    }
                    spawnLoc = new MapLocation(
                            rc.getMapWidth() - 2 * randomLoc.x > 0 ? 0 : rc.getMapWidth(),
                            rc.getMapHeight() - 2 * randomLoc.y > 0 ? 0 : rc.getMapHeight()
                    );

                }
                else {
                    MapLocation curLoc = rc.getLocation();
                    for(Direction d : directions) {
                        MapLocation loc = curLoc.add(d);
                        if (rc.canSenseLocation(loc)) {
                            MapInfo info = rc.senseMapInfo(loc);
                            if(field.get(loc) != info)
                                field.put(loc, info);
                        }
                    }
                    if (rc.canPickupFlag(curLoc)) {
                        rc.pickupFlag(curLoc);
                        rc.setIndicatorString("Holding a flag!");
//                        getPath(curLoc, spawn);
                    }
                    if (supporter1)
                        defender = rc.readSharedArray(0) >= 1;
                    if (supporter2)
                        defender = rc.readSharedArray(0) == 2;

                    // Robot assigned to be defender, hold spawn location
                    if (defender) {
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
                                rc.setIndicatorString("Attacked an enemy!");
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
                        } else if (nearbyAllyFlags.length == 0 && !spawnLoc.isWithinDistanceSquared(rc.getLocation(), 25)) {
                            moveTowards(rc, spawnLoc);
                        }
                        // Hover around the area if nothing to do
                        Direction dir = directions[rng.nextInt(directions.length)];
                        MapLocation nextLoc = curLoc.add(dir);
                        moveTowards(rc, nextLoc);
                    }

                    // We use the check roundNum >= SETUP_ROUNDS to make sure setup phase has ended.
                    else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        Arrays.sort(
                                enemyRobots,
                                Comparator.comparingInt(a -> a.getLocation().distanceSquaredTo(rc.getLocation())));
                        // If we are holding an enemy flag, singularly focus on moving towards
                        // an ally spawn zone to capture it!
                        if (rc.hasFlag()) {
//                            moveTowardsFar(rc, spawn);
                            moveTowards(rc, spawn);
                            rc.writeSharedArray(1, rc.getLocation().x);
                            rc.writeSharedArray(2, rc.getLocation().y);
                        }
                        else if (enemyRobots.length > 0) {
                            // Enemies nearby, deal with this first
                            // Find the nearest enemy robot
                            RobotInfo nearestEnemy = enemyRobots[0];
                            for (int i = 1; i < enemyRobots.length; i++)
                                if (rc.getLocation().distanceSquaredTo(enemyRobots[i].getLocation()) <
                                        rc.getLocation().distanceSquaredTo(nearestEnemy.getLocation()))
                                    nearestEnemy = enemyRobots[i];

                            int flagX = rc.readSharedArray(1);
                            int flagY = rc.readSharedArray(2);
                            if (flagX > 0 || flagY > 0)
                                moveTowards(rc, new MapLocation(flagX, flagY));

                            if (rc.getHealth() < 300)
                                moveTowards(rc, rc.getLocation().directionTo(nearestEnemy.getLocation()).opposite());

                            int dist = rc.getLocation().distanceSquaredTo(nearestEnemy.getLocation());
                            if (dist < 9 || dist > 16) // If we move forward to attack they will get the first hit
                                moveTowards(rc, nearestEnemy.getLocation()); // Try to move towards the nearest enemy
                            // Now attack the nearest enemy
                            if (rc.canAttack(nearestEnemy.getLocation())) {
                                rc.setIndicatorString("Attacked an enemy!");
                                rc.attack(nearestEnemy.getLocation());
                            }
                        }

                        // If we are not holding an enemy flag, let's go find one!
                        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                        if (nearbyFlags.length > 0) {
                            FlagInfo firstFlag = nearbyFlags[0];
                            MapLocation flagLoc = firstFlag.getLocation();
                            Direction dir = rc.getLocation().directionTo(flagLoc);
                            if (rc.canMove(dir)) rc.move(dir);
                        }
                    } else if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS / 2) {
                        // Move around randomly to collect crumbs
                        Direction dir = directions[rng.nextInt(directions.length)];
                        MapLocation nextLoc = rc.getLocation().add(dir);
                        moveTowards(rc, nextLoc);
                    } else {
                        // Move towards the center of the map
                        moveTowards(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
//                        moveTowardsFar(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                        // Last 20 moves start placing traps
                        if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS - 20 &&
                                rng.nextInt() % 9 == 1) {
                            // Check if movement is restricted in any one direction
                            // if yes then the robot is touching the wall and should place a trap
                            for (Direction dir : directions)
                                if (!rc.canMove(dir) && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
                                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                        }
                    }

                    // Heal teammate if possible
                    for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam()))
                        if (rc.canHeal(ally.getLocation()))
                            rc.heal(ally.getLocation());
                    // Go towards the other side of the map if doing nothing else
                    moveTowards(rc, new MapLocation(rc.getMapWidth() - spawnLoc.x, rc.getMapHeight() - spawnLoc.y));
//                    moveTowardsFar(rc, new MapLocation(rc.getMapWidth() - spawnLoc.x, rc.getMapHeight() - spawnLoc.y));
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

    public static void moveTowardsFar(RobotController rc, MapLocation target) throws GameActionException {
//        System.out.println("jj");
        if (rc.getLocation().compareTo(target) == 0) {
            System.out.println("e");
            return;
        }
        if (toMove.isEmpty()) {
            System.out.println("j");
            getPath(rc.getLocation(), target);
        }

        if (toMove.isEmpty()) {
            System.out.println("ee");
            return;
        }
        MapLocation nextMove = toMove.peek();
        if (rc.canMove(rc.getLocation().directionTo(nextMove))) {
            System.out.println("jj");
//            moveTowards(rc, nextMove);
            rc.move(rc.getLocation().directionTo(nextMove));
            toMove.pop();
        } else {
            getPath(rc.getLocation(), target);
            if (toMove.isEmpty()) {
                System.out.println("eee");
                return;
            }
            nextMove = toMove.peek();
            if (rc.canMove(rc.getLocation().directionTo(nextMove))) {
//                moveTowards(rc, nextMove);
                rc.move(rc.getLocation().directionTo(nextMove));
                toMove.pop();
                System.out.println("jjj");
            } else {
                System.out.println("eeee");
            }
        }
    }

    // TODO proper pathfinding (remember to minimize bytecode usage) maybe we wont even need
//    public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
//        if (rc.getLocation().equals(target)) return;
//        Direction bestDir = Direction.CENTER;
//        int bestDistSquared = 9999999;
//        for (Direction direction : directions) {
//            if (direction == lastDir) continue;
//            if (rc.canMove(direction)) {
//                MapLocation nextLoc = rc.getLocation().add(direction);
//                int distSquared = nextLoc.distanceSquaredTo(target);
//                if (distSquared < bestDistSquared) {
//                    bestDistSquared = distSquared;
//                    bestDir = direction;
//                }
//            }
//        }
//        if (bestDir != Direction.CENTER)
//            lastDir = bestDir;
//        if (bestDir != Direction.CENTER) rc.move(bestDir);
//    }

    public static Direction currentDirection = null;

    public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
        if (rc.getLocation().equals(target)) {
            return;
        }
        if (!rc.isActionReady()) {
            return;
        }
        Direction d = rc.getLocation().directionTo(target);
        if (rc.canMove(d)) {
            rc.move(d);
            currentDirection = null; // there is no obstacle we're going around
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
    }

    public static void getPath(MapLocation curLoc, MapLocation target) {
        HashMap<MapLocation, MapLocation> parent = new HashMap<>();
        HashMap<MapLocation, Boolean> visited = new HashMap<>();
        Queue<MapLocation> q = new PriorityQueue<>();
        MapLocation loc = curLoc;
        q.add(loc);
        visited.put(loc, true);
        parent.put(loc, null);
        MapLocation child;
        while(!q.isEmpty()) {
            loc = q.remove();
            if (!loc.equals(target)) {
                for (Direction dir : directions) {
                    child = loc.add(dir);
                    MapInfo info = field.get(child);
                    if (info == null || (visited.get(child) == null && info.isPassable() && !info.isWater() && !info.isWall())) {
                        q.add(child);
                        parent.put(child, loc);
                        visited.put(child, true);
                    }
                }
            }
            else {
                MapLocation pindex = loc;
                toMove = new Stack<>();
                while (!pindex.equals(curLoc)) {
                    toMove.push(pindex);
                    pindex = parent.get(pindex);
                }
            }
        }
    }

    public static void moveTowards(RobotController rc, Direction target) throws GameActionException {
        moveTowards(rc, rc.getLocation().add(target));
    }
}
