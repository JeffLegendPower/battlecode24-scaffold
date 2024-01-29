package v11_2.robots;

import battlecode.common.*;
import v11_2.*;

import java.util.Random;

public class Attacker extends AbstractRobot {

    private Direction continueInThisDirection = null;

    private int defendOn = 9999999;

    private MapLocation lastTarget = null;
    private MapLocation flagTarget;

    private MicroAttacker microAttacker;

    @Override
    public boolean setup(RobotController rc) throws GameActionException {
//        if (RobotPlayer.rng.nextInt(10) == 0)
//            deviant = true;

        defendOn = RobotPlayer.rng.nextInt(10) + 5;
        microAttacker = new MicroAttacker(rc);

        spawn(rc);
        return true;
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        flagTarget = null;

        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
        int[] centerLocationWeights = new int[3];
        int total = 0;
        for (int i=0; i<3; i++) {
            centerLocationWeights[i] = v & 0b11111;
            total += v & 0b11111;
            v >>= 5;
        }

        Integer[] centerSpawnLocationWeightsIndicies = Utils.sort(new Integer[] {0, 1, 2}, (i) -> -centerLocationWeights[i]);

        for (int i = 0; i < 3; i++) {
            if (centerLocationWeights[centerSpawnLocationWeightsIndicies[i]] <= 0) continue;
            if (centerLocationWeights[centerSpawnLocationWeightsIndicies[i]] < total / 2) {
                    if (RobotPlayer.rng.nextInt(3) == 1) {
                    continue;
                }
            }
            MapLocation centerSpawnLocation = RobotPlayer.allyFlagSpawnLocs[centerSpawnLocationWeightsIndicies[i]];
            for (MapLocation adjacent : Utils.getAdjacents(centerSpawnLocation)) {
                if (rc.canSpawn(adjacent)) {
                    rc.spawn(adjacent);
                    spawn = adjacent;
                    return;
                }
            }
        }

        if (lastTarget != null) {
            int bestDist = 999999;
            MapLocation bestLoc = null;
            for (MapLocation spawnLoc : rc.getAllySpawnLocations()) {
                int dist = spawnLoc.distanceSquaredTo(lastTarget);
                if (rc.canSpawn(spawnLoc) && dist < bestDist) {
                    bestDist = dist;
                    bestLoc = spawnLoc;
                }
            }

            if (bestLoc != null) {
                rc.spawn(bestLoc);
                spawn = bestLoc;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flagLoc != null) {
                    flagTarget = flagLoc;
                    break;
                }
            }
        }


        if (flagTarget == null) {
            super.spawn(rc);
            return;
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation finalFlagTarget = flagTarget;
        Utils.sort(spawnLocs, (spawnLoc) -> spawnLoc.distanceSquaredTo(finalFlagTarget));
        for (MapLocation spawnLoc : spawnLocs) {
            if (rc.canSpawn(spawnLoc)) {
                rc.spawn(spawnLoc);
                spawn = spawnLoc;
                return;
            }
        }
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        // First do some quick flag detection stuff

        FlagInfo[] nearbyFlags = Utils.sort(rc.senseNearbyFlags(-1, rc.getTeam().opponent()),
                (flag) -> flag.getLocation().distanceSquaredTo(curLoc));
        detectAndPickupFlags(rc, nearbyFlags);

        // If we see crumbs go for them real quick
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length > 0) {
            Utils.sort(crumbLocs, (crumbLoc) -> crumbLoc.distanceSquaredTo(curLoc));
            MapLocation closestCrumbLoc = crumbLocs[0];
            if (continueInThisDirection != null) {
                if (rc.canMove(continueInThisDirection)) {
                    rc.move(continueInThisDirection);
                }

                if (RobotPlayer.rng.nextInt(7) == 0)
                    continueInThisDirection = null;
            }

            Direction[] idealMovementDirections = Utils.getIdealMovementDirections(curLoc, closestCrumbLoc);
            int i = 0;
            for (Direction idealMoveDir : idealMovementDirections) {
                i++;
                if (rc.canMove(idealMoveDir)) {
                    if (i >= 3) {
                        continueInThisDirection = idealMoveDir;
                    }
                    rc.move(idealMoveDir);
                    return;
                } else {
                    if (rc.canFill(curLoc.add(idealMoveDir))) {
                        rc.fill(curLoc.add(idealMoveDir));
                        return;
                    }
                }
            }

            return;
        }

        MapLocation target = null;
        int bestDist = 999999;

        for (int i = 0; i < 3; i++) {
            MapLocation flag = enemyFlagLocs[i];
            if (flag != null) {
                int dist = flag.distanceSquaredTo(curLoc);
                if (dist < bestDist) {
                    target = flag;
                    bestDist = dist;
                }
            }
        }

        boolean suicide = false;
        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
        int[] centerLocationWeights = new int[3];
        int total = 0;
        for (int i=0; i<3; i++) {
            centerLocationWeights[i] = v & 0b11111;
            total += v & 0b11111;
            v >>= 5;
        }

