package aggressiveplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;

public class Util {
    static int rngSeed = 41;
    static boolean liableForFlag = false;
    static Random rng = new Random(41);

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

    static MapLocation spawnLocation;
    static MapLocation[] allySpawnLocs;

    static MapLocation[] flagDefaultLocations = new MapLocation[3];

    static MapLocation[] sortPositions(MapLocation[] locs, Function<MapLocation, Integer> valueFn, boolean reverse) {
        Arrays.sort(locs, Comparator.comparingInt(valueFn::apply));
        if (reverse) {
            MapLocation[] newWeightedAdjacents = new MapLocation[8];
            for (int i=0; i<8; i++) {
                newWeightedAdjacents[i] = locs[7-i];
            }
            return newWeightedAdjacents;
        } else {
            return locs;
        }
    }

    static MapLocation[] sortPositions(MapLocation[] locs, Function<MapLocation, Integer> valueFn) {
        return sortPositions(locs, valueFn, false);
    }

    static MapLocation[] sortAdjacents(MapLocation l, Function<MapLocation, Integer> valueFn, boolean reverse) {
        MapLocation[] weightedAdjacents = new MapLocation[8];
        for (int i=0; i<8; i++) {
            weightedAdjacents[i] = l.add(directions[i]);
        }
        Arrays.sort(weightedAdjacents, Comparator.comparingInt(valueFn::apply));
        if (reverse) {
            MapLocation[] newWeightedAdjacents = new MapLocation[8];
            for (int i=0; i<8; i++) {
                newWeightedAdjacents[i] = weightedAdjacents[7-i];
            }
            return newWeightedAdjacents;
        } else {
            return weightedAdjacents;
        }
    }

    static MapLocation[] sortAdjacents(MapLocation l, Function<MapLocation, Integer> valueFn) {
        return sortAdjacents(l, valueFn, false);
    }

    /*
        shared info memory management details here
        shared[0] = suggestion movement position
        shared[1] = first flag position
        shared[2] = second flag position
        shared[3] = third flag position
     */

    static int locToInt(MapLocation ml, int bitflag) {
        return (1 << 15) + (bitflag << 14) + (ml.x << 7) + ml.y;
    }

    static MapLocation intToLoc(int v) {
        return new MapLocation((v >> 7) & 0x7f, v & 0x7f);
    }

    static boolean bitflag(int v) {
        return (v & 0x4000) != 0;
    }

    static boolean intIsLoc(int v) {
        return (v & 0x8000) != 0;
    }
}
