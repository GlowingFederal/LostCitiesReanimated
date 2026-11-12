package mcjty.lostcities.legacy;

/** Immutable coordinates for planning, independent of world/chunk ownership. */
public final class BlockPos {
    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    private final int x, y, z;
    public BlockPos(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
    public BlockPos(double x, double y, double z) { this((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z)); }
    public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
    public BlockPos add(int x,int y,int z){return new BlockPos(this.x+x,this.y+y,this.z+z);}
    public BlockPos north(){return add(0,0,-1);} public BlockPos south(){return add(0,0,1);} public BlockPos east(){return add(1,0,0);} public BlockPos west(){return add(-1,0,0);}
    public double distanceSq(double x,double y,double z){double dx=this.x-x,dy=this.y-y,dz=this.z-z;return dx*dx+dy*dy+dz*dz;}
    public BlockPos up(){return add(0,1,0);} public BlockPos down(){return add(0,-1,0);}
    public BlockPos offset(Facing f){return add(f.x,f.y,f.z);}
    public double distanceSq(BlockPos p){double dx=x-p.x,dy=y-p.y,dz=z-p.z;return dx*dx+dy*dy+dz*dz;}
    @Override public int hashCode(){return (x*31+y)*31+z;}
    @Override public boolean equals(Object o){if(!(o instanceof BlockPos))return false;BlockPos p=(BlockPos)o;return x==p.x&&y==p.y&&z==p.z;}
    @Override public String toString(){return x+","+y+","+z;}
}
