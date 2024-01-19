package v9.robots;

import battlecode.common.*;
import v9.Constants;
import v9.RobotPlayer;
import v9.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

import static v9.Pathfinding.*;

public class Attacker extends AbstractRobot {

    public static MapLocation lastAttackerTarget = null;
    public static MapLocation[] attackerTargets;
    private static int curAttackerTargetIndex = 0;
    public static boolean hasRetrievedFlag = false;
    private static ArrayList<Integer> enemyFlagIDs = new ArrayList<>(3);
    private static int currentFlagID = -1;
    private static boolean isHealer = false;
    // There's an edge case where the flagholder is next to the spawn location but dies right there and then another robot picks it up
    // so it says that they picked up the flag twice, this should fix that
    private static boolean aboutToRetrieveFlag = false;
    private static MapLocation flagOrigninalLocation = null;

    private static MapLocation corner = null;

    public boolean setup(RobotController rc, MapLocation curLoc) throws GameActionException {
        MapLocation corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
        MapLocation[] mySpawns = rc.getAllySpawnLocations();

        for (int i = 0; i < 3; i++)
            enemyFlagIDs.add(-1);

        if (corner == null) {
            System.out.println("An attacker was created without corner being found");
            return false; // it needs the corner to organize targets
        }

        // TODO: this could be bugged in some scenarios when they choose the accessible corner that too close to the dam
        attackerTargets = new MapLocation[] {
                new MapLocation(rc.getMapWidth() - mySpawns[0].x, rc.getMapHeight() - mySpawns[0].y),
                new MapLocation(rc.getMapWidth() - mySpawns[1*9+4].x, rc.getMapHeight() - mySpawns[1*9+4].y),
                new MapLocation(rc.getMapWidth() - mySpawns[2*9+4].x, rc.getMapHeight() - mySpawns[2*9+4].y),
        };

        Arrays.sort(attackerTargets, Comparator.comparingInt(a -> a.distanceSquaredTo(corner)));

        if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget) == null) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, attackerTargets[0]);
        }

        if (RobotPlayer.rng.nextInt(4) == 0)
            isHealer = true;

        return true;
    }

    public MapLocation getCurrentTarget(RobotController rc) throws GameActionException {
        /* Figure out what the most optimal target should be */
        MapLocation currentTarget = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
//        if (rc.readSharedArray(Constants.SharedArray.globalAttackTarget) < 2) {
//            currentTarget = attackerTargets[rc.readSharedArray(Constants.SharedArray.globalAttackTarget)];
        MapLocation globalTarget = Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget);
        if (globalTarget != null) {
            int idx = -1;
            for (MapLocation attackerTarget : attackerTargets)
                if (attackerTarget.equals(globalTarget))
                    idx = Arrays.asList(attackerTargets).indexOf(attackerTarget);
            if (idx != -1)
                curAttackerTargetIndex = idx;
            currentTarget = globalTarget;

        } else {
            MapLocation flag = null;
            MapLocation badLocation = new MapLocation(63, 63);
            int i;
            for (i = 0; i < 3; i ++) {
                flag = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flag != null && !flag.equals(badLocation)) {
                    break;
                }
            }
            if (flag != null) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, flag);
                currentTarget = flag;
            }
            else if (rc.getRoundNum() > 250) {
               // Utils.getClosest(rc.senseBroadcastFlagLocations(), rc.getLocation());
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
        // Setting corner location for future searching
        if (corner == null) {
            corner = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagCornerLocs[0]);
        }

        // Get surrounding information
        RobotInfo[] nearestEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearestAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo nearestEnemy = Utils.getClosest(nearestEnemies, curLoc);
        RobotInfo weakestEnemy = Utils.lowestHealth(nearestEnemies);

        RobotInfo nearestAlly = Utils.getClosest(nearestAllies, curLoc);
        FlagInfo[] nearestFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        ArrayList<FlagInfo> nearestFlagsNotPickedUp = new ArrayList<>();
        ArrayList<MapLocation> nearestCapturedFlags = new ArrayList<>();
        MapLocation nearestCapturedFlag;
        for (FlagInfo flag : nearestFlags) {
            if (!flag.isPickedUp())
                nearestFlagsNotPickedUp.add(flag);
        }

        int capturedFlagNum = -1;
        for (int i = 0; i < 3; i++) {
            nearestCapturedFlags.add(Utils.getLocationInSharedArray(rc, Constants.SharedArray.capturedFlagLocs[i]));
        }
        nearestCapturedFlag = Utils.getClosest(nearestCapturedFlags, curLoc, 0);
        if (nearestCapturedFlag != null && rc.canSenseLocation(nearestCapturedFlag)) {
            capturedFlagNum = nearestCapturedFlags.indexOf(nearestCapturedFlag);
            if (rc.senseNearbyRobots(nearestCapturedFlag, 1, rc.getTeam().opponent()).length == 0)
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.capturedFlagLocs[capturedFlagNum], null);
            moveTowards(rc, curLoc, nearestCapturedFlag, true);
            if (rc.canAttack(nearestCapturedFlag))
                rc.attack(nearestCapturedFlag);
        }
        /*for (int i = 0; i < 3; i++) {
            System.out.println("flag " + i + " at " + Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]));
        }*/
