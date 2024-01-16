package MANNAN;

import battlecode.common.*;
import scala.Int;

import javax.swing.text.ParagraphView;
import java.lang.reflect.Array;
import java.util.*;


import static v7.Util.Pair;

public class Pathfinding {

    private static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static ArrayList<MapLocation> prevLocs = new ArrayList<>();
    private static Random rng = new Random();

    private static Deque<MapLocation> toMove = new ArrayDeque<>();
    private static int[][] floodMap = new int[9][9];
    private static MapInfo[][] locMap = new MapInfo[9][9];
    private static boolean[][] visited = new boolean[9][9];
    private static MapLocation start = null;

    private static boolean flood(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater, int depth) throws GameActionException {
        if (Clock.getBytecodeNum() > 6000) return false;
        if (!rc.canSenseLocation(curLoc) ||
                visited[curLoc.y - start.y + 4][curLoc.x - start.x + 4] ||
                locMap[curLoc.y - start.y + 4][curLoc.x - start.x + 4].isWall())
            return true;
        if (!fillWater && locMap[curLoc.y - start.y + 4][curLoc.x - start.x + 4].isWater())
            return true;
        visited[curLoc.y - start.y + 4][curLoc.x - start.x + 4] = true;
        floodMap[curLoc.y - start.y + 4][curLoc.x - start.x + 4] = depth;
        int add = 1;
        if(locMap[curLoc.y - start.y + 4][curLoc.x - start.x + 4].isWater())
            add++;

        // Straight
        if (!flood(rc, new MapLocation(curLoc.x - 1, curLoc.y), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x + 1, curLoc.y), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x, curLoc.y - 1), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x, curLoc.y + 1), target, fillWater, depth + add)) return false;

        // Diagonals
        if (!flood(rc, new MapLocation(curLoc.x - 1, curLoc.y - 1), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x + 1, curLoc.y - 1), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x - 1, curLoc.y + 1), target, fillWater, depth + add)) return false;
        if (!flood(rc, new MapLocation(curLoc.x + 1, curLoc.y + 1), target, fillWater, depth + add)) return false;

        return true;
    }

    private static boolean validCoord(MapLocation testLoc) {
        int x = testLoc.x - start.x + 4;
        int y = testLoc.y - start.y + 4;
        return (x >= 0 && x < 9 && y >= 0 && y < 9);
    }

    private static void prunedDFS(RobotController rc, MapLocation curLoc, MapLocation target, Deque<MapLocation> curPath) throws GameActionException {
        if (curLoc.equals(target)) {
            toMove = curPath;
            return;
        }
        int nextDepth = floodMap[curLoc.y - start.y + 4][curLoc.x - start.x + 4] + 1;
        for (Direction dir : directions) {
            MapLocation child = curLoc.add(dir);
            if (validCoord(child) && floodMap[child.y - start.y + 4][child.x - start.x + 4] == nextDepth) {
                curPath.addLast(child);
                prunedDFS(rc, child, target, curPath);
                curPath.removeLast();
            }
        }
    }

    public static MapLocation closestPointTo(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        MapLocation minDist = curLoc;
        for (MapInfo info : rc.senseNearbyMapInfos())
            if (!info.isWall() && (fillWater || !info.isWater()))
                if (info.getMapLocation().distanceSquaredTo(target) < minDist.distanceSquaredTo(target))
                    minDist = info.getMapLocation();
        return minDist;
    }

    public static void ffdfs(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        if(!rc.isSpawned() || !rc.isMovementReady())
            return;
        if(toMove.isEmpty()) {
            if(!rc.canSenseLocation(target))
                target = closestPointTo(rc, curLoc, target, fillWater);
            start = curLoc;
            MapInfo[] infos = rc.senseNearbyMapInfos();
            for(MapInfo info : infos) {
                MapLocation loc = info.getMapLocation();
//                if (loc.x - curLoc.x + 4 < 0 || loc.y - curLoc.y + 4 < 0)
//                    System.out.println(curLoc.x + ", " + curLoc.y + "  " + loc.x + ", " + loc.y);
                locMap[loc.y - curLoc.y + 4][loc.x - curLoc.x + 4] = info;
            }
            boolean passed = flood(rc, curLoc, target, fillWater, 0);

            if (passed) {
                prunedDFS(rc, curLoc, target, new ArrayDeque<>());
                if (toMove.peekFirst() != null && rc.canMove(curLoc.directionTo(toMove.peekFirst())))
                    rc.move(curLoc.directionTo(toMove.peekFirst()));
//                else
//                    System.out.println(toMove.peekFirst());
            }
            else
                simpleMoveTowards(rc, curLoc, target, fillWater, true, 2);

//            rc.setIndicatorString(passed ? "Passed" : "Failed");
        }
        else {
            Direction dir = curLoc.directionTo(toMove.remove());
            if(rc.canMove(dir))
                rc.move(dir);
        }
    }


    public static Direction simpleMoveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater, boolean doMove, int depth) throws GameActionException {
        if (!rc.isSpawned()) return null; // Prevent NPEs
        if (!rc.isMovementReady()) return null;

        int x = Math.max(1, Math.min(rc.getMapWidth() - 1, target.x));
        int y = Math.max(1, Math.min(rc.getMapHeight() - 1, target.y));
        target = new MapLocation(x, y);


//        rc.setIndicatorString("x: " + target.x + " y: " + target.y);
        // TODO: THIS IS PROBABLY INEFFICIENT!!! FIND BETTER WAY OF CALCING CLOSEST POINT
        int closestPointDist = 999999;
        int dist;
        Direction bestDir = Direction.CENTER;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            if (depth != 0)
                newLoc = curLoc.add(simpleMoveTowards(rc, newLoc, target, false, false, depth-1));
            dist = calculateDistance(newLoc, target);
            if (dist < closestPointDist || (dist == closestPointDist && rng.nextBoolean())) {
                bestDir = dir;
                closestPointDist = dist;
            }
        }


        Direction rightDir = bestDir;
        if (rc.canFill(curLoc.add(bestDir)) && fillWater && rng.nextInt(10) > 8)
            rc.fill(curLoc.add(bestDir));


        while (!rc.senseMapInfo(curLoc.add(rightDir)).isPassable()){// || (rng.nextInt(10) > 7 && inPrevLocs(rc.getLocation().add(rightDir)))) {
            rightDir = rightDir.rotateRight();
            if (rightDir.equals(bestDir)) {
//                prevLocs = new ArrayList<>();
                break;
            }
        }

