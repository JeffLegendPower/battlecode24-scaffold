package v11.robots;

import battlecode.common.*;
import v11.Constants;
import v11.RobotPlayer;
import v11.Utils;

import static microplayer.General.protectedFlagIndex;
import static microplayer.General.rc;

public class Defender extends AbstractRobot {

    private final Direction[] diagonals = new Direction[] {
        Direction.NORTHWEST,
        Direction.NORTHEAST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.CENTER
    };

    private int protectedFlagIndex = -1;

    @Override
    public boolean setup(RobotController rc) throws GameActionException {
        int numDefenders = rc.readSharedArray(Constants.SharedArray.numDefenders);
        if (numDefenders < 3) {
            spawn(rc);
            if (rc.isSpawned() && rc.senseNearbyFlags(0).length > 0) {
                rc.writeSharedArray(Constants.SharedArray.numDefenders, numDefenders + 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
        int enemiesSeen = Math.min(enemies.length, 31);
        if (rc.senseNearbyFlags(1, rc.getTeam()).length == 0) {  // no flag there
            enemiesSeen = Math.max(enemiesSeen - 7, 0);
        }
//        int mask = 0b11111 << (5 * protectedFlagIndex);
//        v &= ~mask;

        int newSafetyLevel = enemiesSeen << (5 * protectedFlagIndex);
        int newV = (v ^ (v & (0b11111 << (5*protectedFlagIndex)))) | newSafetyLevel;
//        int newV = v | newSafetyLevel;
        rc.setIndicatorString("protect " + v + " " + newV);
        rc.writeSharedArray(Constants.SharedArray.defenderAlert, newV);

        if (rc.canBuild(TrapType.WATER, spawn)) {
            rc.build(TrapType.WATER, spawn);
        }

        for (Direction direction : diagonals) {
            MapLocation ahead = curLoc.add(direction);
            if (rc.canBuild(TrapType.STUN, ahead))
                rc.build(TrapType.STUN, ahead);
        }

//        for (Direction direction : RobotPlayer.directions) {
//            MapLocation ahead = curLoc.add(direction);
//            if (rc.canBuild(TrapType.EXPLOSIVE, ahead))
//                rc.build(TrapType.EXPLOSIVE, ahead);
//        }


        for (RobotInfo enemy : enemies) {
            if (rc.canAttack(enemy.location)) {
                rc.attack(enemy.location);
                return;
            }
        }

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : allies) {
            if (rc.canHeal(ally.location)) {
                rc.heal(ally.location);
                return;
            }
        }
    }

    @Override
    public boolean completedTask() {
        return false;
    }

    @Override
    public void tickJailed(RobotController rc) throws GameActionException {
        // Reset defender alert for the specific flag
        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);

        int mask = 0b11111 << (5 * protectedFlagIndex);
        rc.writeSharedArray(Constants.SharedArray.defenderAlert, v & ~mask);
        spawn(rc);
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        if (protectedFlagIndex != -1 && rc.canSpawn(spawn)) {
//            int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
//            v |= (0b11111 << (5*protectedFlagIndex));
//            rc.writeSharedArray(Constants.SharedArray.defenderAlert, v);

            if (spawn != null) {
                rc.spawn(spawn);
            }
        } else {
            int i = 0;
            for (MapLocation spawn : RobotPlayer.allyFlagSpawnLocs) {
                if (rc.canSpawn(spawn)) {
                    rc.spawn(spawn);
                    protectedFlagIndex = i;
                    super.spawn = spawn;
                    break;
                }
                i++;
            }

//            int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
//            v |= (0b11111 << (5*protectedFlagIndex));
//            rc.writeSharedArray(Constants.SharedArray.defenderAlert, v);
        }
    }

    @Override
    public String name() {
        return "Defender";
    }
}
