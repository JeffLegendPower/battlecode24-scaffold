package v11_2;

import battlecode.common.*;
import v11_2.robots.AbstractRobot;
import v11_2.robots.Attacker;
import v11_2.robots.Defender;
import v11_2.robots.FlagCarrier;

import java.util.Random;

public strictfp class RobotPlayer {

    public static final Random rng = new Random();
    public static MapInfo[][] map;
    public static MapInfo[][] lastMap;
    private static AbstractRobot robot = null;
    private static FlagCarrier flagRobot = null;

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
    public static int flagChainDropTurn = -1;

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
            if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i]) == null) {
                Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[i], enemyFlagLocs[i]);
            }
        }

        int numTurnsSinceHadFlag = 5;
        int lastFlagID = 0;

        while (true) {
            tryBuyGlobalUpgrades(rc);

//            if (rc.getRoundNum() == 201) {
//                MapLocation[] broadcast = rc.senseBroadcastFlagLocations();
//                for (MapLocation enemyFlagLoc : broadcast) {
//
//                }
//            }

            if (rc.isSpawned()) {
                if (Utils.getLocationInSharedArray(rc, Constants.SharedArray.coordinatedAttacks[0]) == null) {
                    for(int attack : Constants.SharedArray.coordinatedAttacks) {
                        Utils.storeLocationInSharedArray(rc, attack, null);
                    }
                }

                MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos();
                for (MapInfo info : nearbyMapInfo) {
                    MapLocation loc = info.getMapLocation();
                    map[loc.x][loc.y] = info;
                }

                if (rc.hasFlag() && flagRobot == null) {
                    flagRobot = new FlagCarrier();
                    flagRobot.setup(rc);
                    numTurnsSinceHadFlag = 0;
                    lastFlagID = flagRobot.index;
                } else if (!rc.hasFlag()) {
                    flagRobot = null;
                    numTurnsSinceHadFlag += 1;
                }

//                if (numTurnsSinceHadFlag >= 6) {
//                    int hasFlag = rc.readSharedArray(Constants.SharedArray.carriedFlagIDs[lastFlagID] & 0b1);
//                    if (hasFlag == 0) {
//                        // reset flag
//                        Utils.storeLocationInSharedArray(rc, Constants.SharedArray.enemyFlagLocs[lastFlagID],
//                                Utils.getLocationInSharedArray(rc, Constants.SharedArray.flagOrigins[lastFlagID]));
//                    } else {
//                        numTurnsSinceHadFlag = 0;
//                    }
//                }

                if (rc.hasFlag()) {
                    rc.setIndicatorString(robot.name() + " " + flagRobot.name());
                    flagRobot.tick(rc, rc.getLocation());
                } else {
//                    rc.setIndicatorString(robot.name());
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

//    public static void mapFreshInVisionLocations(RobotController rc) throws GameActionException {
//
//        int mapWidth = rc.getMapWidth();
//        int mapHeight = rc.getMapHeight();
//
//        // scan symmetries from shared array
//        int invalidSymmetriesFromSharedArray = rc.readSharedArray(8);
//        for (int i=0; i<3; i++) {
//            boolean isInvalidSymmetry = (invalidSymmetriesFromSharedArray & (1 << (15-i))) > 0;
//            if (isInvalidSymmetry) {
//                possibleSymmetries[i] = false;
//            }
//        }
//        if (!symmetryWasDetermined) {
//            checkIfSymmetryIsDetermined();
//        }
//
//        // just spawned in
//        if (previousLocationForMappingFreshLocations == null) {
//            MapInfo[] nearbyInfos = rc.senseNearbyMapInfos();
//            for (MapInfo info : nearbyInfos) {
//                MapLocation loc = info.getMapLocation();
//                if (0 <= loc.x && loc.x < mapWidth && 0 <= loc.y & loc.y < mapHeight) {
//                    mapped[loc.x][loc.y] = info.isWall() ? 0b01 : 0b11;
//                }
//            }
//            return;
//        }
//
//        if (previousLocationForMappingFreshLocations.equals(rc.getLocation())) {  // did not move since last turn
//            return;
//        }
//
//        // did move since last turn, scan new locations
//        for (MapLocation l : getFreshInVisionLocations()) {
//            if (0 <= l.x && l.x < mapWidth && 0 <= l.y && l.y < mapHeight) {
//                if (mapped[l.x][l.y] == 0) {
//                    boolean isWall = rc.senseMapInfo(l).isWall();
//                    mapped[l.x][l.y] = isWall ? 0b01 : 0b11;
//                    if (!symmetryWasDetermined) {
//                        int rotationalSymmetryValue = mapped[mapWidth - 1 - l.x][mapHeight - 1 - l.y];
//                        int upDownSymmetryValue = mapped[l.x][mapHeight - 1 - l.y];
//                        int rightLeftSymmetryValue = mapped[mapWidth - 1 - l.x][l.y];
//                        if ((rotationalSymmetryValue & 0b1) == 0b1) {  // already seen rotational symmetry value
//                            if ((rotationalSymmetryValue & 0b11) != mapped[l.x][l.y]) {  // rotational symmetry value is not a wall
//                                possibleSymmetries[0] = false;
//                                checkIfSymmetryIsDetermined();
//                            }
//                        }
//                        if ((upDownSymmetryValue & 0b1) == 0b1) {
//                            if ((upDownSymmetryValue & 0b11) != mapped[l.x][l.y]) {
//                                possibleSymmetries[1] = false;
//                                checkIfSymmetryIsDetermined();
//                            }
//                        }
//                        if ((rightLeftSymmetryValue & 0b1) == 0b1) {
//                            if ((rightLeftSymmetryValue & 0b11) != mapped[l.x][l.y]) {
//                                possibleSymmetries[2] = false;
//                                checkIfSymmetryIsDetermined();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public static MapLocation[] getFreshInVisionLocations() {
//        MapLocation robotLoc = rc.getLocation();
//
//        int x = robotLoc.x;
//        int y = robotLoc.y;
//        switch (previousLocationForMappingFreshLocations.directionTo(robotLoc)) {
//            case NORTH:
//                return new MapLocation[]{
//                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4),
//                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
//                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2)
//                };
//            case NORTHEAST:
//                return new MapLocation[]{
//                        new MapLocation(x-1, y+4), new MapLocation(x, y+4), new MapLocation(x+1, y+4),
//                        new MapLocation(x+2, y+4), new MapLocation(x+2, y+3), new MapLocation(x+3, y+3),
//                        new MapLocation(x+3, y+2), new MapLocation(x+4, y+2), new MapLocation(x+4, y+1),
//                        new MapLocation(x+4, y), new MapLocation(x+4, y-1), new MapLocation(x-2, y+4),
//                        new MapLocation(x+4, y-2)
//                };
//            case EAST:
//                return new MapLocation[]{
//                        new MapLocation(x+2, y+4), new MapLocation(x+3, y+3), new MapLocation(x+4, y+2),
//                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
//                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-3), new MapLocation(x+2, y-4)
//                };
//            case SOUTHEAST:
//                return new MapLocation[]{
//                        new MapLocation(x+4, y+1), new MapLocation(x+4, y), new MapLocation(x+4, y-1),
//                        new MapLocation(x+4, y-2), new MapLocation(x+3, y-2), new MapLocation(x+3, y-3),
//                        new MapLocation(x+2, y-3), new MapLocation(x+2, y-4), new MapLocation(x+1, y-4),
//                        new MapLocation(x, y-4), new MapLocation(x-1, y-4), new MapLocation(x+4, y+2),
//                        new MapLocation(x-2, y-4)
//                };
//            case SOUTH:
//                return new MapLocation[]{
//                        new MapLocation(x-4, y-2), new MapLocation(x-3, y-3), new MapLocation(x-2, y-4),
//                        new MapLocation(x-1, y-4), new MapLocation(x, y-4), new MapLocation(x+1, y-4),
//                        new MapLocation(x+2, y-4), new MapLocation(x+3, y-3), new MapLocation(x+4, y-2)
//                };
//            case SOUTHWEST:
//                return new MapLocation[]{
//                        new MapLocation(x+1,  y-4), new MapLocation(x, y-4), new MapLocation(x-1, y-4),
//                        new MapLocation(x-2, y-4), new MapLocation(x-2, y-3), new MapLocation(x-3, y-3),
//                        new MapLocation(x-3, y-2), new MapLocation(x-4, y-2), new MapLocation(x-4, y-1),
//                        new MapLocation(x-4, y), new MapLocation(x-4, y+1), new MapLocation(x-4, y+2),
//                        new MapLocation(x+2, y-4)
//                };
//            case WEST:
//                return new MapLocation[]{
//                        new MapLocation(x-2, y-4), new MapLocation(x-3, y-3), new MapLocation(x-4, y-2),
//                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
//                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+3), new MapLocation(x-2, y+4)
//                };
//            case NORTHWEST:
//                return new MapLocation[]{
//                        new MapLocation(x-4, y-1), new MapLocation(x-4, y), new MapLocation(x-4, y+1),
//                        new MapLocation(x-4, y+2), new MapLocation(x-3, y+2), new MapLocation(x-3, y+3),
//                        new MapLocation(x-2, y+3), new MapLocation(x-2, y+4), new MapLocation(x-1, y+4),
//                        new MapLocation(x, y+4), new MapLocation(x+1, y+4), new MapLocation(x-4, y-2),
//                        new MapLocation(x+2, y+4)
//                };
//        }
//        System.out.println("big issue 1 !!!");
//        return new MapLocation[0];
//    }
//
//    public static void checkIfSymmetryIsDetermined() throws GameActionException {
//        int validSymmetryCount = 0;
//        int lastValidIndex = 0;
//        int newSharedArrayValue = 0;
//        for (int i = 0; i < 3; i++) {
//            newSharedArrayValue <<= 1;
//            newSharedArrayValue |= possibleSymmetries[i] ? 0 : 1;
//            if (possibleSymmetries[i]) {
//                validSymmetryCount += 1;
//                lastValidIndex = i;
//            }
//        }
//        newSharedArrayValue <<= 13;
//        if (validSymmetryCount == 1) {
//            symmetryWasDetermined = true;
//        }
//        if (rc.readSharedArray(8) != newSharedArrayValue) {
//            if (validSymmetryCount == 1) {
//                if (lastValidIndex == 0) {
//                    System.out.println("ROTATIONAL SYMMETRY");
//                } else if (lastValidIndex == 1) {
//                    System.out.println("UP/DOWN SYMMETRY");
//                } else {
//                    System.out.println("LEFT/RIGHT SYMMETRY");
//                }
//            }
//            rc.writeSharedArray(8, newSharedArrayValue);
//        }
//    }
}
