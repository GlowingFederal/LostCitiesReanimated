package mcjty.lostcities.compat;

import mcjty.lostcities.materials.MaterialResolver;
import mcjty.lostcities.worldgen.highway.*;
import java.util.Optional;
import mcjty.lostcities.legacy.LegacyState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.legacy.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/** One set of registry-name decisions per dimension, never numerical runtime block IDs. */
public final class MaterialWorldData extends WorldSavedData implements HighwayHubPersistence {
    private final Map<HubKey,Optional<HighwayHub>> hubs=new TreeMap<>();
    @Override public Lookup get(HubKey key){return hubs.containsKey(key)?Lookup.known(hubs.get(key)):Lookup.missing();}
    @Override public void put(HubKey key,Optional<HighwayHub> hub){hubs.put(key,hub);markDirty();}
    private final Map<Long,NBTTagCompound> pending=new TreeMap<>();
    private static long position(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public void capturePending(BuildingInfo info){
        NBTTagCompound work=new NBTTagCompound();work.setInteger("X",info.chunkX);work.setInteger("Z",info.chunkZ);
        work.setTag("Loot",conditions(info.getLootTodo()));work.setTag("Spawners",conditions(info.getMobSpawnerTodo()));
        work.setTag("Trees",positions(info.getSaplingTodo()));work.setTag("Lighting",positions(info.getLightingUpdateTodo()));
        NBTTagList tags=new NBTTagList();
        for(BuildingInfo.TagTodo todo:info.getTagTodo()){
            NBTTagCompound entry=coordinates(todo.position);entry.setString("Block",resolver().sourceFor(mcjty.lostcities.legacy.LegacyStates.IDS.getByValue(todo.block)));entry.setString("Tag",todo.tag.toString());tags.appendTag(entry);
        }
        work.setTag("Tags",tags);long key=position(info.chunkX,info.chunkZ);
        if(info.getLootTodo().isEmpty()&&info.getMobSpawnerTodo().isEmpty()&&info.getSaplingTodo().isEmpty()&&info.getLightingUpdateTodo().isEmpty()&&tags.tagCount()==0)pending.remove(key);else pending.put(key,work);info.pendingSaved();markDirty();
    }
    public void restorePending(BuildingInfo info){
        NBTTagCompound work=pending.get(position(info.chunkX,info.chunkZ));if(work==null){info.pendingSaved();return;}
        restoreConditions(work.getTagList("Loot",10),info,false);restoreConditions(work.getTagList("Spawners",10),info,true);
        NBTTagList trees=work.getTagList("Trees",10);for(int i=0;i<trees.tagCount();i++)info.addSaplingTodo(coordinates(trees.getCompoundTagAt(i)));
        NBTTagList lighting=work.getTagList("Lighting",10);for(int i=0;i<lighting.tagCount();i++)info.addLightingUpdateTodo(coordinates(lighting.getCompoundTagAt(i)));
        NBTTagList tags=work.getTagList("Tags",10);for(int i=0;i<tags.tagCount();i++){NBTTagCompound entry=tags.getCompoundTagAt(i);info.addTagTodo(coordinates(entry),(char)mcjty.lostcities.legacy.LegacyStates.IDS.get(resolver().resolve(entry.getString("Block"))),new com.google.gson.JsonParser().parse(entry.getString("Tag")).getAsJsonObject());}
        info.pendingSaved();
    }
    private static void restoreConditions(NBTTagList entries,BuildingInfo info,boolean spawners){
        for(int i=0;i<entries.tagCount();i++){NBTTagCompound entry=entries.getCompoundTagAt(i);BuildingInfo.ConditionTodo todo=entry.hasKey("Condition")?new BuildingInfo.ConditionTodo(entry.getString("Condition"),entry.getString("Part"),entry.getString("Building"),readPlacement(entry)):null;
            if(spawners)info.addSpawnerTodo(coordinates(entry),todo);else info.addLootTodo(coordinates(entry),todo);
        }
    }
    private static NBTTagList conditions(List<Pair<BlockPos,BuildingInfo.ConditionTodo>> entries){
        NBTTagList list=new NBTTagList();for(Pair<BlockPos,BuildingInfo.ConditionTodo> value:entries){NBTTagCompound entry=coordinates(value.getKey());BuildingInfo.ConditionTodo todo=value.getValue();if(todo!=null){entry.setString("Condition",todo.getCondition());entry.setString("Part",todo.getPart());entry.setString("Building",todo.getBuilding());writePlacement(entry,todo.placement);}list.appendTag(entry);}return list;
    }
    private static void writePlacement(NBTTagCompound entry,mcjty.lostcities.core.PlacementContext context){
        if(context==null)return;NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Level",context.level);tag.setInteger("Floor",context.floor);tag.setInteger("Cellars",context.cellars);tag.setInteger("Floors",context.floors);tag.setInteger("ChunkX",context.chunkX);tag.setInteger("ChunkZ",context.chunkZ);tag.setString("BelowPart",context.belowPart);entry.setTag("Placement",tag);
    }
    private static mcjty.lostcities.core.PlacementContext readPlacement(NBTTagCompound entry){
        if(!entry.hasKey("Placement"))return null;NBTTagCompound tag=entry.getCompoundTag("Placement");return new mcjty.lostcities.core.PlacementContext(tag.getInteger("Level"),tag.getInteger("Floor"),tag.getInteger("Cellars"),tag.getInteger("Floors"),tag.getInteger("ChunkX"),tag.getInteger("ChunkZ"),tag.getString("BelowPart"));
    }
    private static NBTTagList positions(List<BlockPos> entries){NBTTagList list=new NBTTagList();for(BlockPos position:entries)list.appendTag(coordinates(position));return list;}
    private static NBTTagCompound coordinates(BlockPos position){NBTTagCompound tag=new NBTTagCompound();tag.setInteger("x",position.getX());tag.setInteger("y",position.getY());tag.setInteger("z",position.getZ());return tag;}
    private static BlockPos coordinates(NBTTagCompound tag){return new BlockPos(tag.getInteger("x"),tag.getInteger("y"),tag.getInteger("z"));}
    private final Map<String, String> choices = new TreeMap<>();
    private MaterialResolver<LegacyState> resolver;
    private ChunkMaterialRecovery recovery;
    public ChunkMaterialRecovery recovery(){resolver();if(recovery==null)recovery=new ChunkMaterialRecovery(this);return recovery;}
    public boolean modern;
    public MaterialWorldData(String name) { super(name); }

    public static MaterialWorldData get(World world) {
        String name = "lostcities_materials_" + world.provider.dimensionId;
        MaterialWorldData data = (MaterialWorldData) world.perWorldStorage.loadData(MaterialWorldData.class, name);
        if (data == null) {
            data = new MaterialWorldData(name);
            data.modern = mcjty.lostcities.core.GenerationMode.modern(world.getSaveHandler().getWorldDirectory(),world.provider.getSaveFolder());
            world.perWorldStorage.setData(name, data);
            data.markDirty();
        }
        return data;
    }
    public MaterialResolver<LegacyState> resolver() {
        if (resolver == null) { resolver = LegacyMaterials.create(); resolver.restore(choices); if(modern){mcjty.lostcities.assets.ModernAssetPack.validateMaterials(resolver::resolve);recovery=new ChunkMaterialRecovery(this);} }
        return resolver;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        modern = nbt.getBoolean("Modern");
        choices.clear();
        NBTTagList list = nbt.getTagList("Materials", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            choices.put(entry.getString("Source"), entry.getString("State"));
        }
        hubs.clear();NBTTagList hubList=nbt.getTagList("HighwayHubs",10);
        for(int i=0;i<hubList.tagCount();i++){
            NBTTagCompound entry=hubList.getCompoundTagAt(i);HubKey key=new HubKey(entry.getInteger("CellX"),entry.getInteger("CellZ"));
            hubs.put(key,entry.getBoolean("Present")?Optional.of(new HighwayHub(key,entry.getInteger("ChunkX"),entry.getInteger("ChunkZ"),entry.getInteger("Potential"),entry.getInteger("Level"))):Optional.empty());
        }
        pending.clear();NBTTagList deferred=nbt.getTagList("PendingPopulation",10);
        for(int i=0;i<deferred.tagCount();i++){NBTTagCompound work=deferred.getCompoundTagAt(i);pending.put(position(work.getInteger("X"),work.getInteger("Z")),work);}
        resolver = null;recovery=null;
    }
    @Override public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Version", 4);
        nbt.setBoolean("Modern", modern);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, String> entry : (resolver == null ? choices : resolver.decisions()).entrySet()) {
            NBTTagCompound value = new NBTTagCompound();
            value.setString("Source", entry.getKey()); value.setString("State", entry.getValue());
            list.appendTag(value);
        }
        nbt.setTag("Materials", list);
        NBTTagList hubList=new NBTTagList();
        for(Map.Entry<HubKey,Optional<HighwayHub>> cell:hubs.entrySet()){
            NBTTagCompound entry=new NBTTagCompound();entry.setInteger("CellX",cell.getKey().planningCellX());entry.setInteger("CellZ",cell.getKey().planningCellZ());entry.setBoolean("Present",cell.getValue().isPresent());
            if(cell.getValue().isPresent()){HighwayHub hub=cell.getValue().get();entry.setInteger("ChunkX",hub.chunkX());entry.setInteger("ChunkZ",hub.chunkZ());entry.setInteger("Potential",hub.potentialScore());entry.setInteger("Level",hub.cityLevel());}hubList.appendTag(entry);
        }
        nbt.setTag("HighwayHubs",hubList);
        NBTTagList deferred=new NBTTagList();for(NBTTagCompound work:pending.values())deferred.appendTag(work);nbt.setTag("PendingPopulation",deferred);

    }
}
