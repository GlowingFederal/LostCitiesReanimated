package mcjty.lostcities.compat;

import mcjty.lostcities.dimensions.world.lost.Transform;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

/** Transforms representable properties even when an optional block has no native rotation override. */
public final class StateTransforms {
    private static final int[][] CACHE=new int[Transform.values().length][];
    public static char transform(char id,Transform transform){
        if(transform==Transform.ROTATE_NONE)return id;
        int[] cache=CACHE[transform.ordinal()];if(cache==null)CACHE[transform.ordinal()]=cache=new int[65536];
        if(cache[id]!=0)return (char)(cache[id]-1);
        IBlockState state=Block.BLOCK_STATE_IDS.getByValue(id);
        boolean mirror=transform==Transform.MIRROR_X||transform==Transform.MIRROR_Z||transform==Transform.MIRROR_90_X;
        if(state.getBlock() instanceof BlockRailBase){
            for(IProperty<?> property:state.getPropertyKeys())if(property.getName().equals("shape"))state=rail(state,property,transform);
        } else {
            for(IProperty<?> property:state.getPropertyKeys()){
                Comparable<?> value=state.getValue(property);
                if(value instanceof EnumFacing){
                    EnumFacing facing=(EnumFacing)value;
                    if(facing.getAxis().isHorizontal()){
                        int x=transform.rotateX(7+facing.getXOffset(),7+facing.getZOffset())-transform.rotateX(7,7);
                        int z=transform.rotateZ(7+facing.getXOffset(),7+facing.getZOffset())-transform.rotateZ(7,7);
                        state=apply(state,property,EnumFacing.getFacingFromVector(x,0,z).getName());
                    }
                } else if(value instanceof EnumFacing.Axis){
                    EnumFacing.Axis axis=(EnumFacing.Axis)value;
                    boolean swap=transform==Transform.ROTATE_90||transform==Transform.ROTATE_270||transform==Transform.MIRROR_90_X;
                    if(swap&&axis!=EnumFacing.Axis.Y)state=apply(state,property,axis==EnumFacing.Axis.X?"z":"x");
                } else if(mirror&&(property.getName().equals("hinge")||property.getName().equals("shape"))){
                    String name=propertyName(property,value);
                    if(name.contains("left"))state=apply(state,property,name.replace("left","right"));
                    else if(name.contains("right"))state=apply(state,property,name.replace("right","left"));
                }
            }
        }
        int result=Block.BLOCK_STATE_IDS.get(state);if(result<0||result>65535)throw new IllegalStateException("Transformed state exceeds legacy palette range: "+state);
        cache[id]=result+1;return (char)result;
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static String propertyName(IProperty property,Comparable value){return property.getName(value);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static IBlockState rail(IBlockState state,IProperty property,Transform transform){Object shape=state.getValue(property);return shape instanceof BlockRailBase.EnumRailDirection?state.withProperty(property,transform.transform((BlockRailBase.EnumRailDirection)shape)):state;}
    private static <T extends Comparable<T>> IBlockState apply(IBlockState state,IProperty<T> property,String name){com.google.common.base.Optional<T> value=property.parseValue(name);return value.isPresent()?state.withProperty(property,value.get()):state;}
    private StateTransforms(){}
}
