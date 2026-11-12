package mcjty.lostcities.dimensions.world;
import mcjty.lostcities.config.*;
import net.minecraft.world.World;
import java.lang.ref.WeakReference;
import java.util.*;
public final class WorldTypeTools {
    private static final Map<Integer,WeakReference<LostCityChunkGenerator>> GENERATORS=new HashMap<>();
    public static void register(LostCityChunkGenerator generator){GENERATORS.put(generator.dimensionId,new WeakReference<>(generator));}
    public static LostCityChunkGenerator getChunkGenerator(int dimension){WeakReference<LostCityChunkGenerator> reference=GENERATORS.get(dimension);return reference==null?null:reference.get();}
    public static LostCityChunkGenerator getLostCityChunkGenerator(World world){LostCityChunkGenerator generator=getChunkGenerator(world.provider.dimensionId);return generator!=null&&generator.worldObj==world?generator:null;}
    public static LostCityProfile getProfile(World world){
        String overlay=LostCityConfiguration.OVERLAY_DIMENSIONS.get(world.provider.dimensionId);
        if(overlay!=null&&!(world.getWorldInfo().getTerrainType() instanceof LostWorldType)&&!mcjty.lostcities.LostCities.dimensionProfiles.containsKey(world.provider.dimensionId)){LostCityProfile configured=LostCityConfiguration.profiles.get(overlay);if(configured==null)throw new IllegalArgumentException("Unknown overlay profile "+overlay);return configured;}
        String name=LostCityConfiguration.DEFAULT_PROFILE;
        String options=world.getWorldInfo().getGeneratorOptions();
        if(options!=null&&!options.trim().isEmpty()){
            com.google.gson.JsonObject object=new com.google.gson.JsonParser().parse(options).getAsJsonObject();
            if(object.has("profile"))name=object.get("profile").getAsString();
        }
        if(mcjty.lostcities.LostCities.dimensionProfiles.containsKey(world.provider.dimensionId))name=mcjty.lostcities.LostCities.dimensionProfiles.get(world.provider.dimensionId);
        LostCityProfile profile=LostCityConfiguration.profiles.get(name);
        if(profile==null)throw new IllegalArgumentException("Unknown city profile "+name);
        return profile;
    }
    public static void clear(){GENERATORS.clear();}
    private WorldTypeTools(){}
}
