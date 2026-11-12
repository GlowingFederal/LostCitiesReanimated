package mcjty.lostcities.core;

import java.io.File;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Arrays;

/** Optional YUNG's Legacy boundary. No provider classes are linked without the provider present. */
public final class OptionalCaveBackend {
    private final Object config,controller,liquids;
    private final Constructor<?> terrain;
    private final MethodHandle caves,ocean,distance,carve,liquid;
    public final boolean ravines,floodedRavines;

    public static OptionalCaveBackend open(ClassLoader loader,File directory,long seed,int dimension,Class<?>... prefix) throws ReflectiveOperationException {
        Class<?> loaderType;
        try{loaderType=Class.forName("com.glowingfederal.bettercaves.CaveConfigLoader",false,loader);}
        catch(ClassNotFoundException absent){return null;}
        Method load=loaderType.getDeclaredMethod("load",File.class,int.class);load.setAccessible(true);
        Object config=load.invoke(null,directory,dimension);
        if(!(Boolean)config.getClass().getMethod("enabledIn",int.class).invoke(config,dimension))return null;
        return new OptionalCaveBackend(loader,config,seed,prefix);
    }
    private OptionalCaveBackend(ClassLoader loader,Object config,long seed,Class<?>[] prefix) throws ReflectiveOperationException {
        this.config=config;
        Class<?> settings=config.getClass(),engine=Class.forName("com.glowingfederal.bettercaves.CaveController",true,loader);
        Class<?> boundary=Class.forName("com.glowingfederal.bettercaves.CaveTerrain",false,loader);
        Class<?> nativeTerrain=Class.forName("com.glowingfederal.bettercaves.NativeTerrain",true,loader);
        Class<?> liquidType=Class.forName("com.glowingfederal.bettercaves.NativeTerrain$Liquids",true,loader);
        controller=engine.getConstructor(long.class,settings).newInstance(seed,config);
        Constructor<?> liquidConstructor=liquidType.getDeclaredConstructor(settings);liquidConstructor.setAccessible(true);liquids=liquidConstructor.newInstance(config);
        Class<?>[] arguments=Arrays.copyOf(prefix,prefix.length+3);arguments[prefix.length]=settings;arguments[prefix.length+1]=engine;arguments[prefix.length+2]=liquidType;
        terrain=nativeTerrain.getDeclaredConstructor(arguments);terrain.setAccessible(true);
        MethodHandles.Lookup lookup=MethodHandles.publicLookup();
        caves=lookup.unreflect(engine.getMethod("carve",boundary,int.class,int.class)).asType(MethodType.methodType(void.class,Object.class,Object.class,int.class,int.class));
        ocean=lookup.unreflect(boundary.getMethod("ocean",int.class,int.class)).asType(MethodType.methodType(boolean.class,Object.class,int.class,int.class));
        distance=lookup.unreflect(boundary.getMethod("oceanDistance",int.class,int.class)).asType(MethodType.methodType(float.class,Object.class,int.class,int.class));
        carve=lookup.unreflect(boundary.getMethod("carve",int.class,int.class,int.class,int.class,boolean.class)).asType(MethodType.methodType(void.class,Object.class,int.class,int.class,int.class,int.class,boolean.class));
        liquid=lookup.unreflect(engine.getMethod("liquidAt",int.class,int.class)).asType(MethodType.methodType(int.class,Object.class,int.class,int.class));
        ravines=settings.getField("ravines").getBoolean(config);floodedRavines=settings.getField("floodedRavines").getBoolean(config);
    }
    public Object terrain(Object... prefix){
        Object[] arguments=Arrays.copyOf(prefix,prefix.length+3);arguments[prefix.length]=config;arguments[prefix.length+1]=controller;arguments[prefix.length+2]=liquids;
        try{return terrain.newInstance(arguments);}catch(ReflectiveOperationException error){throw failure(error);}
    }
    public void caves(Object terrain,int x,int z){try{caves.invokeExact(controller,terrain,x,z);}catch(Throwable error){throw failure(error);}}
    public boolean ocean(Object terrain,int x,int z){try{return (boolean)ocean.invokeExact(terrain,x,z);}catch(Throwable error){throw failure(error);}}
    public float distance(Object terrain,int x,int z){try{return (float)distance.invokeExact(terrain,x,z);}catch(Throwable error){throw failure(error);}}
    public int liquid(int x,int z){try{return (int)liquid.invokeExact(controller,x,z);}catch(Throwable error){throw failure(error);}}
    public void carve(Object terrain,int x,int y,int z,int liquid,boolean flooded){try{carve.invokeExact(terrain,x,y,z,liquid,flooded);}catch(Throwable error){throw failure(error);}}
    private static RuntimeException failure(Throwable error){if(error instanceof InvocationTargetException)error=((InvocationTargetException)error).getCause();if(error instanceof Error)throw (Error)error;return error instanceof RuntimeException?(RuntimeException)error:new IllegalStateException("Better Caves native adapter failed",error);}
}
