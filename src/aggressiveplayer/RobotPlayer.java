package aggressiveplayer;

import battlecode.common.*;

import java.util.*;

import static aggressiveplayer.Util.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    static RobotController rc;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        RobotPlayer.rc = rc;

        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (!rc.isSpawned()) {
                    allySpawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    MapLocation spawnLoc = allySpawnLocs[rng.nextInt(allySpawnLocs.length)];
                    if (rc.canSpawn(spawnLoc)) {
                        rc.spawn(spawnLoc);
                        spawnLocation = spawnLoc;
                        Game.respawn();
                    } else {
                        continue;
                    }
                }
                code();

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

    static void code() throws GameActionException {
        // handle rng
        MapLocation robotPos = rc.getLocation();
        rngSeed = (rngSeed * 641 + robotPos.x * robotPos.y + robotPos.x) % 1381;
        rng.setSeed(rngSeed);

        if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS) {
            Setup.run(rc);
        } else {
            Game.run(rc);
        }
    }
}
