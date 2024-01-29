package v11;

import battlecode.common.*;
import v11.robots.AbstractRobot;
import v11.robots.Attacker;
import v11.robots.Defender;
import v11.robots.FlagCarrier;

import java.util.Random;

public strictfp class RobotPlayer {

    public static final Random rng = new Random();
    public static MapInfo[][] map;
    public static MapInfo[][] lastMap;
    private static AbstractRobot robot = null;
    private static AbstractRobot flagRobot = null;

    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static MapLocation[] allyFlagSpawnLocs = new MapLocation[3];
    public static MapLocation[] enemyFlagLocs;

    public static int flagChainDropTurn = -1; // Prevents flag dropper from picking up flag too quickly so we can do a flag chain

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        map = new MapInfo[rc.getMapWidth()][rc.getMapHeight()];
        lastMap = new MapInfo[rc.getMapWidth()][rc.getMapHeight()];

        MapLocation[] spawns = rc.getAllySpawnLocations();

        int allyFlagSpawnLocIdx = 0;
        spawner:
        for (MapLocation spawn : spawns) {
            MapLocation[] test = new MapLocation[] {
                    spawn.add(Direction.NORTH),
                    spawn.add(Direction.SOUTH),
                    spawn.add(Direction.EAST),
                    spawn.add(Direction.WEST)
            };
            for (MapLocation loc : test) {
                if (Utils.indexOf(spawns, loc) == -1)
                    continue spawner;
            }

            allyFlagSpawnLocs[allyFlagSpawnLocIdx++] = spawn;
        }

        enemyFlagLocs = new MapLocation[] {
                new MapLocation(rc.getMapWidth() - allyFlagSpawnLocs[0].x - 1, rc.getMapHeight() - allyFlagSpawnLocs[0].y - 1),
                new MapLocation(rc.getMapWidth() - allyFlagSpawnLocs[1].x - 1, rc.getMapHeight() - allyFlagSpawnLocs[1].y - 1),
                new MapLocation(rc.getMapWidth() - allyFlagSpawnLocs[2].x - 1, rc.getMapHeight() - allyFlagSpawnLocs[2].y - 1)
        };

        if (robot == null) {
            Defender defender = new Defender();
            Attacker attacker = new Attacker();
            if (defender.setup(rc))
                robot = defender;
            else if (attacker.setup(rc))
                robot = attacker;
        }

        for (int i = 0; i < 3; i++) {
            if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]) == null)
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], enemyFlagLocs[i]);
        }

        while (true) {
            tryBuyGlobalUpgrades(rc);

            if (rc.isSpawned()) {
                MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    map[loc.x][loc.y] = info;
                }

                if (rc.hasFlag() && flagRobot == null) {
                    flagRobot = new FlagCarrier();
                    flagRobot.setup(rc);
                } else if (!rc.hasFlag()) {
                    flagRobot = null;
                }

                if (rc.hasFlag()) {
                    rc.setIndicatorString(robot.name() + " " + flagRobot.name());
                    flagRobot.tick(rc, rc.getLocation());
                } else {
                    rc.setIndicatorString(robot.name());
                    if (rc.getRoundNum() < 200)
                        robot.setupTick(rc, rc.getLocation());
                    else
                        robot.tick(rc, rc.getLocation());
                }

                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    lastMap[loc.x][loc.y] = info;
                }
            } else {
                Pathfinding.visited.clear();
                if (flagRobot != null) {
                    flagRobot.tickJailed(rc);
                    flagRobot = null;
                } else
                    robot.tickJailed(rc);
            }

            Clock.yield();
        }
    }

    private static void tryBuyGlobalUpgrades(RobotController rc) throws GameActionException {
        if (rc.canBuyGlobal(GlobalUpgrade.ATTACK))
            rc.buyGlobal(GlobalUpgrade.ATTACK);
        else if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
            rc.buyGlobal(GlobalUpgrade.HEALING);
        else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
            rc.buyGlobal(GlobalUpgrade.CAPTURING);
    }
}
