package v11.robots;

import battlecode.common.*;
import v11.*;

import java.util.Random;

import static v10_2.Evaluators.staticLocEval;

public class Attacker extends AbstractRobot {

    private Direction continueInThisDirection = null;

    // If a deviant, go to a different flag than the one being targeted by everyone else
    private boolean deviant = false;

    private MapLocation lastTarget = null;

    @Override
    public boolean setup(RobotController rc) throws GameActionException {
        if (RobotPlayer.rng.nextInt(10) == 0)
            deviant = true;
        spawn(rc);
        return true;
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        MapLocation flagTarget = null;
        if (deviant) {
            for (int i = 2; i >= 0; i--) {
                MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flagLoc != null) {
                    flagTarget = flagLoc;
                    break;
                }
            }
        } else {
            int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
            int[] centerLocationWeights = new int[3];
            int total = 0;
            for (int i=0; i<3; i++) {
                centerLocationWeights[i] = v & 0b11111;
                total += v & 0b11111;
                v >>= 5;
            }

            Integer[] centerSpawnLocationWeightsIndicies = Utils.sort(new Integer[] {0, 1, 2}, (i) -> -centerLocationWeights[i]);
            for (int i=0; i<3; i++) {
                if (centerLocationWeights[centerSpawnLocationWeightsIndicies[i]] < total / 2) {
                    if (RobotPlayer.rng.nextInt(3) == 1) {
                        continue;
                    }
                }
                MapLocation centerSpawnLocation = RobotPlayer.allyFlagSpawnLocs[centerSpawnLocationWeightsIndicies[i]];
                for (MapLocation adjacent : Utils.getAdjacents(centerSpawnLocation)) {
                    if (rc.canSpawn(adjacent)) {
                        rc.spawn(adjacent);
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

                if (bestLoc != null) rc.spawn(bestLoc);
            } else {
                for (int i = 0; i < 3; i++) {
                    MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                    if (flagLoc != null) {
                        flagTarget = flagLoc;
                        break;
                    }
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
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
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
        int bestDist = deviant ? -999999 : 999999;
        if (deviant) {
            for (int i = 2; i >= 0; i--) {
                MapLocation flagLoc = Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]);
                if (flagLoc != null) {
                    int dist = flagLoc.distanceSquaredTo(curLoc);
                    if (dist > bestDist) {
                        target = flagLoc;
                        bestDist = dist;
                    }
                }
            }
        } else {
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
        }

        lastTarget = target;

        // TODO if theres no enemy flags then we stop moving i think or ally flags idk
        if (target == null) target = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

        RobotInfo[] allyInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemyInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Utils.sort(enemyInfos, (enemy) -> enemy.getLocation().distanceSquaredTo(curLoc));
        Utils.sort(allyInfos, (ally) -> (ally.getLocation().distanceSquaredTo(curLoc) - rc.getHealth() / 100 - (rc.hasFlag() ? 10000 : 0)));

        int numTraps = 0;
        for (Direction dir : RobotPlayer.directions) {
            MapLocation newLoc = curLoc.add(dir);
            if (rc.onTheMap(newLoc)) {
                if (RobotPlayer.map[newLoc.x][newLoc.y].getTrapType() != TrapType.NONE) {
                    numTraps++;
                }
            }
        }

        // no enemies close enough to attack even if they run in
        if (enemyInfos.length == 0 || enemyInfos[0].getLocation().distanceSquaredTo(curLoc) >= 16) {
            // no enemies nearby-ish -> spam traps when no enemies in 2-step range
            if (enemyInfos.length > 0) {
                if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2) {
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
                RobotInfo closestAlly = allyInfos[0];
                if (closestAlly.getHealth() <= 1000 - rc.getHealAmount()) {
                    for (Direction d : Utils.sort(
                            Utils.getIdealMovementDirections(curLoc, target),
                            (d) -> curLoc.add(d).distanceSquaredTo(closestAlly.getLocation()))
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
                            if (rc.canFill(curLoc.add(d))) {
                                rc.fill(curLoc.add(d));
                            }
                        }
                    }
                    if (rc.canHeal(closestAlly.getLocation())) {
                        rc.heal(closestAlly.getLocation());
                    }
                }
            }

            // no enemies nearby-ish -> no allies nearby
            // Stronger pathfinding to prevent getting stuck
            Pathfinding.moveTowards(rc, curLoc, target, true);
            return;
        }

        // enemies nearby
        RobotInfo closestEnemy = enemyInfos[0];
        MapLocation closestEnemyLoc = closestEnemy.getLocation();
//        for (RobotInfo enemy : enemyInfos) {
//            if (enemy.health < rc.getAttackDamage() && enemy.location.distanceSquaredTo(curLoc) <= 4) {
//                System.out.println("can 1tap " + rc.getID() + " " + enemy.getID() + " " + rc.getAttackDamage() + " " + enemy.health);
//                closestEnemyLoc = enemy.location;
//                break;
//            }
//        }

        if (allyInfos.length >= enemyInfos.length - 3) {  // more allies than enemies, attack
            int distToClosestEnemy = curLoc.distanceSquaredTo(closestEnemy.getLocation());

//            // 1+ steps away, so move then attack
//            if (distToClosestEnemy > 4) {
//                for (Direction d : Utils.getIdealMovementDirections(curLoc, closestEnemyLoc)) {
//                    if (rc.canMove(d)) {
//                        rc.move(d);
//                        if (rc.canAttack(closestEnemyLoc)) {
//                            rc.attack(closestEnemyLoc);
//                        }
//                        return;
//                    } else {
//                        if (rc.canFill(curLoc.add(d))) {
//                            rc.fill(curLoc.add(d));
//                            return;
//                        }
//                    }
//                }
//            }
//
//            // less than one step away, attack then move back
//            // However, if there's way more allies than enemies and your health is high enough, just rush in
//            if (distToClosestEnemy <= 4) {
//                if (rc.canAttack(closestEnemyLoc)) {
//                    rc.attack(closestEnemyLoc);
//                    // TODO added rush in if health high enough just test it out
////                    Direction[] movementDirections = allyInfos.length > enemyInfos.length * 2 && rc.getHealth() > 700 ?
////                            Utils.getIdealMovementDirections(curLoc, closestEnemyLoc) :
////                            Utils.getIdealMovementDirections(closestEnemyLoc, curLoc);
//                    Direction[] movementDirections = Utils.getIdealMovementDirections(closestEnemyLoc, curLoc);
//
//                    for (Direction d : movementDirections) {
//                        if (rc.canMove(d)) {
//                            rc.move(d);
//                            break;
//                        }
//                    }
//                }
//            }

            MapLocation bestTarget = Evals.Action.getBestTarget(rc);
            if (bestTarget != null && rc.canAttack(bestTarget))
                rc.attack(bestTarget);

            int maxScore = -9999999;
            Direction bestDir = null;

            for (Direction direction : rc.getHealth() <= 300 ? RobotPlayer.directions :
                    distToClosestEnemy > 4 ? Utils.getIdealMovementDirections(curLoc, closestEnemyLoc)
                    : Utils.getIdealMovementDirections(closestEnemyLoc, curLoc)) {
                if (!rc.canMove(direction))
                    continue;
                MapLocation loc = curLoc.add(direction);
                int eval = staticLocEval(rc, enemyInfos, allyInfos, loc);
                if (eval > maxScore) {
                    maxScore = eval;
                    bestDir = direction;
                }
            }
            if (bestDir != null)
                rc.move(bestDir);

            bestTarget = Evals.Action.getBestTarget(rc);
            if (bestTarget != null && rc.canAttack(bestTarget))
                rc.attack(bestTarget);

            // If an ally really needs healing, heal them
            if (rc.isActionReady() && numTraps >= 6) {
                for (RobotInfo ally : allyInfos) {
                    if (ally.health <= 700 && rc.canHeal(ally.getLocation())) {
                        rc.heal(ally.getLocation());
                    }
                }
            }

            // spam traps if we still have some action cooldown remaining
            if (rc.getCrumbs() > 1500 - rc.getRoundNum() / 2 && rc.isActionReady()) {
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
        rc.setIndicatorString("spamming traps");
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
        if (closestEnemyLoc.distanceSquaredTo(curLoc) >= 16) {  // can safely flee
            MapLocation finalPathfindGoalLoc = target;
            for (Direction d : Utils.sort(Utils.getIdealMovementDirections(closestEnemyLoc, curLoc), (dir) -> curLoc.add(dir).distanceSquaredTo(finalPathfindGoalLoc))) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    rc.setIndicatorString("fleeing");
                    return;
                } else {
                    if (rc.canFill(curLoc.add(d))) {
                        rc.fill(curLoc.add(d));
                    }
                }
            }
        }

        // die a hero
        if (rc.canAttack(closestEnemyLoc)) {
            rc.attack(closestEnemyLoc);
        }
        for (Direction d : Utils.getIdealMovementDirections(closestEnemyLoc, curLoc)) {
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
                    if ((curLoc.x + curLoc.y) % 2 == 0) {
                        if (rc.canBuild(TrapType.STUN, curLoc)) {
                            rc.build(TrapType.STUN, curLoc);
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
            for (Direction d : Utils.getIdealMovementDirections(curLoc, centerLoc)) {
                if (rc.canMove(d)) {
                    rc.move(d);
                } else {
                    MapLocation newLoc = curLoc.add(d);
                    if (rc.canFill(newLoc)) {
                        rc.fill(newLoc);
                    }
                }
            }
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
