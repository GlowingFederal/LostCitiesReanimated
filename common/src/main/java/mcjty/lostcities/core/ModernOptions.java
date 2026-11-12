package mcjty.lostcities.core;

import mcjty.lostcities.worldgen.highway.HighwayPlannerSettings;
import mcjty.lostcities.worldgen.street.StreetPlannerSettings;

/** Portable profile values. Forge configuration belongs to each version's adapter. */
public final class ModernOptions {
    public interface Source {
        boolean flag(String name,boolean value,String description);
        int integer(String name,int value,int min,int max,String description);
        float decimal(String name,float value,float min,float max,String description);
    }
    public boolean enabled=true,highways=true,streets=true,multiBuildings=true,scattered=true;
    public float cityScale=3,cityOffset=.1F,cityInnerScale=.1F,scatteredMultiplier=1;
    public int spawnDistance1=0,spawnDistance2=0;
    public float spawnMultiplier1=1,spawnMultiplier2=1;
    public boolean surfaceRailStations=true,fastTerrainPlanning=true;
    public int railwayLevelOffset=0,railwaySpacingNorthSouth=10,railwaySpacingEastWest=10;
    public int cityMinHeight=50,cityMaxHeight=150;
    public float openLotParkChance=.8F,primaryBridgeChance=1;
    public boolean parkElevation=true,parkBorder=true;
    public int parkStreetThreshold=3;
    public int primaryBridgeMaximumLength=12,legacyBridgeMaximumLength=128,corridorMaximumLength=128;
    public StreetPlannerSettings streetSettings=new StreetPlannerSettings(8,8,.45F,4,0,2,0,2,4,3,.4F,2,5);
    public HighwayPlannerSettings highwaySettings=new HighwayPlannerSettings(128,16,.2F,2,64,320,2,40,1,3,0,10,10);
    public void load(Source source){
        fastTerrainPlanning=source.flag("fastTerrainPlanning",fastTerrainPlanning,"Sample the native terrain density lattice for remote planning instead of expanding whole raw chunks. Custom terrain adapters retain full sampling.");
        enabled=source.flag("enabled",enabled,"Use modern assets and planning in new worlds. Existing worlds retain their saved generation mode.");
        highways=source.flag("intercityHighways",highways,"Plan highways between city hubs with the upstream reciprocal-selection algorithm.");
        streets=source.flag("hierarchicalStreets",streets,"Use primary, secondary and tertiary streets.");
        cityMinHeight=source.integer("cityMinHeight",cityMinHeight,1,254,"Lowest native terrain height permitted for normal-terrain cities.");
        cityMaxHeight=source.integer("cityMaxHeight",cityMaxHeight,1,254,"Highest native terrain height permitted for normal-terrain cities.");
        if(cityMinHeight>cityMaxHeight)throw new IllegalArgumentException("cityMinHeight exceeds cityMaxHeight");
        openLotParkChance=source.decimal("openLotParkChance",openLotParkChance,0,1,"Park chance in hierarchical open lots, independent of legacy park chance.");
        parkElevation=source.flag("parkElevation",parkElevation,"Raise connected modern park sections by one block.");
        parkBorder=source.flag("parkBorder",parkBorder,"Use street material around modern park edges.");
        parkStreetThreshold=source.integer("parkStreetThreshold",parkStreetThreshold,0,8,"Street or park neighbors among eight surrounding chunks required for an elevated park.");
        streetSettings=new StreetPlannerSettings(
                source.integer("primaryRoadSpacingX",8,8,256,"Primary road candidate spacing in chunks on X."),
                source.integer("primaryRoadSpacingZ",8,8,256,"Primary road candidate spacing in chunks on Z."),
                source.decimal("primaryRoadOptionalChance",.45F,0,1,"Optional primary road activation chance."),
                source.integer("primaryRoadForceEvery",4,1,16,"Guaranteed primary candidate interval."),
                source.integer("secondaryRoadMinCountX",0,0,16,"Minimum secondary X roads per primary block."),
                source.integer("secondaryRoadMaxCountX",2,0,16,"Maximum secondary X roads per primary block."),
                source.integer("secondaryRoadMinCountZ",0,0,16,"Minimum secondary Z roads per primary block."),
                source.integer("secondaryRoadMaxCountZ",2,0,16,"Maximum secondary Z roads per primary block."),
                source.integer("minimumRoadSeparation",4,2,32,"Minimum secondary road separation."),
                source.integer("minimumRoadEdgeDistance",3,2,32,"Minimum secondary distance from primary edge."),
                source.decimal("tertiaryRoadChance",.4F,0,1,"Tertiary dead-end street chance."),
                source.integer("tertiaryRoadMinLength",2,1,32,"Minimum tertiary street length."),
                source.integer("tertiaryRoadMaxLength",5,1,32,"Maximum tertiary street length."));
        primaryBridgeChance=source.decimal("primaryBridgeChance",primaryBridgeChance,0,1,"Chance for a complete planned primary-road bridge span.");
        primaryBridgeMaximumLength=source.integer("primaryBridgeMaximumLength",primaryBridgeMaximumLength,1,64,"Maximum primary-road water gap, in chunks. Both endpoints must be base-level city roads.");
        legacyBridgeMaximumLength=source.integer("legacyBridgeMaximumLength",legacyBridgeMaximumLength,1,1024,"Bounded search length for legacy-style bridges in modern worlds.");
        corridorMaximumLength=source.integer("corridorMaximumLength",corridorMaximumLength,1,1024,"Maximum complete cellar-corridor span in modern worlds. Longer candidates are rejected consistently along the whole span.");
        multiBuildings=source.flag("modernMultibuildings",multiBuildings,"Place complete variable-size multibuildings with bounded region planning.");
        scattered=source.flag("scatteredStructures",scattered,"Generate world-style scattered structures outside cities.");
        scatteredMultiplier=source.decimal("scatteredChanceMultiplier",scatteredMultiplier,0,100,"Multiplier for world-style scattered placement chance.");
        cityScale=source.decimal("cityPerlinScale",cityScale,.001F,10000,"Noise scale when cityChance is -1.");
        cityOffset=source.decimal("cityPerlinOffset",cityOffset,-100,100,"Noise offset when cityChance is -1.");
        cityInnerScale=source.decimal("cityPerlinInnerScale",cityInnerScale,0,100,"Noise amplitude when cityChance is -1.");
        spawnDistance1=source.integer("citySpawnDistance1",spawnDistance1,0,30000000,"Inner distance from world origin for city-density scaling.");
        spawnDistance2=source.integer("citySpawnDistance2",spawnDistance2,0,30000000,"Outer distance; zero disables density scaling.");
        spawnMultiplier1=source.decimal("citySpawnMultiplier1",spawnMultiplier1,0,100,"Inner city-density multiplier.");
        spawnMultiplier2=source.decimal("citySpawnMultiplier2",spawnMultiplier2,0,100,"Outer city-density multiplier.");
        if(spawnDistance2!=0&&spawnDistance2<=spawnDistance1)throw new IllegalArgumentException("citySpawnDistance2 must exceed citySpawnDistance1");
        surfaceRailStations=source.flag("surfaceRailStations",surfaceRailStations,"Permit surface stations when there is enough distance to descend safely.");
        railwayLevelOffset=source.integer("railwayLevelOffset",railwayLevelOffset,-8,2,"Railway elevation offset, in six-block levels.");
        railwaySpacingNorthSouth=source.integer("railwaySpacingNorthSouth",railwaySpacingNorthSouth,4,128,"Even north-south railway spacing in chunks.");
        railwaySpacingEastWest=source.integer("railwaySpacingEastWest",railwaySpacingEastWest,4,128,"Even east-west railway spacing in chunks.");
        highwaySettings=new HighwayPlannerSettings(
                source.integer("highwayPlanningCellSize",128,32,512,"Planning-cell size in chunks."),
                source.integer("highwayHubSampleSpacing",16,1,512,"Distance between candidate hub samples."),
                source.decimal("highwayHubMinimumPotential",.2F,0,1,"Minimum approximate city potential for a hub."),
                source.integer("highwayHubSearchRadiusCells",2,0,8,"Bounded connection search radius in cells."),
                source.integer("highwayMinimumHubDistance",64,0,4096,"Minimum hub separation in chunks."),
                source.integer("highwayMaximumHubDistance",320,0,4096,"Maximum hub separation in chunks."),
                source.integer("highwayMaximumConnectionsPerHub",2,1,8,"Maximum reciprocal neighbor selections per hub."),
                source.integer("highwayMinimumRouteLength",40,0,4096,"Minimum route length in chunks."),
                source.decimal("highwayRouteCityPenalty",1,0,1000,"Penalty for crossing unrelated city terrain."),
                source.integer("highwayLevelFromCities",3,0,4,"0: first endpoint, 1: lower, 2: higher, 3: average, 4: fixed level."),
                source.integer("highwayNetworkLevel",0,0,32,"Fixed highway level above profile ground."),railwaySpacingNorthSouth,railwaySpacingEastWest);
    }
}
