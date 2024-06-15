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
import org.bukkit.Bukkit;

import java.util.Optional;

public class CustomExplosionDamageCalculator extends ExplosionDamageCalculator {
//4.3 resistance per power for block resistance, 1.72 for radius per power
    public Optional<Float> getBlockExplosionResistance(Explosion var0, BlockGetter var1, BlockPos var2, BlockState var3, FluidState var4) {
        if(var3.isAir() && var4.isEmpty()) return Optional.empty();
        else {
            Block block = var3.getBlock();
            float br = Math.max(block.getExplosionResistance(), var4.getExplosionResistance());
            br = br == Blocks.WATER.getExplosionResistance()? block.getExplosionResistance():br;
            String blockName = block.getDescriptionId();

            if(blockName.contains("sandstone")){
                br++;
            }
            if(blockName.contains("wood") || blockName.contains("plank")){
                br++;
            }
            if(blockName.contains("brick")){
                br*=2.1;
            }
            if(blockName.contains("reinforced")){
                br=10001;
            }
            br = br > 10000? br: Math.min(br, 35);
//            Bukkit.broadcastMessage(blockName + " " + br);

            return Optional.of(br);
        }
    }
    public float getEntityDamageAmount(Explosion var0, Entity var1) {
        float dmg = var0.radius() * 2.0F;
        Vec3 center = var0.center();
        double distanceCofactor = Math.sqrt(var1.distanceToSqr(center)) / (double)dmg;
        double dsCofactor = (1.0D - distanceCofactor) * (double)Explosion.getSeenPercent(center, var1);
        return (float)((dsCofactor * dsCofactor + dsCofactor) / 2.0D * 7.0D * (double)dmg + 1.0D);
    }

}
