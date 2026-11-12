package mcjty.lostcities.core;

/** Immutable condition inputs captured where a template is placed, before population. */
public final class PlacementContext {
    public final int level,floor,cellars,floors,chunkX,chunkZ;
    public final String belowPart;
    public PlacementContext(int level,int floor,int cellars,int floors,int chunkX,int chunkZ,String belowPart){
        this.level=level;this.floor=floor;this.cellars=cellars;this.floors=floors;this.chunkX=chunkX;this.chunkZ=chunkZ;this.belowPart=belowPart==null?"<none>":belowPart;
    }
}
