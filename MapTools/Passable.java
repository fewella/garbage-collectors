package MapTools;

import bc.*;

public class Passable{
    private static GameController gc;
    private static short[][] passabilityMatEarth, passabilityMatMars;
    private static short passableTotalEarth, passableTotalMars;

    public static void setup(GameController gameC){
        gc = gameC;
        passabilityMat(gc.startingMap(Planet.Earth));
        passabilityMat(gc.startingMap(Planet.Mars));
    }
    public static short[][] matrix(Planet p){
        if(p == Planet.Earth)
            return passabilityMatEarth;
        else
            return passabilityMatMars;
    }
    public static short total(Planet p){
        if(p == Planet.Earth)
            return passableTotalEarth;
        else
            return passableTotalMars;
    }
    private static void passabilityMat(PlanetMap pm){
        //same outputs as PlanetMap.isPassableTerrainAt
        short[][] out = new short[(int)pm.getHeight()][(int)pm.getWidth()];
        short total = 0;
        for(int y = 0; y < pm.getHeight(); y++){
            for(int x = 0; x < pm.getWidth(); x++){
                out[y][x] = pm.isPassableTerrainAt(new MapLocation(pm.getPlanet(), x, y));
                if(out[y][x] == 1) total++;
            }
        }
        if(pm.getPlanet() == Planet.Earth){
            passabilityMatEarth = out;
            passableTotalEarth = total;
        }
        else{
            passabilityMatMars = out;
            passableTotalMars = total;
        }
    }
}
