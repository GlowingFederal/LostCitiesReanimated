package mcjty.lostcities.legacy;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/** Chunk-local, 0–255 placement buffer. It never accesses or loads a neighboring chunk. */
public final class ChunkPrimer {
    public final char[] data=new char[65536];
    public void setBlockState(int x,int y,int z,LegacyState state){if(x>=0&&x<16&&z>=0&&z<16&&y>=0&&y<256)data[(x<<12)|(z<<8)|y]=(char)LegacyStates.IDS.get(state);}
    public LegacyState getBlockState(int x,int y,int z){return LegacyStates.IDS.getByValue(x<0||x>15||z<0||z>15||y<0||y>255?0:data[(x<<12)|(z<<8)|y]);}
    public Block[] blocks(){Block[] blocks=new Block[65536];for(int i=0;i<blocks.length;i++)blocks[i]=Block.getBlockById(data[i]>>4);return blocks;}
    public byte[] metadata(){byte[] metadata=new byte[65536];for(int i=0;i<metadata.length;i++)metadata[i]=(byte)(data[i]&15);return metadata;}
    public void load(Block[] blocks,byte[] metadata){for(int i=0;i<data.length;i++)data[i]=blocks[i]==null?0:(char)((Block.getIdFromBlock(blocks[i])<<4)|(metadata[i]&15));}
    public Chunk toChunk(World world,int x,int z){return new Chunk(world,blocks(),metadata(),x,z);}
}
