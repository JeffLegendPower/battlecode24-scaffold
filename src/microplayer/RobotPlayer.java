package microplayer;

import battlecode.common.*;

import java.util.Arrays;

import static microplayer.General.*;
import static microplayer.Utility.*;

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
        int roundNum = rc.getRoundNum();
        if (roundNum == 600) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
                allyGlobalUpgrades[0] = true;
                return;
            }
        }
        if (roundNum == 1200) {
            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
                allyGlobalUpgrades[1] = true;
                return;
            }
        }
        if (roundNum == 1800) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                allyGlobalUpgrades[2] = true;
                maxTimeThatBreadCanBeOnTheGround = 25;
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }
        }

        if (roundNum >= 200 && roundNum % 600 < 3) {
            GlobalUpgrade[] upgrades = rc.getGlobalUpgrades(rc.getTeam().opponent());
            for (GlobalUpgrade upgrade : upgrades) {
                switch (upgrade) {
                    case ATTACK:
                        enemyGlobalUpgrades[0] = true;
                        break;
                    case HEALING:
                        enemyGlobalUpgrades[1] = true;
                        break;
                    case CAPTURING:
                        enemyGlobalUpgrades[2] = true;
                        break;
                }
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
        doingBugNav = false;

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
                Carrier.visited.clear();
                lastDroppedFlagValue = rc.readSharedArray(flagCarrierIndex+4) & (1 << 14);
                hasCarrierDroppedFlag[flagCarrierIndex] = true;
                rc.writeSharedArray(flagCarrierIndex+4, lastDroppedFlagValue);
            }
            // carrier died with the flag and 4 turns have passed, and the enemy flag is back in its original position
            if (lastAliveRoundNumber == rc.getRoundNum()-1-maxTimeThatBreadCanBeOnTheGround) {
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
            centerOfMap = new MapLocation(mapWidth/2, mapHeight/2);
            mapped = new int[mapWidth][mapHeight];
        }
        if (id == -1) {
            id = rc.readSharedArray(0);
            if (rc.canWriteSharedArray(0, id+1)) {
                rc.writeSharedArray(0, id+1);
            }
            rc.setIndicatorString("id: " + id);
        }
        if (orderedCenterSpawnLocations == null) {
            orderedCenterSpawnLocations = new MapLocation[3];
            sortableCenterSpawnLocations = new MapLocation[3];
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
                    sortableCenterSpawnLocations[i] = allySpawn1;
                    orderedCenterSpawnLocations[i++] = allySpawn1;
                }
            }
            // possibly spawn in as a protector
            for (MapLocation protectedFlagLoc : orderedCenterSpawnLocations) {
                if (rc.canSpawn(protectedFlagLoc)) {
                    rc.spawn(protectedFlagLoc);
                    for (int i2=0; i2<3; i2++) {
                        if (protectedFlagLoc.equals(orderedCenterSpawnLocations[i2])) {
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
            spawnWeightIndicies = sort(new Integer[]{0, 1, 2}, (i) -> orderedCenterSpawnLocations[i].distanceSquaredTo(centerOfMap));
        } else {
            spawnWeightIndicies = sort(new Integer[]{0, 1, 2}, (i) -> -spawnWeights[i]);
        }
        for (int i=0; i<3; i++) {
            if (spawnWeights[spawnWeightIndicies[i]] < total/2) {
                if (rng.nextInt(4) == 0) {
                    continue;
                }
            }
            MapLocation centerSpawnLocation = orderedCenterSpawnLocations[spawnWeightIndicies[i]];
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
        robotLoc = rc.getLocation();
        mapFreshInVisionLocations();
        previousLocationForMappingFreshLocations = rc.getLocation();
        if (rc.getRoundNum() >= 200) {
            onGameTurn();
        } else {
            onSetupTurn();
        }
    }

    public static void onGameTurn() throws GameActionException {
        // sort the sortable center spawn locations
        sort(sortableCenterSpawnLocations, (loc) -> loc.distanceSquaredTo(robotLoc));

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
            Carrier.onCarrierGameTurn();
            return;
        }

        // flag protector code
        if (isProtector) {
            Protector.onProtectorGameTurn();
            return;
        }

        // go to the crumbs if they are there
        if (Nav.goToCrumbs()) {
            return;
        }

        // base pathfinding to broadcast flag position or guessed flag position
        if (broadcastFlagPathfindLoc == null || (rc.getRoundNum() % 100) == 0) {
            MapLocation[] broadcasted = rc.senseBroadcastFlagLocations();
            if (broadcasted.length == 0) {
                broadcastFlagPathfindLoc = mirroredAcrossMapLocation(orderedCenterSpawnLocations[rng.nextInt(3)]);
            } else {
                rng.nextInt(robotLoc.x * 359 + robotLoc.y * 5995 + 1);
                double theta = rng.nextDouble() * Math.PI * 2;
                double distance = rng.nextInt(3)+7;
                broadcastFlagPathfindLoc = broadcasted[rng.nextInt(broadcasted.length)]
                        .translate((int) (Math.sin(theta)*distance), (int) (Math.cos(theta)*distance));
            }
        }
        MapLocation pathfindGoalLoc = broadcastFlagPathfindLoc;

        // pathfind to found enemy flag location
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
                rc.setIndicatorString("going to found enemy flag location");
            }
        }

        // bug nav
        bugNav : if (doingBugNav) {
            for (MapLocation ml : bugNavVertices) {
                rc.setIndicatorDot(ml, 255, 255,  0);
            }
            if (bugNavVertices[bugNavVertexIndex].distanceSquaredTo(robotLoc) < 2) {
                if (++bugNavVertexIndex == bugNavVertices.length) {
                    doingBugNav = false;
                    rc.setIndicatorString("stopping bugnav");
                    break bugNav;
                }
            }
            pathfindGoalLoc = bugNavVertices[bugNavVertexIndex];
        }

        // lower the cooldown for flag transfers
        if (flagTransferCooldown > 0) {
            flagTransferCooldown -= 1;
        }

        // go to nearby dropped flags
        for (int i=0; i<3; i++) {
            if (!hasCarrierDroppedFlag[i]) {
                continue;
            }
            if (isEnemyFlagDeposited[i]) {
                continue;
            }
            MapLocation droppedFlagLocation = carrierLocations[i];
            if (droppedFlagLocation == null) {
                continue;
            }
            if (Math.max(Math.abs(droppedFlagLocation.x-robotLoc.x), Math.abs(droppedFlagLocation.y-robotLoc.y)) < maxTimeThatBreadCanBeOnTheGround) {
                pathfindGoalLoc = droppedFlagLocation;
            }
        }

        MapLocation closestCenterSpawnLocation = sortableCenterSpawnLocations[0];

        // sense flags
        if (Carrier.senseAndPickupFlags()) {
            return;
        }

        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int closestEnemyDistance = 999;
        RobotInfo closestEnemy = null;
        for (RobotInfo enemy : enemyInfos) {
            int distToEnemy = enemy.getLocation().distanceSquaredTo(robotLoc);
            if (closestEnemy == null || closestEnemyDistance > distToEnemy) {
                closestEnemyDistance = distToEnemy;
                closestEnemy = enemy;
            }
        }

        // attack the enemy flag carrier
        MapLocation finalPathfindGoalLoc = pathfindGoalLoc;
        RobotInfo possibleEnemyCarrier = null;
        for (RobotInfo enemy : enemyInfos) {
            if (enemy.getLocation().distanceSquaredTo(closestCenterSpawnLocation) <= 2) {
                possibleEnemyCarrier = enemy;
            }
            if (enemy.hasFlag) {
                possibleEnemyCarrier = enemy;
                break;
            }
        }
        if (possibleEnemyCarrier != null) {  // attack enemy flag carrier
            rc.setIndicatorString("attacking enemy flag carrier");
            // determine which directions to move in are best
            MapLocation finalPossibleEnemyCarrierLocation = possibleEnemyCarrier.getLocation();
            MapLocation[] weightedAdjacents = sortWithCache(getAdjacents(robotLoc),
                    (loc) -> loc.distanceSquaredTo(finalPossibleEnemyCarrierLocation)
            );

            // actually do the moving
            for (MapLocation adjacent : weightedAdjacents) {
                Direction d = robotLoc.directionTo(adjacent);
                if (rc.canMove(d)) {
                    rc.move(d);
                    break;
                }
            }

            if (rc.canAttack(possibleEnemyCarrier.getLocation())) {
                rc.attack(possibleEnemyCarrier.getLocation());
                return;
            }
        }

        // fill near pathfind location
        if (pathfindGoalLoc.distanceSquaredTo(robotLoc) <= 30) {
            for (Direction d : getTrulyIdealMovementDirections(robotLoc, pathfindGoalLoc)) {
                if (rng.nextInt(8) == 0) {
                    if ((pathfindGoalLoc.x + pathfindGoalLoc.y) % 2 == 0) {
                        if (rc.canFill(pathfindGoalLoc)) {
                            rc.fill(pathfindGoalLoc);
                            break;
                        }
                    }
                }
            }
        }

        rc.setIndicatorLine(robotLoc, pathfindGoalLoc, 0, 255, 0);

        RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        sort(enemyInfos, (enemy) -> enemy.getLocation().distanceSquaredTo(robotLoc));
        sort(allyInfos, (ally) -> ally.getLocation().distanceSquaredTo(robotLoc));

        int distToClosestSpawn = closestCenterSpawnLocation.distanceSquaredTo(robotLoc);

        // no enemies close enough to attack even if they run in
        if (enemyInfos.length == 0 || enemyInfos[0].getLocation().distanceSquaredTo(robotLoc) >= 16) {
            // no enemies nearby-ish -> spam traps when no enemies in 2-step range
            if (enemyInfos.length > 0) {
                if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2
                        || (robotLoc.distanceSquaredTo(pathfindGoalLoc) <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)
                        || (distToClosestSpawn <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)) {
                    for (Direction direction : Direction.allDirections()) {
                        MapLocation newLoc = robotLoc.add(direction);
                        if (rc.getCrumbs() > 5000 || (robotLoc.x + robotLoc.y) % 2 == 0) {
                            if (rc.canBuild(TrapType.STUN, newLoc)) {
                                rc.build(TrapType.STUN, newLoc);
                                break;
                            }
                        }
                    }
                }
            }

            // no enemies nearby-ish -> there are allies nearby
            if (allyInfos.length > 0) {
                RobotInfo weakestAlly = allyInfos[0];
                Direction[] idealDirectionsTowardsAllies = sort(
                        getIdealMovementDirections(robotLoc, pathfindGoalLoc),
                        (d) -> robotLoc.add(d).distanceSquaredTo(weakestAlly.getLocation())
                );
                if (weakestAlly.getHealth() <= 1000 - rc.getHealAmount()) {
                    for (Direction d : idealDirectionsTowardsAllies
                    ) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            rc.setIndicatorString("moved towards goal & ally");
                            if (rc.canHeal(weakestAlly.getLocation())) {
                                rc.heal(weakestAlly.getLocation());
                                rc.setIndicatorString("healed a guy");
                            }
                            return;
                        } else {
                            if (rc.canFill(robotLoc.add(d))) {
                                rc.fill(robotLoc.add(d));
                            }
                        }
                    }
                    if (rc.canHeal(weakestAlly.getLocation())) {
                        rc.heal(weakestAlly.getLocation());
                    }
                }
            }

            // no enemies nearby-ish -> no allies nearby
            for (Direction d : getIdealMovementDirections(robotLoc, pathfindGoalLoc)) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    return;
                }
            }
            for (Direction d : getIdealMovementDirections(robotLoc, pathfindGoalLoc)) {
                MapLocation fillLoc = robotLoc.add(d);
                if (rc.canFill(fillLoc)) {
                    rc.canFill(fillLoc);
                    return;
                }
            }
            return;
        }

        // enemies nearby but allies is more
        if (allyInfos.length >= enemyInfos.length - 1) {
            // attack 1
            if (rc.getActionCooldownTurns() < 10) {
                MapLocation enemyWithLowestHealthLocation = null;
                int healthOfEnemyWithLowestHealth = 9999;
                for (RobotInfo enemy : enemyInfos) {
                    int enemyHealth = enemy.health;
                    if (enemyWithLowestHealthLocation == null || healthOfEnemyWithLowestHealth > enemyHealth) {
                        healthOfEnemyWithLowestHealth = enemyHealth;
                        enemyWithLowestHealthLocation = enemy.getLocation();
                    }
                }
                if (rc.canAttack(enemyWithLowestHealthLocation)) {
                    rc.attack(enemyWithLowestHealthLocation);
                } else {
                    for (RobotInfo enemy : enemyInfos) {
                        if (rc.canAttack(enemy.getLocation())) {
                            rc.attack(enemy.getLocation());
                            break;
                        }
                    }
                }
            }

            // move
            MicroAttacker microAttacker = new MicroAttacker(rc);
            microAttacker.doMicro();

            // attack 2
            if (rc.getActionCooldownTurns() < 10) {
                MapLocation enemyWithLowestHealthLocation = null;
                int healthOfEnemyWithLowestHealth = 9999;
                for (RobotInfo enemy : enemyInfos) {
                    int enemyHealth = enemy.health;
                    if (enemyWithLowestHealthLocation == null || healthOfEnemyWithLowestHealth > enemyHealth) {
                        healthOfEnemyWithLowestHealth = enemyHealth;
                        enemyWithLowestHealthLocation = enemy.getLocation();
                    }
                }
                if (rc.canAttack(enemyWithLowestHealthLocation)) {
                    rc.attack(enemyWithLowestHealthLocation);
                } else {
                    for (RobotInfo enemy : enemyInfos) {
                        if (rc.canAttack(enemy.getLocation())) {
                            rc.attack(enemy.getLocation());
                            break;
                        }
                    }
                }
            }

            // spam traps if we still have some action cooldown remaining
            if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2 && rc.isActionReady()) {
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
            // noinspection Convert2MethodRef
            sort(allyInfos, (ally) -> ally.getHealth());
            if (rc.isActionReady()) {
                for (RobotInfo ally : allyInfos) {
                    if (ally.getHealth() == 1000) {
                        break;
                    }
                    if (rc.canHeal(ally.getLocation())) {
                        rc.heal(ally.getLocation());
                        return;
                    }
                }
            }
            return;
        }

        // more enemies than allies by a bit, spam traps
        rc.setIndicatorString("spamming traps");
        if (robotLoc.isWithinDistanceSquared(mirroredAcrossMapLocation(closestCenterSpawnLocation), 100)) {
            if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
                for (Direction direction : Direction.allDirections()) {
                    MapLocation newLoc = robotLoc.add(direction);
                    if ((newLoc.x + newLoc.y * 2) % 3 == 0) {
                        if (rc.canBuild(TrapType.STUN, robotLoc.add(direction))) {
                            rc.build(TrapType.STUN, robotLoc.add(direction));
                            break;
                        }
                    }
                }
            }
        }

        MapLocation closestEnemyLoc = closestEnemy.getLocation();

        if (closestEnemyDistance >= 16) {  // can safely flee
            Direction[] fleeDirections = sort(
                    getIdealMovementDirections(closestEnemyLoc, robotLoc),
                    (dir) -> robotLoc.add(dir).distanceSquaredTo(finalPathfindGoalLoc)
            );
            for (Direction d : fleeDirections) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    rc.setIndicatorString("fleeing");
                    return;
                }
            }
            for (Direction d : fleeDirections) {
                MapLocation fillLoc = robotLoc.add(d);
                if (rc.canFill(fillLoc)) {
                    rc.fill(fillLoc);
                    rc.setIndicatorString("filling water to flee");
                    return;
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
        // protecting flag
        if (isProtector) {
            Protector.onProtectorSetupTurn();
            return;
        }

        // see crumbs, go to the nearest one
        if (Nav.goToCrumbs()) {
            return;
        }

        // ducks with ids 3-7 go to center before round 81
        if ((3 <= id && id <= 7) && rc.getRoundNum() <= 80) {  // 1-80
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, centerOfMap);
            // move closer if possible
            for (Direction idealMoveDir : idealMovementDirections) {
                MapLocation nextLoc = robotLoc.add(idealMoveDir);
                if (contains(visitedForSetupPathfinding, nextLoc)) {
                    continue;
                }
                if (nextLoc.distanceSquaredTo(centerOfMap) > robotLoc.distanceSquaredTo(centerOfMap)) {
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
                if (nextLoc.distanceSquaredTo(centerOfMap) > robotLoc.distanceSquaredTo(centerOfMap)) {
                    continue;
                }
                if ((nextLoc.x+nextLoc.y) % 2 == 0) {
                    if (rc.canFill(nextLoc)) {
                        rc.fill(nextLoc);
                        return;
                    }
                }
            }
            // if not possible to fill closer, do visited thing
            for (MapLocation nextLoc : sort(getAdjacents(robotLoc), (loc) -> loc.distanceSquaredTo(centerOfMap))) {
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
                // bresenham algo
                boolean wallInTheWay = false;
                if (Math.abs(robotLoc.x-centerOfMap.x) > Math.abs(robotLoc.y-centerOfMap.y)) {  // x change > y change, use x coordinates to do the stuff
                    rc.setIndicatorLine(robotLoc, centerOfMap, 255, 0, 255);
                    int x0 = Math.min(robotLoc.x, centerOfMap.x);
                    int x1 = Math.max(robotLoc.x, centerOfMap.x);
                    int y0 = robotLoc.y;
                    int y1 = centerOfMap.y;
                    double m = ((double) (y1 - y0)) / (x1 - x0);
                    for (int x = x0; x <= x1; x++) {
                        if (robotLoc.x == x) {
                            continue;
                        }
                        double rounded = Math.round(m * (x - x0));
                        int y;
                        if (robotLoc.x > centerOfMap.x) {
                            y = y1 - (int) rounded;
                        } else {
                            y = y0 + (int) rounded;
                        }
                        rc.setIndicatorDot(new MapLocation(x, y), 255, 0, 0);
                        // todo: refine the below to use only the shape is being bugnav'd around instead of the entire map
                        if ((mapped[x][y] & 0b11) == 0b01) {  // there is a wall in the way
                            wallInTheWay = true;
                            break;
                        }
                    }
                } else {  // y change > x change, use y coordinates to do the stuff
                    rc.setIndicatorLine(robotLoc, centerOfMap, 255, 255, 0);
                    int x0 = robotLoc.x;
                    int x1 = centerOfMap.x;
                    int y0 = Math.min(robotLoc.y, centerOfMap.y);
                    int y1 = Math.max(robotLoc.y, centerOfMap.y);
                    double m = ((double) (y1 - y0)) / (x1 - x0);
                    for (int y = y0; y <= y1; y++) {
                        if (robotLoc.y == y) {
                            continue;
                        }
                        double rounded = Math.round((y - y0)/m);
                        int x;
                        if (robotLoc.y > centerOfMap.y) {
                            x = x1 - (int) rounded;
                        } else {
                            x = x0 + (int) rounded;
                        }
                        rc.setIndicatorDot(new MapLocation(x, y), 255, 0, 0);
                        // todo: refine the below to use only the shape is being bugnav'd around instead of the entire map
                        if ((mapped[x][y] & 0b11) == 0b01) {  // there is a wall in the way
                            wallInTheWay = true;
                            break;
                        }
                    }
                }
                if (!wallInTheWay) {
                    doingBugNav = false;
                    rc.setIndicatorString("stopping bugnav we good");
                    bugNavGoingClockwise = null;
                }

                MapLocation nextBugNavLocation = bugNavVertices[bugNavVertexIndex];
                if ((mapped[nextBugNavLocation.x][nextBugNavLocation.y] & 0b11) == 0b01) {  // actually it is a wall, redo bugnav
                    bugNavVertices = genBugNavAroundPath(robotLoc, nextBugNavLocation, centerOfMap);
                    bugNavVertexIndex = 0;
                    nextBugNavLocation = bugNavVertices[bugNavVertexIndex];
                }
                if (nextBugNavLocation.distanceSquaredTo(robotLoc) < 3) {  // if close to this vertex, go to the next one
                    bugNavVertexIndex += 1;
                }
                if (bugNavVertexIndex == bugNavVertices.length) {
                    doingBugNav = false;
                    rc.setIndicatorString("stopping bugnav no moar vertices");
                    bugNavGoingClockwise = null;
                    return;
                }
                for (Direction d : getIdealMovementDirections(robotLoc, bugNavVertices[bugNavVertexIndex])) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        break;
                    }
                }
                return;
            }

            // go away from dam at round 197
            if (rc.getRoundNum() > 197) {
                int tx = 0;
                int ty = 0;
                int amt = 0;
                for (MapLocation loc : getAdjacents(robotLoc)) {
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if ((mapped[loc.x][loc.y] & 0b101) == 0b001) {
                        tx += loc.x;
                        ty += loc.y;
                        amt += 1;
                    }
                }
                if (amt != 0) {  // next to dam
                    for (Direction dir : getIdealMovementDirections(robotLoc, new MapLocation(tx/amt, ty/amt))) {
                        if (rc.canMove(dir.opposite())) {
                            rc.move(dir.opposite());
                            return;
                        }
                    }
                }
            }

            // place traps next to dam
            boolean nextToDam = false;
            for (MapLocation adjacent : getAdjacents(robotLoc)) {
                if (!rc.onTheMap(adjacent)) {
                    continue;
                }
                rc.setIndicatorDot(adjacent, 255, 127, 0);
                // build traps when near the dam
                if (rc.senseMapInfo(adjacent).isDam()) {
                    if ((rc.getCrumbs() >= 1200 && (adjacent.x + adjacent.y) % 2 == 0) || rc.getCrumbs() > 6000) {
                        if (rc.canBuild(TrapType.STUN, robotLoc)) {
                            rc.build(TrapType.STUN, robotLoc);
                            break;
                        }
                    }
                    nextToDam = true;
                }
            }

            // spread out near dam
            if (nextToDam) {
                // move away from dam a little when close to round 200
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                MapLocation[] allyLocations = new MapLocation[allies.length];
                for (int i=0; i<allies.length; i++) {
                    allyLocations[i] = allies[i].getLocation();
                }
                // todo: this does not work
                MapLocation[] spreadOutFromAlliesLocations = sortWithCache(getAdjacents(robotLoc), (loc) -> {
                    int total = 0;
                    for (MapLocation allyLocation : allyLocations) {
                        total -= allyLocation.distanceSquaredTo(loc);
                    }
                    return total;
                });
                for (MapLocation adj : spreadOutFromAlliesLocations) {
                    if (!rc.onTheMap(adj)) {
                        continue;
                    }
                    boolean adjNextToDam = false;
                    for (MapLocation adj2 : getAdjacents(adj)) {
                        if (rc.onTheMap(adj2)) {
                            if ((mapped[adj2.x][adj2.y] & 0b101) == 0b001) {  // dam and explored
                                adjNextToDam = true;
                                break;
                            }
                        }
                    }
                    if (adjNextToDam) {
                        if (rc.canMove(robotLoc.directionTo(adj))) {
                            rc.move(robotLoc.directionTo(adj));
                            return;
                        }
                    }
                }
            }

            // going to the center is a good idea
            rc.setIndicatorLine(robotLoc, centerOfMap, 128, 0, 255);
            MapLocation bugNavWallLocation = null;
            for (Direction d : getTrulyIdealMovementDirections(robotLoc, centerOfMap)) {
                if (rc.canMove(d)) {
                    rc.move(d);
                } else {
                    MapLocation newLoc = robotLoc.add(d);
                    if ((newLoc.x + newLoc.y) % 2 == 0) {
                        if (rc.canFill(newLoc)) {
                            rc.fill(newLoc);
                            return;
                        }
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
            rc.setIndicatorString("starting bugnav");
            doingBugNav = true;
            bugNavVertices = genBugNavAroundPath(robotLoc, bugNavWallLocation, centerOfMap);
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
        Direction[] sortedDirections = sortWithCache(directions, (dir) -> {
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

        if (nearbyAllyLocations.length > 0 && rc.getMovementCooldownTurns() == 0) {
            for (Direction d : sortedDirections) {
                MapLocation fillLoc = robotLoc.add(d);
                if (rc.canFill(fillLoc)) {
                    rc.fill(fillLoc);
                    break;
                }
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
                    boolean isDam = rc.senseMapInfo(l).isDam();
                    mapped[l.x][l.y] = isWall ? (isDam ? 0b001 : 0b101) : (isDam ? 0b011 : 0b111);
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
