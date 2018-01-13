import bc.*;

public class Healer {
    public static void run(GameController gc) {
        Team team = gc.team();
        Team enemy;

        if(team.equals(Team.Red))
            enemy = Team.Blue;
        else
            enemy = Team.Red;

        VecUnit units = gc.myUnits();
        for(int i = 0; i < units.size(); i++) { //...there's gotta be a better way to do this
            Unit currentHealer = units.get(i);

            if(currentHealer.unitType().equals(UnitType.Healer)) { // Start actual healer code

                MapLocation myMapLocation = currentHealer.location().mapLocation();
                int id = currentHealer.id();
                VecUnit friends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.attackRange(), team);

                // If can heal, search for friend with lowest health and heal
                // TODO: Should take unit type into account at some point
                // TODO: Should also check distance to friendly units, and not move out of healing range of them if safe
                long minhealth = 9999;
                int targetID = -1;
                if(gc.isHealReady(id)) {
                    for (int j = 0; j < friends.size(); j++) {
                        Unit friend = friends.get(j);
                        if (friend.health() < minhealth) {
                            minhealth = friend.health();
                            targetID = friend.id();
                        }
                    }
                    if(gc.canHeal(id, targetID)) {  // Should ALWAYS be true
                        gc.heal(id, targetID);
                    }
                }

                // Run away from nearest enemy
                // TODO: Should run away from largest "cluster" of enemies rather than closest

                VecUnit notFriends = gc.senseNearbyUnitsByTeam(myMapLocation, currentHealer.visionRange(), enemy);

                if(gc.isMoveReady(id)) {
                    long closestEnemyDistance = 9999l;
                    MapLocation closestEnemyLoc = null;

                    for (int j = 0; i < notFriends.size(); i++) { 
                        long currentDist = currentHealer.location().mapLocation().distanceSquaredTo(notFriends.get(j).location().mapLocation());
                        if (currentDist < closestEnemyDistance) {
                            closestEnemyDistance = currentDist;
                            closestEnemyLoc = notFriends.get(j).location().mapLocation();
                        }
                    }
                    Direction enemyDir = currentHealer.location().mapLocation().directionTo(closestEnemyLoc);
                    if (gc.canMove(id, enemyDir)) {
                        gc.moveRobot(id, enemyDir);
                    }
                }
            }
        }
    }

}
