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
      System.out.println("karbonite: " + karb);
      //if( workers.size() == 0)
    //	  System.out.println("no workers");
      boolean madeRocket = false;
      int replicate = 0;
      HashSet<Unit> stayFactory = new HashSet<Unit>(); //these units will not go out to look for karbonite/make new factories
    //Factory production
      for (Unit u : Player.factory) {
        VecUnit wF = gc.senseNearbyUnitsByType(u.location().mapLocation(), 2, UnitType.Worker);
        for(int i = 0; i < wF.size() && i < 2; i++) {
        	Unit temp = wF.get(i);
        	stayFactory.add(temp);
        	if(u.health() == u.maxHealth()) {
	        	for( int k = 0; k < 8; k++) {
	        		if( gc.canReplicate(temp.id(), dirs[k])) {
	        			 gc.replicate(temp.id(), dirs[k]); 
	        		}
	        	}
        	}
        	else {
        		if(gc.canBuild(temp.id(), u.id()))
	        		 gc.build(temp.id(), u.id());
        	}
        }
         //uncomment when Healers move away
         /*if(Player.ranger.size()/6 > Player.healer.size()){
            if (gc.canProduceRobot(u.id(), UnitType.Healer))
               gc.produceRobot(u.id(), UnitType.Healer);
         }*/
         //else{
            if (gc.canProduceRobot(u.id(), UnitType.Ranger))
               gc.produceRobot(u.id(), UnitType.Ranger);
         //}
         for (int k=0; k<8; k++)
            if (gc.canUnload(u.id(),dirs[k])) gc.unload(u.id(),dirs[k]);
      }
      for(Unit u : Player.worker) {
         if(!u.location().isOnMap()) continue;
         MapLocation mapLoc = u.location().mapLocation();
         //mars
         if( mapLoc.getPlanet() == Planet.Mars ) {
        	 
         }
         //earth
         else if (!stayFactory.contains(u)){
        	 boolean doneAction = false;
	         VecUnit nearRock = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Rocket);
	         if( gc.round()>250 && nearRock.size() != 0 ) {
	        	 if(gc.canBuild(u.id(), nearRock.get(0).id())) {
	        		 gc.build(u.id(), nearRock.get(0).id());
	        		 doneAction = true;
	        	 }
	        	 else if(gc.canLoad(nearRock.get(0).id(), u.id())) {
	        		 gc.load(nearRock.get(0).id(), u.id());
	        		 System.out.println("worker loaded");
	        		 doneAction = true;
	        	 }
	         }
	         if( gc.round() > 250 && !madeRocket && Player.rocket.size() == 0 && !doneAction) {
	        	 //move from factories
	        	 for( int k = 0; k < 8; k++) {
	        		 if(gc.canBlueprint(u.id(), UnitType.Rocket, dirs[k])) {
	        			 gc.blueprint(u.id(), UnitType.Rocket, dirs[k]);
	        			 madeRocket = true;
	        		 }
	        	 }
	         }
	         VecUnit nearFac = gc.senseNearbyUnitsByType(u.location().mapLocation(), 4, UnitType.Factory);
	         if( nearFac.size() == 0 ) {
	            for (int k=0; k<8; k++) {
	               if (gc.canBlueprint(u.id(),UnitType.Factory, dirs[k])) {
	                  gc.blueprint(u.id(),UnitType.Factory, dirs[k]);
	                  break;
	               }
	            }
	         }
	         if( nearFac.size() != 0 ) {
	        	Unit fac = nearFac.get(0);
	        	Direction avoid = u.location().mapLocation().directionTo(fac.location().mapLocation());
	        	for( int k = 0; k<8; k++ ) {
	        		if(!dirs[k].equals(avoid)) {
	        			if(gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k])) 
	        				gc.moveRobot(u.id(), dirs[k]);
	        		}
	        	}
	         }
	         /*if( (Player.worker.size() <= Player.factory.size()*2 && gc.round() < 150 && replicate < 3) || Player.worker.size() <= Player.factory.size()*1.5) {
	        	 for( int k = 0; k < 8; k++) {
	        		 if( gc.canReplicate(u.id(), dirs[k])) {
	        			 gc.replicate(u.id(), dirs[k]);
	        			 replicate++; 
	        			 System.out.println("replicated");
	        		 }
	        	 }
	         }*/
	         /*if( gc.round() < (225/u.workerBuildHealth())+1 ) {
	        	VecUnit v=gc.senseNearbyUnitsByType(u.location().mapLocation(),2,UnitType.Factory);
	            for (long k=v.size()-1; k>=0; k--) {
	               if (gc.canBuild(u.id(),v.get(k).id())) {
	                  gc.build(u.id(),v.get(k).id());
	               }
	            }
	         }*/
	         for (int k=0; k<8; k++) {
	            if(gc.canHarvest(u.id(), dirs[k])) {
	               if(gc.round() > 1 && karb < 400) {
	                  gc.harvest(u.id(), dirs[k]);
	                  break;
	               }
	            }
	         }
         }
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