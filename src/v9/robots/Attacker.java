package v9.robots;

import battlecode.common.*;
import scala.collection.immutable.Stream;
import v9.Constants;
import v9.RobotPlayer;
import v9.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static v9.Pathfinding.*;

public class Attacker extends AbstractRobot {

    public static MapLocation lastAttackerTarget = null;
    public static MapLocation[] attackerTargets;
    public static boolean hasRetrievedFlag = false;
    private static ArrayList<Integer> enemyFlagIDs = new ArrayList<>(3);
    private static int currentFlagID = -1;

    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
        MapLocation[] mySpawns = rc.getAllySpawnLocations();

        for (int i = 0; i < 3; i++)
            enemyFlagIDs.add(-1);

        if (corner == null) {
//            System.out.println("An attacker was created without corner being found");
            return false; // it needs the corner to organize targets
        }

        // TODO: this could be bugged in some scenarios when they choose the accessible corner that too close to the dam
        attackerTargets = new MapLocation[] {
                new MapLocation(rc.getMapWidth() - mySpawns[0].x, rc.getMapHeight() - mySpawns[0].y),
                new MapLocation(rc.getMapWidth() - mySpawns[1*9+4].x, rc.getMapHeight() - mySpawns[1*9+4].y),
                new MapLocation(rc.getMapWidth() - mySpawns[2*9+4].x, rc.getMapHeight() - mySpawns[2*9+4].y),
        };

        Arrays.sort(attackerTargets, Comparator.comparingInt(a -> a.distanceSquaredTo(corner)));

