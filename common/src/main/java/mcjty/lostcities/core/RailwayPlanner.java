package mcjty.lostcities.core;

import mcjty.lostcities.api.RailChunkType;
import mcjty.lostcities.varia.QualityRandom;

import java.util.LinkedHashMap;
import java.util.Map;


import static mcjty.lostcities.api.RailChunkType.*;
import static mcjty.lostcities.core.RailwayPlanner.RailDirection.*;

/** Upstream railway grid and bounded station approaches; native access is supplied by the owning world. */
public final class RailwayPlanner {
    public interface Environment {
        boolean city(int x,int z);
        int cityLevel(int x,int z);
        int highwayX(int x,int z);
        int highwayZ(int x,int z);
        boolean excluded(int x,int z);
        String part(String key,java.util.Random random,String fallback);
    }
    public static final class Settings {
        public int offset,spacingX=10,spacingZ=10;
        public boolean canEnd,stations=true,tracks=true,surfaceStations=true;
    }
    private final long seed;
    private final Environment environment;
    private final Settings profile;
    public RailwayPlanner(long seed,Settings settings,Environment environment){
        if(settings.spacingX<4||settings.spacingZ<4||(settings.spacingX&1)!=0||(settings.spacingZ&1)!=0)throw new IllegalArgumentException("Railway spacings must be even and at least four chunks");
        this.seed=seed;this.profile=settings;this.environment=environment;
    }
    private static final class ChunkCoord {
        final int x,z;
        ChunkCoord(int x,int z){this.x=x;this.z=z;}
        int chunkX(){return x;}int chunkZ(){return z;}
        ChunkCoord offset(int x,int z){return new ChunkCoord(this.x+x,this.z+z);}
        ChunkCoord east(){return offset(1,0);}ChunkCoord west(){return offset(-1,0);}
        @Override public boolean equals(Object other){return other instanceof ChunkCoord&&((ChunkCoord)other).x==x&&((ChunkCoord)other).z==z;}
        @Override public int hashCode(){return 31*x+z;}
    }
    public RailChunkInfo at(int x,int z){return getRailChunkType(new ChunkCoord(x,z));}
    private boolean city(ChunkCoord key){return environment.city(key.x,key.z);}
    private int highwayX(ChunkCoord key){return environment.highwayX(key.x,key.z);}
    private int highwayZ(ChunkCoord key){return environment.highwayZ(key.x,key.z);}


    public static final int RAILWAY_LEVEL_OFFSET = -3;

    public static int getRailwayLevel(Settings profile) {
        return RAILWAY_LEVEL_OFFSET + profile.offset;
    }

    /*
    Default 10-chunk railway grid:

      .   .   .   .   s   .   s   .   .   .   .   .   .   .   .

      .   .   .   .   s   s   s   .   .   .   .   .   .   .   .

      .   .   .   .   S\  0  /S   .   .   .   .   .   .   .   .
                        |   |
      .   .   .   .   S--=S=--S   .   .   .   .   .   .   .   .
                        |   |
      .   .   .   .   S/  .  \S   .   .   .   .   .   .   .   .

      .   .   .   .   s   s   s   .   .   .   .   .   .   .   .


    00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19
    SS >>          ||          << SS >>          ||          <<

     */

    public enum RailDirection {
        BI,
        WEST,
        EAST
    }

    public static class RailChunkInfo {
        private final RailChunkType type;
        private final RailDirection direction;
        private final int level;
        private final int rails;
        private final String part;

        public static final RailChunkInfo NOTHING = new RailChunkInfo(NONE, BI, 0, 0);

        public RailChunkInfo(RailChunkType type, RailDirection direction, int level, int rails) {
            this(type, direction, level, rails, null);
        }

        public RailChunkInfo(RailChunkType type, RailDirection direction, int level, int rails, String part) {
            this.type = type;
            this.direction = direction;
            this.level = level;
            this.rails = rails;
            this.part = part;
        }

        public RailChunkType getType() {
            return type;
        }

        public RailDirection getDirection() {
            return direction;
        }

        public int getLevel() {
            return level;
        }

        public int getRails() {
            return rails;
        }

        public String getPart() {
            return part;
        }
    }

