package at.pavlov.cannons.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Custom extension of the {@link ExplosionDamageCalculator}.
 * Design Goal: Override Vanilla blast resistance of blocks, and entities.
 */
public class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {
    //4.3 resistance per power for block resistance, 1.72 for radius per power

    /**
     *
     * @param explosion
     * @param blockGetter
     * @param blockPos
     * @param blockState
     * @param fluidState
     * @return the Blast Resistance of a given block.
     */
    // TODO: Implement in an Explosion listener using *pure* spigot-api || paper-api.
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if(blockState.isAir() && fluidState.isEmpty())
            return Optional.empty();
        else {
            // Extract Block
            Block block = blockState.getBlock();

            // Initiate at higher value: Blocks' base blast resistance, or it's fluid state's blast resistance.
            float blastResistance = Math.max(block.getExplosionResistance(), fluidState.getExplosionResistance());

            // Override waterlogged blast resistance with default
            blastResistance = blastResistance == Blocks.WATER.getExplosionResistance()? block.getExplosionResistance():blastResistance;

            // Begin Named Tweaks
            String blockName = block.getDescriptionId();

            // Wood(plank) / Sandstone -based blocks: Increment Once
            if(blockName.contains("sandstone") || blockName.contains("wood") || blockName.contains("plank")){
                blastResistance++;
            }

            // All types of brick: BR = 210%
            if(blockName.contains("brick")){
                blastResistance*=2.1;
            }

            // All "reinforced" blocks: BR = 10001f
            if(blockName.contains("reinforced")){
                blastResistance=10001;
            }

            // If blast resistance > 1000: keep as is. Otherwise, BR is 35, unless already lower.
            blastResistance = blastResistance > 10000? blastResistance: Math.min(blastResistance, 35);

            return Optional.of(blastResistance);
        }
    }

    /**
     *
     * @param explosion
     * @param entity
     *
     * @return entityDamageAmount
     */
    public float getEntityDamageAmount(Explosion explosion, Entity entity) {
        // Set damage equal to explosion diameter.
        float damage = explosion.radius() * 2.0F;
        Vec3 groundZero = explosion.center();

        // dCofactor = sqrt(distance) / damage
        double distanceCofactor = Math.sqrt(entity.distanceToSqr(groundZero)) / (double)damage;

        // dsCofactor = (1 - dCofactor) * seenPercent[gz,e]
        double dsCofactor = (1.0D - distanceCofactor) * (double)Explosion.getSeenPercent(groundZero, entity);

        // (dsCofactor^3) / 2 * 7 * dmg + 1
        return (float)((dsCofactor * dsCofactor + dsCofactor) / 2.0D * 7.0D * (double)damage + 1.0D);
    }

}