//        prevLocs.add(rc.getLocation());
//        if (prevLocs.size() > 7) {
//            prevLocs.remove(0);
//        }

        if (rc.canMove(rightDir) && doMove) {
            rc.move(rightDir);
        }
        return rightDir;
    }

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        ffdfs(rc, curLoc, target, fillWater);
    }

    public static void moveTowards(RobotController rc, MapLocation curLoc, Direction target, boolean fillWater) throws GameActionException {
        moveTowards(rc, curLoc, curLoc.add(target), fillWater);
    }

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, int depth) throws GameActionException {
        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;

        int x = Math.max(1, Math.min(rc.getMapWidth() - 1, target.x));
        int y = Math.max(1, Math.min(rc.getMapHeight() - 1, target.y));
        target = new MapLocation(x, y);
        target = closestPointTo(rc, curLoc, target, false);

        prevLocs2 = new HashSet<>();
//        plyscores = new int[depth];
//        for (int i=0;i<depth;i++)
//            plyscores[i] = 999;
//        scoreHashMap = new HashMap<>();
//        plyHashMap = new HashMap<>();
        nodes = 0;
//        usingPlyMap = 0;

        moveTowardsDepth(rc, curLoc, target, depth, 0, false);//depth == 3);
        System.out.println("Bestmove: " + bestMove);
        if (rc.canMove(curLoc.directionTo(bestMove)))
            rc.move(curLoc.directionTo(bestMove));

        if (depth == 3)
            System.out.println();
    }

    private static MapLocation bestMove = null;

    private static HashSet<MapLocation> prevLocs2 = new HashSet<>();
