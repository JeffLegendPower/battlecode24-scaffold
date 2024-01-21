package badapple;

import battlecode.common.*;

import java.util.Random;

import static badapple.BadAppleData.data;
import static badapple.BadAppleData.setupData;

public strictfp class RobotPlayer {

    static Random rng = new Random();
    static Integer row = null;

    static final Direction[] directions = {
            Direction.NORTHEAST,
            Direction.SOUTHWEST,
            Direction.EAST,
            Direction.WEST,
            Direction.SOUTH,
            Direction.NORTH,
            Direction.NORTHWEST,
            Direction.SOUTHEAST,
    };

    public static void run(RobotController rc) {
        int frame = -1;
        setupData();
        // noinspection InfiniteLoopStatement
        while (true) {
            frame += 1;
            try {
                if (!rc.isSpawned()) {
                    MapLocation[] allySpawnLocs = rc.getAllySpawnLocations();
                    boolean foundSpot = false;
                    for (MapLocation spawnLoc : allySpawnLocs) {
                        if (rc.canSpawn(spawnLoc)) {
                            rc.spawn(spawnLoc);
                            foundSpot = true;
                            break;
                        }
                    }
                    if (!foundSpot) {
                        continue;
                    }
                }

                if (row == null) {
                    int myRow = rc.readSharedArray(0);
                    if (myRow != rc.getMapHeight()) {
                        row = myRow;
                        rc.writeSharedArray(0, myRow + 1);
                    }
                }

                Direction randomDir = directions[rng.nextInt(directions.length)];
                rc.setIndicatorString(String.valueOf(row));
                if (rc.canMove(randomDir)) {
                    rc.move(randomDir);
                }

                int height = rc.getMapHeight();
                if (row != null) {
                    if (frame >= data.length) {
                        continue;
                    }
                    int[] dat = data[frame][row];
                    int startP = 0;
                    int color = 0;
                    for (int v : dat) {
                        for (int i2 = 0; i2 < v; i2++) {
                            rc.setIndicatorDot(new MapLocation(i2 + startP, height-row-1), color, color, color);
                        }
                        color = color == 0 ? 255 : 0;
                        startP += v;
                    }
                }

            } catch (GameActionException e) {
                // we made an illegal move
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // this is really bad
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }
}
