package mcjty.lostcities;

import mcjty.lostcities.api.*;
import mcjty.lostcities.dimensions.world.WorldTypeTools;

public final class LostCitiesImp implements ILostCities {
    @Override public ILostChunkGenerator getLostGenerator(int dimension){return WorldTypeTools.getChunkGenerator(dimension);}
}