//    private static int[] plyscores;
    private static HashMap<MapLocation, Pair<Integer, Integer>> scoreHashMap = new HashMap<>(); // first int score second int ply

    // minimize score
    private static int nodes = 0;
    public static int moveTowardsDepth(RobotController rc, MapLocation curLoc, MapLocation target, int depth, int ply, boolean print) throws GameActionException {
        nodes++;
        int before1 = Clock.getBytecodeNum();
        if (curLoc.equals(target)) return 0;
        if (ply >= depth) return curLoc.distanceSquaredTo(target);

        List<Pair<MapLocation, Integer>> possibleMoves = new ArrayList<>();
//        int curDist = calculateDistance(curLoc, target);
        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).isPassable() && !prevLocs2.contains(newLoc)) {
                int newDist = calculateDistance(newLoc, target);
                possibleMoves.add(new Pair<>(newLoc, newDist));
            }
        }
        int after1 = Clock.getBytecodeNum();
        int before2 = Clock.getBytecodeNum();

        possibleMoves.sort(Comparator.comparingInt(a -> a.b));

        int minScore = 9999999;

        int checked = 0;

        int fails = 0;

        for (Pair<MapLocation, Integer> move : possibleMoves) {
            if (checked > depth - fails) break;
            if (checked > 2) depth--;
//            if (Clock.getBytecodeNum() > 10000) return minScore;
            rc.setIndicatorDot(move.a, 255, 0,0);
            rc.setIndicatorLine(curLoc, move.a, 0, 255, 0);
            prevLocs2.add(curLoc);

            /*if (move.b > plyscores[ply])
                return 999;*/

            Pair<Integer, Integer> score = scoreHashMap.get(move.a);
            if (score == null || score.b > ply) {
                score = new Pair<>(moveTowardsDepth(rc, move.a, target, depth, ply + 1, print) * (ply + 1), ply);
                rc.setIndicatorLine(curLoc, move.a, 0, 0, 255);
                scoreHashMap.put(move.a, score);
            }

            prevLocs2.remove(curLoc);

            if (score.a == 0) {
                return score.a;
            }

            if (score.a < minScore) {
                minScore = score.a;
                fails = 0;
                if (ply == 0)
                    bestMove = move.a;
            } else
                fails++;

//            if (score.a < plyscores[ply]) {
//                plyscores[ply] = score.a;
//            }

            checked++;
        }

        int after2 = Clock.getBytecodeNum();
        if (print)
            System.out.println("side:  depth: " + depth + " ply: " + ply + " nodes: " + nodes + " time2: " + (after2-before2) + " time1: " + (after1-before1));
        return minScore;
    }


    private static LinkedList<MapLocation> best = new LinkedList<>();

    private static String lastmsg = "";
    private static int cycles = 0;

    public static void moveTest(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth) throws GameActionException {
        cycles = 0;
        rc.setIndicatorString(lastmsg);

        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;

        int x = Math.max(1, Math.min(rc.getMapWidth() - 1, target.x));
        int y = Math.max(1, Math.min(rc.getMapHeight() - 1, target.y));
        target = new MapLocation(x, y);

        if (!best.isEmpty()) {
            Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

            if (move == null) {
            } else {
                for (int i = 0; i <= move.b; i++)
                    best.remove(0);

                if (rc.canMove(curLoc.directionTo(move.a))) {
                    rc.move(curLoc.directionTo(move.a));
                }
                return;
            }
        }

        int depth;
        for (depth = 1; depth <= maxDepth; depth++) {
            if (Clock.getBytecodeNum() > 10000) break;
            best = moveTowardsDirect(rc, curLoc, best.isEmpty() ? curLoc : best.getLast(), target, best);
        }
        if (best.isEmpty()) return;
        Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

        if (move == null)
            return;

        for (int i = 0; i <= move.b; i++)
            best.remove(0);

        if (rc.canMove(curLoc.directionTo(move.a))) {
            rc.move(curLoc.directionTo(move.a));
        }

        rc.setIndicatorString("depth: " + depth + " time: " + (Clock.getBytecodeNum()) + " cycles: " + cycles);
        lastmsg = "depth: " + depth + " time: " + (Clock.getBytecodeNum()) + " cycles: " + cycles;
    }

    public static LinkedList<MapLocation> moveTowardsDirect(RobotController rc, MapLocation origLoc, MapLocation curLoc, MapLocation target, LinkedList<MapLocation> current) throws GameActionException {
        cycles++;
        if (curLoc.equals(target)) return current;

        int bestDist = 999999;
        MapLocation bestMove = null;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            int x = Math.max(1, Math.min(rc.getMapWidth() - 1, newLoc.x));
            int y = Math.max(1, Math.min(rc.getMapHeight() - 1, newLoc.y));
            newLoc = new MapLocation(x, y);
            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            MapInfo info = RobotPlayer.map[newLoc.y][newLoc.x];
            if (info != null &&
                    info.isPassable() &&
                    !current.contains(newLoc) &&
                    !origLoc.equals(newLoc) && !curLoc.equals(newLoc)) {

                int newDist = calculateDistance(newLoc, target);
                if (newDist < bestDist) {
                    bestDist = newDist;
                    bestMove = newLoc;
                }
            }
        }
        if (bestMove == null) return current;
        current.add(bestMove);

        if (rc.getRoundNum() == 5)
            System.out.println(bestMove);
        rc.setIndicatorDot(bestMove, 255, 0, 0);
        rc.setIndicatorLine(curLoc, bestMove, 0, 255, 0);

        return current;
    }

    private static Pair<MapLocation, Integer> getLastAdjacent(MapLocation curLoc, List<MapLocation> moves) {
        if (moves == null) return null;
        for (int i = moves.size() - 1; i >= 0; i--) {
            if (moves.get(i).isAdjacentTo(curLoc))
                return new Pair<>(moves.get(i), i);
        }
        return null;
    }

    public static int calculateDistance(MapLocation ml1, MapLocation ml2) {
//        return Math.max(Math.abs(ml1.x - ml2.x), Math.abs(ml1.y - ml2.y)); // Chebyshev distance
        return ml1.distanceSquaredTo(ml2); // Euclidean distance squared
//        return (Math.abs(ml1.x-ml2.x) + Math.abs(ml1.y-ml2.y)); // Manhatten distance
    }
}