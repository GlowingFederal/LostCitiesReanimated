package mcjty.lostcities.setup;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.config.ConfigSetup;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import org.apache.logging.log4j.Logger;
import java.io.*;

public final class ModSetup {
    public static boolean chisel,biomesoplenty,neid,jeid;
    public static File modConfigDir;
    private Logger logger;
    public Logger getLogger(){return logger;}
    public void preInit(FMLPreInitializationEvent event){
        logger=event.getModLog();modConfigDir=event.getModConfigurationDirectory();
        chisel=Loader.isModLoaded("chisel");biomesoplenty=Loader.isModLoaded("BiomesOPlenty")||Loader.isModLoaded("biomesoplenty");
        ConfigSetup.init();
    }
    public void loadAssets(){
        AssetRegistries.reset();
        for(String path:LostCityConfiguration.ASSETS){
            if(path.startsWith("/"))try(InputStream in=LostCities.class.getResourceAsStream(path)){AssetRegistries.load(in,path);}catch(IOException error){throw new UncheckedIOException(error);}
            else if(path.startsWith("$"))AssetRegistries.load(new File(modConfigDir,path.substring(1)));
            else throw new IllegalArgumentException("Invalid asset library path "+path);
        }
        mcjty.lostcities.assets.ModernAssetPack.load(new File(modConfigDir,"lostcities/modern"), object -> {
            byte[] bytes=("["+object.toString()+"]").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            AssetRegistries.load(new java.io.ByteArrayInputStream(bytes),object.get("name").getAsString());
        }, message -> getLogger().info(message));
        mcjty.lostcities.assets.ModernAssetPack.validateReferences(AssetRegistries::exists);
        AssetRegistries.validateCatalogs();
        mcjty.lostcities.assets.LootAssetPack.validate(mcjty.lostcities.legacy.LegacyLoot::validate);
        mcjty.lostcities.assets.ModernAssetPack.validateMaterials(mcjty.lostcities.compat.LegacyMaterials.create()::resolve);
        ConfigSetup.postInit();
        logger.info("Loaded {} building parts",AssetRegistries.PARTS.getCount());
    }
}
