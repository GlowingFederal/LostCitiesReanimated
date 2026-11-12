package mcjty.lostcities.compat;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.legacy.*;
import mcjty.lostcities.materials.*;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import java.util.*;

public final class LegacyMaterials implements MaterialResolver.Registry<LegacyState> {
    private static final ThreadLocal<MaterialResolver<LegacyState>> CURRENT=new ThreadLocal<>();
    private static MaterialResolver<LegacyState> defaults;
    public static boolean useUab=true,substitutions=true;
    public static final Map<String,List<String>> providers=new TreeMap<>();
    public static MaterialResolver<LegacyState> create(){return new MaterialResolver<>(new LegacyMaterials(),useUab,substitutions,providers,message->LostCities.setup.getLogger().debug(message));}
    public static LegacyState resolve(String source){MaterialResolver<LegacyState> current=CURRENT.get();if(current==null){if(defaults==null)defaults=create();current=defaults;}return current.resolve(source);}
    public static MaterialResolver<LegacyState> bind(MaterialResolver<LegacyState> current){MaterialResolver<LegacyState> previous=CURRENT.get();CURRENT.set(current);return previous;}
    public static void restore(MaterialResolver<LegacyState> previous){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}

    @Override public LegacyState find(BlockSpec spec){
        Block block=LegacyRegistries.BLOCKS.getValue(new ResourceLocation(spec.id));
        if(block==null||(block==Blocks.air&&!spec.id.equals("minecraft:air")))return null;
        if(block==Blocks.stone&&spec.metadata!=0)return null;
        if(block==Blocks.stonebrick&&spec.metadata>3)return null;
        if(block==Blocks.sandstone&&spec.metadata>2)return null;
        if("double".equals(spec.property("type",""))&&block instanceof BlockSlab){
            if(block==Blocks.stone_slab)block=Blocks.double_stone_slab;else if(block==Blocks.wooden_slab)block=Blocks.double_wooden_slab;else return null;
        }
        LegacyState state=LegacyStates.state(block,!spec.explicitMetadata?(block instanceof BlockTorch?5:block instanceof BlockChest||block instanceof BlockFurnace||block instanceof BlockLadder?2:0):spec.metadata);
        try {
            for(Map.Entry<String,String> property:spec.properties.entrySet()){
                String key=property.getKey(),value=property.getValue();int m=state.meta;
                if(key.equals("facing")){
                    if(block instanceof BlockChest&&(value.equals("up")||value.equals("down")))value="north";
                    if(block instanceof BlockLever&&!spec.property("face","wall").equals("wall")){
                        boolean eastWest=value.equals("east")||value.equals("west");
                        state=LegacyStates.state(block,(m&8)|(spec.property("face","wall").equals("floor")?(eastWest?6:5):(eastWest?0:7)));continue;
                    }
                    state=LegacyStates.withFacing(state,value);
                } else if(key.equals("half")||key.equals("type")){
                    if(block instanceof BlockDoor)state=LegacyStates.state(block,value.equals("upper")?8:(m&7));
                    else if(block instanceof BlockSlab)state=LegacyStates.state(block,value.equals("top")?(m|8):(m&7));
                    else if(block instanceof BlockStairs)state=LegacyStates.state(block,value.equals("top")?(m|4):(m&~4));
                    else if(block instanceof BlockTrapDoor)state=LegacyStates.state(block,value.equals("top")?(m|8):(m&7));
                    else return null;
                } else if(key.equals("hinge")){
                    if(!(block instanceof BlockDoor))return null;
                    if((m&8)!=0)state=LegacyStates.state(block,value.equals("right")?(m|1):(m&~1));
                } else if(key.equals("open")){
                    if(LegacyStates.isUabBarrel(block)){state=LegacyStates.state(block,Boolean.parseBoolean(value)?m|8:m&7);continue;}
                    if(block instanceof BlockChest)continue;
                    if(!(block instanceof BlockDoor)&&!(block instanceof BlockTrapDoor)&&!(block instanceof BlockFenceGate))return null;
                    if(!(block instanceof BlockDoor)||(m&8)==0)state=LegacyStates.state(block,Boolean.parseBoolean(value)?m|4:m&~4);
                } else if(key.equals("shape")){
                    if(block instanceof BlockRailBase)state=state.withProperty(mcjty.lostcities.legacy.Properties.RAIL,RailShape.valueOf(value.toUpperCase(Locale.ROOT)));
                    else if(!(block instanceof BlockStairs))return null;
                } else if(key.equals("axis")){
                    if(block instanceof BlockLog)state=LegacyStates.state(block,(m&3)|(value.equals("x")?4:value.equals("z")?8:0));
                    else if(block.getClass().getName().equals("ganymedes01.etfuturum.blocks.BlockChain"))state=LegacyStates.state(block,value.equals("x")?1:value.equals("z")?2:0);
                    else if(block!=Blocks.iron_bars)return null;
                } else if(key.equals("lit")){
                    if(block==Blocks.redstone_torch||block==Blocks.unlit_redstone_torch)state=LegacyStates.state(Boolean.parseBoolean(value)?Blocks.redstone_torch:Blocks.unlit_redstone_torch,m);
                } else if(key.equals("powered")){
                    if(block==Blocks.golden_rail)state=LegacyStates.state(block,Boolean.parseBoolean(value)?m|8:m&7);
                    else if(block instanceof BlockLever||block instanceof BlockButton)state=LegacyStates.state(block,Boolean.parseBoolean(value)?m|8:m&7);
                    else if(block instanceof BlockPressurePlate)state=LegacyStates.state(block,Boolean.parseBoolean(value)?1:0);
                } else if(key.equals("face")&&(block instanceof BlockLever||block instanceof BlockButton)){
                    if(block instanceof BlockButton&&!value.equals("wall"))return null;
                } else if(key.equals("stage")&&block instanceof BlockSapling){
                    state=LegacyStates.state(block,(m&7)|(value.equals("1")?8:0));
                } else if(key.equals("persistent")&&block instanceof BlockLeaves)state=LegacyStates.state(block,Boolean.parseBoolean(value)?m|4:m&~4);
                else if(key.equals("waterlogged")||key.equals("distance")||key.equals("north")||key.equals("south")||key.equals("east")||key.equals("west")||key.equals("up")||key.equals("down"))continue;
                else if(key.equals("hanging")){
                    if(block.getClass().getName().equals("ganymedes01.etfuturum.blocks.BlockLantern"))state=LegacyStates.state(block,Boolean.parseBoolean(value)?1:0);
                    else if(block!=Blocks.glowstone)return null;
                }
                else return null;
            }
            return state;
        } catch(IllegalArgumentException exception){return null;}
    }
    @Override public String serialize(LegacyState state){return state.toString();}
    public static LegacyState nativeState(BlockSpec spec){return new LegacyMaterials().find(spec);}
    public static mcjty.lostcities.materials.MaterialRecovery<LegacyState> recovery(Map<String,String> choices){LegacyMaterials registry=new LegacyMaterials();return new mcjty.lostcities.materials.MaterialRecovery<>(choices,new MaterialResolver<>(registry,false,true,Collections.emptyMap(),message->{}),registry);}
    private LegacyMaterials(){}
}
