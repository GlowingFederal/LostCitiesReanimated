package mcjty.lostcities;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
@Mod(modid="lostcities", name="Lost Cities Reanimated", version=BuildVersion.VERSION, acceptedMinecraftVersions="[1.7.10]")
public final class LostCities {
    public static final LostCitiesImp lostCitiesImp=new LostCitiesImp();
    public static final java.util.Map<Integer,String> dimensionProfiles=new java.util.HashMap<>();
    @cpw.mods.fml.common.SidedProxy(clientSide="mcjty.lostcities.client.ClientProxy",serverSide="mcjty.lostcities.setup.CommonProxy")
    public static mcjty.lostcities.setup.CommonProxy proxy;
    public static final String MODID="lostcities";
    public static final mcjty.lostcities.setup.ModSetup setup=new mcjty.lostcities.setup.ModSetup();
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) { setup.preInit(event); }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        mcjty.lostcities.network.ProfileNetwork.init();proxy.init();
        new mcjty.lostcities.dimensions.world.LostWorldType("lostcities",false);
        if(mcjty.lostcities.setup.ModSetup.biomesoplenty)new mcjty.lostcities.dimensions.world.LostWorldType("lostcities_bop",true);
        for(String type:mcjty.lostcities.config.LostCityConfiguration.ADAPTING_WORLDTYPES)new mcjty.lostcities.dimensions.world.LostWorldTypeAdapter(type);
        int id=mcjty.lostcities.config.LostCityConfiguration.DIMENSION_ID;
        if(id!=-1){net.minecraftforge.common.DimensionManager.registerProviderType(id,mcjty.lostcities.dimensions.world.LostWorldProvider.class,false);net.minecraftforge.common.DimensionManager.registerDimension(id,id);dimensionProfiles.put(id,mcjty.lostcities.config.LostCityConfiguration.DIMENSION_PROFILE); }
        for(String configured:mcjty.lostcities.config.LostCityConfiguration.ADDITIONAL_DIMENSIONS){
            String[] entry=configured.split(":",2);if(entry.length!=2)throw new IllegalArgumentException("Expected dimensionId:profile: "+configured);
            int dimension=Integer.parseInt(entry[0]);if(!mcjty.lostcities.config.LostCityConfiguration.profiles.containsKey(entry[1]))throw new IllegalArgumentException("Unknown dimension profile "+entry[1]);
            net.minecraftforge.common.DimensionManager.registerProviderType(dimension,mcjty.lostcities.dimensions.world.LostWorldProvider.class,false);net.minecraftforge.common.DimensionManager.registerDimension(dimension,dimension);dimensionProfiles.put(dimension,entry[1]);
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new mcjty.lostcities.compat.ChunkMaterialRecovery.Events());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new mcjty.lostcities.compat.TerrainOverlay());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new mcjty.lostcities.compat.NativeLootAssets());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new mcjty.lostcities.legacy.BedTravel());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new mcjty.lostcities.legacy.SpawnSelection());
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(new mcjty.lostcities.legacy.TerrainEvents());
    }
    @Mod.EventHandler public void postInit(cpw.mods.fml.common.event.FMLPostInitializationEvent event){setup.loadAssets();}
    @Mod.EventHandler public void aboutToStart(cpw.mods.fml.common.event.FMLServerAboutToStartEvent event){clean();}
    @Mod.EventHandler public void starting(cpw.mods.fml.common.event.FMLServerStartingEvent event){event.registerServerCommand(new mcjty.lostcities.legacy.CityCommand());for(String command:new String[]{"lc_debug","lc_buildpart","lc_exportpart","lc_exportbuilding"})event.registerServerCommand(new mcjty.lostcities.legacy.CityCommand(command));}
    @Mod.EventHandler public void stopped(cpw.mods.fml.common.event.FMLServerStoppedEvent event){clean();}
    @Mod.EventHandler public void interMod(cpw.mods.fml.common.event.FMLInterModComms.IMCEvent event){
        for(cpw.mods.fml.common.event.FMLInterModComms.IMCMessage message:event.getMessages())if(message.key.equalsIgnoreCase("getLostCities")&&message.isStringMessage()){
            try {
                Object callback=Class.forName(message.getStringValue(),true,cpw.mods.fml.common.Loader.instance().getModClassLoader()).newInstance();
                if(callback instanceof java.util.function.Function)((java.util.function.Function<mcjty.lostcities.api.ILostCities,?>)callback).apply(lostCitiesImp);
                else if(callback instanceof com.google.common.base.Function)((com.google.common.base.Function<mcjty.lostcities.api.ILostCities,?>)callback).apply(lostCitiesImp);
                else throw new IllegalArgumentException("Callback must implement Function<ILostCities, ?>");
            }catch(ReflectiveOperationException|RuntimeException error){setup.getLogger().error("Cannot deliver Lost Cities API to "+message.getSender(),error);}
        }
    }
    public static void clean(){
        mcjty.lostcities.dimensions.world.lost.BuildingInfo.cleanCache();mcjty.lostcities.dimensions.world.lost.Highway.cleanCache();
        mcjty.lostcities.dimensions.world.lost.Railway.cleanCache();mcjty.lostcities.dimensions.world.lost.City.cleanCache();
        mcjty.lostcities.dimensions.world.lost.BiomeInfo.cleanCache();mcjty.lostcities.dimensions.world.lost.CitySphere.cleanCache();
        mcjty.lostcities.dimensions.world.WorldTypeTools.clear();mcjty.lostcities.network.ProfileNetwork.clear();
    }
}
