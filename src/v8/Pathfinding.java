package v8;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static v8.RobotPlayer.directions;
import static v8.RobotPlayer.map;
import static v8.Utils.Pair;

public class Pathfinding {
    private static List<MapLocation> best = new ArrayList<>();

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        moveTowards(rc, curLoc, target, 10, fillWater);
    }

    public static void moveAway(RobotController rc, MapLocation curLoc, MapLocation target, boolean fillWater) throws GameActionException {
        moveTowards(rc, curLoc.translate(curLoc.x - target.x, curLoc.y - target.y), curLoc, 10, fillWater);
    }

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth, boolean fillWater) throws GameActionException {

        if (!rc.isSpawned()) return; // Prevent NPEs
        if (!rc.isMovementReady()) return;
        if (curLoc.equals(target)) return;

        try { // TODO: idk this sucks
            if (curLoc.isAdjacentTo(target)) {
                if (rc.canMove(curLoc.directionTo(target))) {
                    rc.move(curLoc.directionTo(target));
                    return;
                }
            }

        } catch (NullPointerException e) {
            System.out.println("Failure to path find to " + target);
            return;
        }


        int x = Math.max(0, Math.min(rc.getMapWidth(), target.x));
        int y = Math.max(0, Math.min(rc.getMapHeight(), target.y));
        target = new MapLocation(x, y);

        if (!best.isEmpty()) {
            Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

            if (move == null) {
            } else {
                for (int i = 0; i <= move.b; i++)
                    best.remove(0);

                if (rc.canFill(move.a) && Utils.canBeFilled(rc, move.a))
                    rc.fill(move.a);

                if (rc.canMove(curLoc.directionTo(move.a))) {
                    rc.move(curLoc.directionTo(move.a));
                }
                return;
            }
        }

        int depth;
        for (depth = 1; depth <= maxDepth; depth++) {
            if (Clock.getBytecodeNum() > 8000) break;
            best = moveTowardsDirect(rc, curLoc, target, best, fillWater);
        }
        if (best.isEmpty()) {
            return;
        }
        Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

        if (move == null) {
            return;
        }

        for (int i = 0; i <= move.b; i++)
            best.remove(0);

        if (rc.canFill(move.a) && Utils.canBeFilled(rc, move.a))
            rc.fill(move.a);

        if (rc.canMove(curLoc.directionTo(move.a))) {
            rc.move(curLoc.directionTo(move.a));
        }
    }

    public static List<MapLocation> moveTowardsDirect(RobotController rc, MapLocation curLoc, MapLocation target, List<MapLocation> current, boolean fillWater) throws GameActionException {
        if (curLoc.equals(target)) return current;

        int bestDist = 999999;
        MapLocation bestMove = null;

        for (Direction dir : directions) {
            MapLocation newLoc = curLoc.add(dir);
            int x = Math.max(1, Math.min(rc.getMapWidth() - 1, newLoc.x));
            int y = Math.max(1, Math.min(rc.getMapHeight() - 1, newLoc.y));
            newLoc = new MapLocation(x, y);
            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            MapInfo info = map[newLoc.y][newLoc.x];
            if (info != null &&
                    (fillWater ? (!info.isWall() && !info.isDam() && !(info.isWater() && Utils.canBeFilled(rc, newLoc))) : info.isPassable())) {
                int newDist = calculateDistance(newLoc, target) + (info.isWater() ? 2 : 0);
                if (newDist < bestDist) {
                    bestDist = newDist;
                    bestMove = newLoc;
                }
            }
        }
        if (bestMove == null) return current;
        current.add(bestMove);

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
        return ml1.distanceSquaredTo(ml2); // Euclidean distance squared
    }
}