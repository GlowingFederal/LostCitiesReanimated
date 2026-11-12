package mcjty.lostcities.compat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcjty.lostcities.dimensions.world.*;
import mcjty.lostcities.legacy.*;
import mcjty.lostcities.materials.*;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import java.util.*;

/** Sparse native fallback snapshots, restricted to optional materials actually selected by Lost Cities. */
public final class ChunkMaterialRecovery {
    private static final String KEY="LostCitiesMaterialRecovery";
    private final MaterialRecovery<LegacyState> materials;
    private final boolean[] providerIds=new boolean[4096];
    private boolean hasProviders;
    private final Map<LegacyState,String> fallbacks=new HashMap<>();
    ChunkMaterialRecovery(MaterialWorldData data){
        materials=LegacyMaterials.recovery(data.resolver().decisions());
        for(String selected:data.resolver().decisions().values()){BlockSpec spec=BlockSpec.parse(selected);if(materials.contains(spec.id)){Block block=LegacyRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(spec.id));if(block!=null){int id=Block.getIdFromBlock(block);if(id>=0&&id<providerIds.length){providerIds[id]=true;hasProviders=true;}}}}
        for(int id=0;id<providerIds.length;id++)if(providerIds[id]){Block block=Block.getBlockById(id);for(int meta=0;meta<16;meta++)fallback(LegacyStates.state(block,meta));}
    }
    private String fallback(LegacyState state){
        if(fallbacks.containsKey(state))return fallbacks.get(state);
        Block block=state.getBlock();int meta=state.meta;Map<String,String> properties=new TreeMap<>();
        String facing=LegacyStates.facing(state);if(facing!=null)properties.put("facing",facing);
        if(block instanceof BlockStairs)properties.put("half",(meta&4)!=0?"top":"bottom");
        if(block instanceof BlockSlab)properties.put("type",block.renderAsNormalBlock()?"double":(meta&8)!=0?"top":"bottom");
        if(block instanceof BlockTrapDoor){properties.put("half",(meta&8)!=0?"top":"bottom");properties.put("open",String.valueOf((meta&4)!=0));}
        if(block instanceof BlockDoor){properties.put("half",(meta&8)!=0?"upper":"lower");if((meta&8)!=0)properties.put("hinge",(meta&1)!=0?"right":"left");else properties.put("open",String.valueOf((meta&4)!=0));}
        boolean orientation=block instanceof BlockStairs||block instanceof BlockDoor||block instanceof BlockTrapDoor||block instanceof BlockFenceGate||LegacyStates.isUabBarrel(block)||block.getClass().getName().endsWith(".BlockLantern")||block.getClass().getName().endsWith(".BlockChain");
        String result=materials.fallback(state.toString(),properties,orientation?0:block instanceof BlockSlab?7:15);fallbacks.put(state,result);return result;
    }
    private NBTTagCompound snapshot(Chunk chunk,NBTTagCompound level){
        Map<LegacyState,List<Integer>> groups=new LinkedHashMap<>();Set<Integer> positions=new HashSet<>();
        NBTTagList sections=level.getTagList("Sections",10);
        if(hasProviders)for(int sectionIndex=0;sectionIndex<sections.tagCount();sectionIndex++){
            NBTTagCompound section=sections.getCompoundTagAt(sectionIndex);byte[] ids=section.getByteArray("Blocks"),metadata=section.getByteArray("Data"),extra=section.getByteArray("Add");int base=section.getByte("Y")<<12;
            if(ids.length!=4096||metadata.length!=2048)throw new IllegalArgumentException("Invalid legacy section arrays");
            for(int index=0;index<4096;index++){
                int shift=(index&1)*4,id=(ids[index]&255)|(extra.length==2048?((extra[index>>1]>>shift)&15)<<8:0);if(!providerIds[id])continue;
                int meta=(metadata[index>>1]>>shift)&15;LegacyState state=LegacyStates.state(Block.getBlockById(id),meta);
                if(fallback(state)==null)continue;int position=base|index;groups.computeIfAbsent(state,k->new ArrayList<>()).add(position);positions.add(position);
            }
        }
        NBTTagCompound result=new NBTTagCompound();result.setInteger("Version",1);NBTTagList palette=new NBTTagList();
        for(Map.Entry<LegacyState,List<Integer>> group:groups.entrySet()){NBTTagCompound entry=new NBTTagCompound();entry.setString("Original",group.getKey().toString());entry.setString("Fallback",fallback(group.getKey()));entry.setIntArray("Positions",group.getValue().stream().mapToInt(Integer::intValue).toArray());palette.appendTag(entry);}result.setTag("Palette",palette);
        NBTTagList tiles=new NBTTagList(),original=level.getTagList("TileEntities",10);for(int i=0;i<original.tagCount();i++){NBTTagCompound tile=original.getCompoundTagAt(i);int y=tile.getInteger("y");if(y>=0&&y<256&&positions.contains((y<<8)|((tile.getInteger("z")&15)<<4)|(tile.getInteger("x")&15)))tiles.appendTag(tile.copy());}result.setTag("Tiles",tiles);return result;
    }
    private static void restore(World world,Chunk chunk,NBTTagCompound snapshot){
        if(snapshot.getInteger("Version")!=1)return;Set<Integer> restored=new HashSet<>();NBTTagList palette=snapshot.getTagList("Palette",10);
        for(int i=0;i<palette.tagCount();i++){NBTTagCompound entry=palette.getCompoundTagAt(i);BlockSpec original=BlockSpec.parse(entry.getString("Original"));
            if(original.id.startsWith("minecraft:")||LegacyRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(original.id))!=null)continue;
            BlockSpec replacement=BlockSpec.parse(entry.getString("Fallback"));if(!replacement.id.startsWith("minecraft:"))continue;LegacyState state=LegacyMaterials.nativeState(replacement);if(state==null||state.getBlock()==Blocks.air)throw new IllegalStateException("Unavailable native recovery state "+replacement);
            for(int position:entry.getIntArray("Positions")){if(position<0||position>65535)throw new IllegalArgumentException("Invalid recovery position");int x=position&15,z=(position>>4)&15,y=position>>8;ExtendedBlockStorage section=chunk.getBlockStorageArray()[y>>4];
                if(section!=null&&section.getBlockByExtId(x,y&15,z)!=Blocks.air)continue;
                if(section==null)chunk.getBlockStorageArray()[y>>4]=section=new ExtendedBlockStorage(y&~15,!world.provider.hasNoSky);
                LegacyState cell=state;if(state.getBlock()==Blocks.chest&&((x+z)&1)!=0)cell=LegacyStates.state(Blocks.trapped_chest,state.meta);section.func_150818_a(x,y&15,z,cell.getBlock());section.setExtBlockMetadata(x,y&15,z,cell.meta);TileEntity old=(TileEntity)chunk.chunkTileEntityMap.remove(new ChunkPosition(x,y,z));if(old!=null)old.invalidate();restored.add(position);
            }
        }
        NBTTagList tiles=snapshot.getTagList("Tiles",10);for(int i=0;i<tiles.tagCount();i++){NBTTagCompound tag=tiles.getCompoundTagAt(i);int x=tag.getInteger("x")&15,z=tag.getInteger("z")&15,y=tag.getInteger("y");if(y<0||y>255||!restored.contains((y<<8)|(z<<4)|x))continue;
            ExtendedBlockStorage section=chunk.getBlockStorageArray()[y>>4];Block block=section.getBlockByExtId(x,y&15,z);TileEntity tile=block.createTileEntity(world,section.getExtBlockMetadata(x,y&15,z));if(tile!=null){tile.readFromNBT(tag);chunk.addTileEntity(tile);}
        }
        if(!restored.isEmpty()){chunk.generateSkylightMap();chunk.setChunkModified();mcjty.lostcities.LostCities.setup.getLogger().info("Recovered {} optional material cells in city chunk {},{}",restored.size(),chunk.xPosition,chunk.zPosition);}
    }
    public static final class Events {
        @SubscribeEvent public void save(ChunkDataEvent.Save event){World world=event.world;if(world.isRemote)return;LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(world);if(generator==null||!generator.modernMode())return;NBTTagCompound level=event.getData().getCompoundTag("Level");level.setTag(KEY,generator.materialData().recovery().snapshot(event.getChunk(),level));}
        @SubscribeEvent public void load(ChunkDataEvent.Load event){if(event.world.isRemote)return;NBTTagCompound level=event.getData().getCompoundTag("Level");if(level.hasKey(KEY,10))restore(event.world,event.getChunk(),level.getCompoundTag(KEY));else{LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(event.world);if(generator!=null&&generator.modernMode()&&generator.materialData().recovery().hasProviders)event.getChunk().setChunkModified();}}
    }
}
