package mcjty.lostcities.core;

import java.io.*;
import java.nio.file.*;

/** Native clients can write level.dat before generating the first overworld chunk. */
public final class GenerationMode {
    public static boolean modern(File worldRoot,String dimensionFolder){
        if(!new File(worldRoot,"level.dat").isFile())return true;
        File dimensionRoot=dimensionFolder==null?worldRoot:new File(worldRoot,dimensionFolder);
        Path region=new File(dimensionRoot,"region").toPath();
        if(!Files.isDirectory(region))return true;
        try(DirectoryStream<Path> files=Files.newDirectoryStream(region,"*.mca")){return !files.iterator().hasNext();}
        catch(IOException error){throw new UncheckedIOException("Cannot determine saved generation mode for "+region,error);}
    }
    private GenerationMode(){}
}
