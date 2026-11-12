package mcjty.lostcities.legacy;
public enum Facing {
    DOWN(0,-1,0),UP(0,1,0),NORTH(0,0,-1),SOUTH(0,0,1),WEST(-1,0,0),EAST(1,0,0);
    public final int x,y,z;
    Facing(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    public int getXOffset(){return x;} public int getYOffset(){return y;} public int getZOffset(){return z;}
    public Facing getOpposite(){return values()[ordinal()^1];}
    public Facing rotateY(){switch(this){case NORTH:return EAST;case EAST:return SOUTH;case SOUTH:return WEST;case WEST:return NORTH;default:return this;}}
}