//        System.out.println("global target at " + Utils.getLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget));

        RobotInfo weakestAlly = Utils.lowestHealth(nearestAllies);

        // This block will run once the attacker succesfully retrieves a flag
        if (rc.isSpawned() && aboutToRetrieveFlag && !rc.hasFlag()) {
            System.out.println("v9 retrieved a flag! " + rc.getID());

            // find the index of said flag, and make sure to reset the global array since the flag no longer exists
            int flagIdx = enemyFlagIDs.indexOf(currentFlagID);
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[flagIdx], null);
//            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, null);

            // reset original location
            flagOrigninalLocation = null;

            // First try to set the next target to an enemy flag if we detected one
            boolean foundFlag = false;
            for (int i = 0; i < 3; i++) {
                MapLocation loc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (loc != null && enemyFlagIDs.get(i) != -1) {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, loc);
                    foundFlag = true;
                    break;
                }
            }
            // If not then look to the next enemy spawn location
            if (!foundFlag && curAttackerTargetIndex < 2) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, attackerTargets[++curAttackerTargetIndex]);
            }
            currentFlagID = -1;
            hasRetrievedFlag = true;
        }
        aboutToRetrieveFlag = false;

        // read shared array for enemy flags
        for (int i = 0; i < 3; i++)
            enemyFlagIDs.set(i, rc.readSharedArray(Constants.SharedArray.enemyFlagIDs[i]) - 1);

        // find flags around it; if it sees one update its location in the array
        for (FlagInfo info : nearestFlags) {
            int index = enemyFlagIDs.indexOf(info.getID());
            int lastNotSeenFlag = enemyFlagIDs.indexOf(-1);
            if (index == -1) {
                System.out.println("I found flag " + info.getID() + " at " + info.getLocation());
                rc.writeSharedArray(Constants.SharedArray.enemyFlagIDs[lastNotSeenFlag], info.getID() + 1);
                index = lastNotSeenFlag;

            } else { //if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index]) != null) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[index], info.getLocation());
            }
        }

        MapLocation currentTarget = getCurrentTarget(rc);

//        if (!currentTarget.equals(lastAttackerTarget))
//            System.out.println("Last: " + lastAttackerTarget + " ct: " + currentTarget);
;
        if (rc.hasFlag()) {
//            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc, curLoc);
            for (MapLocation spawn : rc.getAllySpawnLocations()) {
                if (curLoc.isAdjacentTo(spawn) && !hasRetrievedFlag) {
                    aboutToRetrieveFlag = true;
                    break;
                }
            }
            moveTowards(rc, curLoc, Utils.getClosest(rc.getAllySpawnLocations(), curLoc), true);
            return;
        } else {
            hasRetrievedFlag = false;
        }

        
        // I have reached the target
        if (curLoc.isWithinDistanceSquared(currentTarget, 25)) {
            Utils.incrementSharedArray(rc, Constants.SharedArray.numAtGlobalAttackTarget);
            if (rc.readSharedArray(Constants.SharedArray.numAtGlobalAttackTarget) > 10) {
                if (curAttackerTargetIndex < 2) {
                    // we've been here for a while but nothing is here
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, attackerTargets[++curAttackerTargetIndex]);
                    System.out.println("c " + attackerTargets[curAttackerTargetIndex]);
                    rc.writeSharedArray(Constants.SharedArray.numAtGlobalAttackTarget, 0);
                } else {
                    Utils.storeLocationInSharedArray(rc, Constants.SharedArray.globalAttackTarget, null);
                }
            }
        }

        if (!isHealer && nearestEnemies.length > 0) {
            int dist = curLoc.distanceSquaredTo(nearestEnemy.getLocation());
            if (weakestEnemy == null && rc.canAttack(nearestEnemy.getLocation())) {
                rc.attack(nearestEnemy.getLocation());
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
            } else if (weakestEnemy != null && rc.canAttack((weakestEnemy.getLocation()))) {
                rc.attack(weakestEnemy.getLocation());
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
            } else if (rc.getHealth() >= 800 || nearestAlly == null) { // Micro 2: Only run in if you have enough health
                if (dist <= 10 || dist >= 18) {
                    moveTowards(rc, curLoc, nearestEnemy.getLocation(), true);
                } else if (rc.canMove(nearestEnemy.getLocation().directionTo(curLoc))) {
                    rc.move(nearestEnemy.getLocation().directionTo(curLoc));
//                    moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
                }
            } else {
//                moveTowards(rc, curLoc, nearestAlly.getLocation(), true); // Micro 3: Run towards nearest ally if low
                moveAway(rc, curLoc, nearestEnemy.getLocation(), true);
                if (RobotPlayer.rng.nextInt(4) == 0) {
                    int random = RobotPlayer.rng.nextInt(10);
                    if (random >= 7) {
                        if (rc.canBuild(TrapType.EXPLOSIVE, curLoc))
                            rc.build(TrapType.EXPLOSIVE, curLoc);
                    } else {
                        if (rc.canBuild(TrapType.STUN, curLoc))
                            rc.build(TrapType.STUN, curLoc);
                    }
                }
            }
        }

