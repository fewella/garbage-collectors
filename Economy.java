// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;
class Econ {
   static Direction[] dirs = Direction.values();
   static HashMap<Integer, int[][]> workerBFSMats;
   static HashMap<Integer, Boolean> seen;
   static int stage;
   static void turn(GameController gc, Queue<Unit> workers, Queue<Unit> factory) {
      //VecUnit us=gc.myUnits();
      long karb=gc.karbonite();
      //if( workers.size() == 0)
    //	  System.out.println("no workers");
      for(Unit u : workers) {
         if(!u.location().isOnMap()) continue;
         MapLocation mapLoc = u.location().mapLocation();
         //movement
         if(seen.get(u.id()) == null){
            ArrayList<MapLocation> temp = new ArrayList<>();
            temp.add(MapAnalysis.tempWorkerLoc(u));
            workerBFSMats.put(u.id(), MapAnalysis.BFS(temp));
            seen.put(u.id(), true);
         }
         if(gc.isMoveReady(u.id()) && workerBFSMats != null){
            Direction minD = Direction.Center;
            int min = 9999;
            for(Direction d : dirs){
               MapLocation newLoc = mapLoc.add(d);
               if(workerBFSMats.get(u.id())[newLoc.getY()][newLoc.getX()] < min){
                  min = workerBFSMats.get(u.id())[newLoc.getY()][newLoc.getX()];
                  minD = d;
               }
            }
            if(stage == 0){
               if(min == 0){
                  try {
                     Unit v = gc.senseUnitAtLocation(mapLoc.add(minD));
                     if (gc.canBuild(u.id(),v.id())) {
                        gc.build(u.id(),v.id());
                     }
                  }
                  catch(Exception e){
                     if(gc.canBlueprint(u.id(),UnitType.Factory, minD)){
                        gc.blueprint(u.id(),UnitType.Factory, minD);
                     }
                  }
               }
               else{
                  if(gc.canMove(u.id(), minD))
                     gc.moveRobot(u.id(), minD);
               }
            }
            else{
               if(gc.canMove(u.id(), minD))
                  gc.moveRobot(u.id(), minD);
               if(min == 0){
                  workerBFSMats.put(u.id(), null);
               }
            }
         }
         else{
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
      }   
      while (factory.peek() != null) {
         Unit u=factory.remove();
         //temporary hot garbage
         //remove later
         if(stage == 0){
            stage = 1;
            for(Unit v : workers){
               ArrayList<MapLocation> temp = new ArrayList<>();
               temp.add(MapAnalysis.tempWorkerLoc(v));
               workerBFSMats.put(v.id(), MapAnalysis.BFS(temp));
               seen.put(v.id(), true);
            }
         }
         if(Player.ranger.size() < 3) {
            if (gc.canProduceRobot(u.id(), UnitType.Ranger))
               gc.produceRobot(u.id(), UnitType.Ranger);
         }
         if(karb > 300){
            if (gc.canProduceRobot(u.id(), UnitType.Worker))
               gc.produceRobot(u.id(), UnitType.Worker);
         }
         //end of temporary hot garbage
         if (gc.canProduceRobot(u.id(), UnitType.Ranger))
            gc.produceRobot(u.id(), UnitType.Ranger);
         for (int k=0; k<8; k++)
            if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
      }
   }
}