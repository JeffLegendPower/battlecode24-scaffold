package microplayer;

import battlecode.common.*;

import java.util.Arrays;

import static microplayer.General.*;
import static microplayer.Utility.*;
import static microplayercopy.General.rng;
import static microplayercopy.General.rngSeed;

public class RobotPlayer {
    public static void run(RobotController rc2) {
        rc = rc2;
        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (!tryToSpawnDuck()) {
                    continue;
                }
                lastAliveRoundNumber = rc.getRoundNum();
                buyGlobalUpgrades();
                onTurn();
                randomizeRng();
            } catch (GameActionException gae) {
                System.out.println("GAMEACTIONEXCEPTION ================");
                gae.printStackTrace();
            } catch (Exception exception) {
                System.out.println("EXCEPTION ==========================");
                exception.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    /**
     * buy global upgrades as the game goes on
     */
    public static void buyGlobalUpgrades() throws GameActionException {
        if (rc.getRoundNum() == 600) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
                return;
            }
        }
        if (rc.getRoundNum() == 1200) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                maxTimeThatBreadCanBeOnTheGround = 25;
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
                return;
            }
        }
        if (rc.getRoundNum() == 1800) {
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
            }
        }
    }

    /**
     * attempt to spawn the duck
     * if the duck was already spawned, it returns true
     * if the duck can spawn, then it spawns the duck and returns true
     * if the duck cannot spawn and is not already spawned, return false
     */
    public static boolean tryToSpawnDuck() throws GameActionException {
        // duck is already spawned
        if (rc.isSpawned()) {
            return true;
        }

        // duck is protector, and died
        if (isProtector) {
            // write to shared array that there is a big emergency ! someone has killed the protector, so spawn there for the time being
            int v = rc.readSharedArray(7);
            v |= (0b11111 << (5*protectedFlagIndex));
            rc.writeSharedArray(7, v);
            if (rc.canSpawn(myProtectedFlagLocation)) {
                rc.spawn(myProtectedFlagLocation);
            }
            return false;
        }

        // duck was a carrier, but died
        if (isCarrier) {
            if (flagCarrierIndex == -1) {
                System.out.println("big error 2 !!!");
                isCarrier = false;
                return false;
            }
            if (carrierLocations[flagCarrierIndex] == null) {
                // System.out.println("big error 3 !!!");
                isCarrier = false;
                return false;
            }
            // carrier just died with flag, so write that in the shared array
            if (lastAliveRoundNumber == rc.getRoundNum()-1) {
                System.out.println("flag " + (flagCarrierIndex+1) + " was dropped");
                visited.clear();
                lastDroppedFlagValue = rc.readSharedArray(flagCarrierIndex+4) & (1 << 14);
                hasCarrierDroppedFlag[flagCarrierIndex] = true;
                rc.writeSharedArray(flagCarrierIndex+4, lastDroppedFlagValue);
            }
            // carrier died with the flag and 4 turns have passed, and the enemy flag is back in its original position
            if (lastAliveRoundNumber == rc.getRoundNum()-1- maxTimeThatBreadCanBeOnTheGround) {
                if (rc.readSharedArray(flagCarrierIndex+4) == lastDroppedFlagValue) {  // flag hasn't been picked up since
                    System.out.println("flag " + (flagCarrierIndex+1) + " was reset");
                    rc.writeSharedArray(flagCarrierIndex + 4, 0);
                    rc.writeSharedArray(flagCarrierIndex + 1, locToInt(enemyFlagLocations[flagCarrierIndex], 0, 0));
                }
                lastDroppedFlagValue = -1;
                isCarrier = false;
                flagCarrierIndex = -1;
            }
        }

        // first time spawning in, write ally spawn locations, map size, duck id, center locations of each 3x3 spawn area
        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }
        if (mapHeight == -1 || mapWidth == -1) {
            mapWidth = rc.getMapWidth();
            mapHeight = rc.getMapHeight();
            mapped = new int[mapWidth][mapHeight];
        }
        if (id == -1) {
            id = rc.readSharedArray(0);
            if (rc.canWriteSharedArray(0, id+1)) {
                rc.writeSharedArray(0, id+1);
            }
            rc.setIndicatorString("id: " + id);
        }
        if (centerSpawnLocations == null) {
            centerSpawnLocations = new MapLocation[3];
            int i=0;
            for (MapLocation allySpawn1 : allySpawnLocations) {
                MapLocation[] checks = {
                        allySpawn1.add(Direction.NORTHWEST),
                        allySpawn1.add(Direction.SOUTHEAST),
                        allySpawn1.add(Direction.SOUTHWEST),
                        allySpawn1.add(Direction.NORTHEAST)
                };
                boolean allChecksPassed = true;
                for (MapLocation check : checks) {
                    boolean passed = false;
                    for (MapLocation allySpawn2 : allySpawnLocations) {
                        if (allySpawn2.equals(check)) {
                            passed = true;
                            break;
                        }
                    }
                    if (!passed) {
                        allChecksPassed = false;
                        break;
                    }
                }
                if (allChecksPassed) {
                    rc.setIndicatorDot(allySpawn1, 0, 255, 0);
                    centerSpawnLocations[i++] = allySpawn1;

                }
            }
            for (MapLocation protectedFlagLoc : centerSpawnLocations) {
                if (rc.canSpawn(protectedFlagLoc)) {
                    rc.spawn(protectedFlagLoc);
                    for (int i2=0; i2<3; i2++) {
                        if (protectedFlagLoc.equals(centerSpawnLocations[i2])) {
                            protectedFlagIndex = i2;
                        }
                    }
                    if (protectedFlagIndex == null) {
                        System.out.println("big error 4 !!!");
                    }
                    myProtectedFlagLocation = protectedFlagLoc;
                    isProtector = true;
                    return true;
                }
            }
        }

        // use the shared array and the centers of the 3x3 spawn areas to encourage spawning in certain areas
        int v = rc.readSharedArray(7);
        int[] spawnWeights = new int[3];
        int total = 0;
        for (int i=0; i<3; i++) {
            spawnWeights[i] = v & 0b11111;
            total += v & 0b11111;
            v >>= 5;
        }
        Integer[] spawnWeightIndicies;
        if (Math.max(spawnWeights[2], Math.max(spawnWeights[0], spawnWeights[1])) == 0) {  // no enemies nearby
            MapLocation centerOfMap = new MapLocation(mapWidth/2, mapHeight/2);
            spawnWeightIndicies = sort(new Integer[]{0, 1, 2}, (i) -> centerSpawnLocations[i].distanceSquaredTo(centerOfMap));
        } else {
            spawnWeightIndicies = sort(new Integer[]{0, 1, 2}, (i) -> -spawnWeights[i]);
        }
        for (int i=0; i<3; i++) {
            if (spawnWeights[spawnWeightIndicies[i]] < total/2) {
                if (rng.nextInt(4) == 0) {
                    continue;
                }
            }
            MapLocation centerSpawnLocation = centerSpawnLocations[spawnWeightIndicies[i]];
            for (MapLocation adjacent : getAdjacents(centerSpawnLocation)) {
                if (rc.canSpawn(adjacent)) {
                    rc.spawn(adjacent);
                    return true;
                }
            }
        }
        return false;
    }

    public static void randomizeRng() throws GameActionException {
        // (linear congruential generator)-ish but bad
        MapLocation robotLoc = rc.getLocation();
        if (!rc.isSpawned()) {
            rngSeed = 23892 + rngSeed * 38855;
            rng.setSeed(rngSeed);
            return;
        }
        rngSeed = (rngSeed * 47127 + robotLoc.x * 43 + robotLoc.y * 59) % 481936283;
        rng.setSeed(rngSeed);
        if (Clock.getBytecodesLeft() > 1000) {
            shuffleInPlace(allySpawnLocations);
        }
    }

    public static void onTurn() throws GameActionException {
        mapFreshInVisionLocations();
        previousLocationForMappingFreshLocations = rc.getLocation();
        if (rc.getRoundNum() >= 200) {
            onGameTurn();
        } else {
            onSetupTurn();
        }
    }

    public static void onGameTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        // get the enemies in view
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        sort(enemyInfos, (enemy) -> enemy.getLocation().distanceSquaredTo(robotLoc));

        // read enemy flag locations from shared, save them locally
        for (int i=0; i<3; i++) {
            int v = rc.readSharedArray(i+1);
            MapLocation readLoc = intIsLoc(v) ? intToLoc(v) : null;
            if (readLoc == null) {
                continue;
            }
            boolean isTaken = flag1(v);
            if (isTaken) {
                rc.setIndicatorDot(readLoc, 0, 0, 255);
            } else {
                rc.setIndicatorDot(readLoc, 255, 127, 0);
            }
            enemyFlagLocations[i] = readLoc;
            enemyFlagIsTaken[i] = isTaken;
        }

        // read carrier locations from shared, save them locally
        for (int i=0; i<3; i++) {
            int v = rc.readSharedArray(4+i);
            MapLocation readLoc = intIsLoc(v) ? intToLoc(v) : null;
            if (readLoc == null) {
                continue;
            }
            boolean isDropped = flag1(v);
            boolean isDeposited = flag2(v);
            carrierLocations[i] = readLoc;
            hasCarrierDroppedFlag[i] = isDropped;
            isEnemyFlagDeposited[i] = isDeposited;
            rc.setIndicatorDot(carrierLocations[i], 0, 127, 255);
        }

        // flag carrier code
        if (isCarrier) {
            rc.setIndicatorString("carrying flag " + (flagCarrierIndex+1));

            sort(allySpawnLocations, (spawnLoc) -> spawnLoc.distanceSquaredTo(robotLoc));

            MapLocation[] enemyLocations = new MapLocation[enemyInfos.length];

            RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            MapLocation[] allyLocations = new MapLocation[allyInfos.length];

            MapInfo[] nearbyMapInfos = rc.senseNearbyMapInfos(4);
            MapLocation[] trapLocations = new MapLocation[nearbyMapInfos.length];

            int writeIndex=0;
            for (MapInfo allMapInfo : nearbyMapInfos) {
                if (allMapInfo.getTrapType().equals(TrapType.STUN)) {
                    trapLocations[writeIndex++] = allMapInfo.getMapLocation();
                }
            }
            for (int i=0; i<enemyInfos.length; i++) {
                enemyLocations[i] = enemyInfos[i].getLocation();
            }
            for (int i=0; i<allyInfos.length; i++) {
                allyLocations[i] = allyInfos[i].getLocation();
            }

            MapLocation[] sortedMovementDirections = sort(getAdjacents(robotLoc), (loc) -> {
                int total = 0;
                // avoid enemies
                for (MapLocation enemyLocation : enemyLocations) {
                    total += enemyLocation.distanceSquaredTo(loc);
                }
                // go towards allies
                for (MapLocation allyLocation : allyLocations) {
                    total -= allyLocation.distanceSquaredTo(loc)/2;
                }
                // go towards ally stun traps
                for (MapLocation trapLocation : trapLocations) {
                    if (trapLocation == null) {
                        break;
                    }
                    total -= (trapLocation.distanceSquaredTo(loc) * 2) / 3;
                }
                return loc.distanceSquaredTo(carrierDestination) - total;
            });

            // move the flag carrier
            for (MapLocation adjacent : sortedMovementDirections) {
                if (visited.contains(adjacent)) {
                    continue;
                }
                Direction d = robotLoc.directionTo(adjacent);
                if (rc.canMove(d)) {
                    rc.move(d);
                    lastTimeSinceFlagCarrierMoved = 0;
                    if (!rc.hasFlag()) {  // flag deposited
                        System.out.println("deposited flag " + (flagCarrierIndex+1));
                        writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 1);
                        isCarrier = false;
                        flagCarrierIndex = -1;
                        break;
                    }
                    visited.add(adjacent);
                    writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 0);
                    break;
                }
            }

            // pass the flag if stuck
            if (rc.getMovementCooldownTurns() < 10 && rc.getActionCooldownTurns() < 10) {
                for (MapLocation d : sort(getAdjacents(robotLoc), (loc) -> loc.distanceSquaredTo(allySpawnLocations[0]))) {
                    if (!rc.onTheMap(d)) {
                        continue;
                    }
                    RobotInfo ally = rc.senseRobotAtLocation(d);
                    if (ally == null) {
                        continue;
                    }
                    if (!ally.getTeam().equals(rc.getTeam())) {
                        continue;
                    }
                    if (ally.hasFlag()) {
                        continue;
                    }
                    if (!rc.canDropFlag(ally.getLocation())) {
                        break;
                    }
                    rc.dropFlag(ally.getLocation());
                    isCarrier = false;
                    transferCooldown = 4;
                    rc.writeSharedArray(flagCarrierIndex+4, locToInt(ally.getLocation(), 1, 0));
                    hasCarrierDroppedFlag[flagCarrierIndex] = true;
                    lastDroppedFlagValue = -1;
                    flagCarrierIndex = -1;
                    break;
                }
            }

            // this is part of checking if the flag carrier moved recently and passing the flag if it hasn't
            lastTimeSinceFlagCarrierMoved++;
            if (lastTimeSinceFlagCarrierMoved > 5) {
                lastTimeSinceFlagCarrierMoved = 0;
                visited.clear();
            }
            return;
        }

        // flag protector code
        if (isProtector) {
            // read the spawn suggestion array and write the danger level in the allowed bits
            int v = rc.readSharedArray(7);
            int enemiesSeen = Math.min(enemyInfos.length, 31);
            if (rc.senseNearbyFlags(1, rc.getTeam()).length == 0) {  // no flag there
                enemiesSeen = Math.max(enemiesSeen-7, 0);
            }
            int newSafetyLevel = enemiesSeen << (5*protectedFlagIndex);
            int newV = (v ^ (v & (0b11111 << (5*protectedFlagIndex)))) | newSafetyLevel;
            rc.setIndicatorString("protect " + v + " " + newV);
            rc.writeSharedArray(7, newV);

            if (myProtectedFlagLocation != rc.getLocation()) {
                System.out.println("big error 1 !!!");
                return;
            }

            // place stun traps in the diagonals
            MapLocation[] diagonalsToRobotLoc = new MapLocation[]{
                    robotLoc.add(Direction.NORTHWEST), robotLoc.add(Direction.SOUTHEAST),
                    robotLoc.add(Direction.SOUTHWEST), robotLoc.add(Direction.NORTHEAST)
            };
            sort(diagonalsToRobotLoc, (loc) -> {
                int total=0;
                for (RobotInfo enemy : enemyInfos) {
                    total += enemy.getLocation().distanceSquaredTo(robotLoc);
                }
                return total;
            });
            for (MapLocation diag : diagonalsToRobotLoc) {
                if (rc.canBuild(TrapType.STUN, diag)) {
                    rc.build(TrapType.STUN, diag);
                    return;
                }
            }

            // attack the enemy if can do anything else
            for (RobotInfo enemy : enemyInfos) {
                if (rc.canAttack(enemy.getLocation())) {
                    rc.attack(enemy.getLocation());
                }
            }

            return;
        }

        // see crumbs, go to the nearest one
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length > 0) {
            sort(crumbLocs, (crumbLoc) -> crumbLoc.distanceSquaredTo(robotLoc));
            MapLocation closestCrumbLoc = crumbLocs[0];
            if (continueInThisDirection != null) {
                if (rc.canMove(continueInThisDirection)) {
                    rc.move(continueInThisDirection);
                }
                if (rng.nextInt(7) == 0) {
                    continueInThisDirection = null;
                }
            }
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, closestCrumbLoc);
            int i=0;
            // try to move in the ideal directions
            for (Direction idealMoveDir : idealMovementDirections) {
                i++;
                if (rc.canMove(idealMoveDir)) {
                    if (i >= 3) {
                        continueInThisDirection = idealMoveDir;
                    }
                    rc.move(idealMoveDir);
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(idealMoveDir))) {
                        rc.fill(robotLoc.add(idealMoveDir));
                        return;
                    }
                }
            }

            return;
        }

        // go to nearest flag or nearest broadcast flag location
        if (broadcastFlagPathfindLoc == null || (rc.getRoundNum() % 100) == 0) {
            MapLocation[] broadcasted = rc.senseBroadcastFlagLocations();
            if (broadcasted.length == 0) {
                MapLocation center = new MapLocation(mapWidth/2, mapHeight/2);
                broadcastFlagPathfindLoc = locationInOtherDirection(center, centerSpawnLocations[0]);
            } else {
                rng.nextInt(robotLoc.x * 359 + robotLoc.y * 5995 + 1);
                double theta = rng.nextDouble() * Math.PI * 2;
                double distance = rng.nextInt(3)+7;
                broadcastFlagPathfindLoc = broadcasted[rng.nextInt(broadcasted.length)]
                        .translate((int) (Math.sin(theta)*distance), (int) (Math.cos(theta)*distance));
            }
        }
        MapLocation pathfindGoalLoc = broadcastFlagPathfindLoc;

        int closestFlagDistance = 10000;
        for (int i=0; i<3; i++) {
            if (enemyFlagIsTaken[i]) {
                continue;
            }
            MapLocation enemyFlagLoc = enemyFlagLocations[i];
            if (enemyFlagLoc == null) {
                continue;
            }
            int flagDist = enemyFlagLoc.distanceSquaredTo(robotLoc);
            if (closestFlagDistance > flagDist) {
                closestFlagDistance = flagDist;
                pathfindGoalLoc = enemyFlagLoc;
            }
        }

        // help carriers
        MapLocation closestCarrierLocation = null;
        int closestCarrierDistance = 25;
        for (int i=0; i<3; i++) {
            MapLocation carrierLoc = carrierLocations[i];
            if (carrierLoc == null) {
                continue;
            }
            if (isEnemyFlagDeposited[i]) {
                continue;
            }
            int distance = carrierLoc.distanceSquaredTo(robotLoc);
            if (closestCarrierDistance > distance) {
                closestCarrierDistance = distance;
                closestCarrierLocation = carrierLoc;
            }
        }
        if (closestCarrierLocation != null) {
            pathfindGoalLoc = closestCarrierLocation;
            if (closestCarrierDistance < 8) {
                pathfindGoalLoc = locationInOtherDirection(robotLoc, pathfindGoalLoc);
            }
            // try to spam traps around carrier
            if (enemyInfos.length > 0) {
                rc.setIndicatorString("spamming traps around carrier");
                if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
                    for (Direction direction : Direction.allDirections()) {
                        MapLocation newLoc = robotLoc.add(direction);
                        if ((newLoc.x + newLoc.y) % 2 == 0) {
                            if (rc.canBuild(TrapType.STUN, robotLoc.add(direction))) {
                                rc.build(TrapType.STUN, robotLoc.add(direction));
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (transferCooldown > 0) {
            transferCooldown -= 1;
        }

        // sense flags
        for (FlagInfo flagInfo : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
            MapLocation flagLoc = flagInfo.getLocation();
            if (flagInfo.isPickedUp()) {
                continue;
            }
            findNewFlags: for (int i=0; i<3; i++) {
                if (enemyFlagLocations[i] == null) {
                    for (MapLocation carrierLoc : carrierLocations) {
                        if (carrierLoc == null) {
                            continue;
                        }
                        if (carrierLoc.distanceSquaredTo(flagLoc) <= 2) {
                            break findNewFlags;
                        }
                    }
                    enemyFlagLocations[i] = flagInfo.getLocation();
                    int v = locToInt(flagInfo.getLocation(), 0, 0);
                    if (rc.canWriteSharedArray(i+1, v)) {
                        rc.writeSharedArray(i+1, v);
                        System.out.println("saw flag " + (i+1) + " at " + intToLoc(v));
                    }
                    break;
                } else if (enemyFlagLocations[i].equals(flagInfo.getLocation())) {
                    break;
                }
            }

            if (flagInfo.getLocation().distanceSquaredTo(robotLoc) <= 2) {  // grab the flag
                if (transferCooldown > 0) {
                    continue;
                }
                if (rc.canPickupFlag(flagInfo.getLocation())) {
                    rc.pickupFlag(flagInfo.getLocation());
                    isCarrier = true;
                    sort(allySpawnLocations, (loc) -> loc.distanceSquaredTo(robotLoc));
                    carrierDestination = allySpawnLocations[0];
                    visited.add(robotLoc);
                    for (int i=0; i<3; i++) {
                        if (flagInfo.getLocation().equals(enemyFlagLocations[i])) {
                            flagCarrierIndex = i;
                            System.out.println("flag " + (i+1) + " picked up at " + flagInfo.getLocation());
                            writeLocationToShared(4+i, robotLoc, 0, 0);
                            writeLocationToShared(1+i, enemyFlagLocations[i], 1, 0);
                            return;
                        }
                        if (carrierLocations[i] == null) {
                            continue;
                        }
                        if (carrierLocations[i].equals(flagInfo.getLocation())) {  // picked up dead carrier's flag
                            flagCarrierIndex = i;
                            System.out.println("flag " + (i+1) + " reacquired at " + flagInfo.getLocation());
                            writeLocationToShared(4+i, robotLoc, 0, 0);
                            writeLocationToShared(1+i, enemyFlagLocations[i], 1, 0);
                            return;
                        }
                    }
                }
            }
        }

        RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        sort(allyInfos, (ally) -> (ally.getLocation().distanceSquaredTo(robotLoc) - rc.getHealth() / 100 - (rc.hasFlag() ? 10000 : 0)));

        // no enemies nearby-ish
        // no enemies nearby-ish
        if (enemyInfos.length == 0 || enemyInfos[0].getLocation().distanceSquaredTo(robotLoc) >= 16) {
            // no enemies nearby -> there are allies nearby
            if (allyInfos.length > 0) {
                RobotInfo closestAlly = allyInfos[0];
                if (closestAlly.getHealth() <= 1000 - rc.getHealAmount()) {
                    for (Direction d : sort(
                            getIdealMovementDirections(robotLoc, pathfindGoalLoc),
                            (d) -> robotLoc.add(d).distanceSquaredTo(closestAlly.getLocation()))
                    ) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            rc.setIndicatorString("moved towards goal & ally");
                            if (rc.canHeal(closestAlly.getLocation())) {
                                rc.heal(closestAlly.getLocation());
                                rc.setIndicatorString("healed a guy");
                            }
                            return;
                        } else {
                            if (rc.canFill(robotLoc.add(d))) {
                                rc.fill(robotLoc.add(d));
                            }
                        }
                    }
                    if (rc.canHeal(closestAlly.getLocation())) {
                        rc.heal(closestAlly.getLocation());
                    }
                }
            }

            // no enemies nearby -> no allies nearby
            for (Direction d : getIdealMovementDirections(robotLoc, pathfindGoalLoc)) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(d))) {
                        rc.fill(robotLoc.add(d));
                        return;
                    }
                }
            }
            return;
        }

        // enemies nearby
        RobotInfo closestEnemy = enemyInfos[0];
        MapLocation closestEnemyLoc = closestEnemy.getLocation();
        if (allyInfos.length >= enemyInfos.length-3) {  // more allies than enemies, attack
            int distToClosestEnemy = robotLoc.distanceSquaredTo(closestEnemy.getLocation());

            // 1+ steps away, so move then attack
            if (distToClosestEnemy > 4) {
                for (Direction d : getIdealMovementDirections(robotLoc, closestEnemyLoc)) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        if (rc.canAttack(closestEnemyLoc)) {
                            rc.attack(closestEnemyLoc);
                        }
                        return;
                    } else {
                        if (rc.canFill(robotLoc.add(d))) {
                            rc.fill(robotLoc.add(d));
                            return;
                        }
                    }
                }
            }

            // less than one step away, attack then move back
            if (distToClosestEnemy <= 4) {
                if (rc.canAttack(closestEnemyLoc)) {
                    rc.attack(closestEnemyLoc);
                    for (Direction d : getIdealMovementDirections(closestEnemyLoc, robotLoc)) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            break;
                        }
                    }
                    rc.setIndicatorString("Enemy in range, attacked");
                    return;
                }
                rc.setIndicatorString("Enemy in range, not attacked");
            }

            // spam traps if still can do stuff
            rc.setIndicatorString("spamming traps");
            if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
                for (Direction direction : Direction.allDirections()) {
                    MapLocation newLoc = robotLoc.add(direction);
                    if ((newLoc.x - newLoc.y * 2) % 3 == 1) {
                        if (rc.canBuild(TrapType.STUN, robotLoc.add(direction))) {
                            rc.build(TrapType.STUN, robotLoc.add(direction));
                            break;
                        }
                    }
                }
            }

            // still can perform action, so try to heal others
            if (rc.getActionCooldownTurns() <= 10) {
                for (RobotInfo ally : allyInfos) {
                    if (rc.canHeal(ally.getLocation())) {
                        rc.heal(ally.getLocation());
                        return;
                    }
                }
            }
            return;
        }

        // more enemies than allies by a bit, spam traps
        if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
            for (Direction direction : Direction.allDirections()) {
                if (rc.canBuild(TrapType.STUN, robotLoc.add(direction))) {
                    rc.build(TrapType.STUN, robotLoc.add(direction));
                    break;
                }
            }
        }
        // can safely flee
        if (closestEnemyLoc.distanceSquaredTo(robotLoc) >= 16) {
            MapLocation finalPathfindGoalLoc = pathfindGoalLoc;
            for (Direction d : sort(getIdealMovementDirections(closestEnemyLoc, robotLoc), (dir) -> robotLoc.add(dir).distanceSquaredTo(finalPathfindGoalLoc))) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(d))) {
                        rc.fill(robotLoc.add(d));
                    }
                }
            }
        }

        // die a hero
        if (rc.canAttack(closestEnemyLoc)) {
            rc.attack(closestEnemyLoc);
        }
        for (Direction d : getIdealMovementDirections(closestEnemyLoc, robotLoc)) {
            if (rc.canMove(d)) {
                rc.move(d);
                break;
            }
        }
    }

    public static void onSetupTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        // protecting flag
        if (isProtector) {
            for (MapLocation diag : new MapLocation[]{
                    robotLoc.add(Direction.NORTHWEST), robotLoc.add(Direction.SOUTHEAST),
                    robotLoc.add(Direction.SOUTHWEST), robotLoc.add(Direction.NORTHEAST)}) {
                if (rc.canBuild(TrapType.STUN, diag)) {
                    rc.build(TrapType.STUN, diag);
                }
            }
            return;
        }

        // see crumbs, go to the nearest one
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length > 0) {
            sort(crumbLocs, (crumbLoc) -> crumbLoc.distanceSquaredTo(robotLoc));
            MapLocation closestCrumbLoc = crumbLocs[0];
            if (continueInThisDirection != null) {
                if (rc.canMove(continueInThisDirection)) {
                    rc.move(continueInThisDirection);
                }
                if (rng.nextInt(10) == 0) {
                    continueInThisDirection = null;
                }
            }
            // try to move in the ideal directions
            int i=0;
            for (Direction idealMoveDir : getIdealMovementDirections(robotLoc, closestCrumbLoc)) {
                if (rc.canMove(idealMoveDir)) {
                    rc.move(idealMoveDir);
                    if (i >= 3) {
                        continueInThisDirection = idealMoveDir;
                    }
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(idealMoveDir))) {
                        rc.fill(robotLoc.add(idealMoveDir));
                        return;
                    }
                }
                i++;
            }
            return;
        }

        // ducks with ids 3-7 go to center before round 81
        if ((3 <= id && id <= 7) && rc.getRoundNum() <= 80) {  // 1-80
            MapLocation centerLocation = new MapLocation(mapWidth/2, mapHeight/2);
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, centerLocation);
            // move closer if possible
            for (Direction idealMoveDir : idealMovementDirections) {
                MapLocation nextLoc = robotLoc.add(idealMoveDir);
                if (contains(visitedForSetupPathfinding, nextLoc)) {
                    continue;
                }
                if (nextLoc.distanceSquaredTo(centerLocation) > robotLoc.distanceSquaredTo(centerLocation)) {
                    continue;
                }
                if (rc.canMove(idealMoveDir)) {
                    rc.move(idealMoveDir);
                    visitedForSetupPathfinding[visitedForSetupPathfindingIndex++ % visitedForSetupPathfinding.length] = nextLoc;
                    return;
                }
            }
            // if not possible to move closer, fill closer
            for (Direction idealMoveDir : idealMovementDirections) {
                MapLocation nextLoc = robotLoc.add(idealMoveDir);
                if (nextLoc.distanceSquaredTo(centerLocation) > robotLoc.distanceSquaredTo(centerLocation)) {
                    continue;
                }
                if (rc.canFill(nextLoc)) {
                    rc.fill(nextLoc);
                    return;
                }
            }
            // if not possible to fill closer, do visited thing
            for (MapLocation nextLoc : sort(getAdjacents(robotLoc), (loc) -> loc.distanceSquaredTo(centerLocation))) {
                if (contains(visitedForSetupPathfinding, nextLoc)) {
                    continue;
                }
                if (rc.canMove(robotLoc.directionTo(nextLoc))) {
                    rc.move(robotLoc.directionTo(nextLoc));
                    visitedForSetupPathfinding[visitedForSetupPathfindingIndex++ % visitedForSetupPathfinding.length] = nextLoc;
                    return;
                }
            }
            // no way to get closer because of visited
            Arrays.fill(visitedForSetupPathfinding, null);
            visitedForSetupPathfindingIndex = 0;
            return;
        }

        // go to dam
        if (rc.getRoundNum() > 130) {
            // bug nav
            if (doingBugNav) {
                if (bugNavVertices[bugNavVertexIndex].distanceSquaredTo(robotLoc) < 3) {  // if close to this vertex, go to the next one
                    bugNavVertexIndex += 1;
                }
                return;
            }

            if (rc.getRoundNum() > 195) {  // 196-199
                if (lastSeenDamLocationDuringSetup != null) {
                    for (Direction d : getIdealMovementDirections(lastSeenDamLocationDuringSetup, robotLoc)) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            return;
                        }
                    }
                }
            }
            // go to the dam (stay near it) and place stun traps
            for (MapLocation ml : getAdjacents(robotLoc)) {
                if (!rc.onTheMap(ml)) {
                    continue;
                }
                // build stun traps when near the dam
                if (rc.senseMapInfo(ml).isDam()) {
                    if (rc.canBuild(TrapType.STUN, robotLoc)) {
                        rc.build(TrapType.STUN, robotLoc);
                    }
                    return;
                }
            }
            MapLocation damLocation = null;
            for (MapInfo sensed : shuffleInPlace(rc.senseNearbyMapInfos(-1))) {
                if (sensed.isDam()) {
                    damLocation = sensed.getMapLocation();
                    lastSeenDamLocationDuringSetup = damLocation;
                    break;
                }
            }
            if (damLocation != null) {
                for (Direction idealDir : getIdealMovementDirections(robotLoc, damLocation)) {
                    if (rc.canMove(idealDir)) {
                        rc.move(idealDir);
                    }
                }
            }

            // if cannot see dam, going to the center is a good idea
            MapLocation centerLoc = new MapLocation(mapWidth/2, mapHeight/2);
            rc.setIndicatorLine(robotLoc, centerLoc, 128, 0, 255);
            MapLocation bugNavWallLocation = null;
            for (Direction d : getTrulyIdealMovementDirections(robotLoc, centerLoc)) {
                if (rc.canMove(d)) {
                    rc.move(d);
                } else {
                    MapLocation newLoc = robotLoc.add(d);
                    if (rc.canFill(newLoc)) {
                        rc.fill(newLoc);
                        return;
                    }
                    if ((mapped[newLoc.x][newLoc.y] & 0b10) == 0) {  // is wall
                        bugNavWallLocation = newLoc;
                    }
                }
            }

            // cannot move but it is because ducks are in the way, so just shift around a little
            if (bugNavWallLocation == null) {
                shuffleInPlace(directions);
                for (Direction d : directions) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        return;
                    }
                }
                return;
            }

            // cannot move, bug nav
            doingBugNav = true;
            bugNavVertices = bugNavAroundPath(robotLoc, bugNavWallLocation);
            bugNavVertexIndex = 0;
            return;
        }

        // no allies in sight, just keep bouncing around
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (nearbyAllies.length == 0 && lastMovedSetupExplorationDirection != null) {
            if (rc.canMove(lastMovedSetupExplorationDirection)) {
                rc.move(lastMovedSetupExplorationDirection);
            } else {
                lastMovedSetupExplorationDirection = directions[rng.nextInt(directions.length)];
            }
        }

        // move away from allies
        MapLocation[] nearbyAllyLocations = new MapLocation[nearbyAllies.length];
        for (int i=0; i<nearbyAllies.length; i++) {
            nearbyAllyLocations[i] = nearbyAllies[i].getLocation();
        }
        Direction[] sortedDirections = sort(directions, (dir) -> {
            MapLocation newRobotLoc = robotLoc.add(dir);
            int weight = 0;
            for (MapLocation nearbyAllyLoc : nearbyAllyLocations) {
                weight -= nearbyAllyLoc.distanceSquaredTo(newRobotLoc);
            }
            return weight;
        });
        for (Direction d : sortedDirections) {
            if (rc.canMove(d)) {
                rc.move(d);
                lastMovedSetupExplorationDirection = d;
                break;
            }
        }
    }

    // takes 13000+ bytecode
    public static void debugMappedLocations() {
        for (int x=0; x<mapWidth; x++) {
            for (int y=0; y<mapHeight; y++) {
                int v = mapped[x][y];
                if (v == 0) {
                    continue;
                }
                rc.setIndicatorDot(new MapLocation(x, y), ((v & 0b10) >> 1) * 255, 0, 0);
            }
        }
    }

    public static MapLocation[] getFreshInVisionLocations() {
        MapLocation robotLoc = rc.getLocation();

        int x = robotLoc.x;
        int y = robotLoc.y;
        switch (previousLocationForMappingFreshLocations.directionTo(robotLoc)) {
            case NORTH:
                return new MapLocation[]{
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4),
                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2)
                };
            case NORTHEAST:
                return new MapLocation[]{
                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
                        new MapLocation(x+2, y+4), new MapLocation(x+2, y+3), new MapLocation(x+3, y+3),
                        new MapLocation(x+3, y+2), new MapLocation(x+4, y+2), new MapLocation(x+4, y+1),
                        new MapLocation(x+4, y), new MapLocation(x+4, y-1), new MapLocation(x-2, y+4),
                        new MapLocation(x+4, y-2)
                };
            case EAST:
                return new MapLocation[]{
                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2),
                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-3), new MapLocation(x+2, y-4)
                };
            case SOUTHEAST:
                return new MapLocation[]{
                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-2), new MapLocation(x+3, y-3),
                        new MapLocation(x+2, y-3), new MapLocation(x+2, y-4), new MapLocation(x+1, y-4),
                        new MapLocation(x, y-4), new MapLocation(x-1, y-4), new MapLocation(x+4, y+2),
                        new MapLocation(x-2, y-4)
                };
            case SOUTH:
                return new MapLocation[]{
                        new MapLocation(x-4, y-2), new MapLocation(x-3, y-3), new MapLocation(x-2, y-4),
                        new MapLocation(x-1, y-4), new MapLocation(x, y-4), new MapLocation(x+1, y-4),
                        new MapLocation(x+2, y-4), new MapLocation(x+3, y-3), new MapLocation(x+4, y-2)
                };
            case SOUTHWEST:
                return new MapLocation[]{
                        new MapLocation(x+1,  y-4), new MapLocation(x, y-4), new MapLocation(x-1, y-4),
                        new MapLocation(x-2, y-4), new MapLocation(x-2, y-3), new MapLocation(x-3, y-3),
                        new MapLocation(x-3, y-2), new MapLocation(x-4, y-2), new MapLocation(x-4, y-1),
                        new MapLocation(x-4, y), new MapLocation(x-4, y+1), new MapLocation(x-4, y+2),
                        new MapLocation(x+2, y-4)
                };
            case WEST:
                return new MapLocation[]{
                        new MapLocation(x-2, y-4), new MapLocation(x-3, y-3), new MapLocation(x-4, y-2),
                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4)
                };
            case NORTHWEST:
                return new MapLocation[]{
                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+2), new MapLocation(x-3, y+3),
                        new MapLocation(x-2, y+3), new MapLocation(x-2, y+4), new MapLocation(x-1, y+4),
                        new MapLocation(x, y+4), new MapLocation(x+1, y+4), new MapLocation(x-4, y-2),
                        new MapLocation(x+2, y+4)
                };
        }
        System.out.println("big issue 1 !!!");
        return new MapLocation[0];
    }

    public static void mapFreshInVisionLocations() throws GameActionException {
        // scan symmetries from shared array
        int invalidSymmetriesFromSharedArray = rc.readSharedArray(8);
        for (int i=0; i<3; i++) {
            boolean isInvalidSymmetry = (invalidSymmetriesFromSharedArray & (1 << (15-i))) > 0;
            if (isInvalidSymmetry) {
                possibleSymmetries[i] = false;
            }
        }
        if (!symmetryWasDetermined) {
            checkIfSymmetryIsDetermined();
        }

        // just spawned in
        if (previousLocationForMappingFreshLocations == null) {
            MapInfo[] nearbyInfos = rc.senseNearbyMapInfos();
            for (MapInfo info : nearbyInfos) {
                MapLocation loc = info.getMapLocation();
                if (0 <= loc.x && loc.x < mapWidth && 0 <= loc.y & loc.y < mapHeight) {
                    mapped[loc.x][loc.y] = info.isWall() ? 0b01 : 0b11;
                }
            }
            return;
        }

        if (previousLocationForMappingFreshLocations.equals(rc.getLocation())) {  // did not move since last turn
            return;
        }

        // did move since last turn, scan new locations
        for (MapLocation l : getFreshInVisionLocations()) {
            if (0 <= l.x && l.x < mapWidth && 0 <= l.y && l.y < mapHeight) {
                if (mapped[l.x][l.y] == 0) {
                    boolean isWall = rc.senseMapInfo(l).isWall();
                    mapped[l.x][l.y] = isWall ? 0b01 : 0b11;
                    if (!symmetryWasDetermined) {
                        int rotationalSymmetryValue = mapped[mapWidth - 1 - l.x][mapHeight - 1 - l.y];
                        int upDownSymmetryValue = mapped[l.x][mapHeight - 1 - l.y];
                        int rightLeftSymmetryValue = mapped[mapWidth - 1 - l.x][l.y];
                        if ((rotationalSymmetryValue & 0b1) == 0b1) {  // already seen rotational symmetry value
                            if ((rotationalSymmetryValue & 0b11) != mapped[l.x][l.y]) {  // rotational symmetry value is not a wall
                                possibleSymmetries[0] = false;
                                checkIfSymmetryIsDetermined();
                            }
                        }
                        if ((upDownSymmetryValue & 0b1) == 0b1) {
                            if ((upDownSymmetryValue & 0b11) != mapped[l.x][l.y]) {
                                possibleSymmetries[1] = false;
                                checkIfSymmetryIsDetermined();
                            }
                        }
                        if ((rightLeftSymmetryValue & 0b1) == 0b1) {
                            if ((rightLeftSymmetryValue & 0b11) != mapped[l.x][l.y]) {
                                possibleSymmetries[2] = false;
                                checkIfSymmetryIsDetermined();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void checkIfSymmetryIsDetermined() throws GameActionException {
        int validSymmetryCount = 0;
        int lastValidIndex = 0;
        int newSharedArrayValue = 0;
        for (int i = 0; i < 3; i++) {
            newSharedArrayValue <<= 1;
            newSharedArrayValue |= possibleSymmetries[i] ? 0 : 1;
            if (possibleSymmetries[i]) {
                validSymmetryCount += 1;
                lastValidIndex = i;
            }
        }
        newSharedArrayValue <<= 13;
        if (validSymmetryCount == 1) {
            symmetryWasDetermined = true;
        }
        if (rc.readSharedArray(8) != newSharedArrayValue) {
            if (validSymmetryCount == 1) {
                if (lastValidIndex == 0) {
                    System.out.println("ROTATIONAL SYMMETRY");
                } else if (lastValidIndex == 1) {
                    System.out.println("UP/DOWN SYMMETRY");
                } else {
                    System.out.println("LEFT/RIGHT SYMMETRY");
                }
            }
            rc.writeSharedArray(8, newSharedArrayValue);
        }
    }
}
