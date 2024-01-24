package astartest;

import battlecode.common.*;

import java.util.*;
import java.util.concurrent.RunnableScheduledFuture;

import static microplayer.General.directions;

public class RobotPlayer {
    static RobotController rc;
    static MapLocation[] allySpawnLocations;
    static Integer id = null;

    static int[][] mapped;
    /*

    LSB usage for each int in mapped
    0 | 0=unseen 1=seen
    1 | 0=not wall 1=wall

    */

    static MapLocation centerOfMap;
    static Integer mapWidth = null;
    static Integer mapHeight = null;

    static int rngSeed = 12839;
    static Random rng = new Random(rngSeed);
    static MapLocation robotLoc = null;
    static MapLocation previousLocation = null;

    public static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    public static Direction[] directionsCornersFirst = {
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.SOUTHEAST,
            Direction.NORTHWEST,
            Direction.WEST,
            Direction.EAST,
            Direction.SOUTH,
            Direction.NORTH,
    };

    public static void run(RobotController rc) {
        RobotPlayer.rc = rc;
        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (!trySpawnDuck()) {
                    continue;
                }
                onTurn();
                randomizeRng();
            } catch (GameActionException gae) {
                System.out.println("GAMEACTIONEXCEPTION =========");
                gae.printStackTrace();
            } catch (Exception e) {
                System.out.println("EXCEPTION ===================");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void randomizeRng() {
        rngSeed = (rng.nextInt( robotLoc.x + 142891) + robotLoc.y) % 66500;
        rng.setSeed(rngSeed);
    }

    public static boolean trySpawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            robotLoc = rc.getLocation();
            return true;
        } else {
            previousLocation = null;
            robotLoc = null;
        }

