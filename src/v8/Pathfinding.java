package v8;

import battlecode.common.*;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static v8.RobotPlayer.directions;
import static v8.Utils.Pair;

public class Pathfinding {
    private static List<MapLocation> best = new ArrayList<>();



    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target) throws GameActionException {
        moveTowards(rc, curLoc, target, 10);
    }

    private static HashMap<MapLocation, Integer> cached = new HashMap<>();
    private static MapLocation lastTarget = null;

    public static void moveTowards(RobotController rc, MapLocation curLoc, MapLocation target, int maxDepth) throws GameActionException {

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

            // degrade maxdepth if its too close to target
            int distToTarget = calculateDistance(curLoc, target);
            if (distToTarget < 3) {
                maxDepth = Math.min(1, maxDepth);
                best = new ArrayList<>();
            } else if (distToTarget < 5) {
                maxDepth = Math.min(3, maxDepth);
                best = new ArrayList<>();
            }

            if (lastTarget == null || !lastTarget.equals(target)) {
                cached.clear();
                lastTarget = target;
            }
        } catch (NullPointerException e) {
            System.out.println("Failure to path find to " + target);
            return;
        };


        int x = Math.max(0, Math.min(rc.getMapWidth(), target.x));
        int y = Math.max(0, Math.min(rc.getMapHeight(), target.y));
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
            if (Clock.getBytecodeNum() > 8000) break;
            if (calculateDistance(curLoc, target) < 5 && depth > 2) break; //limit depth for shorter objectives
            best = moveTowardsDirect(rc, curLoc, target, maxDepth, 0, best, 8000);
        }
//        System.out.println(Clock.getBytecodeNum() + " depth: " + depth);
        if (best.isEmpty()) {
            return;
        }
        Pair<MapLocation, Integer> move = getLastAdjacent(curLoc, best);

        if (move == null) {
            return;
        }

        for (int i = 0; i <= move.b; i++)
            best.remove(0);

        if (rc.canMove(curLoc.directionTo(move.a))) {
            rc.move(curLoc.directionTo(move.a));
        }
    }

    public static List<MapLocation> moveTowardsDirect(RobotController rc, MapLocation curLoc, MapLocation target, int depth, int ply, List<MapLocation> current, int maxBytecode) throws GameActionException {
        if (curLoc.equals(target)) return current;
        if (ply >= depth) return current;

        int bestDist = 999999;
        MapLocation bestMove = null;

        // Debug
        boolean fromCache = true;

        for (Direction dir : directions) {
            if (Clock.getBytecodeNum() > maxBytecode) return current;
            MapLocation newLoc = curLoc.add(dir);
            int x = Math.max(1, Math.min(rc.getMapWidth() - 1, newLoc.x));
            int y = Math.max(1, Math.min(rc.getMapHeight() - 1, newLoc.y));
            newLoc = new MapLocation(x, y);
            if (rc.canSenseRobotAtLocation(newLoc)) continue;
            if (RobotPlayer.map[newLoc.y][newLoc.x] != null &&
                    RobotPlayer.map[newLoc.y][newLoc.x].isPassable() &&
                    !current.contains(newLoc)) {

                int newDist = cached.getOrDefault(newLoc, -1);
                if (newDist == -1) {
                    fromCache = false;
                    newDist = calculateDistance(newLoc, target);
                    cached.put(newLoc, newDist);
                }
                if (newDist < bestDist) {
                    bestDist = newDist;
                    bestMove = newLoc;
                }
            }
        }
        if (bestMove == null) {
            return current;
        }
        current.add(bestMove);

        //rc.setIndicatorDot(bestMove, 255, 0,0);
        //rc.setIndicatorLine(curLoc, bestMove, 0, fromCache ? 0 : 255, fromCache ? 255 : 0);

        return moveTowardsDirect(rc, bestMove, target, depth, ply + 1, current, maxBytecode);
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