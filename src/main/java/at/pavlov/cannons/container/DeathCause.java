package at.pavlov.cannons.container;

import at.pavlov.cannons.projectile.Projectile;

import java.util.UUID;

public class DeathCause {
    private final Projectile projectile;
    private final UUID cannonUID;
    private final UUID shooterUID;

    public DeathCause(Projectile projectile, UUID cannonUID, UUID shooterUID){
        this.projectile = projectile;
        this.cannonUID = cannonUID;
        this.shooterUID = shooterUID;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public UUID getCannonUID() {
        return cannonUID;
    }

    public UUID getShooterUID() {
        return shooterUID;
    }
}
