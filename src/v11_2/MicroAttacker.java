package v11_2;

import battlecode.common.*;

import java.util.Random;

public class MicroAttacker {


    static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    final int INF = 1000000;
    boolean shouldPlaySafe = true;
    boolean alwaysInRange = false;
    static int myRange;
    static int myVisionRange;
    static double myDPS;
    boolean severelyHurt = false;
    boolean enemiesTriggeredTrap = false;
    int timeSinceLastTrigger = 0;
    int lastTotalHealth = 9999;
    int lastCurrentDPS = 9999;

    double[] DPS = new double[]{0, 0, 0, 0, 0, 0, 0};
    RobotController rc;
    static double currentDPS = 0;
    static double currentRangeExtended = 20; // vision radius
    static double currentActionRadius;
    static boolean canAttack;
    static int distToSpawn;

    static int distToTarget;
    static int aggressionFactor;

    public MicroAttacker(RobotController rc) {
        myRange = 2;
        myVisionRange = 20;

//        DPS[RobotType.SOLDIER.ordinal()] = 12;
//        DPS[RobotType.SAGE.ordinal()] = 9;
        DPS[0] = 150;
        DPS[1] = 158;
        DPS[2] = 161;
        DPS[3] = 165;
        DPS[4] = 195;
        DPS[5] = 240;

//        rangeExtended[RobotType.SOLDIER.ordinal()] = 20;
//        rangeExtended[RobotType.SAGE.ordinal()] = 34;
//        myDPS = DPS[rc.getType().ordinal()];
        this.rc = rc;
        myDPS = rc.getAttackDamage();
    }

    public boolean doMicro(MapLocation spawn, MapLocation target) {
        try {
            if (!rc.isMovementReady()) return false;

            // TODO: TUNE THIS!!!!!
            distToSpawn = rc.getLocation().distanceSquaredTo(spawn);
            distToTarget = rc.getLocation().distanceSquaredTo(target);
//            shouldPlaySafe = distToTarget > 80 && rc.getRoundNum() > 250 && rc.getRoundNum() % 4 < 2;

            int DPSDist = 4;
            if (distToTarget < 64) {
                DPSDist = 2;
            }
            if (distToSpawn < 25) {
                DPSDist = 2;
            }

            if (distToSpawn <= 16) {
                aggressionFactor = 0;
            } else if (distToTarget <= 49) {
                aggressionFactor = 2;
            } else {
                aggressionFactor = 1;
            }

            severelyHurt = rc.getHealth() < 300;
            RobotInfo[] enemies = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (enemies.length == 0) return false;
            canAttack = rc.isActionReady();

            timeSinceLastTrigger++;
            if (timeSinceLastTrigger > 4)
                enemiesTriggeredTrap = false;

            for (Direction dir : RobotPlayer.directions) {
                MapLocation newLoc = rc.getLocation().add(dir);
                if (!rc.onTheMap(newLoc)) continue;
                MapInfo info = RobotPlayer.map[newLoc.x][newLoc.y];
                MapInfo lastInfo = RobotPlayer.lastMap[newLoc.x][newLoc.y];
                if (info == null || lastInfo == null) continue;

                if (lastInfo.getTrapType() == TrapType.STUN && info.getTrapType() == TrapType.NONE) {
                    enemiesTriggeredTrap = true;
                    timeSinceLastTrigger = 0;
                    break;
                }
            }

            alwaysInRange = false;
            if (!rc.isActionReady()) alwaysInRange = true;
            if (severelyHurt) alwaysInRange = true;

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                microInfo[i] = new MicroInfo(Direction.values()[i], spawn, target);
            }

            int totalEnemyDPS = 0;
            for (RobotInfo unit : enemies) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                currentDPS = DPS[unit.attackLevel];
                if (currentDPS <= 0) continue;
                totalEnemyDPS += currentDPS;
                currentActionRadius = 2;
                microInfo[0].updateEnemy(unit, DPSDist);
                microInfo[1].updateEnemy(unit, DPSDist);
                microInfo[2].updateEnemy(unit, DPSDist);
                microInfo[3].updateEnemy(unit, DPSDist);
                microInfo[4].updateEnemy(unit, DPSDist);
                microInfo[5].updateEnemy(unit, DPSDist);
                microInfo[6].updateEnemy(unit, DPSDist);
                microInfo[7].updateEnemy(unit, DPSDist);
                microInfo[8].updateEnemy(unit, DPSDist);
            }

            if (enemies.length == 0)
                totalEnemyDPS = 1;
            else
                totalEnemyDPS /= enemies.length;

