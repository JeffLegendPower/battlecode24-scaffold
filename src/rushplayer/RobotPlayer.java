package rushplayer;

import battlecode.common.*;
import rushplayer.map.*;

import static rushplayer.General.*;
import static rushplayer.Utility.*;

public class RobotPlayer {
    public static void run(RobotController rc2) {
        rc = rc2;

        // noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (spawnDuck()) {
                    continue;
                }
                randomizeRng();
                buyGlobalUpgrades();
                onTurn();
            } catch (GameActionException gameActionException) {
                System.out.println("GAMEACTIONEXCEPTION ==========");
                gameActionException.printStackTrace();
            } catch (Exception exception) {
                System.out.println("EXCEPTION ====================");
                exception.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void buyGlobalUpgrades() throws GameActionException {
        if (rc.getRoundNum() == 750) {
            if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
            }
        }
        if (rc.getRoundNum() == 1500) {
            if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }
        }
    }

    public static void randomizeRng() {
        MapLocation robotPos = rc.getLocation();
        rngSeed = (rngSeed * 6415828 + robotPos.x * robotPos.y * 582 + robotPos.x * 299) % 49817273;
        rng.setSeed(rngSeed);
    }

    public static boolean spawnDuck() throws GameActionException {
        if (rc.isSpawned()) {
            return false;
        }
        if (allySpawnLocations == null) {
            allySpawnLocations = rc.getAllySpawnLocations();
            shuffleInPlace(allySpawnLocations);
        }
        if (map == null) {
            map = new BaseMap();
            map = identifyMap();
        }

        if (whereShouldSpawn == null) {
            for (MapLocation spawnLoc : allySpawnLocations) {
                if (rc.canSpawn(spawnLoc)) {
                    rc.spawn(spawnLoc);
                    onDuckSpawned();
                    return false;
                }
            }
        } else {
            MapLocation[] sortedAllySpawnLocations = sort(allySpawnLocations.clone(), (loc) -> loc.distanceSquaredTo(whereShouldSpawn), false);
            for (MapLocation spawnLoc : sortedAllySpawnLocations) {
                if (rc.canSpawn(spawnLoc)) {
                    rc.spawn(spawnLoc);
                    onDuckSpawned();
                    return false;
                }
            }
        }
        return true;
    }

    public static void onDuckSpawned() throws GameActionException {
        spawnPosition = rc.getLocation();
        hasHealingFocus = (spawnPosition.x * 599 + spawnPosition.y * 61) % 3 == 0;
        defaultExplorationPoint = symmetricLocation(spawnPosition);
        timeSinceLastMoveForRunBack = 0;

        if (rc.canPickupFlag(rc.getLocation())) {
            rc.pickupFlag(rc.getLocation());
        }

        // died with the flag
        if (carryBackFlagIndex != null) {
            if (enemyFlagLocations[carryBackFlagIndex] != null) {
                writeLocationToShared(carryBackFlagIndex, enemyFlagLocations[carryBackFlagIndex], 0);
                rc.writeSharedArray(carryBackFlagIndex+3, 0);
                carryBackFlagIndex = null;
            }
            lastCarryLocationWhenAlive = null;
        }

        Direction[] shuffledDirections = shuffleInPlace(directions.clone());
        for (Direction d : shuffledDirections) {
            MapLocation offset = rc.getLocation().add(d);
            boolean broken = false;
            for (MapLocation allySpawnLoc : allySpawnLocations) {
                if (offset == allySpawnLoc) {
                    broken = true;
                    break;
                }
            }
            if (broken) {
                continue;
            }
            if (rc.canMove(d)) {
                rc.move(d);
                if (rc.canPickupFlag(offset)) {
                    rc.pickupFlag(offset);
                }
                break;
            }
        }
    }

    // maybe a bad idea
    public static BaseMap identifyMap() {
        int w = rc.getMapWidth();
        int h = rc.getMapHeight();
        BaseMap map = new BaseMap();
        if (w == 31 && h == 31) {
            map = new DefaultSmallMap();
        }
        if (w == 45 && h == 31) {
            map = new DefaultMediumMap();
        }
        if (w == 59 && h == 31) {
            map = new DefaultLargeMap();
        }
        if (w == 59 && h == 59) {
            map = new DefaultHugeMap();
        }
        map.width = w;
        map.height = h;

        for (int i=0; i<allySpawnLocations.length; i++) {
            map.enemySpawnLocations[i] = symmetricLocation(allySpawnLocations[i]);
        }
        return map;
    }

    public static void onTurn() throws GameActionException {
        BytecodeLogger.log("A");
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
            onSetupTurn();
        } else {
            onGameTurn();
        }
        BytecodeLogger.log("Z");
        BytecodeLogger.showIndicator();
        BytecodeLogger.clear();
    }

    public static void onSetupTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        // detect walls
        int roughWaterCount = 0;
        if (rc.getRoundNum() % 2 == 0) {
            for (MapInfo mi : rc.senseNearbyMapInfos()) {
                if (mi.isWall() && !mi.isDam()) {
                    wallLocations.add(mi.getMapLocation());
                }
                if (mi.isWater()) {
                    roughWaterCount++;
                }
            }
            BytecodeLogger.log("B");
        }

        // collect protected flag location data
        for (int i=0; i<3; i++) {
            if (protectedFlagLocations[i] == null) {
                MapLocation newProtectedFlagLocation = readLocationFromShared(61+i);
                if (newProtectedFlagLocation != null) {
                    protectedFlagLocations[i] = newProtectedFlagLocation;
                    continue;
                }
                break;
            } else {
                rc.setIndicatorDot(protectedFlagLocations[i], 0, 255, 255);
            }
        }
        BytecodeLogger.log("C");

        // bring flag to safe location
        if (rc.hasFlag()) {
            decidedProtectorLocationIndex : if (protectorLocationIndex == null) {
                for (int i = 0; i < 3; i++) {
                    if (protectedFlagLocations[i] == null) {
                        protectorLocationIndex = i;
                        protectedFlagLocations[i] = robotLoc;
                        break decidedProtectorLocationIndex;
                    }
                }
                System.out.println("big issue !!");
            }

            writeLocationToShared(61 + protectorLocationIndex, robotLoc, 1);

            // move away from the enemy spawn
            MapLocation avgEnemySpawnLoc = averageLocation(map.enemySpawnLocations);
            MapLocation farAwayLoc = locationInOtherDirection(robotLoc, avgEnemySpawnLoc);
            for (Direction awayDirection : getIdealMovementDirections(robotLoc, farAwayLoc)) {
                MapLocation nextLoc = robotLoc.add(awayDirection);
                if (rc.canMove(awayDirection)) {
                    if (avgEnemySpawnLoc.distanceSquaredTo(robotLoc) < avgEnemySpawnLoc.distanceSquaredTo(nextLoc)) {
                        if (senseLegalStartingFlagPlacement(nextLoc)) {
                            if (nextToWall(robotLoc) && !nextToWall(nextLoc)) {  // stay near the wall
                                continue;
                            }
                            rc.move(awayDirection);
                            timeSinceLastMoveForFlagProtector = 0;
                            return;
                        }
                    }
                }
            }
            timeSinceLastMoveForFlagProtector += 1;

            // prevent dropping the flag at a spawn location if the duck is surrounded by allies when it spawns
            if (rc.getRoundNum() < 10) {
                return;
            }

            // the flag carrying duck can only move every other turn, so if it is unable to move, then start defending the flag
            if (timeSinceLastMoveForFlagProtector > 5) {
                timeSinceLastMoveForFlagProtector = 0;
                // now surrounding the flag safely
                for (int i=0; i<3; i++) {
                    if (protectedFlagLocations[i] == null) {
                        writeLocationToShared(61+i, robotLoc, 0);
                        protectedFlagLocations[i] = robotLoc;
                        break;
                    }
                }
                protectingFlag = true;
                rc.dropFlag(robotLoc);
            } else {
                return;
            }

            BytecodeLogger.log("Y");
        }

        // protect the flag
        if (protectingFlag) {
            MapLocation[] adjacents = getAdjacents(robotLoc);
            // dig water and fill it until level 6, also surround flag and self with water
            for (MapLocation adjacent : adjacents) {
                if (rc.canDig(adjacent)) {
                    rc.dig(adjacent);
                    return;
                }
                if (rc.canFill(adjacent) && rc.getLevel(SkillType.BUILD) != 6) {
                    rc.fill(adjacent);
                    return;
                }
            }

            // place traps
            for (MapLocation adjacent : adjacents) {
                if (rc.canBuild(TrapType.EXPLOSIVE, adjacent)) {
                    rc.build(TrapType.EXPLOSIVE, adjacent);
                    return;
                }
            }
            return;
        }

        // move towards crumbs
        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
        for (MapLocation crumbLocation : sort(crumbLocations, (a) -> a.distanceSquaredTo(robotLoc), false)) {
            Direction[] goToCrumbMovementDirections = getIdealMovementDirections(robotLoc, crumbLocation);
            for (Direction dir : goToCrumbMovementDirections) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(dir))) {
                        rc.fill(robotLoc.add(dir));
                        return;
                    }
                }
            }
        }

        RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

        // spam traps and stuff on deploy
        if (rc.getRoundNum() > 194) {
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemyRobots.length > 4) {
                MapLocation centerLocation = new MapLocation(map.width/2, map.height/2);
                if (rng.nextInt(robotLoc.distanceSquaredTo(centerLocation)) < 15) {
                    if (rc.canBuild(TrapType.STUN, robotLoc)) {
                        rc.build(TrapType.STUN, robotLoc);
                    }
                }
            }
        }

        // get close to the center but also far away from other robots-ish
        if (rc.getRoundNum() > rng.nextInt(40)+120) {
            MapLocation avgEnemySpawnLocation = averageLocation(map.enemySpawnLocations);
            int dx = (avgEnemySpawnLocation.x - robotLoc.x) * 5;
            int dy = (avgEnemySpawnLocation.y - robotLoc.y) * 5;
            if (allyRobots.length > 2) {
                for (RobotInfo allyRobot : allyRobots) {
                    MapLocation allyLocation = allyRobot.getLocation();
                    if (allyLocation == robotLoc) {
                        continue;
                    }
                    dx += robotLoc.x - allyLocation.x;
                    dy += robotLoc.y - allyLocation.y;
                    if (contains(protectedFlagLocations, allyLocation) || allyRobot.hasFlag()) {
                        dx += (avgEnemySpawnLocation.x - robotLoc.x) * 18;
                        dy += (avgEnemySpawnLocation.y - robotLoc.y) * 18;
                    }
                }
            }
            MapLocation uniformDistLocation = robotLoc.translate(dx, dy);
            Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, uniformDistLocation);
            for (Direction dir : idealMovementDirections) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    break;
                } else if (roughWaterCount > 12) {
                    if (rc.canFill(robotLoc.add(dir))) {
                        rc.fill(robotLoc.add(dir));
                    }
                }
            }
            return;
        }

        // move away from ally robots in the beginning (helps with good exploration of area)
        int dx = 0;
        int dy = 0;
        MapLocation centerLocation = new MapLocation(map.width/2, map.height/2);
        for (RobotInfo allyRobot : allyRobots) {
            MapLocation allyLocation = allyRobot.getLocation();
            if (allyLocation == robotLoc) {
                continue;
            }
            dx += robotLoc.x-allyLocation.x;
            dy += robotLoc.y-allyLocation.y;
            if (contains(protectedFlagLocations, allyLocation) || allyRobot.hasFlag()) {
                dx += (centerLocation.x-robotLoc.x)*10;
                dy += (centerLocation.y-robotLoc.y)*10;
            }
        }
        MapLocation uniformDistLocation = robotLoc.translate(dx, dy);
        Direction[] idealMovementDirections = getIdealMovementDirections(robotLoc, uniformDistLocation);
        for (Direction dir : idealMovementDirections) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                break;
            }
        }
    }

    public static boolean senseLegalStartingFlagPlacement(MapLocation myLoc) throws GameActionException {
        for (int i=0; i<3; i++) {
            MapLocation l = readLocationFromShared(61+i);
            if (protectorLocationIndex == i) {
                continue;
            }
            if (l == null) {
                continue;
            }
            if (l.distanceSquaredTo(myLoc) <= 49) {
                rc.setIndicatorString("too close");
                return false;
            }
        }
        StringBuilder total = new StringBuilder();
        for (int i=0; i<3; i++) {
            MapLocation l = readLocationFromShared(61+i);
            total.append(l);
            total.append(",");
        }
        rc.setIndicatorString(total.toString());
        return true;
    }

    public static void onGameTurn() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        // protectors keep setting up
        if (protectingFlag) {
            MapLocation locationToProtect = readLocationFromShared(61+protectorLocationIndex);
            if (locationToProtect == null) {
                return;
            }
            rc.setIndicatorDot(locationToProtect, 0, 255, 255);

            if (locationToProtect.x != robotLoc.x || locationToProtect.y != robotLoc.y) {
                for (Direction d : getIdealMovementDirections(robotLoc, locationToProtect)) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        break;
                    } else {
                        if (rc.canFill(robotLoc.add(d))) {
                            rc.fill(robotLoc.add(d));
                        }
                    }
                }
            } else {
                MapLocation[] adjacents = getAdjacents(robotLoc);
                // dig water and fill it until level 4, also surround flag and self with water
                for (MapLocation adjacent : adjacents) {
                    if (rc.canDig(adjacent)) {
                        rc.dig(adjacent);
                        return;
                    }
                    if (rc.canFill(adjacent) && rc.getLevel(SkillType.BUILD) < 4 && rc.senseMapInfo(adjacent).getTrapType() == TrapType.NONE) {
                        rc.fill(adjacent);
                        return;
                    }
                }

                // place traps
                for (MapLocation adjacent : adjacents) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, adjacent)) {
                        rc.build(TrapType.EXPLOSIVE, adjacent);
                        return;
                    }
                }
                return;
            }

            // when enemies attempt to take the flag
            if (rc.getHealth() < 350 || rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 4) {
                if (whereShouldSpawn != rc.getLocation()) {
                    writeLocationToShared(60, rc.getLocation(), 1);
                }
            } else {
                if (whereShouldSpawn == rc.getLocation()) {
                    rc.writeSharedArray(60, 0);
                }
            }
            return;
        }

        // run back with flag
        if (rc.hasFlag()) {
            runBackWithFlag();
            return;
        }

        // read where allies are carrying flags
        for (int i=0; i<3; i++) {
            enemyFlagCarrierLocations[i] = readLocationFromShared(3+i);
        }

        // prevent fake flag thing
        for (int i=0; i<3; i++) {
            MapLocation enemyFlagLocation = enemyFlagLocations[i];
            if (enemyFlagLocation == null) {
                continue;
            }
            if (enemyFlagLocation.x == robotLoc.x && enemyFlagLocation.y == robotLoc.y) {
                if (!enemyFlagIsPickedUp[i]) {  // flag is here, but reported as not picked up
                    rc.writeSharedArray(i, 0);
                    rc.writeSharedArray(i+3, 0);
                }
            }
        }

        // get where should spawn from index 60
        MapLocation newWhereShouldSpawn = readLocationFromShared(60);
        if (newWhereShouldSpawn != null) {
            whereShouldSpawn = newWhereShouldSpawn;
        } else {
            MapLocation centerLocation = new MapLocation(map.width/2, map.height/2);
            whereShouldSpawn = sort(allySpawnLocations.clone(), (a) -> a.distanceSquaredTo(centerLocation), false)[0];
        }

        MapLocation pathFindLocation = defaultExplorationPoint;

        // move towards bodyguards
        int distanceToCarrier = 256;
        MapLocation nearestFlagCarrierLoc = null;
        for (int i=0; i<3; i++) {
            if (!bitflag(rc.readSharedArray(3+i))) {
                MapLocation ml = enemyFlagCarrierLocations[i];
                if (ml != null) {
                    int newDistance = ml.distanceSquaredTo(robotLoc);
                    if (newDistance < distanceToCarrier && newDistance > 9) {
                        distanceToCarrier = newDistance;
                        nearestFlagCarrierLoc = ml;
                    }
                }
            }
        }
        if (nearestFlagCarrierLoc != null) {
            pathFindLocation = nearestFlagCarrierLoc;
        }

        // fix bug idk why it happening
        if (!rc.isSpawned()) {
            return;
        }

        // move towards crumbs
        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
        for (MapLocation crumbLocation : sort(crumbLocations, (a) -> a.distanceSquaredTo(robotLoc), false)) {
            Direction[] goToCrumbMovementDirections = getIdealMovementDirections(robotLoc, crumbLocation);
            for (Direction dir : goToCrumbMovementDirections) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return;
                } else {
                    if (rc.canFill(robotLoc.add(dir))) {
                        rc.fill(robotLoc.add(dir));
                        return;
                    }
                }
            }
        }

        // read the shared array for flags
        for (int i=0; i<3; i++) {
            MapLocation readEnemyFlagLocation = readLocationFromShared(i);
            // enemy flag location was broadcast
            if (readEnemyFlagLocation != null) {
                // we don't know about it yet
                if (enemyFlagLocations[i] == null) {
                    enemyFlagLocations[i] = readEnemyFlagLocation;
                }
                enemyFlagIsPickedUp[i] = bitflag(rc.readSharedArray(i));
                // if the enemy flag is not picked up yet, go to the location
                if (!enemyFlagIsPickedUp[i]) {
                    pathFindLocation = readEnemyFlagLocation;
                }
            }
        }

        // try to grab flags
        for (int i=0; i<3; i++) {
            if (enemyFlagIsPickedUp[i]) {
                continue;
            }
            MapLocation enemyFlagLoc = enemyFlagLocations[i];
            if (enemyFlagLoc == null) {
                continue;
            }
            if (enemyFlagLoc.isAdjacentTo(robotLoc)) {
                if (rc.canPickupFlag(enemyFlagLoc)) {
                    rc.pickupFlag(enemyFlagLoc);
                    carryBackFlagIndex = i;
                    enemyFlagIsPickedUp[i] = true;
                    writeLocationToShared(i, enemyFlagLoc, 1);
                }
            }
        }

        // debug the enemy flag locations and carrier locations
        for (int i=0; i<3; i++) {
            if (enemyFlagLocations[i] != null) {
                if (enemyFlagIsPickedUp[i]) {
                    rc.setIndicatorDot(enemyFlagLocations[i], 0, 0, 255);
                } else {
                    rc.setIndicatorDot(enemyFlagLocations[i], 255, 0, 255);
                }
            }
        }

        RobotInfo[] enemyDucks = sort(rc.senseNearbyRobots(-1, rc.getTeam().opponent()), (ri) -> {
            if (ri.hasFlag()) {
                rc.setIndicatorDot(ri.getLocation(), 127, 255, 0);
                return -1;
            } else {
                return ri.getLocation().distanceSquaredTo(robotLoc);
            }
        }, false);

        // move towards nearby enemies (especially those with flags)
        if (enemyDucks.length > 1) {
            for (RobotInfo enemy : enemyDucks) {
                boolean hasBroken = false;
                for (Direction d : getIdealMovementDirections(robotLoc, enemy.getLocation())) {
                    if (rc.canMove(d)) {
                        rc.setIndicatorString("moving towards enemies");
                        pathFindLocation = robotLoc.add(d);
                        hasBroken = true;
                        break;
                    }
                }
                if (hasBroken) {
                    break;
                }
            }
        }

        // place traps if overwhelmed
        if (enemyDucks.length >= 2 && rc.getCrumbs() > 1300) {
            MapLocation avgDucksLocation = averageLocation(robotInfosToMapLocations(enemyDucks));
            for (Direction d : getIdealMovementDirections(robotLoc, avgDucksLocation)) {
                MapLocation newTrapLoc = robotLoc.add(d);
                TrapType whichTrapToBuild = (enemyDucks.length >= 4 || rc.getCrumbs() >= 3500) ? TrapType.EXPLOSIVE : TrapType.STUN;
                if (rc.canBuild(whichTrapToBuild, newTrapLoc)) {
                    rc.build(whichTrapToBuild, newTrapLoc);
                    break;
                }
            }
        }

        // delete too close flags from enemyFlagLocations
        deleteMistakenFlags : for (int i1=0; i1<3; i1++) {
            for (int i2=i1+1; i2<3; i2++) {
                MapLocation a = enemyFlagLocations[i1];
                MapLocation b = enemyFlagLocations[i2];
                if (a == null) {
                    continue;
                }
                if (b == null) {
                    continue;
                }
                if (a.distanceSquaredTo(b) <= 36) {
                    System.out.println("too close!!");
                    rc.writeSharedArray(i2, 0);
                    rc.writeSharedArray(i2+3, 0);
                    enemyFlagLocations[i2] = null;
                    enemyFlagIsPickedUp[i2] = false;
                    break deleteMistakenFlags;
                }
            }
        }

        // report out flags
        for (FlagInfo flagInfo : rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
            MapLocation flagLocation = flagInfo.getLocation();
            if (flagInfo.isPickedUp()) {
                continue;
            }
            // make sure the flag is not already known to us
            boolean alreadyKnown = false;
            for (int i=0; i<3; i++) {
                if (enemyFlagLocations[i] == null) {
                    continue;
                }
                if (enemyFlagLocations[i].x == flagLocation.x && enemyFlagLocations[i].y == flagLocation.y) {
                    alreadyKnown = true;
                    break;
                }
            }
            if (alreadyKnown) {
                continue;
            }

            loopOverI : for (int i=0; i<3; i++) {
                if (enemyFlagLocations[i] == null) {  // overwrite the local array and broadcast the flag location
                    for (int i2=0; i2<3; i2++) {
                        MapLocation lastCarriedLoc = enemyFlagCarrierLocations[i2];
                        if (lastCarriedLoc == null) {
                            continue;
                        }
                        if (lastCarriedLoc.x == flagLocation.x && lastCarriedLoc.y == flagLocation.y) {
                            rc.setIndicatorDot(flagLocation, 0, 127,127);
                            break loopOverI;
                        }
                    }
                    for (int i2=0; i2<3; i2++) {
                        if (enemyFlagLocations[i2] == null) {
                            continue;
                        }
                        if (enemyFlagLocations[i2].distanceSquaredTo(flagLocation) <= 30) {  // too close cant possibly bee
                            break loopOverI;
                        }
                    }
                    System.out.println("saw flag #" + i + " at " + flagLocation + " | ");
                    enemyFlagLocations[i] = flagLocation;
                    writeLocationToShared(i, flagLocation, 0);
                    break;
                }
            }


            // in the case that the carrier died with flag there
            for (int i=0; i<3; i++) {
                if (enemyFlagCarrierLocations[i] == null) {
                    continue;
                }
                if (enemyFlagCarrierLocations[i].x == flagLocation.x && enemyFlagCarrierLocations[i].y == flagLocation.y) {
                    if (rc.canPickupFlag(flagLocation)) {  // become the new carrier
                        rc.pickupFlag(flagLocation);
                        carryBackFlagIndex = i;
                        enemyFlagIsPickedUp[i] = true;
                        writeLocationToShared(i, flagLocation, 1);
                        writeLocationToShared(i+3, flagLocation, 0);
                    } else {
                        pathFindLocation = flagLocation;
                    }
                    break;
                }
            }
        }

        // move towards pathfinding location
        if (rc.onTheMap(pathFindLocation)) {
            rc.setIndicatorDot(pathFindLocation, 0, 255, 0);
        }
        for (Direction dir : getIdealMovementDirections(robotLoc, pathFindLocation)) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                break;
            } else {
                if (rc.canFill(robotLoc.add(dir))) {
                    rc.fill(robotLoc.add(dir));
                }
            }
        }

        if (hasHealingFocus) {
            // heal if has not attacked
            RobotInfo[] allyRobots = rc.senseNearbyRobots(2, rc.getTeam());
            if (allyRobots.length != 0) {
                sort(allyRobots, robotInfo -> {
                    int h = robotInfo.getHealth();
                    if (robotInfo.hasFlag()) {
                        return -1;
                    }
                    return h;
                }, false);
                for (RobotInfo ally : allyRobots) {
                    if (!rc.canHeal(ally.getLocation())) {
                        break;
                    }
                    rc.heal(ally.getLocation());
                    break;
                }
            }
        }

        // attack
        boolean hasAttacked = false;
        for (RobotInfo enemy : enemyDucks) {
            rc.setIndicatorDot(enemy.getLocation(), 255, 0, 0);
            if (rc.canAttack(enemy.getLocation())) {
                rc.attack(enemy.getLocation());
                hasAttacked = true;
                break;
            }
        }

        if (!hasHealingFocus) {
            // heal if has not attacked
            if (!hasAttacked) {
                RobotInfo[] allyRobots = rc.senseNearbyRobots(2, rc.getTeam());
                if (allyRobots.length != 0) {
                    sort(allyRobots, robotInfo -> {
                        int h = robotInfo.getHealth();
                        if (robotInfo.hasFlag()) {
                            return -1;
                        }
                        return h;
                    }, false);
                    for (RobotInfo ally : allyRobots) {
                        if (!rc.canHeal(ally.getLocation())) {
                            break;
                        }
                        rc.heal(ally.getLocation());
                    }
                }
            }
        }

        rc.setIndicatorString(reprLocations(rc.senseBroadcastFlagLocations()));
    }

    public static void runBackWithFlag() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();

        // detect walls
        for (MapInfo mi : rc.senseNearbyMapInfos()) {
            if (mi.isWall() && !mi.isDam()) {
                wallLocations.add(mi.getMapLocation());
            }
        }

        // move towards pathfinding location
        MapLocation[] spawnLocations = sort(allySpawnLocations, a -> a.distanceSquaredTo(robotLoc), false);
        rc.setIndicatorLine(robotLoc, spawnLocations[0], 255, 0, 255);
        for (Direction dir : sort(directions, (a) -> {
            int directionBadness = robotLoc.add(a).distanceSquaredTo(spawnLocations[0]);
            if (wallLocations.size() < 30) {
                MapLocation nextLoc = robotLoc;
                for (int i = 0; i < 3; i++) {
                    nextLoc = nextLoc.add(a);
                    if (wallLocations.contains(nextLoc)) {
                        directionBadness += 10000;
                        break;
                    }
                }
            }
            return directionBadness;
        }, false)) {
            MapLocation nextLoc = robotLoc.add(dir);
            if (visitedForRunBack.contains(nextLoc)) {
                continue;
            }
            if (rc.canMove(dir)) {
                visitedForRunBack.add(robotLoc);
                rc.move(dir);
                writeLocationToShared(carryBackFlagIndex+3, nextLoc, robotLoc.distanceSquaredTo(spawnLocations[0]) < 16 ? 1 : 0);
                lastCarryLocationWhenAlive = nextLoc;
                timeSinceLastMoveForRunBack = 0;
                return;
            }
            // todo: fill water that is in the way by dropping the flag and then picking it up again
        }

        timeSinceLastMoveForRunBack += 1;
        if (timeSinceLastMoveForRunBack > 4) {
            // could not move anywhere, so clear visited
            visitedForRunBack.clear();
        }
    }
}
