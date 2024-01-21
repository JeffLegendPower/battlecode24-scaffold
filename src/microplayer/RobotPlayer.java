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
                if (tryToSpawnDuck()) {
                    continue;
                }
                lastAliveRound = rc.getRoundNum();
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

    public static void buyGlobalUpgrades() throws GameActionException {
        if (rc.getRoundNum() == 600) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
                return;
            }
        }
        if (rc.getRoundNum() == 1200) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
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

    public static boolean tryToSpawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            return false;
        }

        if (isCarrier) {
            if (flagCarrierIndex == -1) {
                System.out.println("big error 2 !!!");
                isCarrier = false;
                return true;
            }
            if (carrierLocations[flagCarrierIndex] == null) {
                System.out.println("big error 3 !!!");
                isCarrier = false;
                return true;
            }
            rc.setIndicatorDot(enemyFlagLocations[flagCarrierIndex], 255, 0, 0);
            rc.setIndicatorDot(carrierLocations[flagCarrierIndex], 127, 255, 0);
            // carrier just died with flag
            if (lastAliveRound == rc.getRoundNum()-1) {
                System.out.println("flag " + (flagCarrierIndex+1) + " was dropped");
                visited.clear();
                lastDroppedFlagValue = rc.readSharedArray(flagCarrierIndex+4) & (1 << 14);
                hasCarrierDroppedFlag[flagCarrierIndex] = true;
                rc.writeSharedArray(flagCarrierIndex+4, lastDroppedFlagValue);
            }
            // carrier died with the flag and 4 turns have passed
            if (lastAliveRound == rc.getRoundNum()-5) {
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

        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
        }

        if (mapHeight == -1 || mapWidth == -1) {
            mapWidth = rc.getMapWidth();
            mapHeight = rc.getMapHeight();
        }

        if (id == -1) {
            id = rc.readSharedArray(0);
            if (rc.canWriteSharedArray(0, id+1)) {
                rc.writeSharedArray(0, id+1);
            }
            rc.setIndicatorString("id: " + id);
        }

        if (protectedFlagLocations == null) {
            protectedFlagLocations = new MapLocation[3];
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
                    protectedFlagLocations[i++] = allySpawn1;

                }
            }
            for (MapLocation protectedFlagLoc : protectedFlagLocations) {
                if (rc.canSpawn(protectedFlagLoc)) {
                    rc.spawn(protectedFlagLoc);
                    isProtector = true;
                    return false;
                }
            }
        }

        for (MapLocation allySpawnLocation : allySpawnLocations) {
            if (rc.canSpawn(allySpawnLocation)) {
                rc.spawn(allySpawnLocation);
                return false;
            }
        }
        return true;
    }

    public static void randomizeRng() throws GameActionException {
        // (linear congruential generator)-ish but bad
        MapLocation robotLoc = rc.getLocation();
        rngSeed = (rngSeed * 47127 + robotLoc.x * 43 + robotLoc.y * 59) % 481936283;
        rng.setSeed(rngSeed);
        if (Clock.getBytecodesLeft() > 1000) {
            shuffleInPlace(allySpawnLocations);
        }
    }

    public static void onTurn() throws GameActionException {
        if (rc.getRoundNum() >= 200) {
            onGameTurn();
        } else {
            onSetupTurn();
        }
    }

    public static void onGameTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        sort(enemyInfos, (enemy) -> enemy.getLocation().distanceSquaredTo(robotLoc));

        // read enemy flag locations from shared
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

        // read carrier locations from shared
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

        // flag carrier stuff
        if (isCarrier) {
            rc.setIndicatorString("carrying flag " + (flagCarrierIndex+1));
            sort(allySpawnLocations, (spawnLoc) -> spawnLoc.distanceSquaredTo(robotLoc));
            MapLocation[] enemyLocations = new MapLocation[enemyInfos.length];
            for (int i=0; i<enemyInfos.length; i++) {
                enemyLocations[i] = enemyInfos[i].getLocation();
            }
            for (MapLocation adjacent : sort(getAdjacents(robotLoc), (loc) -> {
                int total = 0;
                for (MapLocation enemyLocation : enemyLocations) {
                    total += enemyLocation.distanceSquaredTo(loc);
                }
                return loc.distanceSquaredTo(carrierDestination) - total;
            })) {
                if (visited.contains(adjacent)) {
                    continue;
                }
                Direction d = robotLoc.directionTo(adjacent);
                if (rc.canMove(d)) {
                    rc.move(d);
                    if (!rc.hasFlag()) {  // flag deposited
                        System.out.println("deposited flag " + (flagCarrierIndex+1));
                        writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 1);
                        isCarrier = false;
                        flagCarrierIndex = -1;
                        return;
                    }
                    visited.add(adjacent);
                    writeLocationToShared(4+flagCarrierIndex, adjacent, 0, 0);
                    return;
                }
            }
            return;
        }

        // protector stuff
        MapLocation[] diagonals = new MapLocation[]{
                robotLoc.add(Direction.NORTHWEST), robotLoc.add(Direction.SOUTHEAST),
                robotLoc.add(Direction.SOUTHWEST), robotLoc.add(Direction.NORTHEAST)
        };
        if (isProtector) {
            rc.setIndicatorString("i am protector");
            sort(diagonals, (loc) -> {
                int total=0;
                for (RobotInfo enemy : enemyInfos) {
                    total += enemy.getLocation().distanceSquaredTo(robotLoc);
                }
                return total;
            });

            for (MapLocation diag : diagonals) {
                if (rc.canBuild(TrapType.STUN, diag)) {
                    rc.build(TrapType.STUN, diag);
                    return;
                }
            }

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
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, closestCrumbLoc);
            // try to move in the ideal directions
            for (Direction idealMoveDir : idealMovementDirections) {
                if (rc.canMove(idealMoveDir)) {
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
        if (broadcastFlagPathfindLoc == null) {
            MapLocation[] broadcasted = rc.senseBroadcastFlagLocations();
            broadcastFlagPathfindLoc = broadcasted[rng.nextInt(broadcasted.length)];
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
            if (closestCarrierDistance > distance && distance >= 6.7) {
                closestCarrierDistance = distance;
                closestCarrierLocation = carrierLoc;
            }
        }
        if (closestCarrierLocation != null) {
            pathfindGoalLoc = closestCarrierLocation;
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
                if (rc.canPickupFlag(flagInfo.getLocation())) {
                    rc.pickupFlag(flagInfo.getLocation());
                    isCarrier = true;
                    sort(allySpawnLocations, (loc) -> loc.distanceSquaredTo(robotLoc));
                    carrierDestination = allySpawnLocations[0];
                    visited.add(robotLoc);
                    for (int i=0; i<3; i++) {
                        if (enemyFlagLocations[i].equals(flagInfo.getLocation())) {
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

        // spam stun traps when see flag
        int closestEnemyFlag = 1000000;
        for (MapLocation flagLoc : enemyFlagLocations) {
            if (flagLoc == null) {
                continue;
            }
            int dist = flagLoc.distanceSquaredTo(robotLoc);
            if (closestEnemyFlag > dist) {
                closestEnemyFlag = dist;
            }
            break;
        }
        if (mapWidth/1.7 + mapHeight/1.7 < closestEnemyFlag && closestEnemyFlag < mapWidth/3.4 + mapHeight/3.4) {
            if (rc.getCrumbs() > 2000 - rc.getRoundNum() / 2) {
                for (Direction direction : Direction.allDirections()) {
                    MapLocation newLoc = robotLoc.add(direction);
                    if ((newLoc.x+newLoc.y) % 2 == 0) {
                        if (rc.canBuild(TrapType.STUN, robotLoc.add(direction))) {
                            rc.build(TrapType.STUN, robotLoc.add(direction));
                            break;
                        }
                    }
                }
            }
        }

        RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        sort(allyInfos, (ally) -> (ally.getLocation().distanceSquaredTo(robotLoc) - rc.getHealth() / 100 - (rc.hasFlag() ? 10000 : 0)));

        // no enemies nearby-ish
        if (enemyInfos.length == 0 || enemyInfos[0].getLocation().distanceSquaredTo(robotLoc) >= 16) {
            // no enemies nearby-ish -> there are allies nearby
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

            // no enemies nearby-ish -> no allies nearby
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
                            rc.setIndicatorString("moved and got in range and attacked");
                        }
                        rc.setIndicatorString("moving closer to enemy");
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

        // more enemies than allies by a bit
        // todo: place traps if overwhelmed
        if (closestEnemyLoc.distanceSquaredTo(robotLoc) >= 16) {  // can safely flee
            MapLocation finalPathfindGoalLoc = pathfindGoalLoc;
            for (Direction d : sort(getIdealMovementDirections(closestEnemyLoc, robotLoc), (dir) -> robotLoc.add(dir).distanceSquaredTo(finalPathfindGoalLoc))) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    rc.setIndicatorString("fleeing");
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
        rc.setIndicatorString("dying a hero");
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
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, closestCrumbLoc);
            // try to move in the ideal directions
            for (Direction idealMoveDir : idealMovementDirections) {
                if (rc.canMove(idealMoveDir)) {
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

        // ducks 3-7 go to center
        if ((3 <= id && id <= 7) && rc.getRoundNum() <= 78) {  // 1-78
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

        // stick to dam
        if (rc.getRoundNum() > 130) {
            if (findDamRotateLeft == null) {
                findDamRotateLeft = rng.nextBoolean();
            }
            for (MapLocation ml : getAdjacents(robotLoc)) {
                if (!rc.onTheMap(ml)) {
                    continue;
                }
                if (rc.senseMapInfo(ml).isDam()) {
                    if ((robotLoc.x+robotLoc.y) % 2 == 0) {
                        if (rc.canBuild(TrapType.STUN, robotLoc)) {
                            rc.build(TrapType.STUN, robotLoc);
                        }
                    }
                    return;
                }
            }
            MapLocation damLocation = null;
            for (MapInfo sensed : shuffleInPlace(rc.senseNearbyMapInfos(-1))) {
                if (sensed.isDam()) {
                    damLocation = sensed.getMapLocation();
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
            if (findDamDirection == null) {
                findDamDirection = directions[0];
            }
            Direction d = findDamDirection;
            for (int i=0; i<8; i++) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    findDamDirection = d;
                    return;
                }
                d = findDamRotateLeft ? d.rotateLeft() : d.rotateRight();
            }
            return;
        }

        // no allies in sight, just keep moving
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
}
