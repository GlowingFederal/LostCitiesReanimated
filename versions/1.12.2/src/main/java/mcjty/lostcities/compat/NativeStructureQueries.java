package mcjty.lostcities.compat;

import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStructure;
import java.lang.invoke.*;
import java.lang.reflect.*;

/** Queries the same authoritative structure instance used for generation without loading terrain. */
public final class NativeStructureQueries {
    private static final MethodHandle WORLD,CAN_SPAWN;
    static {
        try {
            Field world=null;Method spawn=null;
            for(Field field:MapGenBase.class.getDeclaredFields())if(field.getType()==World.class&&!Modifier.isStatic(field.getModifiers())){if(world!=null)throw new IllegalStateException("Ambiguous structure world");world=field;}
            for(Method method:MapGenStructure.class.getDeclaredMethods())if(method.getReturnType()==boolean.class&&java.util.Arrays.equals(method.getParameterTypes(),new Class<?>[]{int.class,int.class})){if(spawn!=null)throw new IllegalStateException("Ambiguous structure placement query");spawn=method;}
            if(world==null||spawn==null)throw new IllegalStateException("Missing native structure placement API");
            world.setAccessible(true);spawn.setAccessible(true);WORLD=MethodHandles.lookup().unreflectSetter(world);CAN_SPAWN=MethodHandles.lookup().unreflect(spawn);
        }catch(ReflectiveOperationException error){throw new ExceptionInInitializerError(error);}
    }
    public static boolean hasStructure(MapGenStructure generator,World world,int x,int z){
        if(generator==null)return false;
        try{WORLD.invokeExact((MapGenBase)generator,world);return (boolean)CAN_SPAWN.invokeExact(generator,x,z);}catch(Throwable error){throw new IllegalStateException("Structure placement query failed for "+generator.getClass().getName(),error);}
    }
    private NativeStructureQueries(){}
}
