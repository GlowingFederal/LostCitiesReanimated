package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.api.*;
import mcjty.lostcities.compat.LegacyMaterials;
import mcjty.lostcities.config.*;
import mcjty.lostcities.dimensions.world.driver.OptimizedDriver;
import mcjty.lostcities.dimensions.world.lost.*;
import mcjty.lostcities.dimensions.world.lost.cityassets.*;
import mcjty.lostcities.dimensions.world.terraingen.LostCitiesTerrainGenerator;
import mcjty.lostcities.legacy.*;
import mcjty.lostcities.materials.MaterialResolver;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.*;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.structure.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.*;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;

/** Native 1.7.10 pipeline around the original Lost Cities planning and placement algorithms. */
public final class LostCityChunkGenerator implements IChunkProvider, ILostChunkGenerator, mcjty.lostcities.core.GenerationScope.CacheOwner {
    private final java.util.Map<Object,Object> metadataCaches=new java.util.IdentityHashMap<>();
    @Override public java.util.Map<Object,Object> metadataCaches(){return metadataCaches;}
    private final java.util.Map<mcjty.lostcities.varia.ChunkCoord,Integer> activeMetadata=new java.util.HashMap<>();
    public boolean metadataActive(mcjty.lostcities.varia.ChunkCoord coord){return activeMetadata.containsKey(coord);}
    private void enterMetadata(int x,int z){for(int dx=0;dx>=-1;dx--)for(int dz=0;dz>=-1;dz--){mcjty.lostcities.varia.ChunkCoord coord=new mcjty.lostcities.varia.ChunkCoord(dimensionId,x+dx,z+dz);activeMetadata.put(coord,activeMetadata.getOrDefault(coord,0)+1);}}
    private void leaveMetadata(int x,int z){for(int dx=0;dx>=-1;dx--)for(int dz=0;dz>=-1;dz--){mcjty.lostcities.varia.ChunkCoord coord=new mcjty.lostcities.varia.ChunkCoord(dimensionId,x+dx,z+dz);int count=activeMetadata.get(coord)-1;if(count==0)activeMetadata.remove(coord);else activeMetadata.put(coord,count);}BuildingInfo.trimCache();}
    public IChunkPrimerFactory otherGenerator;
    public final World worldObj;
    public final long seed;
    public final int dimensionId;
    public final mcjty.lostcities.core.SnapshotRandom rand;
    public final WorldType worldType;
    public WorldStyle worldStyle;
    public final LostCitiesTerrainGenerator terrainGenerator;
    private final LostCityProfile profile,outsideProfile;
    private final TerrainSettings settings;
    private final Map<Long,ChunkPrimer> primers=bounded(64);
    private final Map<Long,ChunkHeightmap> heights=bounded(4096);
    private final MapGenBase caves,ravines;
    private final MapGenMineshaft mineshafts;
    private final MapGenVillage villages;
    private final MapGenStronghold strongholds;
    private final MapGenScatteredFeature temples;
    private mcjty.lostcities.compat.MaterialWorldData materialData;
    public mcjty.lostcities.compat.MaterialWorldData materialData(){if(materialData==null)materialData=mcjty.lostcities.compat.MaterialWorldData.get(worldObj);return materialData;}
    private static <T> Map<Long,T> bounded(final int capacity){return new LinkedHashMap<Long,T>(capacity,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,T> entry){return size()>capacity;}};}
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}

    public LostCityChunkGenerator(World world,long seed){
        worldObj=world;this.seed=seed;dimensionId=world.provider.dimensionId;worldType=world.getWorldInfo().getTerrainType();rand=new mcjty.lostcities.core.SnapshotRandom(seed);
        profile=WorldTypeTools.getProfile(world);
        outsideProfile=profile.isSphereLandscape()&&!profile.CITYSPHERE_OUTSIDE_PROFILE.isEmpty()?LostCityConfiguration.profiles.get(profile.CITYSPHERE_OUTSIDE_PROFILE):profile;
        if(outsideProfile==null)throw new IllegalArgumentException("Unknown outside profile "+profile.CITYSPHERE_OUTSIDE_PROFILE);
        settings=profile.GENERATOR_OPTIONS.isEmpty()?new TerrainSettings():new com.google.gson.Gson().fromJson(profile.GENERATOR_OPTIONS,TerrainSettings.class);
        if(!AssetRegistries.WORLDSTYLES.contains(profile.getWorldStyle()))throw new IllegalArgumentException("Unknown world style "+profile.getWorldStyle());
        MapGenBase selectedCaves=TerrainGen.getModdedMapGen(new MapGenCaves(),InitMapGenEvent.EventType.CAVE),selectedRavines=TerrainGen.getModdedMapGen(new MapGenRavine(),InitMapGenEvent.EventType.RAVINE);
        mcjty.lostcities.compat.BetterCavesAdapter caveAdapter=new mcjty.lostcities.compat.BetterCavesAdapter(world,profile.isDefault(),selectedCaves,selectedRavines);
        MapGenBase adaptedCaves=caveAdapter.caves(selectedCaves,new LostGenCaves(this));
        caves=adaptedCaves.getClass()==MapGenCaves.class?new LostGenCaves(this):adaptedCaves;ravines=caveAdapter.ravines(selectedRavines,new MapGenRavine());
        mineshafts=(MapGenMineshaft)TerrainGen.getModdedMapGen(new MapGenMineshaft(),InitMapGenEvent.EventType.MINESHAFT);
        villages=(MapGenVillage)TerrainGen.getModdedMapGen(new MapGenVillage(),InitMapGenEvent.EventType.VILLAGE);
        strongholds=(MapGenStronghold)TerrainGen.getModdedMapGen(new MapGenStronghold(),InitMapGenEvent.EventType.STRONGHOLD);
        temples=(MapGenScatteredFeature)TerrainGen.getModdedMapGen(new MapGenScatteredFeature(),InitMapGenEvent.EventType.SCATTERED_FEATURE);
        terrainGenerator=new LostCitiesTerrainGenerator(this);terrainGenerator.setup(world);
        WorldTypeTools.register(this);
    }
    private void selectWorldStyle(){
        worldStyle=AssetRegistries.WORLDSTYLES.get(profile.getWorldStyle());
        if(worldStyle==null)throw new IllegalArgumentException("World style "+profile.getWorldStyle()+" is unavailable in "+(mcjty.lostcities.assets.AssetContext.modern()?"modern":"legacy")+" generation mode");
    }
    private mcjty.lostcities.compat.ModernGeneration modernGeneration;
    public mcjty.lostcities.compat.ModernGeneration planning(){if(!mcjty.lostcities.core.GenerationScope.owns(this))return contextual(this::planning);if(modernGeneration==null)modernGeneration=new mcjty.lostcities.compat.ModernGeneration(this);return modernGeneration;}
    public <R> R contextual(java.util.function.Supplier<R> action){
        long previousTerrain=terrainGenerator.captureState();
        Object previousOwner=mcjty.lostcities.core.GenerationScope.bind(this);
        boolean previousAssets=mcjty.lostcities.assets.AssetContext.bind(modernMode());
        mcjty.lostcities.materials.MaterialResolver<LegacyState> previous=LegacyMaterials.bind(null);
        try {
            LegacyMaterials.bind(materialData().resolver());terrainGenerator.setupChars(profile);selectWorldStyle();return action.get();
        }finally{materialData().markDirty();LegacyMaterials.restore(previous);mcjty.lostcities.assets.AssetContext.restore(previousAssets);terrainGenerator.restoreState(previousTerrain);mcjty.lostcities.core.GenerationScope.restore(previousOwner);}
    }
    private <T extends ILostCityAsset> ILostCityAssetRegistry<T> contextualRegistry(final ILostCityAssetRegistry<T> registry){
        return new ILostCityAssetRegistry<T>(){
            public T get(String name){return contextual(()->registry.get(name));}
            public Iterable<T> getIterable(){return contextual(()->{java.util.List<T> values=new java.util.ArrayList<>();for(T value:registry.getIterable())values.add(value);return values;});}
        };
    }
    private final java.util.Map<Long,Integer> planningHeights=new java.util.LinkedHashMap<Long,Integer>(4096,.75F,true){@Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,Integer> entry){return size()>32768;}};
    public int[] samplePlanningHeights(int x,int z){
        return contextual(()->{terrainGenerator.generateHeightmap(x,z);
        int water=getProfile().getLiquidBlock().getBlock()==Blocks.air?0:getProfile().GROUNDLEVEL-getProfile().WATERLEVEL_OFFSET;
        return mcjty.lostcities.core.DensityHeightSampler.heights(terrainGenerator.heightMap,water);});
    }
    public int planningHeight(int x,int z){
        if(!getProfile().modern.fastTerrainPlanning||otherGenerator!=null||!getProfile().isDefault())return getHeightmap(x,z).getAverageHeight();
        long key=((long)x<<32)^(z&0xffffffffL);Integer height=planningHeights.get(key);
        if(height==null){height=mcjty.lostcities.core.DensityHeightSampler.average(samplePlanningHeights(x,z));planningHeights.put(key,height);}return height;
    }
    private void capturePendingNeighborhood(int x,int z){for(int dx=0;dx>=-1;dx--)for(int dz=0;dz>=-1;dz--)materialData().capturePending(BuildingInfo.getBuildingInfo(x+dx,z+dz,this));}
    public boolean modernMode(){return materialData().modern && getProfile().modern.enabled;}
    public LostCityProfile getProfile(){return profile;}
    public LostCityProfile getOutsideProfile(){return outsideProfile;}
    public TerrainSettings getSettings(){return settings;}
    public boolean hasMansion(int x,int z){return false;} // Vanilla 1.7.10 has no mansion/monument generator.
    public boolean hasOceanMonument(int x,int z){return false;}

    public ChunkPrimer generatePrimer(int x,int z){
        if(!mcjty.lostcities.core.GenerationScope.owns(this))return contextual(()->generatePrimer(x,z));
        mcjty.lostcities.core.SnapshotRandom.Snapshot before=rand.snapshot();
        try {
        rand.setSeed(seed^x*341873128712L^z*132897987541L);
        terrainGenerator.setupChars(profile);
        ChunkPrimer primer=new ChunkPrimer();if(otherGenerator==null)terrainGenerator.doCoreChunk(x,z,primer);else otherGenerator.fillChunk(x,z,primer);return primer;
    
        } finally { rand.restore(before); }
    }
    public ChunkHeightmap getHeightmap(int x,int z){
        if(!mcjty.lostcities.core.GenerationScope.owns(this))return contextual(()->getHeightmap(x,z));
        long key=key(x,z);ChunkHeightmap height=heights.get(key);
        if(height==null){ChunkPrimer primer=primers.get(key);if(primer==null){primer=generatePrimer(x,z);primers.put(key,primer);}
            OptimizedDriver driver=new OptimizedDriver();driver.setPrimer(primer);
            height=new ChunkHeightmap(driver,profile.LANDSCAPE_TYPE,profile.GROUNDLEVEL,(char)LegacyStates.IDS.get(profile.getBaseBlock()));heights.put(key,height);
        }return height;
    }
    public ChunkPrimer getChunkPrimer(int x,int z,boolean city){
        if(city&&profile.isDefault())return new ChunkPrimer();
        ChunkPrimer primer=primers.remove(key(x,z));return primer==null?generatePrimer(x,z):primer;
    }
    @Override public Chunk provideChunk(int x,int z){
        enterMetadata(x,z);
        long previousTerrain=terrainGenerator.captureState();
        Object previousOwner=mcjty.lostcities.core.GenerationScope.bind(this);
        boolean previousAssets=mcjty.lostcities.assets.AssetContext.bind(modernMode());
        selectWorldStyle();
        MaterialResolver<LegacyState> previous=LegacyMaterials.bind(materialData().resolver());
        try {
            terrainGenerator.setupChars(profile);
            boolean city=BuildingInfo.isCity(x,z,this);
            ChunkPrimer primer=getChunkPrimer(x,z,city);
            terrainGenerator.generate(x,z,primer);
            BiomeGenBase[] biomes=worldObj.getWorldChunkManager().loadBlockGeneratorData(null,x*16,z*16,16,16);
            if(otherGenerator==null||city||!otherGenerator.replaceSurface(x,z,primer,biomes))terrainGenerator.replaceBlocksForBiome(x,z,primer,biomes);
            Block[] blocks=primer.blocks();byte[] metadata=primer.metadata();
            if(profile.GENERATE_CAVES)mcjty.lostcities.compat.BetterCavesAdapter.generate(caves,this,worldObj,x,z,blocks,metadata);
            if(profile.GENERATE_RAVINES&&(!city||!profile.PREVENT_LAKES_RAVINES_IN_CITIES))mcjty.lostcities.compat.BetterCavesAdapter.generate(ravines,this,worldObj,x,z,blocks,metadata);
            if(profile.GENERATE_MINESHAFTS)mineshafts.func_151539_a(this,worldObj,x,z,blocks);
            if(profile.GENERATE_VILLAGES&&(!city||!profile.PREVENT_VILLAGES_IN_CITIES))villages.func_151539_a(this,worldObj,x,z,blocks);
            if(profile.GENERATE_STRONGHOLDS)strongholds.func_151539_a(this,worldObj,x,z,blocks);
            if(profile.GENERATE_SCATTERED)temples.func_151539_a(this,worldObj,x,z,blocks);
            if(profile.isSpheres()){primer.load(blocks,metadata);terrainGenerator.finishSphere(x,z,primer);blocks=primer.blocks();metadata=primer.metadata();}
            Chunk chunk=new Chunk(worldObj,blocks,metadata,x,z);
            byte[] biomeIds=chunk.getBiomeArray();for(int i=0;i<256;i++)biomeIds[i]=(byte)biomes[i].biomeID;
            chunk.generateSkylightMap();capturePendingNeighborhood(x,z);return chunk;
        } finally {leaveMetadata(x,z);materialData().markDirty();LegacyMaterials.restore(previous);mcjty.lostcities.assets.AssetContext.restore(previousAssets);terrainGenerator.restoreState(previousTerrain);mcjty.lostcities.core.GenerationScope.restore(previousOwner);}
    }
    @Override public void populate(IChunkProvider provider,int cx,int cz){
        enterMetadata(cx,cz);
        long previousTerrain=terrainGenerator.captureState();
        Object previousOwner=mcjty.lostcities.core.GenerationScope.bind(this);
        boolean falling=BlockFalling.fallInstantly;BlockFalling.fallInstantly=true;
        boolean previousAssets=mcjty.lostcities.assets.AssetContext.bind(modernMode());
        selectWorldStyle();
        MaterialResolver<LegacyState> previous=LegacyMaterials.bind(materialData().resolver());
        try {
            int x=cx*16,z=cz*16;rand.setSeed(seed);long a=rand.nextLong()/2*2+1,b=rand.nextLong()/2*2+1;rand.setSeed(cx*a+cz*b^seed);
            boolean city=BuildingInfo.isCity(cx,cz,this),village=false;
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(provider,worldObj,rand,cx,cz,false));
            if(profile.GENERATE_MINESHAFTS)mineshafts.generateStructuresInChunk(worldObj,rand,cx,cz);
            if(profile.GENERATE_VILLAGES&&(!city||!profile.PREVENT_VILLAGES_IN_CITIES))village=villages.generateStructuresInChunk(worldObj,rand,cx,cz);
            if(profile.GENERATE_STRONGHOLDS)strongholds.generateStructuresInChunk(worldObj,rand,cx,cz);
            if(profile.GENERATE_SCATTERED)temples.generateStructuresInChunk(worldObj,rand,cx,cz);
            if(profile.GENERATE_LAKES&&(!city||!profile.PREVENT_LAKES_RAVINES_IN_CITIES)&&!village&&rand.nextInt(4)==0&&TerrainGen.populate(provider,worldObj,rand,cx,cz,village,PopulateChunkEvent.Populate.EventType.LAKE))new WorldGenLakes(profile.getLiquidBlock().getBlock()).generate(worldObj,rand,x+rand.nextInt(16)+8,rand.nextInt(256),z+rand.nextInt(16)+8);
            if(profile.GENERATE_LAKES&&(!city||!profile.PREVENT_LAKES_RAVINES_IN_CITIES)&&!village&&rand.nextInt(8)==0&&TerrainGen.populate(provider,worldObj,rand,cx,cz,village,PopulateChunkEvent.Populate.EventType.LAVA)){
                int lx=x+rand.nextInt(16)+8,ly=rand.nextInt(rand.nextInt(248)+8),lz=z+rand.nextInt(16)+8;
                if(ly<63||rand.nextInt(10)==0)new WorldGenLakes(Blocks.lava).generate(worldObj,rand,lx,ly,lz);
            }
            if(profile.GENERATE_DUNGEONS&&TerrainGen.populate(provider,worldObj,rand,cx,cz,village,PopulateChunkEvent.Populate.EventType.DUNGEON))for(int i=0;i<8;i++)new WorldGenDungeons().generate(worldObj,rand,x+rand.nextInt(16)+8,rand.nextInt(256),z+rand.nextInt(16)+8);
            BiomeGenBase biome=worldObj.getBiomeGenForCoords(x+16,z+16);biome.decorate(worldObj,rand,x,z);
            if(TerrainGen.populate(provider,worldObj,rand,cx,cz,village,PopulateChunkEvent.Populate.EventType.ANIMALS))SpawnerAnimals.performWorldGenSpawning(worldObj,biome,x+8,z+8,16,16,rand);
            if(TerrainGen.populate(provider,worldObj,rand,cx,cz,village,PopulateChunkEvent.Populate.EventType.ICE))for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++){
                int wx=x+8+dx,wz=z+8+dz,height=worldObj.getPrecipitationHeight(wx,wz);
                if(worldObj.isBlockFreezable(wx,height-1,wz))worldObj.setBlock(wx,height-1,wz,Blocks.ice,0,2);
                if(worldObj.func_147478_e(wx,height,wz,true))worldObj.setBlock(wx,height,wz,Blocks.snow_layer,0,2);
            }
            BuildingInfo details=BuildingInfo.getBuildingInfo(cx,cz,this);
            for(BlockPos pos:details.getSaplingTodo())if(worldObj.getBlock(pos.getX(),pos.getY(),pos.getZ())==Blocks.sapling)((net.minecraft.block.BlockSapling)Blocks.sapling).func_149878_d(worldObj,pos.getX(),pos.getY(),pos.getZ(),rand);
            details.clearSaplingTodo();generateVines(cx,cz);
            finishStructures(cx,cz);capturePendingNeighborhood(cx,cz);
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(provider,worldObj,rand,cx,cz,village));
        } finally {leaveMetadata(cx,cz);materialData().markDirty();LegacyMaterials.restore(previous);mcjty.lostcities.assets.AssetContext.restore(previousAssets);terrainGenerator.restoreState(previousTerrain);mcjty.lostcities.core.GenerationScope.restore(previousOwner);BlockFalling.fallInstantly=falling;}
    }

    private void generateVines(int chunkX,int chunkZ) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX,chunkZ,this);

        if (info.hasBuilding) {
            BuildingInfo adjacent = info.getXmax();
            int bottom = Math.max(adjacent.getCityGroundLevel() + 3, adjacent.hasBuilding ? adjacent.getMaxHeight() : (adjacent.getCityGroundLevel() + 3));
            for (int z = 0; z < 15; z++) {
                for (int y = bottom; y < (info.getMaxHeight()); y++) {
                    if (rand.nextFloat() < profile.VINE_CHANCE) {
                        createVineStrip(rand, worldObj, bottom, 2, new BlockPos(cx + 16, y, cz + z), new BlockPos(cx + 15, y, cz + z));
                    }
                }
            }
        }
        if (info.getXmax().hasBuilding) {
            BuildingInfo adjacent = info.getXmax();
            int bottom = Math.max(info.getCityGroundLevel() + 3, info.hasBuilding ? info.getMaxHeight() : (info.getCityGroundLevel() + 3));
            for (int z = 0; z < 15; z++) {
                for (int y = bottom; y < (adjacent.getMaxHeight()); y++) {
                    if (rand.nextFloat() < profile.VINE_CHANCE) {
                        createVineStrip(rand, worldObj, bottom, 8, new BlockPos(cx + 15, y, cz + z), new BlockPos(cx + 16, y, cz + z));
                    }
                }
            }
        }

        if (info.hasBuilding) {
            BuildingInfo adjacent = info.getZmax();
            int bottom = Math.max(adjacent.getCityGroundLevel() + 3, adjacent.hasBuilding ? adjacent.getMaxHeight() : (adjacent.getCityGroundLevel() + 3));
            for (int x = 0; x < 15; x++) {
                for (int y = bottom; y < (info.getMaxHeight()); y++) {
                    if (rand.nextFloat() < profile.VINE_CHANCE) {
                        createVineStrip(rand, worldObj, bottom, 4, new BlockPos(cx + x, y, cz + 16), new BlockPos(cx + x, y, cz + 15));
                    }
                }
            }
        }
        if (info.getZmax().hasBuilding) {
            BuildingInfo adjacent = info.getZmax();
            int bottom = Math.max(info.getCityGroundLevel() + 3, info.hasBuilding ? info.getMaxHeight() : (info.getCityGroundLevel() + 3));
            for (int x = 0; x < 15; x++) {
                for (int y = bottom; y < (adjacent.getMaxHeight()); y++) {
                    if (rand.nextFloat() < profile.VINE_CHANCE) {
                        createVineStrip(rand, worldObj, bottom, 1, new BlockPos(cx + x, y, cz + 15), new BlockPos(cx + x, y, cz + 16));
                    }
                }
            }
        }
    }

    private void createVineStrip(Random random, World world, int bottom, int direction, BlockPos pos, BlockPos vineHolderPos) {
        if (world.isAirBlock(vineHolderPos.getX(),vineHolderPos.getY(),vineHolderPos.getZ())) {
            return;
        }
        if (!world.isAirBlock(pos.getX(),pos.getY(),pos.getZ())) {
            return;
        }
        world.setBlock(pos.getX(),pos.getY(),pos.getZ(),Blocks.vine,direction,2);
        pos = pos.down();
        while (pos.getY() >= bottom && random.nextFloat() < .8f) {
            if (!world.isAirBlock(pos.getX(),pos.getY(),pos.getZ())) {
                return;
            }
            world.setBlock(pos.getX(),pos.getY(),pos.getZ(),Blocks.vine,direction,2);
            pos = pos.down();
        }
    }

    private java.util.function.Predicate<BlockPos> editorBounds(int x,int z,int y,int width,int depth,int height){return pos->pos.getX()>=x*16&&pos.getX()<x*16+width&&pos.getZ()>=z*16&&pos.getZ()<z*16+depth&&pos.getY()>=y&&pos.getY()<y+height;}
    public void prepareEditorPart(int x,int z,int y,int width,int depth,int height){
        java.util.function.Predicate<BlockPos> selected=editorBounds(x,z,y,width,depth,height);
        for(int dx=0;dx>=-1;dx--)for(int dz=0;dz>=-1;dz--){BuildingInfo info=BuildingInfo.getBuildingInfo(x+dx,z+dz,this);info.getTagTodo().removeIf(todo->selected.test(todo.position));info.getLootTodo().removeIf(todo->selected.test(todo.getKey()));info.getMobSpawnerTodo().removeIf(todo->selected.test(todo.getKey()));info.getLightingUpdateTodo().removeIf(selected);info.getSaplingTodo().removeIf(selected);}
        capturePendingNeighborhood(x,z);
    }
    public void finishEditorPart(int x,int z,int y,int width,int depth,int height){
        java.util.function.Predicate<BlockPos> selected=editorBounds(x,z,y,width,depth,height);
        for(int dx=0;dx>=-1;dx--)for(int dz=0;dz>=-1;dz--){
            int cx=x+dx,cz=z+dz;
            BuildingInfo info=BuildingInfo.getBuildingInfo(cx,cz,this);
            // Tree growth can enqueue more native work; retain every job outside the edit.
            for(BlockPos pos:new java.util.ArrayList<>(info.getSaplingTodo())){
                if(selected.test(pos)&&worldObj.getBlock(pos.getX(),pos.getY(),pos.getZ())==Blocks.sapling)
                    ((net.minecraft.block.BlockSapling)Blocks.sapling).func_149878_d(worldObj,pos.getX(),pos.getY(),pos.getZ(),rand);
            }
            info.getSaplingTodo().removeIf(selected);
            finishStructures(cx,cz,selected);
        }
        capturePendingNeighborhood(x,z);
    }
    private void finishStructures(final int cx,final int cz){finishStructures(cx,cz,pos->true);}
    private void finishStructures(final int cx,final int cz,java.util.function.Predicate<BlockPos> selected){
        BuildingInfo info=BuildingInfo.getBuildingInfo(cx,cz,this);
        for(BuildingInfo.TagTodo todo:info.getTagTodo()){
            if(!selected.test(todo.position))continue;
            TileEntity tile=worldObj.getTileEntity(todo.position.getX(),todo.position.getY(),todo.position.getZ());
            if(tile!=null&&worldObj.getBlock(todo.position.getX(),todo.position.getY(),todo.position.getZ())==mcjty.lostcities.legacy.LegacyStates.IDS.getByValue(todo.block).getBlock())mcjty.lostcities.compat.BlockEntityTags.apply(tile,todo.tag);
        }
        info.getTagTodo().removeIf(todo->selected.test(todo.position));
        for(Pair<BlockPos,BuildingInfo.ConditionTodo> pair:info.getMobSpawnerTodo()){
            BlockPos pos=pair.getKey();if(!selected.test(pos))continue;TileEntity te=worldObj.getTileEntity(pos.getX(),pos.getY(),pos.getZ());
            if(te instanceof TileEntityMobSpawner){
                String mob=mcjty.lostcities.legacy.LegacyLoot.entity(condition(pair.getValue(),info,pos));
                net.minecraft.nbt.NBTTagCompound data=new net.minecraft.nbt.NBTTagCompound();te.writeToNBT(data);
                net.minecraft.nbt.NBTTagCompound properties=data.getCompoundTag("SpawnData");
                if(properties.hasKey("id"))properties.setString("id",mob);
                data.setString("EntityId",mob);data.setTag("SpawnData",properties);
                net.minecraft.nbt.NBTTagCompound potential=new net.minecraft.nbt.NBTTagCompound();
                potential.setString("Type",mob);potential.setInteger("Weight",1);potential.setTag("Properties",properties.copy());
                net.minecraft.nbt.NBTTagList potentials=new net.minecraft.nbt.NBTTagList();potentials.appendTag(potential);
                data.setTag("SpawnPotentials",potentials);te.readFromNBT(data);te.markDirty();
                worldObj.markBlockForUpdate(pos.getX(),pos.getY(),pos.getZ());
            }
        }info.getMobSpawnerTodo().removeIf(todo->selected.test(todo.getKey()));
        for(Pair<BlockPos,BuildingInfo.ConditionTodo> pair:info.getLootTodo()){
            BlockPos pos=pair.getKey();if(!selected.test(pos))continue;TileEntity te=worldObj.getTileEntity(pos.getX(),pos.getY(),pos.getZ());
            if(profile.GENERATE_LOOT&&te instanceof net.minecraft.inventory.IInventory&&rand.nextFloat()>=profile.CHEST_WITHOUT_LOOT_CHANCE)mcjty.lostcities.legacy.LegacyLoot.fill((net.minecraft.inventory.IInventory)te,pair.getValue()==null?"lostcities:chests/lostcitychest":condition(pair.getValue(),info,pos),rand);
        }info.getLootTodo().removeIf(todo->selected.test(todo.getKey()));
        for(BlockPos pos:info.getLightingUpdateTodo())if(selected.test(pos))worldObj.markBlockForUpdate(pos.getX(),pos.getY(),pos.getZ());info.getLightingUpdateTodo().removeIf(selected);
    }
    private ConditionContext conditionContext(BuildingInfo.ConditionTodo todo,BlockPos pos){
        BuildingInfo target=BuildingInfo.getBuildingInfo(Math.floorDiv(pos.getX(),16),Math.floorDiv(pos.getZ(),16),this);
        mcjty.lostcities.core.PlacementContext saved=todo.placement;
        final int x=saved==null?target.chunkX:saved.chunkX,z=saved==null?target.chunkZ:saved.chunkZ;
        return new ConditionContext(saved==null?Math.floorDiv(pos.getY()-target.groundLevel,6):saved.level,saved==null?Math.floorDiv(pos.getY()-target.getCityGroundLevel(),6):saved.floor,saved==null?target.floorsBelowGround:saved.cellars,saved==null?target.getNumFloors():saved.floors,todo.getPart(),todo.getBuilding(),x,z){
            @Override public boolean isSphere(){return CitySphere.isInSphere(x,z,pos,LostCityChunkGenerator.this);}
            @Override public String getBiome(){return String.valueOf(mcjty.lostcities.legacy.LegacyBiomes.name(worldObj.getBiomeGenForCoords(pos.getX(),pos.getZ())));}
        }.withBelowPart(saved==null?"<none>":saved.belowPart);
    }
    private String condition(BuildingInfo.ConditionTodo todo,final BuildingInfo info,final BlockPos pos){
        Condition condition=AssetRegistries.CONDITIONS.get(todo.getCondition());if(condition==null)throw new IllegalArgumentException("Missing condition "+todo.getCondition());
        String value=condition.getRandomValue(rand,conditionContext(todo,pos));if(value==null)throw new IllegalArgumentException("No matching condition "+todo.getCondition());return value;
    }
    @Override public ILostChunkInfo getChunkInfo(int x,int z){return contextual(() -> BuildingInfo.getBuildingInfo(x,z,this));}
    @Override public int getRealHeight(int level){return profile.GROUNDLEVEL+level*6;}
    @Override public ILostCityAssetRegistry<ILostCityBuilding> getBuildings(){return contextualRegistry(AssetRegistries.BUILDINGS.<ILostCityBuilding>cast());}
    @Override public ILostCityAssetRegistry<ILostCityMultiBuilding> getMultiBuildings(){return contextualRegistry(AssetRegistries.MULTI_BUILDINGS.<ILostCityMultiBuilding>cast());}
    @Override public ILostCityAssetRegistry<ILostCityCityStyle> getCityStyles(){return contextualRegistry(AssetRegistries.CITYSTYLES.<ILostCityCityStyle>cast());}
    @Override public Chunk loadChunk(int x,int z){return provideChunk(x,z);}
    @Override public boolean chunkExists(int x,int z){return true;}
    @Override public boolean saveChunks(boolean all,IProgressUpdate progress){return true;}
    @Override public boolean unloadQueuedChunks(){return false;}
    @Override public boolean canSave(){return true;}
    @Override public String makeString(){return "LostCitiesReanimated";}
    @Override public List getPossibleCreatures(EnumCreatureType type,int x,int y,int z){return worldObj.getBiomeGenForCoords(x,z).getSpawnableList(type);}
    @Override public ChunkPosition func_147416_a(World world,String name,int x,int y,int z){return "Stronghold".equals(name)?strongholds.func_151545_a(world,x,y,z):null;}
    @Override public int getLoadedChunkCount(){return 0;}
    @Override public void recreateStructures(int x,int z){
        if(profile.GENERATE_MINESHAFTS)mineshafts.func_151539_a(this,worldObj,x,z,null);
        if(profile.GENERATE_STRONGHOLDS)strongholds.func_151539_a(this,worldObj,x,z,null);
        if(profile.GENERATE_SCATTERED)temples.func_151539_a(this,worldObj,x,z,null);
        if(profile.GENERATE_VILLAGES&&(!profile.PREVENT_VILLAGES_IN_CITIES||!BuildingInfo.isCity(x,z,this)))villages.func_151539_a(this,worldObj,x,z,null);
    }
    @Override public void saveExtraData(){}
}
