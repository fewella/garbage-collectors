// import the API.
// See xxx for the javadocs.
import bc.*;
import java.util.*;

class Econ {
   static Direction[] dirs = Direction.values();
   static HashMap<Integer, int[][]> workerBFSMats;
   static ArrayList<MapLocation> workerLocs;
   static HashMap<Integer, Boolean> seen;
   static int stage;
   static void turn(GameController gc) {
      Collections.shuffle(Arrays.asList(dirs));
      //VecUnit us=gc.myUnits();
      long karb=gc.karbonite();
      //if( workers.size() == 0)
    //	  System.out.println("no workers");
      boolean madeRocket = false;
      boolean replicate = false;
      for(Unit u : Player.worker) {
         if(!u.location().isOnMap()) continue;
         MapLocation mapLoc = u.location().mapLocation();
         //movement
         for (int k=0; k<8; k++) {
            if(gc.canHarvest(u.id(), dirs[k])) {
               if(gc.round() > 1 && karb < 400) {
                  gc.harvest(u.id(), dirs[k]);
                  break;
               }
            }
         }
         VecUnit nearRock = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Rocket);
         if( nearRock.size() != 0 ) {
        	 if(gc.canBuild(u.id(), nearRock.get(0).id()))
        		 gc.build(u.id(), nearRock.get(0).id());
        	 else if(gc.canLoad(nearRock.get(0).id(), u.id())) {
        		 gc.load(nearRock.get(0).id(), u.id());
        		 System.out.println("worker loaded");
        	 }
         }
         if( gc.round() > 250 && !madeRocket && Player.rocket.size() == 0 && Player.factory.size() >= Player.worker.size() ) {
        	 //move from factories
        	 for( int k = 0; k < 8; k++) {
        		 if(gc.canBlueprint(u.id(), UnitType.Rocket, dirs[k])) {
        			 gc.blueprint(u.id(), UnitType.Rocket, dirs[k]);
        			 madeRocket = true;
        		 }
        	 }
         }
         if( (Player.worker.size() <= Player.factory.size() && gc.round() < 150) || Player.worker.size() < 3) {
        	 for( int k = 0; k < 8; k++) {
        		 if( gc.canReplicate(u.id(), dirs[k])) {
        			 gc.replicate(u.id(), dirs[k]);
        			 replicate = true; 
        		 }
        	 }
         }
         if(!seen.containsKey(u.id())){
            ArrayList<MapLocation> temp = new ArrayList<>();
            temp.add(MapAnalysis.tempWorkerLoc(u));
            workerBFSMats.put(u.id(), MapAnalysis.BFS(temp));
            seen.put(u.id(), true);
            System.out.println("Sent new worker " + u.id() + " to x:" + temp.get(0).getX() + ", y:" + temp.get(0).getY());
         }
         if(workerBFSMats.get(u.id()) != null && nearRock.size() == 0){
            Direction minD = Direction.Center;
            int min = 9999;
            for (Direction d : dirs) {
               MapLocation newLoc = mapLoc.add(d);
               if (newLoc.getX() < 0 || newLoc.getY() < 0 || newLoc.getX() >= Player.mapEarth.getWidth() || newLoc.getY() >= Player.mapEarth.getHeight())
                  continue;
               int newMin = workerBFSMats.get(u.id())[newLoc.getY()][newLoc.getX()];
               if (newMin < min && gc.canMove(u.id(), d) || newMin == 0) {
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
                  if(gc.isMoveReady(u.id()) && gc.canMove(u.id(), minD))
                     gc.moveRobot(u.id(), minD);
               }
            }
            else{
               boolean canLoad = true;
               if(nearRock.size() != 0)
            	   canLoad = stayByRocket(gc, u, nearRock.get(0));
               if(canLoad) {
            	   if(gc.isMoveReady(u.id()) && gc.canMove(u.id(), minD))
            		   gc.moveRobot(u.id(), minD);
            	   if(min == 0){
            		   workerBFSMats.put(u.id(), null);
            	   }
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
      //Factory production
      for (Unit u : Player.factory) {
         if(u.health() == u.maxHealth() && stage == 0){
            stage = 1;
            for(Unit v : Player.worker){
               ArrayList<MapLocation> temp = new ArrayList<>();
               temp.add(MapAnalysis.tempWorkerLoc(v));
               workerBFSMats.put(v.id(), MapAnalysis.BFS(temp));
               System.out.println("Sent worker " + v.id() + " to x:" + temp.get(0).getX() + ", y:" + temp.get(0).getY());
            }
         }
         /*else if(karb > 300 && Player.ranger.size() > 10){
            if (gc.canProduceRobot(u.id(), UnitType.Worker))
               gc.produceRobot(u.id(), UnitType.Worker);
         }*/
         //uncomment when Healers move away
         /*else if(Player.ranger.size()/6 > Player.healer.size()){
            if (gc.canProduceRobot(u.id(), UnitType.Healer))
               gc.produceRobot(u.id(), UnitType.Healer);
         }*/
         else{
            if (gc.canProduceRobot(u.id(), UnitType.Ranger))
               gc.produceRobot(u.id(), UnitType.Ranger);
         }
         for (int k=0; k<8; k++)
            if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
      }
   }
   public static boolean stayByRocket(GameController gc, Unit u, Unit r) {
	   if( !u.location().mapLocation().isAdjacentTo(r.location().mapLocation()) ) {
		   if(gc.isMoveReady(u.id()) && gc.canMove(u.id(), u.location().mapLocation().directionTo(r.location().mapLocation()))) {
			   gc.moveRobot(u.id(), u.location().mapLocation().directionTo(r.location().mapLocation()));
			   return false;
		   }
		   return true;
	   }
	   return true;
   }
}