        return true;
    }

    public MapLocation getCurrentTarget(RobotController rc) throws GameActionException {
        /* Figure out what the most optimal target should be */
        MapLocation currentTarget = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        if (rc.readSharedArray(Constants.SharedArray.globalAttackTarget) < 2) {
            currentTarget = attackerTargets[rc.readSharedArray(Constants.SharedArray.globalAttackTarget)];
        } else {
            MapLocation flag = null;
            int i;
            for (i = 0; i < 3; i ++) {
                flag = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flag != null) {
                    break;
                }
            }
            if (flag != null)
                currentTarget = flag;
            else {

                MapLocation corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
                int height = rc.getMapHeight();
                int width = rc.getMapWidth();

                boolean yzero = corner.y < height / 2;
                boolean xzero = corner.x < width / 2;

                for (int y = yzero ? height - 1 : 0; yzero ? y >= 0 : y < height; y += yzero ? -3 : 3) {
                    for (int x = xzero ? width - 1 : 0; xzero ? x >= 0 : x < width; x += xzero ? -3 : 3) {
                        MapInfo info = RobotPlayer.map[y][x];
                        if (info == null) {
                            return new MapLocation(x, y);
                        }
                    }
                }
            }
        }

        return currentTarget;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearestAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo nearestEnemy = Utils.getClosest(nearestEnemies, curLoc);
        RobotInfo nearestAlly = Utils.getClosest(nearestAllies, curLoc);
        FlagInfo[] nearestFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

        RobotInfo weakestAlly = Utils.lowestHealth(nearestAllies);

        for (int i = 0; i < 3; i++)
            enemyFlagIDs.set(i, rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1);

        for (FlagInfo info : nearestFlags) {
            int index = enemyFlagIDs.indexOf(info.getID());
            int lastNotSeenFlag = enemyFlagIDs.indexOf(-1);
            if (index == -1) {
//                System.out.println("I found flag " + info.getID() + " at " + info.getLocation());
                rc.writeSharedArray(Constants.SharedArray.enemyFlagIDs[lastNotSeenFlag], info.getID() + 1);
                index = lastNotSeenFlag;

            }
            if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index]) != null) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], info.getLocation());
            }
        }

        MapLocation currentTarget = getCurrentTarget(rc);

        if (rc.hasFlag()) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, curLoc);
            for (MapLocation spawn : rc.getAllySpawnLocations()) {
                if (curLoc.isAdjacentTo(spawn) && !hasRetrievedFlag) {
                    rc.writeSharedArray(Constants.SharedArray.globalAttackTarget, rc.readSharedArray(Constants.SharedArray.globalAttackTarget) + 1);
//                    System.out.println("I retrieved a flag! " + rc.getID());
                    int flagIdx = enemyFlagIDs.indexOf(currentFlagID);
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[flagIdx], null);
                    currentFlagID = -1;
                    hasRetrievedFlag = true;
                }
            }
            moveTowards(rc, curLoc, Utils.getClosest(rc.getAllySpawnLocations(), curLoc), true);
            return;
        } else {
            hasRetrievedFlag = false;
        }


        // I have reached the target
        if (curLoc.equals(currentTarget)) {
            Utils.incrementSharedArray(rc, Constants.SharedArray.numAtGlobalAttackTarget);
            if (rc.readSharedArray(Constants.SharedArray.numAtGlobalAttackTarget) > 25) {
                // we've been here for a while but nothing is here
                rc.writeSharedArray(Constants.SharedArray.globalAttackTarget, rc.readSharedArray(Constants.SharedArray.globalAttackTarget) + 1);
                rc.writeSharedArray(Constants.SharedArray.numAtGlobalAttackTarget, 0);
            }
        }

        MapLocation flagHolderLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc);

        if (lastAttackerTarget != currentTarget || flagHolderLoc != null) {
            rc.writeSharedArray(Constants.SharedArray.numAtGlobalAttackTarget, 0);
        }

        if (flagHolderLoc != null) {
            Direction dir = flagHolderLoc.directionTo(curLoc);
            moveTowards(rc, curLoc, flagHolderLoc.add(dir).add(dir), true);
            if (rc.canHeal(flagHolderLoc))
                rc.heal(flagHolderLoc);
            else if (nearestEnemy != null && rc.canAttack(nearestEnemy.getLocation()))
                rc.attack(nearestEnemy.getLocation());
        }

        if (nearestFlags.length > 0) {
            if (rc.canPickupFlag(nearestFlags[0].getLocation())) {
                currentFlagID = nearestFlags[0].getID();
                rc.pickupFlag(nearestFlags[0].getLocation());
            }
            moveTowards(rc, curLoc, nearestFlags[0].getLocation(), true);
        } else if (nearestEnemies.length > 0) {

            if (rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
                // Micro 1: Run in to attack and then run away right after
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
            } else if (rc.getHealth() >= 600 || nearestAlly == null) { // Micro 2: Only run in if you have enough health
                moveTowards(rc, curLoc, nearestEnemy.getLocation(), true);
            } else {
                moveTowards(rc, curLoc, nearestAlly.getLocation(), true); // Micro 3: Run towards nearest ally if low
                if (RobotPlayer.rng.nextInt(3) == 0) {
                    int random = RobotPlayer.rng.nextInt(10);
                    if (random >= 8) {
                        if (rc.canBuild(TrapType.EXPLOSIVE, curLoc))
                            rc.build(TrapType.EXPLOSIVE, curLoc);
                        else if (rc.canBuild(TrapType.STUN, curLoc))
                            rc.build(TrapType.STUN, curLoc);
                    }
                }
            }
        } else {
//            if (nearestFriends.length > 0 && rng.nextInt(5) == 1) {
//                moveTowards(rc, curLoc, nearestFriends[0].getLocation(), true);
            if (rc.getRoundNum() < 200) {
                moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), true);
            } else {
                moveTowards(rc, curLoc, currentTarget, true);
            }
        }

        // Micro 4: Heal the weakest ally first
        if (nearestAllies.length > 0) {
            if (rc.canHeal(weakestAlly.getLocation()))
                rc.heal(weakestAlly.getLocation());
        }
        lastAttackerTarget = currentTarget;
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        if (rc.isSpawned()) return;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation target = getCurrentTarget(rc);
        if (target == null) {
            //System.out.println("USING DEFAULT SPAWN");
            super.spawn(rc);
            return;
        }
        //System.out.println("USING ATACKER SPAWN");
        Arrays.sort(spawnLocs, Comparator.comparingInt(a -> a.distanceSquaredTo(target)));
        for (MapLocation spawnLoc : spawnLocs) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                break;
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }
}
