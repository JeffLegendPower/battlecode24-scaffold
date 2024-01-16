package MANNAN;

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

    static MapLocation lastLocation = null;
    static ArrayList<MapLocation> prevLocs = new ArrayList<>();
    static String[] types = {"Simple", "Flood", "Depth 2", "Depth 3", "Depth 4"};
    static int mytype = -1;

    public static MapInfo[][] map;
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
        // Everything you say here will be directly viewable in your terminal when you run a match



        MapLocation spawnLoc = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        int rand = rng.nextInt();

        map = new MapInfo[rc.getMapHeight()][rc.getMapWidth()];

        MapLocation spawn = null;
        while (true) {

            turnCount += 1;  // We have now been alive for one more turn!
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()) {
                    mytype = rc.readSharedArray(0);

                    if (mytype > 3) continue;
                    rc.writeSharedArray(0, rc.readSharedArray(0) + 1);
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
//                    mytype = 3;

                    continue;

                }

                if (!rc.isSpawned())
                    continue;


                MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    map[loc.y][loc.x] = info;
                }


                MapLocation target = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

                int before = Clock.getBytecodeNum();
                if (mytype == 0) {
                    //Pathfinding.simpleMoveTowards(rc, rc.getLocation(), target, false, true, 1);
                } else if (mytype == 1 && false) {
                    Pathfinding.moveTowards(rc, rc.getLocation(), target, false);
                } else if (mytype == 2 && false) {
                    Pathfinding.moveTowards(rc, rc.getLocation(), target, 2);
                } else if (mytype == 3) {
                    Pathfinding.moveTest(rc, rc.getLocation(), target, 20);
                }
                else {
                    //Pathfinding.moveTowards(rc, rc.getLocation(), target, 4);
                }
                int after = Clock.getBytecodeNum();
//                if (mytype <= 3)
//                    rc.setIndicatorString("type: " + types[mytype] + " using: " + (after-before));

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();


            /*} *catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();*/
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }



        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    public static int calculateDistance(MapLocation ml1, MapLocation ml2) {
        // Use Manhatten distance for now
        return (Math.abs(ml1.x-ml2.x) + Math.abs(ml1.y-ml2.y));
    }

    public static boolean inPrevLocs(MapLocation loc) {
        for (MapLocation prevLoc : prevLocs) {
            if (loc.equals(prevLoc))
                return true;
        }
        return false;
    }


}

class DJNode implements Comparator<DJNode> {

    // Member variables of this class
    public int node;
    public int cost;

    // Constructors of this class

    // Constructor 1
    public DJNode() {}

    // Constructor 2
    public DJNode(int node, int cost)
    {

        // This keyword refers to current instance itself
        this.node = node;
        this.cost = cost;
    }


    // Method 1
    @Override public int compare(DJNode node1, DJNode node2)
    {

        if (node1.cost < node2.cost)
            return -1;

        if (node1.cost > node2.cost)
            return 1;

        return 0;
    }
}