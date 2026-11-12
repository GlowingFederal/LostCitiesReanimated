package mcjty.lostcities.compat;

import com.google.gson.*;
import mcjty.lostcities.assets.*;
import mcjty.lostcities.core.*;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.dimensions.world.lost.cityassets.*;
import mcjty.lostcities.worldgen.highway.*;
import mcjty.lostcities.worldgen.street.*;
import java.util.*;

/** Version-owned boundary between pure modern planners and native legacy generation. */
public final class ModernGeneration {
    private final LostCityChunkGenerator provider;
    public final ModernCityDistribution cities;
    public final HierarchicalStreetPlanner streets;
    public final IntercityHighwayPlanner highways;
    public final ScatteredPlanner scattered;
    public final RailwayPlanner railway;
    public final PrimaryBridgePlanner bridges;
    private final Map<Long,MultiBuildingPlanner.Placement> predefinedMulti=new HashMap<>();
    private final MultiBuildingPlanner multi;

    public ModernGeneration(final LostCityChunkGenerator provider){
        this.provider=provider;
        cities=new ModernCityDistribution(provider.seed,provider.getProfile().CITY_CHANCE,provider.getProfile().CITY_MINRADIUS,provider.getProfile().CITY_MAXRADIUS,provider.getProfile().modern,(x,z,factor)->BiomeSupport.modify(provider,x,z,factor),BiomeSupport.maximumMultiplier(provider));
        streets=new HierarchicalStreetPlanner(provider.seed,"legacy:"+provider.dimensionId,provider.getProfile().modern.streetSettings);
        bridges=new PrimaryBridgePlanner(provider.seed,"legacy:"+provider.dimensionId,streets,provider.getProfile().modern.primaryBridgeMaximumLength,provider.getProfile().modern.primaryBridgeChance,new PrimaryBridgePlanner.Environment(){
            public boolean city(int x,int z){return BuildingInfo.isCityRaw(x,z,provider,BuildingInfo.getProfile(x,z,provider));}
            public boolean water(int x,int z){return BiomeSupport.matches(BiomeInfo.getBiomeInfo(provider,new mcjty.lostcities.varia.ChunkCoord(provider.dimensionId,x,z)).getBiomes(),new JsonPrimitive("#minecraft:is_water"))||provider.planningHeight(x,z)<provider.getProfile().GROUNDLEVEL-provider.getProfile().WATERLEVEL_OFFSET;}
            public boolean endpoint(int x,int z){BuildingInfo info=BuildingInfo.getBuildingInfo(x,z,provider);return info.isCity&&!info.hasBuilding&&info.isPrimaryRoad()&&info.cityLevel==0;}
        });
        highways=new IntercityHighwayPlanner(provider.seed,"legacy:"+provider.dimensionId,provider.getProfile().modern.highwaySettings,cities,(x,z)->BuildingInfo.getCityLevel(x,z,provider),provider.materialData());
        RailwayPlanner.Settings rails=new RailwayPlanner.Settings();
        rails.offset=provider.getProfile().modern.railwayLevelOffset;rails.spacingX=provider.getProfile().modern.railwaySpacingEastWest;rails.spacingZ=provider.getProfile().modern.railwaySpacingNorthSouth;
        rails.canEnd=provider.getProfile().RAILWAYS_CAN_END;rails.stations=provider.getProfile().RAILWAY_STATIONS_ENABLED;rails.tracks=provider.getProfile().RAILWAYS_ENABLED;rails.surfaceStations=provider.getProfile().modern.surfaceRailStations;
        railway=new RailwayPlanner(provider.seed,rails,new RailwayPlanner.Environment(){
            public boolean city(int x,int z){return BuildingInfo.isCityRaw(x,z,provider,BuildingInfo.getProfile(x,z,provider));}
            public int cityLevel(int x,int z){return BuildingInfo.getCityLevel(x,z,provider);}
            public int highwayX(int x,int z){return Highway.getXHighwayLevel(x,z,provider,provider.getProfile());}
            public int highwayZ(int x,int z){return Highway.getZHighwayLevel(x,z,provider,provider.getProfile());}
            public boolean excluded(int x,int z){return provider.getProfile().isSphereLandscape()&&CitySphere.onCitySphereBorder(x,z,provider);}
            public String part(String key,Random random,String fallback){return ModernGeneration.this.part("railways",key,random,fallback);}
        });
        JsonObject world=ModernAssetPack.definition("worldstyles",provider.getProfile().getWorldStyle());
        JsonObject scatterSettings=world!=null&&world.has("scattered")?world.getAsJsonObject("scattered"):new JsonObject();
        scattered=new ScatteredPlanner(provider.seed,scatterSettings,provider.getProfile().modern.scatteredMultiplier,new ScatteredPlanner.Environment(){
            public boolean biome(int x,int z,JsonElement matcher){return BiomeSupport.matches(BiomeInfo.getBiomeInfo(provider,new mcjty.lostcities.varia.ChunkCoord(provider.dimensionId,x,z)).getBiomes(),matcher);}
            public boolean occupied(int x,int z){
                if(BuildingInfo.isCityRaw(x,z,provider,provider.getProfile()))return true;
                if(Highway.getXHighwayLevel(x,z,provider,provider.getProfile())>=0||Highway.getZHighwayLevel(x,z,provider,provider.getProfile())>=0)return true;
                if(railway.at(x,z).getType()!=mcjty.lostcities.api.RailChunkType.NONE)return true;
                BuildingInfo info=BuildingInfo.getBuildingInfo(x,z,provider);
                return info.hasBridge(provider,Orientation.X)!=null||info.hasBridge(provider,Orientation.Z)!=null;
            }
            public ScatteredPlanner.Terrain terrain(int x,int z){
                mcjty.lostcities.dimensions.world.ChunkHeightmap map=provider.getHeightmap(x,z);int minimum=255,maximum=0,sum=0;
                for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++){int h=map.getHeight(dx,dz);minimum=Math.min(minimum,h);maximum=Math.max(maximum,h);sum+=h;}
                return new ScatteredPlanner.Terrain(minimum,maximum,sum/256);
            }
            public Integer highway(int x,int z,int axis){
                int level=axis==0?Highway.getXHighwayLevel(x,z,provider,provider.getProfile()):Highway.getZHighwayLevel(x,z,provider,provider.getProfile());
                if(level<0)return null;BuildingInfo info=BuildingInfo.getBuildingInfo(x,z,provider);return info.isTunnel(level)?null:info.groundLevel+6*level;
            }
            public int seaLevel(){return provider.getProfile().GROUNDLEVEL-provider.getProfile().WATERLEVEL_OFFSET;}
            public boolean voidTerrain(){return !provider.getProfile().isDefault()&&!provider.getProfile().isCavern();}
        });
        JsonObject settings=world!=null&&world.has("multisettings")?world.getAsJsonObject("multisettings"):new JsonObject();
        for(PredefinedCity city:AssetRegistries.PREDEFINED_CITIES.getIterable())if(city.getDimension()==provider.dimensionId)for(PredefinedCity.PredefinedBuilding entry:city.getPredefinedBuildings())if(entry.isMulti()){
            MultiBuilding layout=AssetRegistries.MULTI_BUILDINGS.get(entry.getBuilding());if(layout==null)throw new IllegalArgumentException("Missing predefined multibuilding "+entry.getBuilding());
            int x=city.getChunkX()+entry.getRelChunkX(),z=city.getChunkZ()+entry.getRelChunkZ();MultiBuildingPlanner.Placement placement=new MultiBuildingPlanner.Placement(new MultiBuildingPlanner.Template(layout.getName(),layout.getDimX(),layout.getDimZ()),x,z);
            for(int dx=0;dx<layout.getDimX();dx++)for(int dz=0;dz<layout.getDimZ();dz++)if(predefinedMulti.put(coordinate(x+dx,z+dz),placement)!=null)throw new IllegalArgumentException("Overlapping predefined multibuildings");
        }
        multi=new MultiBuildingPlanner(new MultiBuildingPlanner.Environment(){
            public String style(int x,int z){return City.getCityStyle(x,z,provider,provider.getProfile()).getName();}
            public MultiBuildingPlanner.Template select(String style,int x,int z,Random random){String name=AssetRegistries.CITYSTYLES.get(style).getRandomMultiBuilding(random,x,z);MultiBuilding building=AssetRegistries.MULTI_BUILDINGS.get(name);return building==null?null:new MultiBuildingPlanner.Template(building.getName(),building.getDimX(),building.getDimZ());}
            public boolean valid(int x,int z,MultiBuildingPlanner.Template template,String style){
                if(predefinedMulti.containsKey(coordinate(x,z)))return false;
                if(!BuildingInfo.isCityRaw(x,z,provider,provider.getProfile())||City.getPredefinedBuilding(x,z,provider)!=null||City.getPredefinedStreet(x,z,provider)!=null)return false;
                if(provider.getProfile().modern.streets&&streets.getRoadType(x,z)==PlannedRoadType.PRIMARY)return false;
                if(Highway.getXHighwayLevel(x,z,provider,provider.getProfile())>=0||Highway.getZHighwayLevel(x,z,provider,provider.getProfile())>=0)return false;
                Railway.RailChunkInfo rail=Railway.getRailChunkType(x,z,provider,provider.getProfile());
                return !rail.getType().isSurface()&&!rail.getType().isStation();
            }
        },integer(settings,"areasize",10),integer(settings,"minimum",1),integer(settings,"maximum",5),integer(settings,"attempts",50),decimal(settings,"correctstylefactor",.8F));
    }
    public String part(String group,String key,Random random,String fallback){
        JsonObject world=ModernAssetPack.definition("worldstyles",provider.getProfile().getWorldStyle());
        if(world!=null&&world.has("parts")){JsonObject parts=world.getAsJsonObject("parts");if(parts.has(group)&&parts.getAsJsonObject(group).has(key))return choose(parts.getAsJsonObject(group).get(key),random);}
        return fallback;
    }
    public BuildingPart bridgePart(BuildingInfo info,Orientation orientation){
        int axis=orientation==Orientation.X?0:1;PrimaryBridgePlanner.Span span=bridges.at(info.chunkX,info.chunkZ,axis);if(span==null)return null;
        BuildingInfo start=BuildingInfo.getBuildingInfo(span.minX,span.minZ,provider);String name=start.getCityStyle().select("largebridges",new Random(span.id),span.minX,span.minZ);
        if(name==null)name="bridge_large_open";BuildingPart part=AssetRegistries.PARTS.get(name);if(part==null)throw new IllegalArgumentException("Missing primary bridge part "+name);return part;
    }
    public String transportPart(String name,Random random){
        if(name.startsWith("highway_"))return part("highways",name.substring(8),random,name);
        if(name.startsWith("station_")||name.startsWith("rails_"))return part("railways",name.replace("_",""),random,name);
        if(name.startsWith("monorails_"))return part("monorails",name.substring(10),random,name);
        return name;
    }
    public String streetPart(BuildingInfo info,String key,Random random){
        PlannedRoadType type=info.plannedRoadType;
        String family=type==PlannedRoadType.PRIMARY?"largeparts":type==PlannedRoadType.TERTIARY?"tertiaryparts":"parts";
        JsonElement settings=info.getCityStyle().setting("streetblocks",family);
        if(settings==null&&type==PlannedRoadType.TERTIARY)settings=info.getCityStyle().setting("streetblocks","parts");
        if(settings!=null&&settings.getAsJsonObject().has(key))return choose(settings.getAsJsonObject().get(key),random);
        return (type==PlannedRoadType.PRIMARY?"street_large_":"street_")+key;
    }
    private static String choose(JsonElement value,Random random){if(value.isJsonPrimitive())return value.getAsString();JsonArray list=value.getAsJsonArray();return list.size()==0?null:list.get(random==null?0:random.nextInt(list.size())).getAsString();}
    private static long coordinate(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public MultiBuildingPlanner.Placement predefinedMultiAt(int x,int z){return predefinedMulti.get(coordinate(x,z));}
    public MultiBuildingPlanner.Placement multiAt(int x,int z){MultiBuildingPlanner.Placement predefined=predefinedMultiAt(x,z);return predefined!=null?predefined:provider.getProfile().modern.multiBuildings?multi.at(x,z):null;}
    public boolean roadAt(int x,int z){return provider.getProfile().modern.streets&&streets.getRoadType(x,z)!=PlannedRoadType.NONE;}
    private static int integer(JsonObject object,String key,int fallback){return object.has(key)?object.get(key).getAsInt():fallback;}
    private static float decimal(JsonObject object,String key,float fallback){return object.has(key)?object.get(key).getAsFloat():fallback;}
}
