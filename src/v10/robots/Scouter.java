package v10.robots;

import battlecode.common.*;
import v10.Constants;
import v10.Pathfinding;
import v10.RobotPlayer;
import v10.Utils;

import java.util.ArrayList;

public class Scouter extends AbstractRobot {

    private MapLocation currentTarget = null;
    int scoutNum = -1;

    MapLocation corner = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        int numScouts = rc.readSharedArray(Constants.SharedArray.numberScouts);
        if (numScouts < 3) {
            rc.writeSharedArray(Constants.SharedArray.numberScouts, numScouts + 1);
            scoutNum = numScouts;

            return true;
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        // Set up corner for future searching
        if (corner == null) {
            corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
        }

        FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);

        detectAndPickupFlags(rc, enemyFlags);

        if (enemyFlags.length > 0 && currentTarget == null)
            currentTarget = enemyFlags[0].getLocation();

        MapLocation[] enemyFlagsBroadcast = rc.senseBroadcastFlagLocations();
        for (MapLocation flag : enemyFlagsBroadcast) {
            if (currentTarget == null && RobotPlayer.map[flag.y][flag.x] == null) {
                currentTarget = flag;
                break;
            }
        }

        if (currentTarget != null) {
            Pathfinding.moveTowards(rc, curLoc, currentTarget, true);
            if (curLoc.distanceSquaredTo(currentTarget) <= 4) {
                currentTarget = null;
            }
        }

        else if (nearbyCrumbs.length > 0) {
            // Get some crumbs since if we see them
            Pathfinding.moveTowards(rc, curLoc, nearbyCrumbs[0], true);
        } else if (rc.getRoundNum() < 200) {
            // We can't run around the whole map yet so just move randomly
            Pathfinding.moveTowards(rc, curLoc, curLoc.add(RobotPlayer.directions[RobotPlayer.rng.nextInt(8)]), true);
        } else {
            // Run around unexplored areas of the map
            int height = rc.getMapHeight();
            int width = rc.getMapWidth();

            boolean yzero = corner.y < height / 2;
            boolean xzero = corner.x < width / 2;

            MapLocation target = null;

            int giveUp = 0;
            while (giveUp < 1000) {
                int x = RobotPlayer.rng.nextInt(width);
                int y = RobotPlayer.rng.nextInt(height);
                MapInfo info = RobotPlayer.map[y][x];
                if (info == null) {
                    target = new MapLocation(x, y);
                    break;
                }
                giveUp++;
            }

            if (target != null) {
                Pathfinding.moveTowardsAfraid(rc, curLoc, target, true);
            }
            else {
                Pathfinding.moveTowards(rc, curLoc, curLoc.add(RobotPlayer.directions[RobotPlayer.rng.nextInt(8)]), true);
            }
        }

        // Broadcast your findings
        int i = 0;
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dy = -2; dy <= 2; dy += 4) {
                MapLocation loc = curLoc.translate(dx, dy);
                loc = Utils.clamp(loc, rc);
                MapInfo info = RobotPlayer.map[loc.y][loc.x];
                if (info != null) {
                    Utils.storeInfoInSharedArray(rc, Constants.SharedArray.scoutInfoChannels[scoutNum * 4 + i], info);
                }

                i++;
            }
        }
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        currentTarget = null;
        super.spawn(rc);
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
