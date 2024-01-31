package v11_3.robots;

import battlecode.common.*;
import v11_3.Constants;
import v11_3.Pathfinding;
import v11_3.Utils;
import v11_3.robots.AbstractRobot;
import v11_3.RobotPlayer;

public class FlagCarrier extends AbstractRobot {

    private MapLocation bestSpawn;
    private int flagID = -1;
    public int index = -1;
    private int turnsSinceDroppedFlag = 0;
    // TODO replace with an array to optimize bytecode
    @Override
    public boolean setup(RobotController rc) throws GameActionException {
        if (!rc.isSpawned() || !rc.hasFlag()) {
            System.out.println("v11 flag carrier tried to be instantiated without a flag...?");
            return false;
        }


        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
        int[] centerLocationWeights = new int[3];
        int[] centerLocationDists = new int[3];
        int largestWeight = -1;
        int largestLocDist = -1;
        int total = 0;
        for (int i=0; i<3; i++) {
            centerLocationWeights[i] = v & 0b11111;
            centerLocationDists[i] = RobotPlayer.allyFlagSpawnLocs[i].distanceSquaredTo(rc.getLocation());
            if (centerLocationWeights[i] > largestWeight)
                largestWeight = centerLocationWeights[i];
            if (centerLocationDists[i] > largestLocDist)
                largestLocDist = centerLocationDists[i];
            total += v & 0b11111;
            v >>= 5;
        }

        if (largestLocDist == 0)
            largestLocDist += 1;
        if (largestWeight == 0)
            largestWeight += 1;



        int finalLargestWeight = largestWeight;
        int finalLargestLocDist = largestLocDist;
        double[] spawnScores = new double[3];
        double bestScore = 99999;
        for(int i = 2; --i >= 0;) {
            spawnScores[i] = centerLocationWeights[i] * 1.0 / largestWeight * .2 + centerLocationDists[i] * 1.0 / largestLocDist * 2;
            if (spawnScores[i] < bestScore) {
                bestScore = spawnScores[i];
                bestSpawn = RobotPlayer.allyFlagSpawnLocs[i];
            }
        }

        flagID = rc.senseNearbyFlags(1)[0].getID();

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
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        detectAndPickupFlags(rc, enemyFlags);
        rc.setIndicatorLine(curLoc, bestSpawn, 0, 100, 255);
        RobotInfo closest = Utils.getClosest(enemyInfos, curLoc);
        /*if (enemyInfos.length > 0 && closest.location.isWithinDistanceSquared(curLoc, 12))
            Pathfinding.moveTowards(rc, curLoc, closest.location, false);
        else*/
            Pathfinding.moveTowards(rc, curLoc, bestSpawn, false);

        curLoc = rc.getLocation();
        if (rc.getHealth() < 300 && allies.length > 0) {
            for(Direction d : Utils.getIdealMovementDirections(curLoc, Utils.getClosest(allies, curLoc).location)) {
                if (rc.canDropFlag(curLoc.add(d)))
                    rc.dropFlag(curLoc.add(d));
            }
        }
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
