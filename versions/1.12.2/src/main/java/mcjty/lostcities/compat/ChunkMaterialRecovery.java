package mcjty.lostcities.compat;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import mcjty.lostcities.dimensions.world.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.IProperty;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import mcjty.lostcities.materials.*;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import java.util.*;

/** Sparse native fallback snapshots, restricted to optional materials actually selected by Lost Cities. */
public final class ChunkMaterialRecovery {
    private static final String KEY="LostCitiesMaterialRecovery";
    private final MaterialRecovery<IBlockState> materials;
    private final boolean[] providerIds=new boolean[4096];
    private boolean hasProviders;
    private final Map<IBlockState,String> fallbacks=new HashMap<>();
    ChunkMaterialRecovery(MaterialWorldData data){
        materials=LegacyMaterials.recovery(data.resolver().decisions());
        for(String selected:data.resolver().decisions().values()){BlockSpec spec=BlockSpec.parse(selected);if(materials.contains(spec.id)){Block block=ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(spec.id));if(block!=null){int id=Block.getIdFromBlock(block);if(id>=0&&id<providerIds.length){providerIds[id]=true;hasProviders=true;}}}}
        for(int id=0;id<providerIds.length;id++)if(providerIds[id]){Block block=Block.getBlockById(id);for(int meta=0;meta<16;meta++)fallback(block.getStateFromMeta(meta));}
    }
    private static String serialize(IBlockState state){return state.getBlock().getRegistryName()+"@"+state.getBlock().getMetaFromState(state);}
    private String fallback(IBlockState state){
        if(fallbacks.containsKey(state))return fallbacks.get(state);
        Map<String,String> properties=new TreeMap<>();Set<String> supported=new HashSet<>(Arrays.asList("facing","half","open","hinge","axis","powered","shape","hanging","face","lit"));
        for(Map.Entry<IProperty<?>,Comparable<?>> entry:state.getProperties().entrySet())if(supported.contains(entry.getKey().getName()))properties.put(entry.getKey().getName(),String.valueOf(entry.getValue()));
        if(state.getBlock() instanceof BlockSlab){properties.put("type",((BlockSlab)state.getBlock()).isDouble()?"double":properties.getOrDefault("half","bottom"));properties.remove("half");}
        boolean orientation=state.getBlock() instanceof BlockStairs||state.getBlock() instanceof BlockDoor||state.getBlock() instanceof BlockTrapDoor||state.getBlock() instanceof BlockFenceGate||state.getPropertyKeys().stream().anyMatch(p->p.getName().equals("facing")||p.getName().equals("axis")||p.getName().equals("hanging")||p.getName().equals("powered"));
        String result=materials.fallback(serialize(state),properties,orientation?0:state.getBlock() instanceof BlockSlab?7:15);fallbacks.put(state,result);return result;
    }
    private NBTTagCompound snapshot(Chunk chunk,NBTTagCompound level){
        Map<IBlockState,List<Integer>> groups=new LinkedHashMap<>();Set<Integer> positions=new HashSet<>();
        NBTTagList sections=level.getTagList("Sections",10);
        if(hasProviders)for(int sectionIndex=0;sectionIndex<sections.tagCount();sectionIndex++){
            NBTTagCompound section=sections.getCompoundTagAt(sectionIndex);byte[] ids=section.getByteArray("Blocks"),metadata=section.getByteArray("Data"),extra=section.getByteArray("Add");int base=section.getByte("Y")<<12;
            if(ids.length!=4096||metadata.length!=2048)throw new IllegalArgumentException("Invalid legacy section arrays");
            for(int index=0;index<4096;index++){
                int shift=(index&1)*4,id=(ids[index]&255)|(extra.length==2048?((extra[index>>1]>>shift)&15)<<8:0);if(!providerIds[id])continue;
                int meta=(metadata[index>>1]>>shift)&15;IBlockState state=Block.getBlockById(id).getStateFromMeta(meta);
                if(fallback(state)==null)continue;int position=base|index;groups.computeIfAbsent(state,k->new ArrayList<>()).add(position);positions.add(position);
            }
        }
        NBTTagCompound result=new NBTTagCompound();result.setInteger("Version",1);NBTTagList palette=new NBTTagList();
        for(Map.Entry<IBlockState,List<Integer>> group:groups.entrySet()){NBTTagCompound entry=new NBTTagCompound();entry.setString("Original",serialize(group.getKey()));entry.setString("Fallback",fallback(group.getKey()));entry.setIntArray("Positions",group.getValue().stream().mapToInt(Integer::intValue).toArray());palette.appendTag(entry);}result.setTag("Palette",palette);
        NBTTagList tiles=new NBTTagList(),original=level.getTagList("TileEntities",10);for(int i=0;i<original.tagCount();i++){NBTTagCompound tile=original.getCompoundTagAt(i);int y=tile.getInteger("y");if(y>=0&&y<256&&positions.contains((y<<8)|((tile.getInteger("z")&15)<<4)|(tile.getInteger("x")&15)))tiles.appendTag(tile.copy());}result.setTag("Tiles",tiles);return result;
    }
    private static void restore(World world,Chunk chunk,NBTTagCompound snapshot){
        if(snapshot.getInteger("Version")!=1)return;Set<Integer> restored=new HashSet<>();NBTTagList palette=snapshot.getTagList("Palette",10);
        for(int i=0;i<palette.tagCount();i++){NBTTagCompound entry=palette.getCompoundTagAt(i);BlockSpec original=BlockSpec.parse(entry.getString("Original"));
            if(original.id.startsWith("minecraft:")||LegacyMaterials.nativeState(original)!=null)continue;
            BlockSpec replacement=BlockSpec.parse(entry.getString("Fallback"));if(!replacement.id.startsWith("minecraft:"))continue;IBlockState state=LegacyMaterials.nativeState(replacement);if(state==null||state.getBlock()==Blocks.AIR)throw new IllegalStateException("Unavailable native recovery state "+replacement);
            for(int position:entry.getIntArray("Positions")){if(position<0||position>65535)throw new IllegalArgumentException("Invalid recovery position");int x=position&15,z=(position>>4)&15,y=position>>8;ExtendedBlockStorage section=chunk.getBlockStorageArray()[y>>4];
                if(section!=null&&!LegacyMaterials.isMissing(section.get(x,y&15,z).getBlock()))continue;
                if(section==null)chunk.getBlockStorageArray()[y>>4]=section=new ExtendedBlockStorage(y&~15,world.provider.hasSkyLight());
                IBlockState cell=state;if(state.getBlock()==Blocks.CHEST&&((x+z)&1)!=0)cell=Blocks.TRAPPED_CHEST.getStateFromMeta(state.getBlock().getMetaFromState(state));section.set(x,y&15,z,cell);TileEntity old=(TileEntity)chunk.getTileEntityMap().remove(new BlockPos(chunk.x*16+x,y,chunk.z*16+z));if(old!=null)old.invalidate();restored.add(position);
            }
        }
        NBTTagList tiles=snapshot.getTagList("Tiles",10);for(int i=0;i<tiles.tagCount();i++){NBTTagCompound tag=tiles.getCompoundTagAt(i);int x=tag.getInteger("x")&15,z=tag.getInteger("z")&15,y=tag.getInteger("y");if(y<0||y>255||!restored.contains((y<<8)|(z<<4)|x))continue;
            ExtendedBlockStorage section=chunk.getBlockStorageArray()[y>>4];Block block=section.get(x,y&15,z).getBlock();TileEntity tile=block.createTileEntity(world,section.get(x,y&15,z));if(tile!=null){tile.readFromNBT(tag);chunk.addTileEntity(tile);}
        }
        if(!restored.isEmpty()){chunk.generateSkylightMap();chunk.markDirty();mcjty.lostcities.LostCities.setup.getLogger().info("Recovered {} optional material cells in city chunk {},{}",restored.size(),chunk.x,chunk.z);}
    }
    public static final class Events {
        @SubscribeEvent public void save(ChunkDataEvent.Save event){World world=event.getWorld();if(world.isRemote)return;LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(world);if(generator==null||!generator.modernMode())return;NBTTagCompound level=event.getData().getCompoundTag("Level");level.setTag(KEY,generator.materialData().recovery().snapshot(event.getChunk(),level));}
        @SubscribeEvent public void load(ChunkDataEvent.Load event){if(event.getWorld().isRemote)return;NBTTagCompound level=event.getData().getCompoundTag("Level");if(level.hasKey(KEY,10))restore(event.getWorld(),event.getChunk(),level.getCompoundTag(KEY));else{LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(event.getWorld());if(generator!=null&&generator.modernMode()&&generator.materialData().recovery().hasProviders)event.getChunk().markDirty();}}
    }
}
