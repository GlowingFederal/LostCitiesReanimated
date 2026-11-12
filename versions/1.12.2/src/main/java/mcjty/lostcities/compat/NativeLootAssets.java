package mcjty.lostcities.compat;

import mcjty.lostcities.assets.LootAssetPack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.storage.loot.*;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import java.util.*;

/** Custom tables use the authoritative native manager, Forge loaders and deferred chest format. */
public final class NativeLootAssets {
    private static final Map<World,Set<String>> prepared=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void spawn(WorldEvent.CreateSpawnPosition event){prepare(event.getWorld());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void load(WorldEvent.Load event){prepare(event.getWorld());}
    @SubscribeEvent public void unload(WorldEvent.Unload event){prepared.remove(event.getWorld());}
    public static void prepare(World world){
        if(world.isRemote||!(world instanceof WorldServer)||prepared.containsKey(world))return;
        boolean changed=LootAssetPack.materialize(world.getSaveHandler().getWorldDirectory());
        LootTableManager manager=world.getLootTableManager();if(manager==null)return;
        if(changed)manager.reloadLootTables();
        List<String> errors=new ArrayList<>();
        LootAssetPack.validate((id,json)->{
            if(manager.getLootTableFromLocation(new ResourceLocation(id))==LootTable.EMPTY_LOOT_TABLE)errors.add(id);
            for(String reference:LootAssetPack.references(json))if(!reference.equals("minecraft:empty")&&manager.getLootTableFromLocation(new ResourceLocation(reference))==LootTable.EMPTY_LOOT_TABLE)errors.add(id+" references "+reference);
        });
        if(!errors.isEmpty())throw new IllegalArgumentException("Cannot load custom loot tables through the native Forge loot engine: "+errors);
        prepared.put(world,new HashSet<>(LootAssetPack.files(world.getSaveHandler().getWorldDirectory()).keySet()));
    }
    public static String table(World world,String requested){
        prepare(world);requested=new ResourceLocation(requested).toString();
        return prepared.get(world).contains(requested)?requested:mcjty.lostcities.materials.LootTableAliases.for112(requested);
    }
}
