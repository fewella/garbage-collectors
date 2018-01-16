// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;

class Econ {
   static Direction[] dirs = Direction.values();
   static void turn(GameController gc) {
	  Queue<Unit> workers = Player.worker;
	  Queue<Unit> factory = Player.factory;
      //VecUnit us=gc.myUnits();
      long karb=gc.karbonite();
      int wsize = workers.size();
      while (workers.peek() != null) {
         Unit u=workers.remove();
         for (int k=0; k<8; k++) {
         	if(gc.canHarvest(u.id(), dirs[k])) {
         		if(gc.round() > 1 && karb < 400) {
         			gc.harvest(u.id(), dirs[k]);
         			break;
         		}
         		continue;
         	}
             if (gc.canBlueprint(u.id(),UnitType.Factory, dirs[k])) {
                gc.blueprint(u.id(),UnitType.Factory, dirs[k]);
                break;
             }
         }
         VecUnit v=gc.senseNearbyUnitsByType(u.location().mapLocation(),2,UnitType.Factory);
         for (long k=v.size()-1; k>=0; k--) {
            if (gc.canBuild(u.id(),v.get(k).id())) {
               gc.build(u.id(),v.get(k).id());
               break;
            }
         }
         //if( gc.canSenseLocation(MapAnalysis.factoryQueue.peek().x) && )
         moveTo(gc, u, MapAnalysis.factoryQueue.peek().x);
      } 
      int rsize = Player.ranger.size();
      while (factory.peek() != null) {
         Unit u=factory.remove();
         if (rsize%8 == 0 && rsize != 0 && gc.canProduceRobot(u.id(), UnitType.Healer))
        	 gc.produceRobot(u.id(), UnitType.Healer);
         else if (gc.canProduceRobot(u.id(),UnitType.Ranger)) {
            gc.produceRobot(u.id(),UnitType.Ranger);
            rsize++;
         }
         for (int k=0; k<8; k++)
            if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
      }
   }
   public static void moveTo(GameController g, Unit w, MapLocation target) {
	   //fix
	   for (int k=0; k<8; k++) {
		   if(g.canMove(w.id(), dirs[k]) && g.isMoveReady(w.id())) {
			   g.moveRobot(w.id(), dirs[k]);
		   }
	   }
   }
}