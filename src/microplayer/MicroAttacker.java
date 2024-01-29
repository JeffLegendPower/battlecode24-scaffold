package microplayer;

import battlecode.common.*;

import static microplayer.General.*;

public class MicroAttacker {

    public static MapLocation pathfindGoalLocForMicroAttacker;

    static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    static int myRange = 2;
    static int myVisionRange = 20;
    static double myDPS;
    static double injuryAmplifier = 1;
    boolean severelyHurt = false;
    boolean enemiesTriggeredTrap = false;
    int timeSinceLastTrigger = 0;

//  double[] DPS = new double[]{150, 158, 161, 165, 195, 240, 0};
    double[] DPS = new double[]{150, 162, 169, 178, 220, 260, 365};  // modified slightly to account for cooldown

    public MicroAttacker() {
        myDPS = rc.getAttackDamage();
        injuryAmplifier = (850.0/(300+rc.getHealth()) - 0.8)/2.0 + 0.87;
    }

    static double currentDPS = 0;
    static double currentRangeExtended = 20;
    static double currentActionRadius;
    static boolean canAttack;

    public void doMicro() throws GameActionException {
        if (!rc.isMovementReady()) return;
        severelyHurt = rc.getHealth() < 500-(60*rc.getLevel(SkillType.HEAL));
        RobotInfo[] enemies = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
        if (enemies.length == 0) return;
        canAttack = rc.isActionReady();

        timeSinceLastTrigger++;
        if (timeSinceLastTrigger > 4) {
            enemiesTriggeredTrap = false;
        }
        alwaysInRange = !rc.isActionReady();
        if (severelyHurt) {
            alwaysInRange = true;
        }

        MicroInfo[] microInfo = new MicroInfo[8];
        if (!rc.isSpawned()) {
            return;
        }
        microInfo[0] = new MicroInfo(Direction.NORTH);
        microInfo[1] = new MicroInfo(Direction.NORTHEAST);
        microInfo[2] = new MicroInfo(Direction.EAST);
        microInfo[3] = new MicroInfo(Direction.SOUTHEAST);
        microInfo[4] = new MicroInfo(Direction.SOUTH);
        microInfo[5] = new MicroInfo(Direction.SOUTHWEST);
        microInfo[6] = new MicroInfo(Direction.WEST);
        microInfo[7] = new MicroInfo(Direction.NORTHWEST);

        for (RobotInfo unit : enemies) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) {
                break;
            }
            currentDPS = DPS[unit.attackLevel];
            if (currentDPS <= 0) {
                continue;
            }
            currentActionRadius = 2;
            microInfo[0].updateEnemy(unit);
            microInfo[1].updateEnemy(unit);
            microInfo[2].updateEnemy(unit);
            microInfo[3].updateEnemy(unit);
            microInfo[4].updateEnemy(unit);
            microInfo[5].updateEnemy(unit);
            microInfo[6].updateEnemy(unit);
            microInfo[7].updateEnemy(unit);
        }

        enemies = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
        for (RobotInfo unit : enemies) {
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) {
                break;
            }
            currentDPS = DPS[unit.attackLevel];
            if (currentDPS <= 0) {
                continue;
            }
            microInfo[0].updateAlly(unit);
            microInfo[1].updateAlly(unit);
            microInfo[2].updateAlly(unit);
            microInfo[3].updateAlly(unit);
            microInfo[4].updateAlly(unit);
            microInfo[5].updateAlly(unit);
            microInfo[6].updateAlly(unit);
            microInfo[7].updateAlly(unit);
        }

        MicroInfo bestMicro = microInfo[0];
        MicroInfo secondBestMicro = null;
        for (int i = 1; i < 8; ++i) {
            if (microInfo[i].isBetterThan(bestMicro)) {
                secondBestMicro = bestMicro;
                bestMicro = microInfo[i];
            }
        }

        if (bestMicro.dir == Direction.CENTER) {
            return;
        }
        if (rc.canFill(bestMicro.location)) {
            if ((bestMicro.location.x + bestMicro.location.y) % 2 == 0) {
                rc.fill(bestMicro.location);
            }
        } else if (rc.canMove(bestMicro.dir)) {
            rc.move(bestMicro.dir);
        } else if (secondBestMicro != null && rc.canMove(secondBestMicro.dir)) {
            rc.move(secondBestMicro.dir);
        }
    }

    class MicroInfo {
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;
        double DPSreceived = 0;
        double enemiesTargeting = 0;
        double alliesTargeting = 0;
        boolean canMove = true;
        int distToEnemyFlagHolder = INF;
        int distToNearestAllySpawn = INF;
        int distToPathfindGoalLoc;

        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;

            for (MapLocation spawn : orderedCenterSpawnLocations) {
                int dist = spawn.distanceSquaredTo(location);
                if (dist < distToNearestAllySpawn) {
                    distToNearestAllySpawn = dist;
                }
            }
            distToPathfindGoalLoc = location.distanceSquaredTo(pathfindGoalLocForMicroAttacker);
        }

        void updateEnemy(RobotInfo unit) {
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (unit.hasFlag)
                distToEnemyFlagHolder = dist;
            if (dist < minDistanceToEnemy) {
                minDistanceToEnemy = dist;
            }
            if (dist <= currentActionRadius) DPSreceived += DPS[unit.attackLevel] + (enemyGlobalUpgrades[0] ? 60 : 0) * injuryAmplifier;
            if (dist <= currentRangeExtended) enemiesTargeting += DPS[unit.attackLevel] + (enemyGlobalUpgrades[0] ? 60 : 0) * injuryAmplifier;
        }

        void updateAlly(RobotInfo unit) {
            if (!canMove) return;
            alliesTargeting += DPS[unit.attackLevel] + (allyGlobalUpgrades[0] ? 60 : 0);
            // TODO test if this actually gains 1 by 1
            if (unit.location.distanceSquaredTo(location) <= currentActionRadius) {
                DPSreceived -= DPS[unit.healLevel] + (allyGlobalUpgrades[1] ? 50 : 0);
            }
        }

        int safe() {
            if (!canMove) return -1;
            if (DPSreceived > 0 && severelyHurt) return 0; // TODO test if adding severelyHurt actually gains 1 by 1
            if (enemiesTargeting < alliesTargeting && shouldPlaySafe) return 1; // TODO test if adding shouldPlaySafe actually gains 1 by 1
            if (enemiesTargeting > alliesTargeting && (!shouldPlaySafe)) return 1; // TODO i think this is never called?
            return 2;
        }

        boolean inRange(){
            if (alwaysInRange) {
                return true;
            }
            return minDistanceToEnemy <= myRange;
        }

        // equal => true
        boolean isBetterThan(MicroInfo other) {
            if (distToEnemyFlagHolder < other.distToEnemyFlagHolder) return true;

            if (rng.nextInt(6) == 0) {
                if (pathfindGoalLocForMicroAttacker != null) {
                    if (distToPathfindGoalLoc > other.distToPathfindGoalLoc) {
                        return false;
                    }
                }
            }
            if (distToNearestAllySpawn < mapWidth+mapHeight) {
                if (distToNearestAllySpawn > other.distToNearestAllySpawn) {
                    rc.setIndicatorString("semi-protecting ally spawn");
                    return false;
                }
            }

            if (safe() > other.safe()) return true;
            if (safe() < other.safe()) return false;

            if (inRange() && !other.inRange()) return true;
            if (!inRange() && other.inRange()) return false;

            if (alliesTargeting > other.alliesTargeting + 2) return true; // TODO test if this actually gains 1 by 1
            if (alliesTargeting < other.alliesTargeting - 2) return false; // TODO test if this actually gains 1 by 1
            if (severelyHurt) {
                if (alliesTargeting > other.alliesTargeting) return true;
                if (alliesTargeting < other.alliesTargeting) return false;
            }

            if (inRange() && distToNearestAllySpawn > 25) return minDistanceToEnemy >= other.minDistanceToEnemy;
            else return minDistanceToEnemy <= other.minDistanceToEnemy;
        }
    }

}