        for (int i = 0; i < 3; i++) {
            if (centerLocationWeights[i] < total * .65) continue; // TODO: Test if this actually is doing something
            rc.setIndicatorDot(RobotPlayer.allyFlagSpawnLocs[i], 0, 255, 0);
            suicide = true;
            break;
        }


        lastTarget = target;

        // TODO if theres no enemy flags then we stop moving i think or ally flags idk
        if (target == null) {
            target = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

            MapLocation[] enemyFlagBroadcasts = Utils.sort(rc.senseBroadcastFlagLocations(), (loc) -> loc.distanceSquaredTo(curLoc));
            if (enemyFlagBroadcasts.length > 0) {
                target = enemyFlagBroadcasts[0];
            } else {
                target = RobotPlayer.allyFlagSpawnLocs[new Random().nextInt(3)];
            }
        }

        RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Utils.sort(enemyInfos, (enemy) -> enemy.getLocation().distanceSquaredTo(curLoc));
        Utils.sort(allyInfos, (ally) -> (ally.getLocation().distanceSquaredTo(curLoc) - rc.getHealth() / 100 - (rc.hasFlag() ? 10000 : 0)));

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        int distToClosestSpawn = Utils.getClosest(spawnLocs, curLoc).distanceSquaredTo(curLoc);

        int numTraps = 0;
        for (Direction dir : RobotPlayer.directions) {
            MapLocation newLoc = curLoc.add(dir);
            if (rc.onTheMap(newLoc)) {
                if (RobotPlayer.map[newLoc.x][newLoc.y].getTrapType() != TrapType.NONE) {
                    numTraps++;
                }
            }
        }

        if (nearbyFlags.length > 0) {
            MapLocation closestFlag = null;
            for (FlagInfo flag : nearbyFlags) {
                if (!flag.isPickedUp()) {
                    closestFlag = flag.getLocation();
                }
            }
            if (closestFlag != null && curLoc.distanceSquaredTo(closestFlag) <= 64) {
                Pathfinding.moveTowards(rc, curLoc, closestFlag, true);
            }
        }

        // no enemies close enough to attack even if they run in
        if (enemyInfos.length == 0 || enemyInfos[0].getLocation().distanceSquaredTo(curLoc) >= 16) {
            // no enemies nearby-ish -> spam traps when no enemies in 2-step range
            if (enemyInfos.length > 0) {
                if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2
                        || (curLoc.distanceSquaredTo(target) <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)
                        || (distToClosestSpawn <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)) {
                    for (Direction direction : Direction.allDirections()) {
                        MapLocation newLoc = curLoc.add(direction);
                        if (rc.getCrumbs() > 5000 || (curLoc.x + curLoc.y) % 2 == 0) {
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
                if (weakestAlly.hasFlag()) {
                    v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
                    centerLocationWeights = new int[3];
                    total = 0;
                    for (int i=0; i<3; i++) {
                        centerLocationWeights[i] = v & 0b11111;
                        total += v & 0b11111;
                        v >>= 5;
                    }


                    int[] finalCenterLocationWeights = centerLocationWeights;
                    Integer[] centerSpawnLocationWeightsIndicies = Utils.sort(new Integer[] {0, 1, 2}, (i) -> finalCenterLocationWeights[i]);

                    MapLocation bestSpawn = RobotPlayer.allyFlagSpawnLocs[centerSpawnLocationWeightsIndicies[0]];

                    rc.setIndicatorLine(curLoc, allyInfos[0].getLocation(), 255,0,0);
                    Pathfinding.moveTowards(rc, curLoc, bestSpawn, true);
                }
                else if (weakestAlly.getHealth() <= 1000 - rc.getHealAmount()) {
                    for (Direction d : Utils.sort(
                            Utils.getIdealMovementDirections(curLoc, target),
                            (d) -> curLoc.add(d).distanceSquaredTo(weakestAlly.getLocation()))
                    ) {
                        if (rc.canMove(d)) {
                            rc.move(d);
                            if (rc.canHeal(weakestAlly.getLocation())) {
                                rc.heal(weakestAlly.getLocation());
                            }
                            return;
                        } else {
                            if (rc.canFill(curLoc.add(d))) {
                                rc.fill(curLoc.add(d));
                            }
                        }
                    }
                    if (rc.canHeal(weakestAlly.getLocation())) {
                        rc.heal(weakestAlly.getLocation());
                    }
                }
            }


            Pathfinding.moveTowards(rc, curLoc, target, true);

            return;
        }

        // enemies nearby
        RobotInfo weakestEnemy = Utils.lowestHealth(enemyInfos);
        RobotInfo closestEnemy = enemyInfos[0];
        MapLocation bestEnemyLoc = weakestEnemy.getLocation();


        if (allyInfos.length >= enemyInfos.length - 6) {  // more allies than enemies, attack
            int distToClosestEnemy = curLoc.distanceSquaredTo(weakestEnemy.getLocation());

            if (rc.canAttack(bestEnemyLoc)) {
                rc.setIndicatorDot(bestEnemyLoc, 9, 9, 255);
                rc.attack(bestEnemyLoc);
            }

            int b4 = Clock.getBytecodeNum();
            microAttacker.doMicro(suicide, curLoc.distanceSquaredTo(target), curLoc.distanceSquaredTo(Utils.getClosest(rc.getAllySpawnLocations(), curLoc)));
            int af = Clock.getBytecodeNum();

            MapLocation newClosestEnemyLoc = Utils.getClosest(enemyInfos, rc.getLocation()).location;

            allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
            enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());


            if (rc.canAttack(newClosestEnemyLoc)) {
                rc.setIndicatorDot(newClosestEnemyLoc, 9, 9, 255);
                rc.attack(newClosestEnemyLoc);
            }

            // spam traps if we still have some action cooldown remaining
            if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2 && rc.isActionReady()) {
//                    || (curLoc.distanceSquaredTo(target) <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)
//                    || (distToClosestSpawn <= 49 && rc.getCrumbs() > 1000 - rc.getRoundNum() / 2)) {
                for (Direction direction : Direction.allDirections()) {
                    MapLocation newLoc = curLoc.add(direction);
                    if ((newLoc.x - newLoc.y * 2) % 3 == 1) {
                        if (rc.canBuild(TrapType.STUN, curLoc.add(direction))) {
                            rc.build(TrapType.STUN, curLoc.add(direction));
                            break;
                        }
                    }
                }
            }

            // still can perform action, so try to heal others
            if (rc.isActionReady()) {
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
//        rc.setIndicatorString("spamming traps");
        if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
            for (Direction direction : Direction.allDirections()) {
                MapLocation newLoc = curLoc.add(direction);
                if ((newLoc.x + newLoc.y) % 2 == 0) {
                    if (rc.canBuild(TrapType.STUN, curLoc.add(direction))) {
                        rc.build(TrapType.STUN, curLoc.add(direction));
                        break;
                    }
                }
            }
        }

        if (bestEnemyLoc.distanceSquaredTo(curLoc) >= 16) {  // can safely flee
            MapLocation finalPathfindGoalLoc = target;
            for (Direction d : Utils.sort(Utils.getIdealMovementDirections(bestEnemyLoc, curLoc), (dir) -> curLoc.add(dir).distanceSquaredTo(finalPathfindGoalLoc))) {
                if (rc.canMove(d)) {
                    rc.move(d);
//                    rc.setIndicatorString("fleeing");
                    return;
                } else {
                    if (rc.canFill(curLoc.add(d))) {
                        rc.fill(curLoc.add(d));
                    }
                }
            }
        }

        // die a hero
        if (rc.canAttack(bestEnemyLoc)) {
            rc.setIndicatorDot(bestEnemyLoc, 9, 9, 255);
            rc.attack(bestEnemyLoc);
        }
        for (Direction d : Utils.getIdealMovementDirections(bestEnemyLoc, curLoc)) {
//            System.out.println("Im moving stupidly");
            if (rc.canMove(d)) {
                rc.move(d);
                break;
            }
        }
    }

