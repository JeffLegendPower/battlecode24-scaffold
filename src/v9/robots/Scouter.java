package v9.robots;

import battlecode.common.*;
import v9.Constants;
import v9.Pathfinding;
import v9.Utils;
import v9.RobotPlayer;

import java.util.ArrayList;

public class Scouter extends AbstractRobot {

//    MapLocation[] enemyFlags = new MapLocation[3];
    private ArrayList<Integer> enemyFlagIDs = new ArrayList<>();
    private MapLocation currentTarget = null;
    int scoutNum = -1;

    MapLocation corner = null;

    @Override
    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        int numScouts = rc.readSharedArray(Constants.SharedArray.numberScouts);
        if (numScouts < 3) {
            rc.writeSharedArray(Constants.SharedArray.numberScouts, numScouts + 1);
            scoutNum = numScouts;

            if (enemyFlagIDs.size() < 3)
                for (int i = 0; i < 3; i++)
                    enemyFlagIDs.add(-1);

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
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (int i = 0; i < 3; i++)
            enemyFlagIDs.set(i, rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1);

        for (FlagInfo info : enemyFlags) {
            int index = enemyFlagIDs.indexOf(info.getID());
            int lastNotSeenFlag = enemyFlagIDs.indexOf(-1);
            if (index == -1) {
                System.out.println("Scouter found flag " + info.getID() + " at " + info.getLocation());
                rc.writeSharedArray(Constants.SharedArray.enemyFlagIDs[lastNotSeenFlag], info.getID() + 1);
                index = lastNotSeenFlag;
            } else {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], info.getLocation());
            }
        }
        if (currentTarget != null && rc.canSenseLocation(currentTarget))
            currentTarget = null;
        MapLocation[] enemyFlagsBroadcast = rc.senseBroadcastFlagLocations();
        for (MapLocation flag : enemyFlagsBroadcast) {
            if (currentTarget == null && RobotPlayer.map[flag.y][flag.x] == null) {
                currentTarget = flag;
                break;
            }
        }

        if (currentTarget != null) {
//            rc.setIndicatorString("Moving towards " + currentTarget);
            Pathfinding.moveTowards(rc, curLoc, currentTarget, true);
        } else if (nearbyEnemies.length > 0) {
            MapLocation nearestEnemy = Utils.getClosest(nearbyEnemies, curLoc).getLocation();
            if (rc.canAttack(nearestEnemy))
                rc.attack(nearestEnemy);
            Pathfinding.moveAway(rc, curLoc, nearestEnemy, true);
        } else if (nearbyCrumbs.length > 0) {
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

//            unexplored_search:
//            for (int y = yzero ? height - 1 : 0; yzero ? y >= 0 : y < height; y += yzero ? -3 : 3) {
//                for (int x = xzero ? width - 1 : 0; xzero ? x >= 0 : x < width; x += xzero ? -3 : 3) {
//                    MapInfo info = RobotPlayer.map[y][x];
//                    if (info == null) {
//                        target = new MapLocation(x, y);
//                        break unexplored_search;
//                    }
//                }
//            }

            if (target != null) {
                Pathfinding.moveTowards(rc, curLoc, target, true);
//                rc.setIndicatorString("Moving towards " + target);
            }
            else {
                Pathfinding.moveTowards(rc, curLoc, curLoc.add(RobotPlayer.directions[RobotPlayer.rng.nextInt(8)]), true);
//                rc.setIndicatorString("Moving randomly");
            }
        }

        // side job
//        if (rc.getCrumbs() > 1500 && RobotPlayer.rng.nextInt(5) == 0) {
//            if (rc.canBuild(TrapType.STUN, curLoc))
//                rc.build(TrapType.STUN, curLoc);
//        }

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
