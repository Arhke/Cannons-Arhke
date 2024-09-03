package at.pavlov.cannons.event;

import java.util.UUID;

import org.antlr.v4.runtime.misc.NotNull;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import at.pavlov.cannons.projectile.Projectile;

public class ProjectileImpactEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final org.bukkit.entity.Projectile projectile_entity;
    private Projectile projectile;
    private Location impactLocation;
    private final UUID shooter;
    private final float explosion_power;
    private boolean cancelled;

    public ProjectileImpactEvent(@NotNull org.bukkit.entity.Projectile projectile_entity, Projectile projectile, float explosion_power, Location impactLocation, UUID shooter) {
        this.projectile_entity = projectile_entity;
        this.projectile = projectile;
        this.explosion_power = explosion_power;
        this.impactLocation = impactLocation;
        this.shooter = shooter;
        this.cancelled = false;
    }

    public UUID getShooterUID() {
	return this.shooter;
    }

    public @NotNull Location getImpactLocation() {
	return this.impactLocation;
    }

    public void setImpactLocation(Location impactLocation) {
	this.impactLocation = impactLocation;
    }

    public Projectile getProjectile() {
	return this.projectile;
    }

    public void setProjectile(Projectile projectile) {
	this.projectile = projectile;
    }

    public org.bukkit.entity.Projectile getProjectileEntity() {
        return projectile_entity;
    }

    public boolean isCancelled() {
	return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
	this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }

    public float getExplosionPower() {
        return explosion_power;
    }
}
