package v10_2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static v10_2.RobotPlayer.*;
import static v10_2.Utils.*;
public class Pathfinding {
    private static List<MapLocation> best = new ArrayList<>();
    public static MapLocation currentTarget = null;

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc, target, 10, fillWater, false);
//        rc.setIndicatorString("Target: " + target);
    }

    // Will avoid robots
    public static void moveTowardsAfraid(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc, target, 10, fillWater, true);
//        rc.setIndicatorString("Target: " + target);
    }

    public static void moveAway(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        IterativeGreedy(rc, curLoc.translate(curLoc.x - target.x, curLoc.y - target.y), curLoc, 10, fillWater, false);
    }
    private static MapLocation lastTarget = null;
    public static HashMap<MapLocation, Integer> visited = new HashMap<>();

    public static void IterativeGreedy(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth, boolean fillWater, boolean afraid) throws GameActionException {
        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;
        if (curLoc.equals(target)) return;

        currentTarget = target;

        if (lastTarget != null && !lastTarget.isWithinDistanceSquared(target, 10))
            visited.clear();
        if (lastTarget == null || !lastTarget.equals(target)) {
            best.clear();
            lastTarget = target;
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
                } else {
                    best.clear();
                }
//                return;
            }
        }

        int depth;
        int startBytecode = Clock.getBytecodeNum();
        for (depth = 1; depth <= maxDepth; depth++) {
            if (Clock.getBytecodeNum() - startBytecode > 6000 || Clock.getBytecodeNum() > 20000) break;
            best = moveTowardsDirect(rc, best.isEmpty() ? curLoc : best.get(best.size() - 1), target, best, fillWater, afraid);

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
        } else {
            best.clear();
        }

        // DEBUG
//        MapLocation lastLoc = curLoc;
//        for (MapLocation loc : best) {
//            rc.setIndicatorDot(loc, 255, 0, 0);
//            rc.setIndicatorLine(lastLoc, loc, 0, 255, 0);
//            lastLoc = loc;
//        }
    }

    private static Direction lastDir = Direction.CENTER;

    public static List<MapLocation> moveTowardsDirect(RobotController rc, MapLocation curLoc, MapLocation target, List<MapLocation> current, boolean fillWater, boolean afraid) throws GameActionException {
        if (curLoc.equals(target)) return current;

        double minScore = 999999;
        MapLocation bestMove = null;
        Direction bestDir = Direction.CENTER;

        int centerx = rc.getMapWidth() / 2;
        int centery = rc.getMapHeight() / 2;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            newLoc = clamp(newLoc, rc);
            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            MapInfo info = map[newLoc.y][newLoc.x];
            if (info != null && !current.contains(newLoc) &&
                (fillWater ? (!info.isWall() && !info.isDam() && !(info.isWater() && !Utils.canBeFilled(rc, newLoc))) : info.isPassable())) {
                double score = calculateDistance(newLoc, target);
                int thisVisited = visited.getOrDefault(newLoc, 0);
                score *= (info.isWater() ? 1.35 : 1)
//                        * (isOnWall(rc, newLoc) ? 1 : 1.1)
                        * ((thisVisited == 0) ? 1 : thisVisited * 1.1)
                        * (dir == lastDir.opposite() ? 1.2 : 1);

                if (afraid) {
                    score *= (enemiesNearby(rc, newLoc) ? 1.6 : 1);
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

    private static boolean enemiesNearby(RobotController rc, MapLocation loc) throws GameActionException {
        // Check in 3x3 area around loc
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Don't check the center loc
                MapLocation newLoc = loc.translate(dx, dy);
                if (rc.canSenseRobotAtLocation(newLoc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                    if (robot != null && robot.getTeam().equals(rc.getTeam().opponent())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}