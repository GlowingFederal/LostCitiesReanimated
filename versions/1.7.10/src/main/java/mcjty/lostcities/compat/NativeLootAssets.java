package mcjty.lostcities.compat;

import mcjty.lostcities.assets.LootAssetPack;
import net.minecraft.world.*;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.google.gson.*;

/** Inventory contents are persisted natively; compiled custom definitions are retained with the world. */
public final class NativeLootAssets {
    private static final class Catalog {final Map<String,File> files;final Map<String,JsonObject> tables=new HashMap<>();Catalog(File world){files=LootAssetPack.files(world);}}
    private static final Map<World,Catalog> prepared=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void spawn(WorldEvent.CreateSpawnPosition event){prepare(event.world);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void load(WorldEvent.Load event){prepare(event.world);}
    @SubscribeEvent public void unload(WorldEvent.Unload event){prepared.remove(event.world);}
    public static void prepare(World world){
        if(world.isRemote||!(world instanceof WorldServer)||prepared.containsKey(world))return;
        LootAssetPack.materialize(world.getSaveHandler().getWorldDirectory());Catalog catalog=new Catalog(world.getSaveHandler().getWorldDirectory());
        LootAssetPack.validate((id,json)->{JsonObject saved=read(catalog.files.get(id),id);mcjty.lostcities.legacy.LegacyLoot.validate(id,saved);catalog.tables.put(id,saved);});prepared.put(world,catalog);
    }
    public static JsonObject definition(World world,String id){
        if(world==null)return LootAssetPack.definition(id);prepare(world);Catalog catalog=prepared.get(world);if(!catalog.files.containsKey(id))return null;
        JsonObject table=catalog.tables.get(id);if(table==null){table=read(catalog.files.get(id),id);mcjty.lostcities.legacy.LegacyLoot.validate(id,table);catalog.tables.put(id,table);}return table;
    }
    private static JsonObject read(File file,String id){if(file==null)throw new IllegalArgumentException("Missing saved loot table "+id);try(Reader reader=Files.newBufferedReader(file.toPath(),StandardCharsets.UTF_8)){return new JsonParser().parse(reader).getAsJsonObject();}catch(IOException error){throw new UncheckedIOException(error);}}
}
