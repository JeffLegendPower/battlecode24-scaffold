package turtleplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Random;

public class General {

    public static RobotController rc;

    public static int rngSeed = 238;
    public static Random rng = new Random(rngSeed);

    public static MapLocation[] allySpawnLocations;

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

    /*
        Shared array
        not used yet
     */

}
