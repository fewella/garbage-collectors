// import the API.
// See xxx for the javadocs.
import bc.*;
class Econ {
   static Direction[] dirs = Direction.values();
   static void turn(GameController gc) {
      VecUnit us=gc.myUnits();
      long karb=gc.karbonite();
      if (karb>=100) {
         for (long i=us.size()-1; i>=0; i--) {
            Unit u=us.get(i);
            if (u.unitType()==UnitType.Worker) {
               for (int k=0; k<8; k++) {
                  if (gc.canBlueprint(u.id(),UnitType.Factory, dirs[k])) {
                     gc.blueprint(u.id(),UnitType.Factory, dirs[k]);
                     break;
                  }
               }
            }
         }   
      }
      for (long i=us.size()-1; i>=0; i--) {
         Unit u=us.get(i);
         if (u.unitType()==UnitType.Factory) {
            if (gc.canProduceRobot(u.id(),UnitType.Ranger)) {
               gc.produceRobot(u.id(),UnitType.Ranger);
            }
            for (int k=0; k<8; k++) {
               if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
            }
         }
      }
   }
}