    private final Map<ChunkCoord,RailChunkInfo> RAIL_INFO=new LinkedHashMap<ChunkCoord,RailChunkInfo>(256,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<ChunkCoord,RailChunkInfo> eldest){return size()>16384;}
    };

    public void cleanCache() {
        RAIL_INFO.clear();
    }

    /**
     * The station grid repeats every two configured railway spacings. There is no station at the
     * origin of each repeating cell, leaving room for the alternating north-south connections.
     */
    private RailChunkInfo getRailChunkTypeInternal(ChunkCoord key) {
        int chunkX = key.chunkX();
        int chunkZ = key.chunkZ();
        QualityRandom randomRailChunkType = new QualityRandom(seed + chunkZ * 2600003897L + chunkX * 43600002517L);


        int railwayLevel = getRailwayLevel(profile);


        // @todo make all settings based on rand below configurable
        float r = randomRailChunkType.nextFloat();

        int spacingX = profile.spacingX;
        int spacingZ = profile.spacingZ;
        int halfSpacingX = spacingX / 2;
        int mx = Math.floorMod(chunkX + 1, spacingX * 2);       // The +1 preserves the original grid origin
        int mz = Math.floorMod(chunkZ + 1, spacingZ * 2);
        if (mx == 0 && mz == spacingZ) {
            if (!city(key)) {
                // There is no city here. So no station. But we still need a railway. A station at this
                // point will get a three line rail through it
                if (profile.canEnd) {
                    // Check if there are stations at either side
                    boolean cityEast = city(key.offset(spacingX, 0)) ||
                            city(key.offset(spacingX, -spacingZ)) ||
                            city(key.offset(spacingX, spacingZ));
                    boolean cityWest = city(key.offset(-spacingX, 0)) ||
                            city(key.offset(-spacingX, -spacingZ)) ||
                            city(key.offset(-spacingX, spacingZ));
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, railwayLevel, 3);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, railwayLevel, 3);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, railwayLevel, 3);
            }
            return getStationType(key, r, 3,
                    randomRailChunkType.nextFloat() < .5f ? environment.part("stationopen",randomRailChunkType,"station_open") : environment.part("stationopenroof",randomRailChunkType,"station_openroof"));
        }
        if (mx == spacingX && mz == 0) {
            if (!city(key)) {
                // There is no city here. So no station either. But we still need a railway. A station at this
                // point will get a two line rail through it
                if (profile.canEnd) {
                    // Check if there are stations at either side
                    boolean cityEast = city(key.offset(spacingX, -spacingZ)) ||
                            city(key.offset(spacingX, spacingZ));
                    boolean cityWest = city(key.offset(-spacingX, -spacingZ)) ||
                            city(key.offset(-spacingX, spacingZ));
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, railwayLevel, 2);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, railwayLevel, 2);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, railwayLevel, 2);
            }
            return getStationType(key, r, 2,
                    randomRailChunkType.nextFloat() < .5f ? environment.part("stationopen",randomRailChunkType,"station_open") : environment.part("stationopenroof",randomRailChunkType,"station_openroof"));
        }
        if (mx == spacingX && mz == spacingZ) {
            if (!city(key)) {
                // There is no city here. So no station either. But we still need a railway. A station at this
                // point will get a single line rail through it
                if (profile.canEnd) {
                    // Check if there are stations at either side
                    boolean cityEast = city(key.offset(spacingX, 0));
                    boolean cityWest = city(key.offset(-spacingX, 0));
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, railwayLevel, 1);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, railwayLevel, 1);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, railwayLevel, 1);
            }
            return getStationType(key, r, 1,
                    randomRailChunkType.nextFloat() < .5f ? environment.part("stationopen",randomRailChunkType,"station_open") : environment.part("stationopenroof",randomRailChunkType,"station_openroof"));
        }
        if (mx == 0 && mz == 0) {
            return RailChunkInfo.NOTHING;
        }

        if (mz == 0 || mz == spacingZ) {
            // Handle the rail sections left or right of every station
            if ((mx >= spacingX + halfSpacingX + 1 && mz != 0)
                    || (mx >= halfSpacingX + 1 && mx < spacingX)) {
                ChunkCoord east = key.east();
                RailChunkInfo adjacent = getRailChunkType(east);
                RailDirection direction = adjacent.getDirection();
                if (direction == BI || adjacent.getType() == RAILS_END_HERE) {
                    direction = WEST;
                }
                return testAdjacentRailChunk(r, adjacent, direction, key.west());
            }
            if ((mx >= 1 && mx < halfSpacingX && mz != 0)
                    || (mx > spacingX && mx < spacingX + halfSpacingX)) {
                ChunkCoord west = key.west();
                RailChunkInfo adjacent = getRailChunkType(west);
                RailDirection direction = adjacent.getDirection();
                if (direction == BI || adjacent.getType() == RAILS_END_HERE) {
                    direction = EAST;
                }
                return testAdjacentRailChunk(r, adjacent, direction, key.east());
            }
            if (mz == 0 && mx == halfSpacingX) {
                if (profile.canEnd) {
                    boolean cityWest = city(key.offset(-halfSpacingX, -spacingZ)) ||
                            city(key.offset(-halfSpacingX, spacingZ));
                    boolean cityEast = city(key.offset(halfSpacingX, 0));
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                }
                return new RailChunkInfo(DOUBLE_BEND, EAST, railwayLevel, 1);
            }
            if (mz == 0 && mx == spacingX + halfSpacingX) {
                if (profile.canEnd) {
                    boolean cityEast = city(key.offset(halfSpacingX, -spacingZ)) ||
                            city(key.offset(halfSpacingX, spacingZ));
                    boolean cityWest = city(key.offset(-halfSpacingX, 0));
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                }
                return new RailChunkInfo(DOUBLE_BEND, WEST, railwayLevel, 1);
            }
            if (mz == spacingZ && mx == halfSpacingX) {
                if (profile.canEnd) {
                    boolean cityEast = city(key.offset(halfSpacingX, 0));
                    boolean cityWest = city(key.offset(-halfSpacingX, 0));
                    if (!cityEast && !cityWest) {
                        // Check the double bends
                        RailChunkInfo typeNorth = getRailChunkType(key.offset(0, -spacingZ));
                        if (typeNorth.getType() == NONE) {
                            RailChunkInfo typeSouth = getRailChunkType(key.offset(0, spacingZ));
                            if (typeSouth.getType() == NONE) {
                                return RailChunkInfo.NOTHING;
                            }
                        }
                    }
                }
                return new RailChunkInfo(THREE_SPLIT, EAST, railwayLevel, 3);
            }
            if (mz == spacingZ && mx == spacingX + halfSpacingX) {
                if (profile.canEnd) {
                    boolean cityEast = city(key.offset(halfSpacingX, 0));
                    boolean cityWest = city(key.offset(-halfSpacingX, 0));
                    if (!cityEast && !cityWest) {
                        // Check the double bends
                        RailChunkInfo typeNorth = getRailChunkType(key.offset(0, -spacingZ));
                        if (typeNorth.getType() == NONE) {
                            RailChunkInfo typeSouth = getRailChunkType(key.offset(0, spacingZ));
                            if (typeSouth.getType() == NONE) {
                                return RailChunkInfo.NOTHING;
                            }
                        }
                    }
                }
                return new RailChunkInfo(THREE_SPLIT, WEST, railwayLevel, 3);
            }
            return RailChunkInfo.NOTHING;
        }
        if (mx == halfSpacingX) {
            if (profile.canEnd) {
                RailChunkInfo typeNorth = getRailChunkType(key.offset(0, -(mz % spacingZ)));
                RailChunkInfo typeSouth = getRailChunkType(key.offset(0, -(mz % spacingZ) + spacingZ));
                if (typeNorth.getType() == NONE || typeSouth.getType() == NONE) {
                    return RailChunkInfo.NOTHING;
                }
            }
            return new RailChunkInfo(VERTICAL, EAST, railwayLevel, 1);
        }
        if (mx == spacingX + halfSpacingX) {
            if (profile.canEnd) {
                RailChunkInfo typeNorth = getRailChunkType(key.offset(0, -(mz % spacingZ)));
                RailChunkInfo typeSouth = getRailChunkType(key.offset(0, -(mz % spacingZ) + spacingZ));
                if (typeNorth.getType() == NONE || typeSouth.getType() == NONE) {
                    return RailChunkInfo.NOTHING;
                }
            }
            return new RailChunkInfo(VERTICAL, WEST, railwayLevel, 1);
        }

        return RailChunkInfo.NOTHING;
    }

    private RailChunkInfo getStationType(ChunkCoord coord, float r, int rails, String part) {
        int cityLevel = environment.cityLevel(coord.x,coord.z);
        int railwayLevel = getRailwayLevel(profile);
        int maximumDescent = profile.spacingX - 2;
        if (cityLevel > 2 || cityLevel - railwayLevel > maximumDescent || !profile.surfaceStations) {
            // We are too high here. We need an underground station
            return new RailChunkInfo(STATION_UNDERGROUND, BI, railwayLevel, rails);
        }
        // If there is a highway exactly at this spot we cannot have a station. @todo? How to solve this
        int highwayX = highwayX(coord);
        int highwayZ = highwayZ(coord);
        if ((highwayX != -1 && cityLevel >= highwayX) || (highwayZ != -1 && cityLevel >= highwayZ)) {
            // @todo Problem! We cannot have a station here! At least we cannot get stairs to the top here
            // Because this is very rare we just generate an underground station because that looks reasonable
            return new RailChunkInfo(STATION_UNDERGROUND, BI, railwayLevel, rails);
        } else {
            // Check if there is a highway directly adjacent (east/west) to the station. In that case we go to underground station mode
            highwayZ = highwayZ(coord.west());
            if (highwayZ != -1 && cityLevel >= highwayZ) {
                return new RailChunkInfo(STATION_UNDERGROUND, BI, railwayLevel, rails);
            }
            highwayZ = highwayZ(coord.east());
            if (highwayZ != -1 && cityLevel >= highwayZ) {
                return new RailChunkInfo(STATION_UNDERGROUND, BI, railwayLevel, rails);
            }
        }

        return r < .5f ? new RailChunkInfo(STATION_SURFACE, BI, cityLevel, rails, part) : new RailChunkInfo(STATION_UNDERGROUND, BI, railwayLevel, rails);
    }

    private RailChunkInfo getRailChunkType(ChunkCoord coord) {
        RailChunkInfo cached = RAIL_INFO.get(coord);
        if (cached != null) {
            return cached;
        }
        RailChunkInfo info = getRailChunkTypeInternal(coord);
        if (environment.excluded(coord.x,coord.z)) {
            info = RailChunkInfo.NOTHING;
        } else if (info.getType().isStation()) {
            if (!profile.stations) {
                info = RailChunkInfo.NOTHING;
            }
        } else {
            if (!profile.tracks) {
                info = RailChunkInfo.NOTHING;
            }
        }
        RailChunkInfo existing = RAIL_INFO.putIfAbsent(coord, info);
        return existing == null ? info : existing;
    }

    private void removeRailChunkType(ChunkCoord coord) {
        RAIL_INFO.put(coord, RailChunkInfo.NOTHING);
    }

    private RailChunkInfo testAdjacentRailChunk(float r, RailChunkInfo adjacent, RailDirection direction, ChunkCoord coord) {
        int railwayLevel = getRailwayLevel(profile);
        switch (adjacent.getType()) {
            case NONE:
                return RailChunkInfo.NOTHING;
            case STATION_SURFACE:
                // chunkX actually points to the next chunk. If there is a highway there we want to avoid that and go down this level already
                int highwayX = highwayX(coord);
                int highwayZ = highwayZ(coord);
                if ((highwayX != -1 && adjacent.getLevel() == highwayX) || (highwayZ != -1 && adjacent.getLevel() == highwayZ)) {
                    // We have a highway there so go down here by setting r to 1
                    r = 1;
                }
                // An extension consumes one of the available approach chunks. Each remaining
                // sloped part can descend at most two levels before the next north-south track.
                int maximumDescentAfterExtension = profile.spacingX - 4;
                if (r < .4f && adjacent.getLevel() - railwayLevel <= maximumDescentAfterExtension) {
                    return new RailChunkInfo(STATION_EXTENSION_SURFACE, direction, adjacent.getLevel(), adjacent.getRails());
                } else {
                    return startDescending(adjacent, direction, railwayLevel);
                }
            case STATION_UNDERGROUND:
                return r < .4f
                        ? new RailChunkInfo(STATION_EXTENSION_UNDERGROUND, direction, adjacent.getLevel(), adjacent.getRails())
                        : new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
            case STATION_EXTENSION_SURFACE:
                return startDescending(adjacent, direction, railwayLevel);
            case STATION_EXTENSION_UNDERGROUND:
                return new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
            case GOING_DOWN_FURTHER:
            case GOING_DOWN_ONE_FROM_SURFACE:
            case GOING_DOWN_TWO_FROM_SURFACE:
                if (adjacent.getLevel() == railwayLevel) {
                    return new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
                } else {
                    return new RailChunkInfo(GOING_DOWN_FURTHER, direction, adjacent.getLevel() - 2, adjacent.getRails());
                }
            case THREE_SPLIT:
                break;
            case VERTICAL:
                break;
            case DOUBLE_BEND:
                break;
            case RAILS_END_HERE:
                if (direction == adjacent.getDirection()) {
                    return new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
                } else {
                    return RailChunkInfo.NOTHING;
                }
            case HORIZONTAL:
                return adjacent;
        }
        throw new RuntimeException("This is really impossible!");
    }

    private static RailChunkInfo startDescending(RailChunkInfo adjacent, RailDirection direction, int railwayLevel) {
        int difference = adjacent.getLevel() - railwayLevel;
        if ((difference & 1) == 1) {
            return new RailChunkInfo(GOING_DOWN_ONE_FROM_SURFACE, direction, adjacent.getLevel() - 1, adjacent.getRails());
        } else {
            return new RailChunkInfo(GOING_DOWN_TWO_FROM_SURFACE, direction, adjacent.getLevel() - 2, adjacent.getRails());
        }
    }

}
