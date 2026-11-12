package mcjty.lostcities.legacy;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

/** A native 1.7.10 block and four-bit metadata, not a modern block-state implementation. */
public final class LegacyState {
    private final Block block;
    public final int meta;
    LegacyState(Block block,int meta){if(block==null||meta<0||meta>15)throw new IllegalArgumentException("Invalid native block/metadata");this.block=block;this.meta=meta;}
    public Block getBlock(){return block;}
    public Material getMaterial(){return block.getMaterial();}
    public <T> LegacyState withProperty(Property<T> property,T value){
        int m=meta;
        if(property==Properties.WOOD)m=(m&12)|((Wood)value).ordinal();
        else if(property==Properties.DECAYABLE)m=(Boolean)value?(m&~4):(m|4);
        else if(property==Properties.RAIL)m=((RailShape)value).ordinal()|(block==Blocks.golden_rail?(m&8):0);
        else throw new IllegalArgumentException("Unsupported property "+property.name);
        return LegacyStates.state(block,m);
    }
    @SuppressWarnings("unchecked") public <T> T getValue(Property<T> property){
        if(property==Properties.RAIL)return (T)RailShape.values()[Math.min(block==Blocks.golden_rail?meta&7:meta,9)];
        if(property==Properties.WOOD)return (T)Wood.values()[Math.min(meta&7,5)];
        if(property==Properties.DECAYABLE)return (T)Boolean.valueOf((meta&4)==0);
        throw new IllegalArgumentException("Unsupported property "+property.name);
    }
    public LegacyState withRotation(Rotation rotation){
        if(rotation.turns==0)return this;
        String facing=LegacyStates.facing(this);
        if(facing==null)return this;
        Facing direction=Facing.valueOf(facing.toUpperCase(java.util.Locale.ROOT));
        for(int i=0;i<rotation.turns;i++)direction=direction.rotateY();
        return LegacyStates.withFacing(this,direction.name().toLowerCase(java.util.Locale.ROOT));
    }
    @Override public int hashCode(){return System.identityHashCode(block)*31+meta;}
    @Override public boolean equals(Object o){return o instanceof LegacyState&&((LegacyState)o).block==block&&((LegacyState)o).meta==meta;}
    @Override public String toString(){return LegacyRegistries.blockName(block)+"@"+meta;}
}
