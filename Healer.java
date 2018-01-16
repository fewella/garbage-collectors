import bc.*;

import java.util.LinkedList;

public class Healer {
    public static void run(GameController gc, LinkedList<Unit> healers) {
        Team team = gc.team();
        Team enemy = (team==Team.Blue) ? Team.Red:Team.Blue;

        for(Unit currentHealer:healers) {
            // Unit currentHealer = units.get(i);

            if(currentHealer.unitType().equals(UnitType.Healer)) { // Start actual healer code

                MapLocation myMapLocation = currentHealer.location().mapLocation();
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

                // Run away from nearest enemy, or move towards most damaged friend
                // TODO: Should run away from largest "cluster" of enemies rather than closest

                VecUnit notFriends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.visionRange(), enemy);

                if(gc.isMoveReady(id)) {
                    long closestEnemyDistance = 9999l;
                    MapLocation closestEnemyLoc = null;  // If this is null at the end, indicates sees no bad guys.

                    for (int i = 0; i < notFriends.size(); i++) {
                        long currentDist = currentHealer.location().mapLocation().distanceSquaredTo(notFriends.get(i).location().mapLocation());
                        if (currentDist < closestEnemyDistance) {
                            closestEnemyDistance = currentDist;
                            closestEnemyLoc = notFriends.get(i).location().mapLocation();
                        }
                    }

                    if (closestEnemyLoc != null) {
                        Direction enemyDir = currentHealer.location().mapLocation().directionTo(closestEnemyLoc);
                        if(gc.canMove(id, enemyDir))
                            gc.moveRobot(id, enemyDir);
                    }
                    else {
                        gc.canMove(id, currentHealer.location().mapLocation().directionTo(targetLoc.mapLocation()));
                    }
                }
            }
        }
    }

}
