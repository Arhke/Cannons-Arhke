package at.pavlov.cannons.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class RayTrace {
    Vector vNorm;
    public RayTrace(Vector vector){
        this.vNorm = vector.clone().normalize();
    }

    /**
     *
     * @param loc
     * @return the vector to add to get it to the next location
     */
    public Vector nextInterSection(Location loc) {
        double toNextX = ((vNorm.getX()> 0?Math.floor(loc.getX())+1:Math.ceil(loc.getX())-1) - loc.getX()) / vNorm.getX(),
                toNextY = ((vNorm.getY()> 0?Math.floor(loc.getY())+1:Math.ceil(loc.getY())-1) - loc.getY()) / vNorm.getY(),
                toNextZ = ((vNorm.getZ()> 0?Math.floor(loc.getZ())+1:Math.ceil(loc.getZ())-1) - loc.getZ()) / vNorm.getZ();
        return this.vNorm.clone().multiply(Math.min(toNextX, Math.min(toNextY, toNextZ)));
    }
}
