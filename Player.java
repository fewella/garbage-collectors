// import the API.
// See xxx for the javadocs.
import bc.*;

import java.util.PriorityQueue;

public class Player {
    static Direction[] dirs  = Direction.values();

    //global variables
    static GameController gc;
    static PlanetMap map;

    public static void main(String[] args) {
        try {
            //connect to the manager, starting the game
            gc = new GameController();
            map = gc.startingMap(gc.planet());

            //map analysis
            MapAnalysis.setup();

            //queue research
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Worker);
            gc.queueResearch(UnitType.Rocket);
            gc.queueResearch(UnitType.Ranger);
        }
        catch(Exception e){
            System.out.println("Exception during setup");
            e.printStackTrace();
        }
        while (true) {
            try{
                //game cycle
                Econ.turn(gc);

                gc.nextTurn();
            }
            catch(Exception e){
                System.out.println("Exception during game");
                e.printStackTrace();
            }
        }
    }
}