package v11_2.robots;

import battlecode.common.*;
import v11_2.Constants;
import v11_2.Pathfinding;
import v11_2.Utils;

import java.util.ArrayList;

import static v11_2.Utils.sort;

public class FlagCarrier extends AbstractRobot {

    private MapLocation[] spawns;
    private int flagID = -1;
    public int index = -1;
    private int turnsSinceDroppedFlag = 0;
    // TODO replace with an array to optimize bytecode
    private ArrayList<MapLocation> visited = new ArrayList<>();

    @Override
    public boolean setup(RobotController rc) throws GameActionException {
        if (!rc.isSpawned() || !rc.hasFlag()) {
            System.out.println("v11 flag carrier tried to be instantiated without a flag...?");
            return false;
        }

        MapLocation[] mySpawns = rc.getAllySpawnLocations();
        spawns = new MapLocation[] {
                new MapLocation(mySpawns[0].x, mySpawns[0].y),
                new MapLocation(mySpawns[1*9+4].x, mySpawns[1*9+4].y),
                new MapLocation(mySpawns[2*9+4].x, mySpawns[2*9+4].y),
        };

        flagID = rc.senseNearbyFlags(0)[0].getID();
        visited.clear();

//        int closest = 9999999;
        for (int i = 0; i < 3; i++) {
            int flagID = rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1;
//            System.out.println(flagID + " " + this.flagID);
            if (flagID == this.flagID) {
                index = i;
                break;
            }
//            if (flagLoc == null) continue;
//            int dist = flagLoc.distanceSquaredTo(rc.getLocation());
//            if (dist < closest) {
//                closest = dist;
//                index = i;
//            }
        }

        Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], null);

        return true;
    }

    @Override
    public void tickJailed(RobotController rc) throws GameActionException {
//        int index = Utils.indexOf(enemyFlagIDs, flagID);
        if (index == -1) System.out.println("error??");
        Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index],
                Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[index]));
        rc.writeSharedArray(Constants.SharedArray.carriedFlagIDs[index], 0);
        spawn(rc);
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        detectAndPickupFlags(rc, enemyFlags);
        RobotInfo[] allyInfos = rc.senseNearbyRobots(4, rc.getTeam());

        sort(spawns, (spawnLoc) -> spawnLoc.distanceSquaredTo(curLoc));
        sort(allyInfos, (allyInfo) -> allyInfo.getLocation().distanceSquaredTo(spawns[0]));

        MapLocation[] enemyLocations = new MapLocation[enemyInfos.length];
        MapInfo[] allMapInfos = rc.senseNearbyMapInfos(4);
        MapLocation[] trapLocations = new MapLocation[allMapInfos.length];

        RobotInfo closestEnemy = Utils.getClosest(enemyInfos, curLoc);

        if (closestEnemy != null && closestEnemy.getLocation().distanceSquaredTo(curLoc) <= 4) {
            Pathfinding.moveAway(rc, curLoc, closestEnemy.getLocation(), false);
        }

        Pathfinding.moveTowards(rc, curLoc, spawns[0], false);

//        for (MapLocation adjacent : sort(getAdjacents(curLoc), (loc) -> {
//            // Sort by best location to move to
//            int total = 0;
//
//            // Avoid enemies
//            for (MapLocation enemyLocation : enemyLocations) {
//                total += enemyLocation.distanceSquaredTo(loc);
//            }
//
//            // Go towards ally stun traps
//            for (MapLocation trapLocation : trapLocations) {
//                if (trapLocation == null) {
//                    break;
//                }
//                total -= (trapLocation.distanceSquaredTo(loc) * 2) / 3;
//            }
//            return loc.distanceSquaredTo(spawns[0]) - total;
//        })) {
//            if (visited.contains(adjacent)) {
//                continue;
//            }
//            Direction d = curLoc.directionTo(adjacent);
//            if (rc.canMove(d)) {
//                rc.move(d);
//                if (!rc.hasFlag()) {
//                    flagID = -1;
//                    break;
//                }
//                visited.add(adjacent);
//                break;
//            }
//        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }

    @Override
    public String name() {
        return "Flag Carrier";
    }
}
