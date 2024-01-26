package v11;

import battlecode.common.*;

public class MicroAttacker {

    static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    boolean hurt = false;
    static int myRange;
    static int myVisionRange;
    static double myDPS;
    boolean severelyHurt = false;
    boolean enemiesTriggeredTrap = false;
    int timeSinceLastTrigger = 0;

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

    final static int MAX_RUBBLE_DIFF = 5;

    public boolean doMicro() {
        try {
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = true;
            severelyHurt = rc.getHealth() < 300;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;
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

//            int uIndex = units.length;
//            while (uIndex-- > 0){
//                RobotInfo r = units[uIndex];
//                switch(r.getType()){
//                    case SOLDIER:
//                    case SAGE:
//                        shouldPlaySafe = true;
//                        break;
//                    default:
//                        break;
//                }
//            }
//            if (!shouldPlaySafe) return false;

            alwaysInRange = false;
            if (!rc.isActionReady()) alwaysInRange = true;
            if (severelyHurt) alwaysInRange = true;

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(Direction.values()[i]);

//            int minRubble = microInfo[8].rubble;
//            if (microInfo[7].canMove && minRubble > microInfo[7].rubble) minRubble = microInfo[7].rubble;
//            if (microInfo[6].canMove && minRubble > microInfo[6].rubble) minRubble = microInfo[6].rubble;
//            if (microInfo[5].canMove && minRubble > microInfo[5].rubble) minRubble = microInfo[5].rubble;
//            if (microInfo[4].canMove && minRubble > microInfo[4].rubble) minRubble = microInfo[4].rubble;
//            if (microInfo[3].canMove && minRubble > microInfo[3].rubble) minRubble = microInfo[3].rubble;
//            if (microInfo[2].canMove && minRubble > microInfo[2].rubble) minRubble = microInfo[2].rubble;
//            if (microInfo[1].canMove && minRubble > microInfo[1].rubble) minRubble = microInfo[1].rubble;
//            if (microInfo[0].canMove && minRubble > microInfo[0].rubble) minRubble = microInfo[0].rubble;

//            minRubble += MAX_RUBBLE_DIFF;

//            if (microInfo[8].rubble > minRubble) microInfo[8].canMove = false;
//            if (microInfo[7].rubble > minRubble) microInfo[7].canMove = false;
//            if (microInfo[6].rubble > minRubble) microInfo[6].canMove = false;
//            if (microInfo[5].rubble > minRubble) microInfo[5].canMove = false;
//            if (microInfo[4].rubble > minRubble) microInfo[4].canMove = false;
//            if (microInfo[3].rubble > minRubble) microInfo[3].canMove = false;
//            if (microInfo[2].rubble > minRubble) microInfo[2].canMove = false;
//            if (microInfo[1].rubble > minRubble) microInfo[1].canMove = false;
//            if (microInfo[0].rubble > minRubble) microInfo[0].canMove = false;

//            boolean danger = rc.senseMapInfo(rc.getLocation()).getTeamTerritory() != rc.getTeam();

            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
//                int t = unit.getType().ordinal();
                currentDPS = DPS[unit.attackLevel];
                if (currentDPS <= 0) continue;
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

            units = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
//                currentDPS = DPS[unit.getType().ordinal()] / (10 + rc.senseRubble(unit.getLocation()));
                currentDPS = DPS[unit.attackLevel];
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

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            if (bestMicro.dir == Direction.CENTER) return true;

            if (rc.canFill(bestMicro.location))
                rc.fill(bestMicro.location);
            else if (rc.canMove(bestMicro.dir)) {
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

//            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
//            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

//            for (RobotInfo enemy : enemies) {
//                int dist = enemy.location.distanceSquaredTo(location);
//                if (dist <= currentActionRadius)
//                    DPSreceived += DPS[enemy.attackLevel];
//                if (dist <= minDistanceToEnemy)
//                    minDistanceToEnemy = dist;
//            }
//
//            for (RobotInfo ally : allies) {
//                int dist = ally.location.distanceSquaredTo(location);
//                if (dist <= currentActionRadius)
//                    alliesTargeting += DPS[ally.attackLevel];
//            }


//            else{
////                try {
////                    rubble = rc.senseRubble(this.location);
////                } catch (Exception e){
////                    e.printStackTrace();
////                }
//                if (!hurt){
//                    try{
////                        this.DPSreceived -= myDPS/(10 + rc.senseRubble(this.location));
////                        this.alliesTargeting += myDPS/(10 + rc.senseRubble(this.location));
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//                    minDistanceToEnemy = rangeExtended[RobotType.SOLDIER.ordinal()];
//                } else minDistanceToEnemy = INF;
//            }
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
            // TODO test if this actually gains 1 by 1
            if (unit.location.distanceSquaredTo(location) <= currentActionRadius)
                DPSreceived -= DPS[unit.healLevel];
        }

        int safe(){
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
            if (distToEnemyFlagHolder < M.distToEnemyFlagHolder) return true;
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