            int totalAllyHealth = 0;
            RobotInfo[] allies = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
            for (RobotInfo unit : allies) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                currentDPS = DPS[unit.attackLevel];
                totalAllyHealth += unit.getHealth();
                if (currentDPS <= 0) continue;
                microInfo[0].updateAlly(unit);
                microInfo[1].updateAlly(unit);
                microInfo[2].updateAlly(unit);
                microInfo[3].updateAlly(unit);
                microInfo[4].updateAlly(unit);
                microInfo[5].updateAlly(unit);
                microInfo[6].updateAlly(unit);
                microInfo[7].updateAlly(unit);
                microInfo[8].updateAlly(unit);
            }


            if (allies.length == 0)
                totalAllyHealth = 0;
            else
                totalAllyHealth /= allies.length;

            lastTotalHealth = totalAllyHealth;
            lastCurrentDPS = totalEnemyDPS;

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            if (bestMicro.dir == Direction.CENTER) return true;

            if (rc.canFill(bestMicro.location))
                rc.fill(bestMicro.location);
            if (rc.canMove(bestMicro.dir)) {
                rc.move(bestMicro.dir);
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    class MicroInfo{
        Direction dir;
        MapLocation location;

        int minDistanceToEnemy = INF;
        int minDistanceToAlly = INF;
        double DPSreceived = 0;
        double enemiesTargeting = 0;
        double alliesTargeting = 0;
        boolean canMove = true;
        int distToEnemyFlagHolder = INF;
        int distToSpawn;
        int distToTarget;

        public MicroInfo(Direction d, MapLocation spawn, MapLocation target) throws GameActionException {
            dir = d;
            location = rc.getLocation().add(d);
            distToSpawn = location.distanceSquaredTo(spawn);
            distToTarget = location.distanceSquaredTo(target);

            if (d != Direction.CENTER && !rc.canMove(d)) canMove = false;

            for (MapLocation ml : RobotPlayer.allyFlagSpawnLocs) {
                int dist = ml.distanceSquaredTo(location);
                if (dist < distToSpawn) distToSpawn = dist;
            }

        }

        void updateEnemy(RobotInfo unit, int DPSDist) {
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (unit.hasFlag)
                distToEnemyFlagHolder = dist;
            if (dist < minDistanceToEnemy) {
                minDistanceToEnemy = dist;
            }

            if (dist <= DPSDist) DPSreceived += DPS[unit.attackLevel];
            if (dist <= currentRangeExtended) enemiesTargeting += DPS[unit.attackLevel];
        }

        void updateAlly(RobotInfo unit) {
            if (!canMove) return;
            alliesTargeting += DPS[unit.attackLevel] * 4;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToAlly) {
                minDistanceToAlly = dist;
            }
            // TODO test if this actually gains 1 by 1
            if (unit.location.distanceSquaredTo(location) <= currentActionRadius)
                DPSreceived -= DPS[unit.healLevel];
        }


        int safe() {
            if (minDistanceToEnemy <= 2 && shouldPlaySafe) return -2;
            if (!canMove) return -1;
            if (!canAttack) return -1;
            if (DPSreceived > 0 && severelyHurt) return 0; // TODO test if adding severelyHurt actually gains 1 by 1
            if (enemiesTargeting < alliesTargeting) return 2; // TODO test if adding shouldPlaySafe actually gains 1 by 1
            return 3;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo M) {

            if (distToEnemyFlagHolder < M.distToEnemyFlagHolder) return true;
            if (M.distToEnemyFlagHolder < distToEnemyFlagHolder) return false;
//            System.out.println("a");

////            //TODO: do these gain?
            if (canAttack && inRange() && !M.inRange()) return false;
            if (canAttack && !inRange() && M.inRange()) return true;

            if (aggressionFactor == 2 && distToTarget < M.distToTarget) return true;
            if (aggressionFactor == 2 && distToTarget > M.distToTarget) return false;

            if (aggressionFactor == 0 && distToSpawn > M.distToSpawn) return true;
            if (aggressionFactor == 0 && distToSpawn < M.distToSpawn) return false;

            if (aggressionFactor == 0 && safe() > M.safe()) return true;
            if (aggressionFactor == 0 && safe() < M.safe()) return false;

            if (aggressionFactor == 0 && inRange() && !M.inRange()) return true;
            if (aggressionFactor == 0 && !inRange() && M.inRange()) return false;


            if (aggressionFactor != 2 && severelyHurt) {
                if (alliesTargeting > M.alliesTargeting) return true;
                if (alliesTargeting < M.alliesTargeting) return false;
            }

            if (inRange() && distToSpawn > 25) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;
        }
    }

}