    @Override
    public void setupTick(RobotController rc, MapLocation curLoc) throws GameActionException {
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();
        // stick to dam
        if (rc.getRoundNum() > 130) {
            for (MapLocation ml : Utils.getAdjacents(curLoc)) {
                if (!rc.onTheMap(ml))
                    continue;

                if (rc.senseMapInfo(ml).isDam()) {
//                    if ((curLoc.x + curLoc.y) % 2 == 0) {
//                        if (rc.canBuild(TrapType.STUN, curLoc)) {
//                            rc.build(TrapType.STUN, curLoc);
//                        }
//                    }

                    RobotInfo closestAlly = Utils.getClosest(rc.senseNearbyRobots(-1, rc.getTeam()), curLoc);
                    if (closestAlly == null || closestAlly.location.isAdjacentTo(curLoc))
                        return;
                    else if (closestAlly != null) {
                        for (Direction dir : Utils.getIdealMovementDirections(closestAlly.location, curLoc)) {
                            if (rc.canMove(dir)) {
                                rc.move(dir);
                                break;
                            }
                        }
                    }
                    return;
                }
            }
            MapLocation damLocation = null;
            for (MapInfo sensed : Utils.shuffleInPlace(rc.senseNearbyMapInfos(-1))) {
                if (sensed.isDam()) {
                    damLocation = sensed.getMapLocation();
                    break;
                }
            }
            if (damLocation != null) {
                for (Direction idealDir : Utils.getIdealMovementDirections(curLoc, damLocation)) {
                    if (rc.canMove(idealDir)) {
                        rc.move(idealDir);
                    }
                }
            }

            MapLocation centerLoc = new MapLocation(mapWidth / 2, mapHeight / 2);
//            for (Direction d : Utils.getIdealMovementDirections(curLoc, centerLoc)) {
//                if (rc.canMove(d)) {
//                    rc.move(d);
//                } else {
//                    MapLocation newLoc = curLoc.add(d);
//                    if (rc.canFill(newLoc)) {
//                        rc.fill(newLoc);
//                    }m
//                }
//            }
            Pathfinding.moveTowards(rc, curLoc, centerLoc, true);
        } else {
            Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(8)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }

    @Override
    public String name() {
        return "Attacker";
    }
}
