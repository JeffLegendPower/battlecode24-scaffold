package v2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static v2.RobotPlayer.directions;
import static v2.RobotPlayer.map;
import static v2.Utils.Pair;

public class Pathfinding {
    private static List<MapLocation> best = new ArrayList<>();

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc, target, 10, fillWater);
    }

    public static void moveAway(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc.translate(curLoc.x - target.x, curLoc.y - target.y), curLoc, 10, fillWater);
    }

    private static HashMap<MapLocation, Double> cachedScores = new HashMap<>();
    private static MapLocation lastTarget = null;
    private static HashMap<MapLocation, Integer> visited = new HashMap<>();

    public static void IterativeGreedy(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth, boolean fillWater) throws GameActionException {

        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;
        if (curLoc.equals(target)) return;

        if (lastTarget == null || !lastTarget.equals(target)) {
            cachedScores.clear();
            lastTarget = target;
            visited.clear();
            best.clear();
        }

        if (curLoc.isAdjacentTo(target)) {
            if (rc.canFill(target) && Utils.canBeFilled(rc, target))
                rc.fill(target);

            if (rc.canMove(curLoc.directionTo(target))) {
                rc.move(curLoc.directionTo(target));
                visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
                return;
            } else {
                best.clear();
            }
        }

        target = clamp(target, rc);

        if (!best.isEmpty()) {
            Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

            if (move != null) {
                if (move.b >= 0)
                    best.subList(0, move.b + 1).clear();

                if (rc.canFill(move.a) && Utils.canBeFilled(rc, move.a))
                    rc.fill(move.a);
                if (rc.canMove(curLoc.directionTo(move.a))) {
                    rc.move(curLoc.directionTo(move.a));
                    visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
                } else
                    best.clear();
                return;
            }
        }

        int depth;
        for (depth = 1; depth <= maxDepth; depth++) {
            if (Clock.getBytecodeNum() > 8000) break;
            best = moveTowardsDirect(rc, best.isEmpty() ? curLoc : best.get(best.size() - 1), target, best, fillWater);

        }
        if (best.isEmpty())
            return;

        Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

        if (move == null) {
            best.clear();
            return;
        }

        if (move.b >= 0)
            best.subList(0, move.b + 1).clear();

        if (rc.canFill(move.a) && Utils.canBeFilled(rc, move.a))
            rc.fill(move.a);

        if (rc.canMove(curLoc.directionTo(move.a))) {
            rc.move(curLoc.directionTo(move.a));
            visited.put(curLoc, visited.getOrDefault(curLoc, 0) + 1);
        } else
            best.clear();
    }

    private static Direction lastDir = Direction.CENTER;

    public static List<MapLocation> moveTowardsDirect(RobotController rc, MapLocation curLoc, MapLocation target, List<MapLocation> current, boolean fillWater) throws GameActionException {
        if (curLoc.equals(target)) return current;

        double minScore = 999999;
        MapLocation bestMove = null;
        Direction bestDir = Direction.CENTER;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            newLoc = clamp(newLoc, rc);
            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            MapInfo info = map[newLoc.y][newLoc.x];
            if (info != null &&
                    info.isPassable() &&
                    !current.contains(newLoc) &&
                (fillWater ? (!info.isWall() && !info.isDam() && !(info.isWater() && !Utils.canBeFilled(rc, newLoc))) : info.isPassable())) {
                double score = cachedScores.getOrDefault(newLoc, -1d);
                if (score == -1) {
                    score = calculateDistance(newLoc, target);
                    cachedScores.put(newLoc, score);
                }
                int thisVisited = visited.getOrDefault(newLoc, 0);
                score *= (info.isWater() ? 1.1 : 1)
//                        * (isOnWall(rc, newLoc) ? 1 : 1.1)
                        * ((thisVisited == 0) ? 1 : thisVisited * 1.1)
                        * (dir == lastDir.opposite() ? 1.2 : 1);
                if (score < minScore) {
                    minScore = score;
                    bestMove = newLoc;
                    bestDir = dir;
                }
            }
        }
        lastDir = bestDir;

        if (bestMove == null) return current;

        rc.setIndicatorDot(bestMove, 255, 0, 0);
        rc.setIndicatorLine(curLoc, bestMove, 0, 255, 0);
        current.add(bestMove);

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

    private static MapLocation clamp(MapLocation loc, RobotController rc) {
        return new MapLocation(Math.max(1, Math.min(rc.getMapWidth() - 1, loc.x)), Math.max(1, Math.min(rc.getMapHeight() - 1, loc.y)));
    }
}