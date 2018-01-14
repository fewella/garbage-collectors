import bc.*;
// import the API.
// See xxx for the javadocs.



class ComBot {
   static Direction[] dirs = Direction.values();
   static GameController gc;
   static int fights=8;
   static  Location[] fight=new Location[fights];
   static Unit[] myR=new Unit[200];
   static int rangers=0;
   static void turn(GameController gameC) {
      gc=gameC;
      VecUnit us=gc.units();
   }
}