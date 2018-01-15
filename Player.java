// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;

public class Player {
    static Direction[] dirs  = Direction.values();

    //global variables
    static GameController gc;
    static PlanetMap map, mapEarth, mapMars;
    static Veci32 arrayEarth, arrayMars;

    public static void main(String[] args) {
    	Queue<Unit> worker = new LinkedList<Unit>();
        Queue<Unit> knight = new LinkedList<Unit>();
        Queue<Unit> ranger = new LinkedList<Unit>();
        Queue<Unit> healer = new LinkedList<Unit>();
        Queue<Unit> mage = new LinkedList<Unit>();
        Queue<Unit> factory = new LinkedList<Unit>();
        Queue<Unit> rocket = new LinkedList<Unit>(); 
        try {
            //connect to the manager, starting the game
            gc = new GameController();
            map = gc.startingMap(gc.planet());
            mapEarth = gc.startingMap(Planet.Earth);
            mapMars = gc.startingMap(Planet.Mars);

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
                arrayEarth = gc.getTeamArray(Planet.Earth);
                arrayMars = gc.getTeamArray(Planet.Mars);

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
                Econ.turn(gc, worker, factory);

                gc.nextTurn();
            }
            catch(Exception e){
                System.out.println("Exception during game");
                e.printStackTrace();
            }
        }
    }
}