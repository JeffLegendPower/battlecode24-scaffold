package v11_2.robots;

import battlecode.common.*;
import v11_2.Constants;
import v11_2.MicroAttacker;
import v11_2.RobotPlayer;
import v11_2.Pathfinding;
import v11_2.Utils;

public class Vegetable extends AbstractRobot {

    private int[] enemies = new int[3];
    private MapLocation currentTarget = null;
    private boolean isCompleted = false;
    private MicroAttacker microAttacker = null;
    private int numTurnsAlive = 0;

    @Override
    public boolean setup(RobotController rc) throws GameActionException {
        numTurnsAlive = 0;
        return true;
    }

    @Override
    public void tick(RobotController rc, MapLocation curLoc) throws GameActionException {
        if (microAttacker == null)
            microAttacker = new MicroAttacker(rc);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length > 2 && numTurnsAlive < 7) {
            if (!curLoc.isWithinDistanceSquared(currentTarget, 10))
                Pathfinding.moveTowards(rc, curLoc, currentTarget, false);
//            else
//                microAttacker.doMicro(curLoc.distanceSquaredTo(currentTarget), curLoc.distanceSquaredTo(currentTarget));
            MapLocation closestEnemy = Utils.getClosest(enemies, currentTarget).location;
            if (rc.canAttack(closestEnemy))
                rc.attack(closestEnemy);
            else {
                MapLocation buildTarget = curLoc.add(curLoc.directionTo(closestEnemy));
                if (rc.canBuild(TrapType.STUN, buildTarget))
                    rc.build(TrapType.STUN, buildTarget);
            }
        }
        else {
            isCompleted = true;
        }
        numTurnsAlive++;
    }

    @Override
    public boolean completedTask() {
        return isCompleted;
    }

    @Override
    public void tickJailed(RobotController rc) throws GameActionException {
        int v = rc.readSharedArray(Constants.SharedArray.defenderAlert);
        for (int i = 0; i < 3; i++) {
            enemies[i] = (v >> (5 * i)) & 0b11111;
            if (enemies[i] >= 5) {
                currentTarget = RobotPlayer.allyFlagSpawnLocs[i];
                int newV = (v ^ (v & (0b11111 << (5 * i)))) | (enemies[i] - 5);
                rc.writeSharedArray(Constants.SharedArray.defenderAlert, newV);
                spawn(rc);
                break;
            }
        }
    }

    @Override
    public void spawn(RobotController rc) throws GameActionException {
        if (currentTarget == null)
            return;

        for (MapLocation spawn : rc.getAllySpawnLocations()) {
            if (spawn.isAdjacentTo(currentTarget) && rc.canSpawn(spawn)) {
                rc.spawn(spawn);
                break;
            }
        }
    }

    @Override
    public String name() {
        return "Vegetable";
    }
}
