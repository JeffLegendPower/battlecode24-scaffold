package v11_3;

import battlecode.common.*;

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

    static int distToTarget;

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

    public boolean doMicro(int distToTarget, int distToSpawn, MapLocation robotTarget) {
        try {
            if (!rc.isMovementReady()) return false;

            // TODO: TUNE THIS!!!!!

//            shouldPlaySafe = distToTarget > 80 && rc.getRoundNum() > 250 && rc.getRoundNum() % 4 < 2;

            int DPSDist = 4;
            if (distToTarget < 80) {
                DPSDist = 2;
            }
            if (distToSpawn < 50) {
                DPSDist = 2;
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

            Direction[] noGo = Utils.getIdealMovementDirections(rc.getLocation(), robotTarget);

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                microInfo[i] = new MicroInfo(Direction.values()[i]);
            }

            int totalEnemyDPS = 0;
            for (RobotInfo unit : enemies) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                currentDPS = DPS[unit.attackLevel];
                if (currentDPS <= 0) continue;
                totalEnemyDPS += currentDPS;
                currentActionRadius = 2;
                if (microInfo[0] != null) microInfo[0].updateEnemy(unit, DPSDist);
                if (microInfo[1] != null) microInfo[1].updateEnemy(unit, DPSDist);
                if (microInfo[2] != null) microInfo[2].updateEnemy(unit, DPSDist);
                if (microInfo[3] != null) microInfo[3].updateEnemy(unit, DPSDist);
                if (microInfo[4] != null) microInfo[4].updateEnemy(unit, DPSDist);
                if (microInfo[5] != null) microInfo[5].updateEnemy(unit, DPSDist);
                if (microInfo[6] != null) microInfo[6].updateEnemy(unit, DPSDist);
                if (microInfo[7] != null) microInfo[7].updateEnemy(unit, DPSDist);
                if (microInfo[8] != null) microInfo[8].updateEnemy(unit, DPSDist);
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
                if (microInfo[0] != null) microInfo[0].updateAlly(unit);
                if (microInfo[1] != null) microInfo[1].updateAlly(unit);
                if (microInfo[2] != null) microInfo[2].updateAlly(unit);
                if (microInfo[3] != null) microInfo[3].updateAlly(unit);
                if (microInfo[4] != null) microInfo[4].updateAlly(unit);
                if (microInfo[5] != null) microInfo[5].updateAlly(unit);
                if (microInfo[6] != null) microInfo[6].updateAlly(unit);
                if (microInfo[7] != null) microInfo[7].updateAlly(unit);
                if (microInfo[8] != null) microInfo[8].updateAlly(unit);
            }


            if (allies.length == 0)
                totalAllyHealth = 0;
            else
                totalAllyHealth /= allies.length;

            lastTotalHealth = totalAllyHealth;
            lastCurrentDPS = totalEnemyDPS;

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i] == null) continue;
                if (bestMicro == null) {
                    bestMicro = microInfo[i];
                    continue;
                }
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
        MapLocation target = null;

        int minDistanceToEnemy = INF;
        int minDistanceToAlly = INF;
        double DPSreceived = 0;
        double enemiesTargeting = 0;
        double alliesTargeting = 0;
        boolean canMove = true;
        int distToEnemyFlagHolder = INF;
        int distToNearestAllySpawn = INF;
        int weakest = INF;

        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);

            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;

            for (MapLocation spawn : RobotPlayer.allyFlagSpawnLocs) {
                int dist = spawn.distanceSquaredTo(location);
                if (dist < distToNearestAllySpawn) distToNearestAllySpawn = dist;
            }


        }

        void updateEnemy(RobotInfo unit, int DPSDist) {
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (unit.hasFlag)
                distToEnemyFlagHolder = dist;
            if (dist < minDistanceToEnemy) {
                minDistanceToEnemy = dist;
//                target = unit.getLocation();
            }
            if (unit.getHealth() < weakest) {
                target = unit.getLocation();
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
            if (!canMove) return -1;
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

//            if (minDistanceToEnemy <= 2 && M.minDistanceToEnemy > 2) return false;
//            if (minDistanceToEnemy > 2 && M.minDistanceToEnemy <= 2) return true;

            if (distToEnemyFlagHolder < M.distToEnemyFlagHolder) return true;
            if (M.distToEnemyFlagHolder < distToEnemyFlagHolder) return false;

            if (safe() > M.safe()) return true;
            if (safe() < M.safe()) return false;

            if (inRange() && !M.inRange()) return true;
            if (!inRange() && M.inRange()) return false;

            if (severelyHurt) {
                if (alliesTargeting > M.alliesTargeting) return true;
                if (alliesTargeting < M.alliesTargeting) return false;
            }

            if (inRange() && distToNearestAllySpawn > 25) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;
        }
    }

}