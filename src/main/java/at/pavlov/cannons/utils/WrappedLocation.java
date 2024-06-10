package at.pavlov.cannons.utils;

import org.bukkit.Location;
public class WrappedLocation {
    Location _loc;
    public WrappedLocation(Location loc){
        _loc = loc.clone();
    }

    /**
     * a way to add value to location without changing original location
     */
    public Location add(double x, double y, double z){
        return new Location(_loc.getWorld(), _loc.getX() + x, _loc.getY() + y, _loc.getZ() + z);
    }
    public void addModify(double x, double y, double z){
        _loc.add(x, y, z);
    }
    public Location get(){
        return _loc.clone();
    }
}
