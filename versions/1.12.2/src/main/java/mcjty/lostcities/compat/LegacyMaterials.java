package mcjty.lostcities.compat;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.materials.BlockSpec;
import mcjty.lostcities.materials.MaterialResolver;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

public final class LegacyMaterials implements MaterialResolver.Registry<IBlockState> {
    private static final ThreadLocal<MaterialResolver<IBlockState>> CURRENT = new ThreadLocal<>();
    private static MaterialResolver<IBlockState> defaults;
    public static boolean useUab = true;
    public static boolean substitutions = true;
    public static final Map<String, List<String>> providers = new TreeMap<>();

    public static MaterialResolver<IBlockState> create() {
        return new MaterialResolver<>(new LegacyMaterials(), useUab, substitutions, providers,
                message -> LostCities.setup.getLogger().debug(message));
    }
    public static IBlockState resolve(String source) {
        MaterialResolver<IBlockState> resolver = CURRENT.get();
        if (resolver == null) {
            if (defaults == null) defaults = create();
            resolver = defaults;
        }
        return resolver.resolve(source);
    }
    public static MaterialResolver<IBlockState> bind(MaterialResolver<IBlockState> resolver) {
        MaterialResolver<IBlockState> previous = CURRENT.get();
        CURRENT.set(resolver);
        return previous;
    }
    public static void restore(MaterialResolver<IBlockState> previous) {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    @Override public IBlockState find(BlockSpec spec) {
        ResourceLocation id = new ResourceLocation(spec.id);
        if (!ForgeRegistries.BLOCKS.containsKey(id)) return null;
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || (isMissing(block) && !spec.id.equals("minecraft:air"))) return null;
        boolean doubleSlab = "double".equals(spec.property("type",""));
        if (doubleSlab && block instanceof BlockSlab && !((BlockSlab)block).isDouble()) {
            if (block == Blocks.STONE_SLAB) block=Blocks.DOUBLE_STONE_SLAB;
            else if (block == Blocks.STONE_SLAB2) block=Blocks.DOUBLE_STONE_SLAB2;
            else if (block == Blocks.WOODEN_SLAB) block=Blocks.DOUBLE_WOODEN_SLAB;
            else return null;
        }
        if(block==Blocks.STRUCTURE_VOID)block=Blocks.COMMAND_BLOCK;
        IBlockState state;
        try {
            state = spec.explicitMetadata?block.getStateFromMeta(spec.metadata):block.getDefaultState();
            if (spec.explicitMetadata&&block.getMetaFromState(state) != spec.metadata) return null;
            for (Map.Entry<String, String> entry : spec.properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(key.equals("facing")&&block instanceof net.minecraft.block.BlockChest&&(value.equals("up")||value.equals("down")))value="north";
                if (key.equals("waterlogged")) continue; // Neither legacy target represents waterlogged cells.
                if(block instanceof net.minecraft.block.BlockLever){
                    if(key.equals("face"))continue;
                    if(key.equals("facing")&&!spec.property("face","wall").equals("wall")){
                        boolean eastWest=value.equals("east")||value.equals("west");
                        value=spec.property("face","wall").equals("floor")?(eastWest?"up_x":"up_z"):(eastWest?"down_x":"down_z");
                    }
                }
                if (block instanceof BlockSlab && key.equals("type")) { if(doubleSlab) continue; key = "half"; }
                IProperty<?> property = null;
                for (IProperty<?> candidate : state.getPropertyKeys()) if (candidate.getName().equals(key)) property = candidate;
                if (property == null) {
                    // Connection/shape properties are derived by the legacy engine after placement.
                    if (key.equals("shape") || key.equals("north") || key.equals("south") || key.equals("east") || key.equals("west")
                            || key.equals("up") || key.equals("down") || key.equals("distance") || key.equals("persistent")) continue;
                    if (key.equals("hanging") && block == Blocks.GLOWSTONE) continue;
                    if (key.equals("axis") && block == Blocks.IRON_BARS) continue;
                    if (key.equals("open")&&block instanceof net.minecraft.block.BlockChest)continue;
                    if (key.equals("powered")&&(block instanceof net.minecraft.block.BlockTrapDoor||block instanceof net.minecraft.block.BlockDoor))continue;
                    if (key.equals("lit") && (block == Blocks.REDSTONE_TORCH || block == Blocks.UNLIT_REDSTONE_TORCH)) {
                        state = (Boolean.parseBoolean(value)?Blocks.REDSTONE_TORCH:Blocks.UNLIT_REDSTONE_TORCH).getStateFromMeta(state.getBlock().getMetaFromState(state));
                        continue;
                    }
                    return null;
                }
                state = apply(state, property, value);
                if (state == null) return null;
            }
            return state;
        } catch (IllegalArgumentException exception) { return null; }
    }

    private static <T extends Comparable<T>> IBlockState apply(IBlockState state, IProperty<T> property, String value) {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, parsed.get()) : null;
    }
    @Override public String serialize(IBlockState state) {
        // Metadata represents persisted properties; derived connections must not be serialized as decisions.
        return state.getBlock().getRegistryName() + "@" + state.getBlock().getMetaFromState(state);
    }
    public static boolean isMissing(Block block){return block==null||block==Blocks.AIR||block.getClass().getName().equals("net.minecraftforge.registries.GameData$BlockCallbacks$BlockDummyAir");}
    public static IBlockState nativeState(BlockSpec spec){return new LegacyMaterials().find(spec);}
    public static mcjty.lostcities.materials.MaterialRecovery<IBlockState> recovery(Map<String,String> choices){LegacyMaterials registry=new LegacyMaterials();return new mcjty.lostcities.materials.MaterialRecovery<>(choices,new MaterialResolver<>(registry,false,true,Collections.emptyMap(),message->{}),registry);}
    private LegacyMaterials() {}
}