//        MapLocation flagHolderLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagHolderLoc);


        if ((lastAttackerTarget != null && !lastAttackerTarget.equals(currentTarget))) { //|| flagHolderLoc != null) {
//            System.out.println("e " + (lastAttackerTarget != null && !lastAttackerTarget.equals(currentTarget)) + " " + (flagHolderLoc != null));
            rc.writeSharedArray(Constants.SharedArray.numAtGlobalAttackTarget, 0);
        }

        // DEBUG
//        if (isHealer)
//            rc.setIndicatorDot(curLoc, 0, 255, 0);
//        else
//            rc.setIndicatorDot(curLoc, 255, 0, 0);


        // Healer logic if there's a flagholder that needs protecting
        if (isHealer) {
            for (RobotInfo ally : nearestAllies) {
                if (ally.hasFlag()) {
                    Direction dir = ally.getLocation().directionTo(curLoc);
                    int rand = new Random().nextInt(3) + 1;
                    MapLocation flagholderTarget = ally.getLocation();
                    for (int i = 0; i < rand; i++)
                        flagholderTarget = flagholderTarget.add(dir);
                    moveTowards(rc, curLoc, flagholderTarget, true);
                    flagholderTarget = ally.getLocation();
                    if (ally.health <= 750 && rc.canHeal(flagholderTarget))
                        rc.heal(flagholderTarget);
                    else if (rc.canHeal(weakestAlly.getLocation()))
                        rc.heal(weakestAlly.getLocation());
                }
            }
        }

        if (!nearestFlagsNotPickedUp.isEmpty() && rc.canPickupFlag(nearestFlags[0].getLocation()) && nearestEnemies.length < 3) { // Attacker pickup logic
            if (rc.canPickupFlag(nearestFlags[0].getLocation())) {
                currentFlagID = nearestFlags[0].getID();
                rc.pickupFlag(nearestFlags[0].getLocation());

                flagOrigninalLocation = nearestFlags[0].getLocation();
                // Setting to an impossible coordinate to indicate that it's been picked up
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[enemyFlagIDs.indexOf(currentFlagID)], new MapLocation(63, 63));
            }
            moveTowards(rc, curLoc, nearestFlags[0].getLocation(), true);
        } else {
//            if (nearestFriends.length > 0 && rng.nextInt(5) == 1) {
//                moveTowards(rc, curLoc, nearestFriends[0].getLocation(), true);
            if (rc.getRoundNum() < 200) {
                if (rc.getRoundNum() > 150 && RobotPlayer.rng.nextInt(10) >= 2 && rc.canBuild(TrapType.EXPLOSIVE, curLoc)) {
                    // Place some explosives a few rounds before wall drop
                    rc.build(TrapType.EXPLOSIVE, curLoc);
                }
                moveTowards(rc, curLoc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), true);
            } else {
                moveTowards(rc, curLoc, currentTarget, true);
            }
        }

        // Micro 4: Attackers heal the weakest ally first
        if (nearestAllies.length > 0) {
            if (rc.canHeal(weakestAlly.getLocation()))
                rc.heal(weakestAlly.getLocation());
        }
        lastAttackerTarget = currentTarget;
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        if (flagOrigninalLocation != null) {
            Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[enemyFlagIDs.indexOf(currentFlagID)], flagOrigninalLocation);
            flagOrigninalLocation = null;
        }
        if (rc.isSpawned()) return;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation target = getCurrentTarget(rc);
        if (target == null) {
            super.spawn(rc);
            return;
        }
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
