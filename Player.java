import bc.*;
import java.util.*;

public class Player {
    static Direction[] dirs  = Direction.values();

    //global variables
    static GameController gc;
    static PlanetMap map, mapEarth, mapMars;
    static Veci32 arrayEarth, arrayMars;
    static Queue<Unit> worker, knight, ranger, healer, mage, factory, rocket;

    public static void main(String[] args) {
        try {
            //connect to the manager, starting the game
            gc = new GameController();
            map = gc.startingMap(gc.planet());
            mapEarth = gc.startingMap(Planet.Earth);
            mapMars = gc.startingMap(Planet.Mars);

            MapAnalysis.setup();
            Econ.setup();
            Rocket.occupied = new int[(int)mapMars.getHeight()][(int)mapMars.getWidth()];

            //queue research
            gc.queueResearch(UnitType.Ranger); //25
            gc.queueResearch(UnitType.Ranger); //125
            gc.queueResearch(UnitType.Worker); //150
            gc.queueResearch(UnitType.Rocket); //250
            gc.queueResearch(UnitType.Ranger); //450
            gc.queueResearch(UnitType.Worker); //525
            gc.queueResearch(UnitType.Worker); //600
            gc.queueResearch(UnitType.Worker); //675
            gc.queueResearch(UnitType.Healer); //700
            gc.queueResearch(UnitType.Healer); //800
            
            ComBot.init(gc);
        }
        catch(Exception e){
            System.out.println("Exception during setup");
            e.printStackTrace();
        }
        while (true) {
            try{
                //game cycle
                worker = new LinkedList<>();
                knight = new LinkedList<>();
                ranger = new LinkedList<>();
                healer = new LinkedList<>();
                mage = new LinkedList<>();
                factory = new LinkedList<>();
                rocket = new LinkedList<>();

                VecUnit units = gc.myUnits();
                //System.out.println("Units "+ units.size());
                for(int i = 0; i < units.size(); i++) {
                    Unit temp = units.get(i);
                    if (temp.unitType()==UnitType.Worker) {
                        worker.add(temp);
                        //System.out.println("added worker");
                    }
                    else if (temp.unitType()==UnitType.Knight)
                        knight.add(temp);
                    else if (temp.unitType()==UnitType.Ranger)
                        ranger.add(temp);
                    else if (temp.unitType()==UnitType.Healer)
                        healer.add(temp);
                    else if (temp.unitType()==UnitType.Mage)
                        mage.add(temp);
                    else if (temp.unitType()==UnitType.Factory)
                        factory.add(temp);
                    else
                        rocket.add(temp);
                }
                MapAnalysis.turn();
                Econ.turn(gc);
                Rocket.launch(gc);
                ComBot.turn();
                Healer.run(gc);
                gc.nextTurn();
            }
            catch(Exception e){
                System.out.println("Exception during game");
                e.printStackTrace();
            }
        }
    }
}