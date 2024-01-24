package v11;

import battlecode.common.*;

public class MicroAttacker {

    static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    final int INF = 1000000;
    boolean attacker = false;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    boolean hurt = false; //TODO: if hurt we want to go back to archon
    static int myRange;
    static int myVisionRange;
    static double myDPS;
    boolean severelyHurt = false;

    double[] DPS = new double[]{0, 0, 0, 0, 0, 0, 0};
    int[] rangeExtended = new int[]{0, 0, 0, 0, 0, 0, 0};
    RobotController rc;

    MicroAttacker(RobotController rc) {
        attacker = true;
        myRange = 2;
        myVisionRange = 20;

        DPS[RobotType.SOLDIER.ordinal()] = 12;
        DPS[RobotType.SAGE.ordinal()] = 9;
        rangeExtended[RobotType.SOLDIER.ordinal()] = 20;
        rangeExtended[RobotType.SAGE.ordinal()] = 34;
        myDPS = DPS[rc.getType().ordinal()];
    }

    static double currentDPS = 0;
    static double currentRangeExtended;
    static double currentActionRadius;
    static boolean canAttack;

    final static int MAX_RUBBLE_DIFF = 5;

    boolean doMicro() {
        try {
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            severelyHurt = rc.getHealth() < 300;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;
            canAttack = rc.isActionReady();

            int uIndex = units.length;
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
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(RobotPlayer.directions[i]);

            int minRubble = microInfo[8].rubble;
            if (microInfo[7].canMove && minRubble > microInfo[7].rubble) minRubble = microInfo[7].rubble;
            if (microInfo[6].canMove && minRubble > microInfo[6].rubble) minRubble = microInfo[6].rubble;
            if (microInfo[5].canMove && minRubble > microInfo[5].rubble) minRubble = microInfo[5].rubble;
            if (microInfo[4].canMove && minRubble > microInfo[4].rubble) minRubble = microInfo[4].rubble;
            if (microInfo[3].canMove && minRubble > microInfo[3].rubble) minRubble = microInfo[3].rubble;
            if (microInfo[2].canMove && minRubble > microInfo[2].rubble) minRubble = microInfo[2].rubble;
            if (microInfo[1].canMove && minRubble > microInfo[1].rubble) minRubble = microInfo[1].rubble;
            if (microInfo[0].canMove && minRubble > microInfo[0].rubble) minRubble = microInfo[0].rubble;

            minRubble += MAX_RUBBLE_DIFF;

            if (microInfo[8].rubble > minRubble) microInfo[8].canMove = false;
            if (microInfo[7].rubble > minRubble) microInfo[7].canMove = false;
            if (microInfo[6].rubble > minRubble) microInfo[6].canMove = false;
            if (microInfo[5].rubble > minRubble) microInfo[5].canMove = false;
            if (microInfo[4].rubble > minRubble) microInfo[4].canMove = false;
            if (microInfo[3].rubble > minRubble) microInfo[3].canMove = false;
            if (microInfo[2].rubble > minRubble) microInfo[2].canMove = false;
            if (microInfo[1].rubble > minRubble) microInfo[1].canMove = false;
            if (microInfo[0].rubble > minRubble) microInfo[0].canMove = false;

            boolean danger = rc.senseMapInfo(rc.getLocation()).getTeamTerritory() != rc.getTeam();

            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                int t = unit.getType().ordinal();
                currentDPS = DPS[t] / (10 + rc.senseRubble(unit.getLocation()));
                if (currentDPS <= 0) continue;
                //if (danger && Robot.comm.isEnemyTerritory(unit.getLocation())) currentDPS*=1.5;
                currentRangeExtended = rangeExtended[t];
                currentActionRadius = unit.getType().actionRadiusSquared;
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
//                if (currentDPS <= 0) continue;
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
//        double DPSreceived = 0;
        double enemiesTargeting = 0;
        double alliesTargeting = 0;
        boolean canMove = true;
//        int rubble = 0;

        public MicroInfo(Direction dir){
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;
            else{
//                try {
//                    rubble = rc.senseRubble(this.location);
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
                if (!hurt){
                    try{
//                        this.DPSreceived -= myDPS/(10 + rc.senseRubble(this.location));
//                        this.alliesTargeting += myDPS/(10 + rc.senseRubble(this.location));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    minDistanceToEnemy = rangeExtended[RobotType.SOLDIER.ordinal()];
                } else minDistanceToEnemy = INF;
            }
        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToEnemy)  minDistanceToEnemy = dist;
            if (dist <= currentActionRadius) DPSreceived += currentDPS;
            if (dist <= currentRangeExtended) enemiesTargeting += currentDPS;
        }

        void updateAlly(RobotInfo unit){
            if (!canMove) return;
            alliesTargeting += currentDPS;
        }

        int safe(){
            if (!canMove) return -1;
            if (DPSreceived > 0) return 0;
            if (enemiesTargeting > alliesTargeting) return 1;
            return 2;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo M){

            if (safe() > M.safe()) return true;
            if (safe() < M.safe()) return false;

            if (inRange() && !M.inRange()) return true;
            if (!inRange() && M.inRange()) return false;

            if (!severelyHurt) {
                if (alliesTargeting > M.alliesTargeting) return true;
                if (alliesTargeting < M.alliesTargeting) return false;
            }

            if (inRange()) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;
        }
    }

}