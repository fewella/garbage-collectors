import bc.*;
import java.util.*;
class Rocket {
	static Direction[] dirs = Direction.values();
	static int[][] occupied;
	public static void launch(GameController gc) {
		for( Unit r:Player.rocket) {
			if(r.structureGarrison().size() != r.structureMaxCapacity() && r.location().isOnPlanet(Planet.Earth)) {
				VecUnit loUnit = gc.senseNearbyUnitsByTeam(r.location().mapLocation(),1 , gc.team());
				for( int i = 0; i < loUnit.size(); i++ ) {
					if(gc.canLoad(r.id(), loUnit.get(i).id()))
						gc.load(r.id(), loUnit.get(i).id());
				}
			}
			if(r.structureGarrison().size() == r.structureMaxCapacity() || r.health() < 120 || gc.round() > 500) {
				short[][] karb = MapAnalysis.karboniteMatMars((int)(gc.round()+gc.currentDurationOfFlight())).x;
				int minScore = 9999;
				MapLocation dest = new MapLocation(Planet.Mars, 10, 10);	//shouldn't happen
				for(int y = 0; y < Player.mapMars.getHeight(); y++){
					for(int x = 0; x < Player.mapMars.getWidth(); x++) {
						if (MapAnalysis.passabilityMatMars[y][x] == 0) continue;
						if (occupied[y][x] == 1) continue;
						int score = 5 * karb[y][x] - MapAnalysis.karboniteConnected(Planet.Mars, x, y);
						for (int dy = -1; dy <= 1; dy++) {
							if (y + dy < 0 || y + dy >= Player.mapMars.getHeight()) continue;
							for (int dx = -1; dx <= 1; dx++) {
								if (x + dx < 0 || x + dx >= Player.mapMars.getWidth()) continue;
								score -= karb[y+dy][x+dx];
							}
						}
						if(score < minScore){
							minScore = score;
							dest = new MapLocation(Planet.Mars, x, y);
						}
					}
				}
				if( gc.canLaunchRocket(r.id(), dest) ) {
					gc.launchRocket(r.id(), dest);
					occupied[dest.getY()][dest.getX()] = 1;
					System.out.println("Launched");
				}
				else
					System.out.println("Bad dest");
			}
			if( r.location().isOnPlanet(Planet.Mars) ) {
				for( int k = 0; k < 8; k++ ) {
					if( gc.canUnload(r.id(), dirs[k]) ) {
						gc.unload(r.id(), dirs[k]);
					}
				}
			}
		}
	}
}