        if (rc.readSharedArray(0) == 1) {  // one duck has already been spawned in
            return false;
        }

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        centerOfMap = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        mapped = new int[mapWidth][mapHeight];

        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }

        for (MapLocation allySpawn : allySpawnLocations) {
            if (rc.canSpawn(allySpawn)) {
                rc.spawn(allySpawn);
                System.out.println("spawned in at " + allySpawn);
                if (id == null) {
                    id = rc.readSharedArray(0);
                    rc.writeSharedArray(0, id + 1);
                }
                robotLoc = rc.getLocation();
                return true;
            }
        }
        return false;
    }

    public static void onTurn() throws GameActionException {

        // a star
        aStar();

        // scan for fresh locations
        mapFreshInVisionLocations();

        // debugMappedLocations();

        previousLocation = robotLoc;

    }

    static MapLocation[] getAdjacents(MapLocation loc) {
        MapLocation[] locations = new MapLocation[8];
        for (int i=0; i<8; i++) {
            locations[i] = loc.add(directions[i]);
        }
        return locations;
    }

    static MapLocation[] getAdjacentsCornersFirst(MapLocation loc) {
        MapLocation[] locations = new MapLocation[8];
        for (int i=0; i<8; i++) {
            locations[i] = loc.add(directionsCornersFirst[i]);
        }
        return locations;
    }

    static int[][] aStarGoalDistSquaredMemo;
    static MapLocation aStarGoal;
    static int aStarGoalX;
    static int aStarGoalY;

    public static int aStarHeuristic(MapLocation a, MapLocation b) {
        return a.distanceSquaredTo(aStarGoal)-b.distanceSquaredTo(aStarGoal);
    }

    public static void aStar() throws GameActionException {
        aStarGoal = centerOfMap;
        aStarGoalX = aStarGoal.x;
        aStarGoalY = aStarGoal.y;
        aStarGoalDistSquaredMemo = new int[mapWidth][mapHeight];
        boolean isFirst = true;

        PriorityQueue<MapLocation> queue = new PriorityQueue<>(RobotPlayer::aStarHeuristic);
        queue.add(robotLoc);

        HashMap<MapLocation, Direction> adjToMoveTo = new HashMap<>();
        for (MapLocation adj : getAdjacents(robotLoc)) {
            adjToMoveTo.put(adj, robotLoc.directionTo(adj));
        }

        HashMap<MapLocation, Integer> gScore = new HashMap<>();
        gScore.put(robotLoc, 0);

        loop: while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            if (current.equals(aStarGoal)) {
                break;
                // done
            }

            for (MapLocation neighbor : getAdjacentsCornersFirst(current)) {
                if (isFirst) {
                    adjToMoveTo.put(robotLoc, adjToMoveTo.get(neighbor));
                }
                if (!rc.onTheMap(neighbor)) {
                    continue;
                }
                if ((mapped[neighbor.x][neighbor.y] & 0b10) > 0) {  // is not passable
                    continue;
                }
                int tentativeGScore = gScore.getOrDefault(current, 99999999) + 1;
                if (mapped[neighbor.x][neighbor.y] == 0) {  // not explored yet, so we can just get there probably
                    Direction moveD = adjToMoveTo.get(current);
                    rc.setIndicatorDot(current, 0, 255, 0);
                    if (rc.canMove(moveD)) {
                        rc.move(moveD);
                        robotLoc = robotLoc.add(moveD);
                        break loop;
                    }
                    moveD = moveD.rotateRight();
                    if (rc.canMove(moveD)) {
                        rc.move(moveD);
                        robotLoc = robotLoc.add(moveD);
                        break loop;
                    }
                    moveD = moveD.rotateLeft().rotateLeft();
                    if (rc.canMove(moveD)) {
                        rc.move(moveD);
                        robotLoc = robotLoc.add(moveD);
                        break loop;
                    }
                    break loop;
                }
                if (tentativeGScore < gScore.getOrDefault(neighbor, 99999999)) {
                    if (!queue.contains(neighbor)) {
                        adjToMoveTo.put(neighbor, adjToMoveTo.get(current));
                        gScore.put(neighbor, tentativeGScore);
                        queue.add(neighbor);
                    }
                }
            }
            isFirst = false;
        }
        for (MapLocation ml : gScore.keySet()) {
            int v = 255-(gScore.get(ml)+aStarGoal.distanceSquaredTo(ml)/5)*6;
            rc.setIndicatorDot(ml, v, v, v);
        }

    }

    public static void debugMappedLocations() {
        // draw the seen locations (uses up like 13000+ bytecode)
        for (int x=0; x<mapWidth; x++) {
            for (int y=0; y<mapHeight; y++) {
                int v = mapped[x][y];
                if (v == 0) {
                    continue;
                }
                rc.setIndicatorDot(new MapLocation(x, y), ((v & 0b10) >> 1) * 255, 0, 0);
            }
        }
    }

    public static MapLocation[] getFreshInVisionLocations() {
        int x = robotLoc.x;
        int y = robotLoc.y;
        switch (previousLocation.directionTo(robotLoc)) {
            case NORTH:
                return new MapLocation[]{
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4),
                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2)
                };
            case NORTHEAST:
                return new MapLocation[]{
                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
                        new MapLocation(x+2, y+4), new MapLocation(x+2, y+3), new MapLocation(x+3, y+3),
                        new MapLocation(x+3, y+2), new MapLocation(x+4, y+2), new MapLocation(x+4, y+1),
                        new MapLocation(x+4, y), new MapLocation(x+4, y-1), new MapLocation(x-2, y+4),
                        new MapLocation(x+4, y-2)
                };
            case EAST:
                return new MapLocation[]{
                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2),
                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-3), new MapLocation(x+2, y-4)
                };
            case SOUTHEAST:
                return new MapLocation[]{
                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-2), new MapLocation(x+3, y-3),
                        new MapLocation(x+2, y-3), new MapLocation(x+2, y-4), new MapLocation(x+1, y-4),
                        new MapLocation(x, y-4), new MapLocation(x-1, y-4), new MapLocation(x+4, y+2),
                        new MapLocation(x-2, y-4)
                };
            case SOUTH:
                return new MapLocation[]{
                        new MapLocation(x-4, y-2), new MapLocation(x-3, y-3), new MapLocation(x-2, y-4),
                        new MapLocation(x-1, y-4), new MapLocation(x, y-4), new MapLocation(x+1, y-4),
                        new MapLocation(x+2, y-4), new MapLocation(x+3, y-3), new MapLocation(x+4, y-2)
                };
            case SOUTHWEST:
                return new MapLocation[]{
                        new MapLocation(x+1,  y-4), new MapLocation(x, y-4), new MapLocation(x-1, y-4),
                        new MapLocation(x-2, y-4), new MapLocation(x-2, y-3), new MapLocation(x-3, y-3),
                        new MapLocation(x-3, y-2), new MapLocation(x-4, y-2), new MapLocation(x-4, y-1),
                        new MapLocation(x-4, y), new MapLocation(x-4, y+1), new MapLocation(x-4, y+2),
                        new MapLocation(x+2, y-4)
                };
            case WEST:
                return new MapLocation[]{
                        new MapLocation(x-2, y-4), new MapLocation(x-3, y-3), new MapLocation(x-4, y-2),
                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4)
                };
            case NORTHWEST:
                return new MapLocation[]{
                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+2), new MapLocation(x-3, y+3),
                        new MapLocation(x-2, y+3), new MapLocation(x-2, y+4), new MapLocation(x-1, y+4),
                        new MapLocation(x, y+4), new MapLocation(x+1, y+4), new MapLocation(x-4, y-2),
                        new MapLocation(x+2, y+4)
                };
        }
        System.out.println("big issue 1 !!!");
        return new MapLocation[0];
    }

    public static void mapFreshInVisionLocations() throws GameActionException {

        if (previousLocation == null) {  // just spawned in
            MapInfo[] nearbyInfos = rc.senseNearbyMapInfos();
            for (MapInfo info : nearbyInfos) {
                MapLocation loc = info.getMapLocation();
                if (0 <= loc.x && loc.x < mapWidth && 0 <= loc.y & loc.y < mapHeight) {
                    mapped[loc.x][loc.y] = info.isPassable() ? 0b01 : 0b11;
                }
            }
            return;
        }
        if (previousLocation.equals(robotLoc)) {  // did not move since last turn
            return;
        }

        // did move since last turn, scan new locations
        for (MapLocation l : getFreshInVisionLocations()) {
            if (0 <= l.x && l.x < mapWidth && 0 <= l.y && l.y < mapHeight) {
                if (mapped[l.x][l.y] == 0) {
                    mapped[l.x][l.y] = rc.senseMapInfo(l).isPassable() ? 0b01 : 0b11;
                }
            }
        }

    }
}
