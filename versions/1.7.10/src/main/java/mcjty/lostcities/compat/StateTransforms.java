package mcjty.lostcities.compat;

import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.legacy.*;
import net.minecraft.block.*;

/** Native metadata transformations for all supported vanilla/UAB shapes and functional blocks. */
public final class StateTransforms {
    private static final int[][] CACHE=new int[Transform.values().length][];
    public static char transform(char id,Transform transform){
        if(transform==Transform.ROTATE_NONE)return id;
        int[] cache=CACHE[transform.ordinal()];if(cache==null)CACHE[transform.ordinal()]=cache=new int[65536];if(cache[id]!=0)return (char)(cache[id]-1);
        LegacyState state=LegacyStates.IDS.getByValue(id);Block block=state.getBlock();int meta=state.meta;
        boolean mirror=transform==Transform.MIRROR_X||transform==Transform.MIRROR_Z||transform==Transform.MIRROR_90_X;
        if(block instanceof BlockRailBase)state=state.withProperty(mcjty.lostcities.legacy.Properties.RAIL,transform.transform(state.getValue(mcjty.lostcities.legacy.Properties.RAIL)));
        else if(block.getClass().getName().equals("ganymedes01.etfuturum.blocks.BlockChain")){boolean swap=transform==Transform.ROTATE_90||transform==Transform.ROTATE_270||transform==Transform.MIRROR_90_X;if(swap&&(meta==1||meta==2))state=LegacyStates.state(block,3-meta);}
        else if(block instanceof BlockLever&&((meta&7)==0||(meta&7)>=5)){boolean swap=transform==Transform.ROTATE_90||transform==Transform.ROTATE_270||transform==Transform.MIRROR_90_X;if(swap){int direction=meta&7;state=LegacyStates.state(block,(meta&8)|(direction==0?7:direction==7?0:direction==5?6:5));}}
        else if(block instanceof BlockLog){int axis=meta&12;boolean swap=transform==Transform.ROTATE_90||transform==Transform.ROTATE_270||transform==Transform.MIRROR_90_X;if(swap&&(axis==4||axis==8))state=LegacyStates.state(block,(meta&3)|(axis==4?8:4));}
        else {
            String facing=LegacyStates.facing(state);
            if(facing!=null){Facing direction=Facing.valueOf(facing.toUpperCase(java.util.Locale.ROOT));if(direction.y==0){
                int x=transform.rotateX(7+direction.x,7+direction.z)-transform.rotateX(7,7),z=transform.rotateZ(7+direction.x,7+direction.z)-transform.rotateZ(7,7);
                for(Facing candidate:Facing.values())if(candidate.x==x&&candidate.z==z&&candidate.y==0)state=LegacyStates.withFacing(state,candidate.name().toLowerCase(java.util.Locale.ROOT));
            }}
            if(mirror&&block instanceof BlockDoor&&(meta&8)!=0)state=LegacyStates.state(block,state.meta^1);
        }
        int result=LegacyStates.IDS.get(state);cache[id]=result+1;return (char)result;
    }
    private StateTransforms(){}
}
