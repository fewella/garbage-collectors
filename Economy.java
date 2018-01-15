// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;
class Econ {
   static Direction[] dirs = Direction.values();
   static void turn(GameController gc, Queue<Unit> workers, Queue<Unit> factory) {
      //VecUnit us=gc.myUnits();
      long karb=gc.karbonite();
      if( workers.size() == 0)
    	  System.out.println("no workers");
      while (workers.peek() != null) {
         Unit u=workers.remove();
         VecUnit v=gc.senseNearbyUnitsByType(u.location().mapLocation(),2,UnitType.Factory);
         for (long k=v.size()-1; k>=0; k--) {
            if (gc.canBuild(u.id(),v.get(k).id())) {
               gc.build(u.id(),v.get(k).id());
            }
         }
         for (int k=0; k<8; k++) {
            if (gc.canBlueprint(u.id(),UnitType.Factory, dirs[k])) {
               gc.blueprint(u.id(),UnitType.Factory, dirs[k]);
               break;
            }
         }
      }   
      while (factory.peek() != null) {
         Unit u=factory.remove();
         if (gc.canProduceRobot(u.id(),UnitType.Ranger))
            gc.produceRobot(u.id(),UnitType.Ranger);
         for (int k=0; k<8; k++)
            if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
      }
   }
}