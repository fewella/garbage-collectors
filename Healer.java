import bc.*;
import java.util.ArrayList;
import java.util.Queue;

public class Healer {
    public static void run(GameController gc) {
        Queue<Unit> healers = Player.healer;
        Team team = gc.team();
        Team enemy = (team==Team.Blue) ? Team.Red:Team.Blue;

        for(Unit currentHealer:healers) {
            // Unit currentHealer = units.get(i);

            if(currentHealer.unitType().equals(UnitType.Healer)) { // Start actual healer code
                MapLocation myMapLocation = null;

                try {
                    myMapLocation = currentHealer.location().mapLocation();
                } catch(Exception e){
                    continue;
                }

                int id = currentHealer.id();
                VecUnit friends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.attackRange(), team);

                // If can heal, search for friend with lowest health and heal
                // TODO: Should take unit type into account at some point
                // TODO: Should also check distance to friendly units, and not move out of healing range of them if safe
                long minhealth = 9999;
                int targetID = -1;
                Location targetLoc = null;
                if(gc.isHealReady(id)) {
                    for (int i = 0; i < friends.size(); i++) {
                        Unit friend = friends.get(i);
                        if (friend.health() < minhealth) {
                            minhealth = friend.health();
                            targetID = friend.id();
                            targetLoc = friend.location();
                        }
                    }
                    if(targetID != 1 && gc.canHeal(id, targetID)) {  // Should ALWAYS be true
                        gc.heal(id, targetID);
                    }
                }
                else {  // If can't heal, still set target to someone
                    targetLoc = friends.get(0).location();
                }

                // Run away from nearest enemy, or move towards most damaged friend
                // TODO: Should run away from largest "cluster" of enemies rather than closest

                VecUnit notFriends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.visionRange(), enemy);


                if(gc.isMoveReady(id)) { // Just move towards unhealthiest ranger
                    Direction[] dirs = {Direction.Northwest, Direction.North, Direction.Northeast, Direction.West, Direction.East, Direction.Southwest, Direction.South, Direction.Southeast};

                    long closestEnemyDistance = 9999l;
                    MapLocation closestEnemyLoc = null;  // If this is null at the end, indicates sees no bad guys.

                    for (int i = 0; i < notFriends.size(); i++) {
                        long currentDist = myMapLocation.distanceSquaredTo(notFriends.get(i).location().mapLocation());
                        if (currentDist < closestEnemyDistance) {
                            closestEnemyDistance = currentDist;
                            closestEnemyLoc = notFriends.get(i).location().mapLocation();
                        }
                    }
                    if (closestEnemyLoc != null) { // Means NOT clear of baddies

                        Direction enemyDirOpp = bc.bcDirectionOpposite(myMapLocation.directionTo(closestEnemyLoc));
                        if(gc.canMove(id, enemyDirOpp))
                            gc.moveRobot(id, enemyDirOpp);
                    }
                    else {
                        ArrayList<MapLocation> targetLocs = new ArrayList<MapLocation>();
                        targetLocs.add(targetLoc.mapLocation());
                        int[][] out = MapAnalysis.BFS(targetLocs);

                        int x = myMapLocation.getX();
                        int y = myMapLocation.getY();

                        int minInd = -1;
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
                        }

                    }
                }
            }
        }
    }

}
