package mcjty.lostcities.dimensions.world.driver;

import mcjty.lostcities.legacy.LegacyState;
import mcjty.lostcities.legacy.LegacyStates;
import mcjty.lostcities.legacy.ChunkPrimer;

public interface IPrimerDriver {

    void setPrimer(ChunkPrimer primer);

    ChunkPrimer getPrimer();

    IPrimerDriver current(int x, int y, int z);

    IPrimerDriver current(IIndex index);

    /// Return a copy of the current position
    IIndex getCurrent();

    /// Increment the height of the current position
    void incY();
    /// Increment the height of the current position
    void incY(int amount);
    /// Decrement the height of the current position
    void decY();

    void incX();

    void incZ();

    int getX();

    int getY();

    int getZ();

    IIndex getIndex(int x, int y, int z);

    default void setBlockRange(int x,int y,int z,int y2,int c){setBlockRange(x,y,z,y2,nativeChar(c));}

    default void setBlockRangeSafe(int x,int y,int z,int y2,int c){setBlockRangeSafe(x,y,z,y2,nativeChar(c));}

    /// Set a block at the current position
    default IPrimerDriver block(int c){return block(nativeChar(c));}

    /// Set a block at the current position
    IPrimerDriver block(LegacyState c);

    /// Set a block at the current position and increase the height with 1
    default IPrimerDriver add(int c){return add(nativeChar(c));}

    default int getState(){return getBlock();}

    default int getStateDown(){return getBlockDown();}
    default int getStateEast(){return getBlockEast();}
    default int getStateWest(){return getBlockWest();}
    default int getStateSouth(){return getBlockSouth();}
    default int getStateNorth(){return getBlockNorth();}

    default int getState(int x,int y,int z){return getBlock(x,y,z);}

    /** Compatibility entry points for integrations using native packed chars. */
    default void setBlockRange(int x,int y,int z,int y2,char c){setBlockRange(x,y,z,y2,(int)c);}
    default void setBlockRangeSafe(int x,int y,int z,int y2,char c){setBlockRangeSafe(x,y,z,y2,(int)c);}
    default IPrimerDriver block(char c){return block((int)c);}
    default IPrimerDriver add(char c){return add((int)c);}
    default char getBlock(){return nativeChar(getState());}
    default char getBlockDown(){return nativeChar(getStateDown());}
    default char getBlockEast(){return nativeChar(getStateEast());}
    default char getBlockWest(){return nativeChar(getStateWest());}
    default char getBlockSouth(){return nativeChar(getStateSouth());}
    default char getBlockNorth(){return nativeChar(getStateNorth());}
    default char getBlock(int x,int y,int z){return nativeChar(getState(x,y,z));}
    static char nativeChar(int state){
        if(state<0||state>65535)throw new IllegalArgumentException("Extended block state requires the integer driver API");
        return (char)state;
    }

    IPrimerDriver copy();

}
