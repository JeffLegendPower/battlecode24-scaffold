package bytecoderesearch;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

public class RobotPlayer {
    static RobotController rc = null;
    static MapLocation[] allySpawnLocations = null;
    static MapLocation[] centerSpawnLocations = null;

    public static void run(RobotController rc) {
        RobotPlayer.rc = rc;
        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (!tryToSpawnDuck()) {
                    continue;
                }
                onTurn();
            } catch (GameActionException gae) {
                System.out.println("GAMEACTIONEXCEPTION ================");
                gae.printStackTrace();
            } catch (Exception exception) {
                System.out.println("EXCEPTION ==========================");
                exception.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static boolean tryToSpawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            return true;
        }

        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }

        for (MapLocation spawnLoc : allySpawnLocations) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                return true;
            }
        }
        return false;
    }

    static int stepDistance(MapLocation a, MapLocation b) {
        // distance in how many steps to get there
        return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
    }

    static MapLocation a = new MapLocation(3, 4);
    static MapLocation b = new MapLocation(3, 8);

    static <T> T[] sort(T[] list, Function<T, Integer> valueFn) {
        Arrays.sort(list, Comparator.comparingInt(valueFn::apply));
        return list;
    }

    public static void onTurn() throws GameActionException {
        int i = 4;

        int bcBefore = Clock.getBytecodeNum();

        int thing = 23 < i ? 4 : 6;

        rc.setIndicatorString(String.valueOf(Clock.getBytecodeNum()-bcBefore-1));

        if (rc.getRoundNum() == 100) {
            rc.resign();
        }
    }
}
