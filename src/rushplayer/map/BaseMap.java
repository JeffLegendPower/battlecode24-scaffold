package rushplayer.map;

import battlecode.common.*;
import rushplayer.General.*;

public class BaseMap {

    public enum MapSymmetry {
        ROTATIONAL,
        HORIZONTAL,
        VERTICAL
    }

    public final MapSymmetry symmetry = MapSymmetry.ROTATIONAL;
    public MapLocation[] enemySpawnLocations = new MapLocation[27];
    public Integer width;
    public Integer height;
}
