package mcjty.lostcities.legacy;

import net.minecraft.block.*;
import net.minecraft.init.Blocks;

/** Interns native block/metadata pairs once; placement uses packed chars without registry lookups. */
public final class LegacyStates {
    private static final LegacyState[] STATES=new LegacyState[65536];
    public static final StateIds IDS=new StateIds();
    public static LegacyState state(Block block){return state(block,0);}
    public static LegacyState state(Block block,int meta){
        int id=(Block.getIdFromBlock(block)<<4)|meta;
        if(id<0||id>=65536||meta<0||meta>15)throw new IllegalArgumentException("Block outside native 1.7.10 range: "+block+"@"+meta);
        LegacyState state=STATES[id];
        if(state==null||state.getBlock()!=block)STATES[id]=state=new LegacyState(block,meta);
        return state;
    }
    public static final class StateIds {
        public int get(LegacyState state){return (Block.getIdFromBlock(state.getBlock())<<4)|state.meta;}
        public LegacyState getByValue(int id){return state(Block.getBlockById(id>>4),id&15);}
    }
    public static boolean isUabBarrel(Block block){return block.getClass().getName().equals("ganymedes01.etfuturum.blocks.BlockBarrel");}
    public static String facing(LegacyState state){
        Block b=state.getBlock();int m=state.meta;
        if(isUabBarrel(b))return new String[]{"down","up","north","south","west","east"}[Math.min(m&7,5)];
        if(b instanceof BlockStairs)return new String[]{"east","west","south","north"}[m&3];
        if(b instanceof BlockDoor)return (m&8)!=0?null:new String[]{"east","south","west","north"}[m&3];
        if(b instanceof BlockTrapDoor)return new String[]{"north","south","west","east"}[m&3];
        if(b instanceof BlockAnvil||b instanceof BlockFenceGate)return new String[]{"south","west","north","east"}[m&3];
        if(b instanceof BlockLever||b instanceof BlockButton)return (m&7)>=1&&(m&7)<=4?new String[]{"up","east","west","south","north"}[m&7]:null;
        if(b instanceof BlockTorch)return m>=1&&m<=4?new String[]{"up","east","west","south","north"}[m]:"up";
        if(b instanceof BlockDispenser)return new String[]{"down","up","north","south","west","east"}[Math.min(m&7,5)];
        if(b instanceof BlockLadder||b instanceof BlockChest||b instanceof BlockFurnace)return m>=2&&m<=5?new String[]{"down","up","north","south","west","east"}[m]:"north";
        return null;
    }
    public static LegacyState withFacing(LegacyState state,String value){
        Block b=state.getBlock();int m=state.meta;
        String[] order;
        if(isUabBarrel(b)){String[] sides={"down","up","north","south","west","east"};for(int i=0;i<sides.length;i++)if(sides[i].equals(value))return state(b,(m&8)|i);throw new IllegalArgumentException("Invalid barrel facing "+value);}
        if(b instanceof BlockStairs)order=new String[]{"east","west","south","north"};
        else if(b instanceof BlockDoor){if((m&8)!=0)return state;order=new String[]{"east","south","west","north"};}
        else if(b instanceof BlockTrapDoor)order=new String[]{"north","south","west","east"};
        else if(b instanceof BlockAnvil||b instanceof BlockFenceGate)order=new String[]{"south","west","north","east"};
        else if(b instanceof BlockLever||b instanceof BlockButton){order=new String[]{"unused","east","west","south","north"};for(int i=1;i<order.length;i++)if(order[i].equals(value))return state(b,(m&8)|i);throw new IllegalArgumentException("Invalid attachment facing "+value);}
        else if(b instanceof BlockTorch){order=new String[]{"unused","east","west","south","north","up"};m=0;}
        else if(b instanceof BlockDispenser){String[] sides={"down","up","north","south","west","east"};for(int i=0;i<sides.length;i++)if(sides[i].equals(value))return state(b,(m&8)|i);throw new IllegalArgumentException("Invalid dispenser facing "+value);}
        else if(b instanceof BlockLadder||b instanceof BlockChest||b instanceof BlockFurnace){order=new String[]{"down","up","north","south","west","east"};m=0;}
        else throw new IllegalArgumentException("No native facing for "+state);
        for(int i=0;i<order.length;i++)if(order[i].equals(value))return state(b,(m&~3)|i);
        throw new IllegalArgumentException("Invalid facing "+value+" for "+state);
    }
    private LegacyStates(){}
}
