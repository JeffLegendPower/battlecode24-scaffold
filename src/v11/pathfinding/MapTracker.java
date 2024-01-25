package v11.pathfinding;

import battlecode.common.MapLocation;

public class MapTracker {

    final static int MAX_MAP_SIZE = 64;
    final static int INT_BITS = 32;
    final static int ARRAY_SIZE = 128;

    static int[] visitedLocations = new int[ARRAY_SIZE];


    public MapTracker(){
    }

    public void reset(){
        visitedLocations = new int[ARRAY_SIZE];
    }

    public void add(MapLocation loc){
        int arrayPos = loc.x + MAX_MAP_SIZE*(loc.y/INT_BITS);
        int bitPos = loc.y%INT_BITS;
        visitedLocations[arrayPos] |= (1 << bitPos);
    }

    public boolean check(MapLocation loc){
        int arrayPos = loc.x + MAX_MAP_SIZE*(loc.y/INT_BITS);
        int bitPos = loc.y%INT_BITS;
        return ((visitedLocations[arrayPos] & (1 << bitPos)) > 0);
    }

}