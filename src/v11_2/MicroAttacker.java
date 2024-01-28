package v11_2;

import battlecode.common.*;

public class MicroAttacker {

    static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    final int INF = 1000000;
    boolean shouldPlaySafe = true;
    boolean alwaysInRange = false;
    boolean hurt = false;
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

    static double currentDPS = 0;
    static double currentRangeExtended = 20; // vision radius
    static double currentActionRadius;
    static boolean canAttack;

    static boolean gainTerritoryMode;

    final static int MAX_RUBBLE_DIFF = 5;

    public boolean doMicro(boolean suicide) {
        try {
            if (!rc.isMovementReady()) return false;

            severelyHurt = rc.getHealth() < 300;
            RobotInfo[] enemies = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (enemies.length == 0) return false;
            canAttack = rc.isActionReady();
            //shouldPlaySafe = !canAttack;

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
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(Direction.values()[i]);

//            boolean danger = rc.senseMapInfo(rc.getLocation()).getTeamTerritory() != rc.getTeam();

            int totalEnemyDPS = 0;
            for (RobotInfo unit : enemies) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
//                int t = unit.getType().ordinal();
                currentDPS = DPS[unit.attackLevel];
                if (currentDPS <= 0) continue;
                totalEnemyDPS += currentDPS;
//                if (danger && Robot.comm.isEnemyTerritory(unit.getLocation())) currentDPS*=1.5;
//                currentRangeExtended = rangeExtended[t];
                currentActionRadius = 2;
                microInfo[0].updateEnemy(unit);
                microInfo[1].updateEnemy(unit);
                microInfo[2].updateEnemy(unit);
                microInfo[3].updateEnemy(unit);
                microInfo[4].updateEnemy(unit);
                microInfo[5].updateEnemy(unit);
                microInfo[6].updateEnemy(unit);
                microInfo[7].updateEnemy(unit);
                microInfo[8].updateEnemy(unit);
            }

            if (enemies.length == 0)
                totalEnemyDPS = 1;
            else
                totalEnemyDPS /= enemies.length;

            int totalAllyHealth = 0;
            RobotInfo[] allies = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
            for (RobotInfo unit : allies) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
//                currentDPS = DPS[unit.getType().ordinal()] / (10 + rc.senseRubble(unit.getLocation()));
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

            float currentDPSMultiplier;
            currentDPSMultiplier = (float) (lastTotalHealth - totalAllyHealth) / lastCurrentDPS;
            if (currentDPSMultiplier < 0)
                currentDPSMultiplier = 1;
//            System.out.println(currentDPSMultiplier + " " + rc.getID());
            microInfo[0].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[1].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[2].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[3].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[4].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[5].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[6].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[7].updateDPSMultiplier(currentDPSMultiplier);
            microInfo[8].updateDPSMultiplier(currentDPSMultiplier);

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
        int distToNearestAllySpawn = INF;

        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);

            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;

            for (MapLocation spawn : RobotPlayer.allyFlagSpawnLocs) {
                int dist = spawn.distanceSquaredTo(location);
                if (dist < distToNearestAllySpawn) distToNearestAllySpawn = dist;
            }


        }

        void updateEnemy(RobotInfo unit) {
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (unit.hasFlag)
                distToEnemyFlagHolder = dist;
            if (dist < minDistanceToEnemy) {
                minDistanceToEnemy = dist;
            }

            if (dist <= currentActionRadius) DPSreceived += DPS[unit.attackLevel];
            if (dist <= currentRangeExtended) enemiesTargeting += DPS[unit.attackLevel];
        }

        void updateAlly(RobotInfo unit) {
            if (!canMove) return;
            alliesTargeting += DPS[unit.attackLevel];
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToAlly) {
                minDistanceToAlly = dist;
            }
            // TODO test if this actually gains 1 by 1
            if (unit.location.distanceSquaredTo(location) <= currentActionRadius)
                DPSreceived -= DPS[unit.healLevel];
        }

        void updateDPSMultiplier(float multiplier) {
            DPSreceived *= multiplier;
            alliesTargeting *= 2;
        }

        int safe() {
            if (!canMove) return -1;

            if (DPSreceived > 0 && severelyHurt) return 0; // TODO test if adding severelyHurt actually gains 1 by 1
            if (enemiesTargeting < alliesTargeting && shouldPlaySafe) return 1; // TODO test if adding shouldPlaySafe actually gains 1 by 1
            if (enemiesTargeting > alliesTargeting && (!shouldPlaySafe)) return 1; // TODO i think this is never called?
            return 2;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo M) {
//            int mygood = 0;
//            int theirgood = 0;
//
//            if (distToEnemyFlagHolder < M.distToEnemyFlagHolder) mygood += 3;
//            if (distToEnemyFlagHolder > M.distToEnemyFlagHolder) theirgood += 3;
////            System.out.println("a");
//
//            if (safe() > M.safe()) mygood += 2;
//            if (safe() < M.safe()) theirgood += 2;
//
//            if (inRange() && !M.inRange()) mygood += 2;
//            if (!inRange() && M.inRange()) theirgood += 2;
//
//            if (alliesTargeting > M.alliesTargeting + 6) mygood += 1; // TODO test if this actually gains 1 by 1
//            if (alliesTargeting < M.alliesTargeting - 6) theirgood += 1; // TODO test if this actually gains 1 by 1
//            if (severelyHurt) {
//                if (alliesTargeting > M.alliesTargeting) mygood += 1;
//                if (alliesTargeting < M.alliesTargeting) theirgood += 1;
//            }
//
//            if (inRange() && distToNearestAllySpawn > 25) {
//                if (minDistanceToEnemy >= M.minDistanceToEnemy)
//                    mygood += 1;
//                else
//                    theirgood += 1;
//            } else {
//                if (minDistanceToEnemy <= M.minDistanceToEnemy)
//                    mygood += 1;
//                else
//                    theirgood += 1;
//            }
//            return mygood > theirgood;
            if (distToEnemyFlagHolder < M.distToEnemyFlagHolder) return true;
            if (M.distToEnemyFlagHolder < distToEnemyFlagHolder) return false;
//            System.out.println("a");

            if (safe() > M.safe()) return true;
            if (safe() < M.safe()) return false;

            if (inRange() && !M.inRange()) return true;
            if (!inRange() && M.inRange()) return false;

            if (alliesTargeting > M.alliesTargeting + 6) return true; // TODO test if this actually gains 1 by 1
            if (alliesTargeting < M.alliesTargeting - 6) return false; // TODO test if this actually gains 1 by 1
            if (severelyHurt) {
                if (alliesTargeting > M.alliesTargeting) return true;
                if (alliesTargeting < M.alliesTargeting) return false;
            }

            if (inRange() && distToNearestAllySpawn > 25) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;
        }
    }

}