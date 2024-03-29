import bc.*;
import java.util.ArrayList;
import java.util.Queue;

public class Healer {

    static int[][] out;

    public static void run(GameController gc) {
        boolean searchedForRangers = false;
        boolean fly = false;
        Queue<Unit> healers = Player.healer;
        Queue<Unit> rockets = Player.rocket;
        ArrayList<MapLocation> rocketLocs = new ArrayList<MapLocation>();
        Team team = gc.team();
        Team enemy = (team==Team.Blue) ? Team.Red:Team.Blue;

        for(Unit r : rockets) {
            if((r.health() > (4 * r.maxHealth() / 5) || r.structureIsBuilt() != 0) && r.structureGarrison().size() < r.structureMaxCapacity() && r.rocketIsUsed() == 0) {
                rocketLocs.add(r.location().mapLocation());
                fly = true;
            }
        }

        if(fly)
            out = MapAnalysis.BFS(rocketLocs);

        for(Unit currentHealer:healers) {

            if(currentHealer.unitType().equals(UnitType.Healer)) { // Start actual healer code, should ALWAYS be true
                MapLocation myMapLocation;

                try {
                    myMapLocation = currentHealer.location().mapLocation(); //Test if on rocket or something
                } catch(Exception e){
                    continue;
                }

                // Healing logic
                int id = currentHealer.id();
                VecUnit friends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.attackRange(), team);

                // If can heal, search for friend with lowest health and heal

                double minhealth = 9999;
                int targetID = -1;

                if(gc.isHealReady(id)) {
                    for (int i = 0; i < friends.size(); i++) {
                        Unit friend = friends.get(i);
                        if (friend.health()/friend.maxHealth() < minhealth && !(gc.planet() == Planet.Mars && friend.unitType() == UnitType.Worker)) {  // Don't heal workers on Mars
                            minhealth = friend.health()/friend.maxHealth();
                            targetID = friend.id();
                        }
                    }
                    if(targetID != 1 && gc.canHeal(id, targetID)) {  // Should ALWAYS be true
                        gc.heal(id, targetID);
                    }
                }


                // Moving logic

                if(!(searchedForRangers || fly)) {
                    ArrayList<MapLocation> targetLocs = new ArrayList<MapLocation>();
                    for(Unit ranger : Player.ranger) {
                        if(ranger.health() < ranger.maxHealth()) {
                            try {
                                targetLocs.add(ranger.location().mapLocation());
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    }
                    out = MapAnalysis.BFS(targetLocs);
                    searchedForRangers = true;
                    //print(out);
                }

                VecUnit notFriends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.visionRange(), enemy);

                if(gc.isMoveReady(id)) { // Just move towards unhealthiest ranger
                    Direction[] dirs = {Direction.Southwest, Direction.South, Direction.Southeast, Direction.West, Direction.East, Direction.Northwest, Direction.North, Direction.Northeast};

                    long closestEnemyDistance = 9999l;
                    MapLocation closestEnemyLoc = null;  // If this is null at the end, indicates sees no bad guys.

                    for (int i = 0; i < notFriends.size(); i++) {
                        long currentDist = myMapLocation.distanceSquaredTo(notFriends.get(i).location().mapLocation());
                        if (currentDist < closestEnemyDistance) {
                            closestEnemyDistance = currentDist;
                            closestEnemyLoc = notFriends.get(i).location().mapLocation();
                        }
                    }
                    if (closestEnemyLoc != null) {

                        Direction enemyDirOpp = bc.bcDirectionOpposite(myMapLocation.directionTo(closestEnemyLoc));
                        if(gc.canMove(id, enemyDirOpp)) {
                            gc.moveRobot(id, enemyDirOpp);
                        }
                    }
                    else {

                        Direction bestDir = Direction.Center;
                        int min = 99999;
                        for(Direction dir : dirs) {
                            MapLocation curLoc = myMapLocation.add(dir);
                            if(curLoc.getX() < 0 || curLoc.getY() < 0 || curLoc.getX() >= gc.startingMap(gc.planet()).getWidth() || curLoc.getY() >= gc.startingMap(gc.planet()).getHeight())
                                continue;
                            int curMin = out[curLoc.getY()][curLoc.getX()];
                            if(curMin < min && gc.canMove(id, dir)) {
                                min = curMin;
                                bestDir = dir;
                            }
                        }

                        if(gc.canMove(id, bestDir))
                            gc.moveRobot(id, bestDir);

                        /*int x = myMapLocation.getX();
                        int y = myMapLocation.getY();

                        int minInd = -1;

                        //ArrayList<Direction> dirs = new ArrayList<Direction>();
                        //ArrayList<Integer> dists = new ArrayList<Integer>();


                        if(y > 0 && x > 0 && y < out.length - 1 && x < out[0].length - 1) {
                            int[] dists = {out[y - 1][x - 1], out[y][x - 1], out[y + 1][x - 1], out[y - 1][x], out[y + 1][x], out[y - 1][x - 1], out[y][x - 1], out[y + 1][x - 1]};
                            int min = 99999;
                            for(int i = 0; i < 8; i++) {
                                if(dists[i] < min) {
                                    minInd = i;
                                    min = dists[i];
                                }
                            }
                        }

                        if(minInd != -1 && gc.canMove(id, dirs[minInd])) {
                            gc.moveRobot(id, dirs[minInd]);
                        }*/

                    }
                }
            }
        }
    }

}
