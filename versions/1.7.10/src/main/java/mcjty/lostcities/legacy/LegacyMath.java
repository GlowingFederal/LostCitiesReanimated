package mcjty.lostcities.legacy;
public final class LegacyMath {
    public static double clamp(double value,double min,double max){return Math.max(min,Math.min(value,max));}
    public static int floor(double value){return (int)Math.floor(value);}
    public static double clampedLerp(double a,double b,double fraction){return a+(b-a)*Math.max(0,Math.min(1,fraction));}
    private LegacyMath(){}
}
