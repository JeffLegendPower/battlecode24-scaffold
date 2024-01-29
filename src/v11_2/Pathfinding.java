package v11_2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static v11_2.RobotPlayer.directions;
import static v11_2.RobotPlayer.map;
import static v11_2.Utils.Pair;
import static v11_2.Utils.clamp;
public class Pathfinding {
//    private static List<MapLocation> best = new ArrayList<>();
    private static MapLocation[] best = new MapLocation[51];
    public static MapLocation currentTarget = null;

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc, target, 50, fillWater, false);
//        rc.setIndicatorString("Target: " + target);
    }

    // Will avoid robots
    public static void moveTowardsAfraid(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc, target, 50, fillWater, true);
//        rc.setIndicatorString("Target: " + target);
    }

    public static void moveAway(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc.translate(curLoc.x - target.x, curLoc.y - target.y), curLoc, 5, fillWater, false);
    }
    private static MapLocation lastTarget = null;
    public static HashMap<MapLocation, Integer> visited = new HashMap<>();

    public static void IterativeGreedy(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth, boolean fillWater, boolean afraid) throws GameActionException {
        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;
        if (curLoc.equals(target)) return;

        currentTarget = target;
        lastDir = Direction.CENTER;

        if (lastTarget != null && !lastTarget.isWithinDistanceSquared(target, 10))
            visited.clear();
        if (lastTarget == null || !lastTarget.equals(target)) {
//            best.clear();
            best = new MapLocation[51];
            lastTarget = target;
        }

        if (curLoc.isAdjacentTo(target)) {
            if (rc.canFill(target))
                rc.fill(target);

            if (rc.canMove(curLoc.directionTo(target))) {
                rc.move(curLoc.directionTo(target));
                visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
                return;
            } else {
//                best.clear();
                best = new MapLocation[51];
            }
        }

        target = clamp(target, rc);

//        if (!best.isEmpty()) {
        if (best[0] != null) {
            Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

            if (move != null) {
                if (move.b >= 0)
                    System.arraycopy(best, move.b + 1, best, 0, best.length - move.b - 1);


                if (rc.canFill(move.a))
                    rc.fill(move.a);
                if (rc.canMove(curLoc.directionTo(move.a))) {
                    rc.move(curLoc.directionTo(move.a));
                    visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
                } else {
//                    best.clear();
                    best = new MapLocation[51];
                }
            }
        }

        int depth;
        int startBytecode = Clock.getBytecodeNum();

//        MapLocation lastLoc = curLoc;
        int idx = -1;
        for (int i = 0; i < best.length; i++) {
            if (best[i] == null) break;
            idx = i;
        }

        for (depth = 1; depth <= maxDepth; depth++) {
            if ((Clock.getBytecodeNum() - startBytecode > 6000 || Clock.getBytecodeNum() > 20000)) break;
            MapLocation lastLoc = idx == -1 ? curLoc : best[idx];
            if (lastLoc == null || idx + 1 >= best.length) break;
            best = moveTowardsDirect(rc, lastLoc, target, best, ++idx, fillWater, afraid);
        }

        if (best[0] == null)
            return;

        Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

        if (move == null) {
            best = new MapLocation[51];
            return;
        }

        if (move.b >= 0)
            System.arraycopy(best, move.b + 1, best, 0, best.length - move.b - 1);

        if (rc.canFill(move.a))
            rc.fill(move.a);

        if (rc.canMove(curLoc.directionTo(move.a))) {
            rc.move(curLoc.directionTo(move.a));
            visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
        } else {
            best = new MapLocation[51];
        }

        // DEBUG
//        if (new Random().nextInt(5) == 0) {
//            MapLocation prevLoc = curLoc;
//            int curDist = curLoc.distanceSquaredTo(target);
//            for (MapLocation loc : best) {
//                if (loc == null) continue;
////                rc.setIndicatorDot(loc, 255, 0, 0);
//                // Have a color gradient from gray to black/red, gray is same as current dist, red is closer, black is farther
//                int dist = loc.distanceSquaredTo(target);
//                int r = (int) (255 * (1 - (double) dist / curDist));
//                int g = 0;
//                int b = (int) (255 * ((double) dist / curDist));
//                rc.setIndicatorDot(loc, r, g, b);
//                rc.setIndicatorLine(prevLoc, loc, 0, 255, 0);
//                prevLoc = loc;
//            }
//        }
    }

    private static Direction lastDir = Direction.CENTER;

    public static MapLocation[] moveTowardsDirect(RobotController rc, MapLocation curLoc, MapLocation target, MapLocation[] current, int idx, boolean fillWater, boolean afraid) throws GameActionException {
        if (curLoc.equals(target)) return current;

        double minScore = 999999;
        MapLocation bestMove = null;
        Direction bestDir = Direction.CENTER;

        int centerx = rc.getMapWidth() / 2;
        int centery = rc.getMapHeight() / 2;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            if (!rc.onTheMap(newLoc)) continue;
            newLoc = clamp(newLoc, rc);

            Direction opposite = lastDir.opposite();
            if (dir == opposite || dir == opposite.rotateLeft() || dir == opposite.rotateRight()) continue;

//            if (numOverlap(adjacents(rc, newLoc), current) >= 5) continue;

            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            MapInfo info = map[newLoc.x][newLoc.y];
            if (info != null && Utils.indexOf(current, newLoc) == -1 &&
                    (fillWater ? (!info.isWall() && !info.isDam()) : info.isPassable())) {
                double score = newLoc.distanceSquaredTo(target);
                int thisVisited = visited.getOrDefault(newLoc, 0);

//                Direction opposite = dir.opposite();

                score *= (info.isWater() ? 1.35 : 1)
                        * ((thisVisited == 0) ? 1 : thisVisited * 1.5);

                if (afraid) {
                    score *= (double) (enemiesNearby(rc, newLoc) + 1);
//                    int centerdist = Math.max(Math.abs(centerx - newLoc.x), Math.abs(centery - newLoc.y));
//                    score *= (centerdist <= Math.min(rc.getMapWidth(), rc.getMapHeight()) / 4 ? 2 : 1);

                }

                if (score < minScore) {
                    minScore = score;
                    bestMove = newLoc;
                    bestDir = dir;
                }
            }
        }
        lastDir = bestDir;

        if (bestMove == null) return current;

        current[idx] = bestMove;

        return current;
    }

    private static Pair<MapLocation, Integer> getLastAdjacent(MapLocation curLoc, MapLocation[] moves) {
        if (moves == null) return null;
        for (int i = moves.length - 1; i >= 0; i--) {
            if (moves[i] == null) continue;
            if (moves[i].isAdjacentTo(curLoc))
                return new Pair<>(moves[i], i);
        }
        return null;
    }

    private static boolean isOnWall(RobotController rc, MapLocation loc) {
        boolean isWallHugging = false;
        for (Direction dir : directions) {
            MapLocation newLoc = clamp(loc.add(dir), rc);
            MapInfo info = map[newLoc.y][newLoc.x];
            if (info != null && info.isWall()) {
                isWallHugging = true;
                break;
            }
        }
        return isWallHugging;
    }

    public static int calculateDistance(MapLocation ml1, MapLocation ml2) {
        return ml1.distanceSquaredTo(ml2); // Euclidean distance squared
    }

    private static int enemiesNearby(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length;
    }
}