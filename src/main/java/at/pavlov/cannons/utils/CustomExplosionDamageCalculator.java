package at.pavlov.cannons.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

public class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {

    public Optional<Float> getBlockExplosionResistance(Explosion var0, BlockGetter var1, BlockPos var2, BlockState var3, FluidState var4) {
        if(var3.isAir() && var4.isEmpty()) return Optional.empty();
        else {
            float br = Math.max(var3.getBlock().getExplosionResistance(), var4.getExplosionResistance());
            br = br == Blocks.WATER.getExplosionResistance()? 0:br;
            return Optional.of(br > 10000? br: Math.min(br, 9));
        }
    }

}
