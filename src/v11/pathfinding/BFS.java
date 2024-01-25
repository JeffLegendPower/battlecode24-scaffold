package v11.pathfinding;

import battlecode.common.*;
import v11.pathfinding.MapTracker;
import v11.MicroAttacker;
import v11.pathfinding.Pathfinding2;

public abstract class BFS {

    final int BYTECODE_REMAINING = 1000;
    final int GREEDY_TURNS = 4;

    Pathfinding2 path;
    MicroAttacker micro;
    static RobotController rc;
    MapTracker mapTracker = new MapTracker();

    int turnsGreedy = 0;
    MapLocation currentTarget = null;




    public BFS(RobotController rc){
        BFS.rc = rc;
        this.path = new Pathfinding2(rc);
        this.micro = new MicroAttacker(rc);
    }

    public void reset(){
        turnsGreedy = 0;
        mapTracker.reset();
    }

    public void update(MapLocation target){
        if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
            reset();
        } else --turnsGreedy;
        currentTarget = target;
        mapTracker.add(rc.getLocation());
    }

    public void activateGreedy(){
        turnsGreedy = GREEDY_TURNS;
    }

    public void move(MapLocation target){
        move(target, false);
    }

    public void move(MapLocation target, boolean greedy) {
        if (!rc.isMovementReady()){
            return;
        }

        //System.out.println("Before micro " + Clock.getBytecodeNum());

        if (micro.doMicro()) {
            //rc.setIndicatorString("Did micro");
            reset();
            return;
        }

        //System.out.println("After micro " + Clock.getBytecodeNum());

        if (target == null) return;

        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0) {

            int t = Clock.getBytecodesLeft();
            Direction dir = getBestDir(target);
            if (Clock.getBytecodesLeft() < t)
                System.out.println("over bytecode limit");
            t = Clock.getBytecodesLeft() - t;
            rc.setIndicatorString("Using bfs!!! " + t);
            if (dir != null && !mapTracker.check(rc.getLocation().add(dir))){
                move(dir);
                return;
            } else activateGreedy();
        }

        if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING){
            path.move(target);
            --turnsGreedy;
        }
    }

    public void move(Direction dir){
        try{
            if (!rc.canMove(dir)) return;
            rc.move(dir);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public abstract Direction getBestDir(MapLocation target);
}