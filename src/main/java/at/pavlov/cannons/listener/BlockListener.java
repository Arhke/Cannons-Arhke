package at.pavlov.cannons.listener;


import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.container.ItemHolder;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.utils.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import org.apache.commons.math.exception.OutOfRangeException;
import org.apache.commons.math.linear.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.craftbukkit.v1_20_R4.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BlockListener implements Listener
{
	private final Cannons plugin;

	public BlockListener(Cannons plugin)
	{
		this.plugin = plugin;
	}


    @EventHandler
    public void blockExplodeEvent(BlockExplodeEvent event) {
        if (plugin.getMyConfig().isRelayExplosionEvent()) {
            EntityExplodeEvent explodeEvent = new EntityExplodeEvent(null, event.getBlock().getLocation(), event.blockList(), event.getYield());
            Bukkit.getServer().getPluginManager().callEvent(explodeEvent);
            event.setCancelled(explodeEvent.isCancelled());
        }

        //cannons event - remove unbreakable blocks like bedrock
        //this will also affect other plugins which spawn bukkit explosions
        List<Block> blocks = event.blockList();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            for (BlockData unbreakableBlock : plugin.getMyConfig().getUnbreakableBlocks()) {
                if (unbreakableBlock.matches(block.getBlockData())) {
                    blocks.remove(i--);
                }
            }
        }

        //search for destroyed cannons
        plugin.getEntityListener().ExplosionEventHandler(event.blockList());
    }

    /**
     * Water will not destroy button and torches
     * @param event
     */
    @EventHandler
    public void BlockFromTo(BlockFromToEvent event)
    {
        Block block = event.getToBlock();
        Cannon cannon = plugin.getCannonManager().getCannon(block.getLocation(), null);
        if (cannon !=  null)//block.getType() == Material.STONE_BUTTON || block.getType() == Material.WOOD_BUTTON || block.getType() == Material.   || block.getType() == Material.TORCH)
        {
            if (cannon.isCannonBlock(block))
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * prevent fire on cannons
     * @param event
     */
    @EventHandler
    public void BlockSpread(BlockSpreadEvent  event)
    {
        Block block = event.getBlock().getRelative(BlockFace.DOWN);
        Cannon cannon = plugin.getCannonManager().getCannon(block.getLocation(), null);

        if (cannon !=  null)
        {
            if (cannon.isCannonBlock(block))
            {
                event.setCancelled(true);
            }
        }
    }


    /**
     * retraction pistons will trigger this event. If the pulled block is part of a cannon, it is canceled
     * @param event - BlockPistonRetractEvent
     */
    @EventHandler
    public void BlockPistonRetract(BlockPistonRetractEvent event)
    {
        // when piston is sticky and has a cannon block attached delete the
        // cannon
        if (event.isSticky())
        {
            Location loc = event.getBlock().getRelative(event.getDirection(), 2).getLocation();
            Cannon cannon = plugin.getCannonManager().getCannon(loc, null);
            if (cannon != null)
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * pushing pistons will trigger this event. If the pused block is part of a cannon, it is canceled
     * @param event - BlockPistonExtendEvent
     */
    @EventHandler
    public void BlockPistonExtend(BlockPistonExtendEvent event)
    {
        // when the moved block is a cannonblock
        for (Iterator<Block> iter = event.getBlocks().iterator(); iter.hasNext();)
        {
            // if moved block is cannonBlock delete cannon
            Cannon cannon = plugin.getCannonManager().getCannon(iter.next().getLocation(), null);
            if (cannon != null)
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * if the block catches fire this event is triggered. Cannons can't burn.
     * @param event - BlockBurnEvent
     */
    @EventHandler
    public void BlockBurn(BlockBurnEvent event)
    {
        // the cannon will not burn down
        if (plugin.getCannonManager().getCannon(event.getBlock().getLocation(), null) != null)
        {
            event.setCancelled(true);
        }
    }

    /**
     * if one block of the cannon is destroyed, it is removed from the list of cannons
     * @param event - BlockBreakEvent
     */
    @EventHandler
    public void BlockBreak(BlockBreakEvent event)
    {

        Cannon cannon = plugin.getCannonManager().getCannon(event.getBlock().getLocation(), null);
        if (cannon != null)
        {
            //breaking is only allowed when the barrel is broken - minor stuff as buttons are canceled
            //you can't break your own cannon in aiming mode
            //breaking cannon while player is in selection (command) mode is not allowed
            Cannon aimingCannon = null;
            if (plugin.getAiming().isInAimingMode(event.getPlayer().getUniqueId()))
                 aimingCannon = plugin.getAiming().getCannonInAimingMode(event.getPlayer());

            if (cannon.isDestructibleBlock(event.getBlock().getLocation()) && (aimingCannon==null||!cannon.equals(aimingCannon)) && !plugin.getCommandListener().isSelectingMode(event.getPlayer())) {
                plugin.getCannonManager().removeCannon(cannon, false, true, BreakCause.PlayerBreak);
                plugin.logDebug("cannon broken:  " + cannon.isDestructibleBlock(event.getBlock().getLocation()));
            }
            else {
                event.setCancelled(true);
                plugin.logDebug("cancelled cannon destruction: " + cannon.isDestructibleBlock(event.getBlock().getLocation()));
            }
        }

        //if the the last block on a cannon is broken and signs are required
        if (event.getBlock().getBlockData() instanceof WallSign){
            WallSign sign = (WallSign) event.getBlock().getBlockData();
            cannon = plugin.getCannonManager().getCannon(event.getBlock().getRelative(sign.getFacing().getOppositeFace()).getLocation(), null);
            plugin.logDebug("cancelled cannon sign  " + event.getBlock().getRelative(sign.getFacing().getOppositeFace()));
            if (cannon != null && cannon.getCannonDesign().isSignRequired() && cannon.getNumberCannonSigns() <= 1) {
                plugin.logDebug("cancelled cannon sign destruction");
                event.setCancelled(true);
            }
        }

    }
    BukkitTask bt = null;
    @EventHandler
    public void cannonBallHit(ProjectileImpactEvent event) {

        List<Vector3D> locList = new ArrayList<>();

        //===================<Block Parsing>====================
        WrappedLocation impact = new WrappedLocation(event.getImpactLocation());
        double xSum = 0, ySum = 0, zSum = 0;
        for (int z = -2; z <= 2; z++){
            for (int y = -2; y <= 2; y++){
                for (int x = -2; x<=2; x++) {
                    Location l = impact.add(x, y, z);
                    Material material = l.getBlock().getType();
                    if(material.isBlock() && material.isSolid()){
                        locList.add(new Vector3D(x, y, z));
                        xSum+= Integer.compare(x, 0);
                        ySum+=Integer.compare(y, 0);
                        zSum+=Integer.compare(z, 0);
                    }
                }
            }
        }

        if (locList.size() == 0){
            return;
        }
        //=============<POV Casting>=======================
        Vector vProj = event.getImpactLocation().getDirection().clone().normalize().multiply(5);
        Direction3D direction = Direction3D.getDirectionFromCentroid(xSum + vProj.getX(), ySum + vProj.getY(), zSum + vProj.getZ());
        // directionized X then directionized Y (these two are offset by +2)
        double totalX = 0, totalY = 0, totalZ = 0;
        Double[][] locMap = new Double[5][5];
        int entryCount = 0;
        for(Vector3D entry: locList){
            try{
                int x = (int)direction.getVectorX(entry)+2,
                        y = (int)direction.getVectorY(entry)+2,
                        z = (int)direction.getVectorZ(entry);
                if (locMap[x][y] == null) {
                    entryCount++;
                    totalX += x;
                    totalY += y;
                    totalZ += z;
                    locMap[x][y] = (double) z;
                }else if (direction.isPositive() && locMap[x][y] > z || !direction.isPositive() && locMap[x][y] < z){
                    totalZ-=locMap[x][y];
                    totalZ+=z;
                    locMap[x][y] = (double) z;
                }
            }catch (ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
            }
        }
        //============<Defining Centroid>===============
        totalX/=entryCount;
        totalY/=entryCount;
        totalZ/=entryCount;
        double[] centroid = direction.getDoubleArrayFromDirectionVector(totalX, totalY, totalZ);
        //============<Real Matrix Initialization>===============
        RealMatrix matrix = new Array2DRowRealMatrix(entryCount,3);
        entryCount = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                Double value = locMap[x+2][y+2];
                if (value == null){
                    continue;
                }
                try {
                    double[] row = direction.getDoubleArrayFromDirectionVector(x, y, value);
                    new Location(event.getImpactLocation().getWorld(), row[0], row[1], row[2]).add(event.getImpactLocation().getBlockX(), event.getImpactLocation().getBlockY(), event.getImpactLocation().getBlockZ() ).getBlock().setType(Material.GLASS);
                    row[0]-=centroid[0];
                    row[1]-=centroid[1];
                    row[2]-=centroid[2];

                    matrix.setRow(entryCount, row);

//                    bc(Arrays.toString(row));
                    entryCount++;
                }catch(OutOfRangeException e){
                    e.printStackTrace();
                }
            }
        }


        //=============<EigenVector>======================
        matrix = matrix.transpose().multiply(matrix);
        EigenDecomposition ed = new EigenDecompositionImpl(matrix, 1);
        RealVector normalVector;
        try{
            normalVector = ed.getEigenvector(2);
//            bc(ed.getEigenvector(0)+" " + ed.getRealEigenvalue(0));
//            bc(ed.getEigenvector(1)+" " + ed.getRealEigenvalue(1));
//            bc(ed.getEigenvector(2)+" " + ed.getRealEigenvalue(2));
        }
        catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            return;
        }


        //====================<Penetration Calculation>===============
        double pen = event.getProjectile().getPenetration()+(Math.random()*0.6-0.3)*event.getProjectile().getPenetration();
//        bc("starting pen" + pen);
//        Bukkit.broadcastMessage("Pen " + pen);
        Vector projVelocity = event.getProjectileEntity().getVelocity();
        Vector perp = new Vector(normalVector.getEntry(0), normalVector.getEntry(1), normalVector.getEntry(2));
        Bukkit.broadcastMessage(perp.toString());
        if(bt != null && !bt.isCancelled()){
            bt.cancel();
        }
        bt = new BukkitRunnable() {
            int i = 0;
            @Override
            public void run(){
                WrappedLocation wl = new WrappedLocation(event.getImpactLocation());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                event.getImpactLocation().getWorld().spawnParticle(Particle.FALLING_DRIPSTONE_LAVA, wl.get(), 5);
                wl.addModify(perp.getX(), perp.getY(), perp.getZ());
                if (i++ > 9999){
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
        double impactCos = Math.abs(Math.cos(projVelocity.angle(perp)));
//        bc("impactCos" + impactCos);
        Player player = Bukkit.getPlayer(event.getShooterUID());
        assert player != null;
        if (impactCos < 0.34202014332){

            if (Math.random() > 0.5)player.sendMessage("Ricochet ~ !");
            else player.sendMessage("We didn't even scratch them.");
            player.getWorld().playSound(event.getImpactLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, 1);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, 1);
            event.setCancelled(true);
            return;
        }
        else{
            pen *= impactCos;
        }




        //==================<RayCasting>==============
        double startPen = pen;
        RayTrace rt = new RayTrace(projVelocity);
        Location rayLoc = event.getProjectileEntity().getLocation().clone();
        List<Block> blockList = new ArrayList<>();
        boolean hasReachedWall = false;
        int yetToReachWall = 0;
        int damage = 0;
        while(pen>0){
//            bc(rayLoc.getBlock().getType()+"");
//            bc(rayLoc.getBlock().getType().isSolid()+"");
            if (!rayLoc.getBlock().getType().isSolid()){
                if (hasReachedWall){
                    break;
                }else{
                    Vector nextVector = rt.nextInterSection(rayLoc);
                    rayLoc.add(nextVector);
                    yetToReachWall++;
                    if(yetToReachWall < 10){
                        continue;
                    }else{
                        event.setCancelled(true);
                        return;
                    }
                }
            }else if(!hasReachedWall){
                hasReachedWall = true;
            }

            Vector nextVector = rt.nextInterSection(rayLoc);
            try {

//                float blastResist = blast.getInt(-1,rayLoc.getBlock().getType().name());
                float blastResist = (float)(Math.random()*2);
                pen-= blastResist == -1?1:blastResist;
                if (pen <= 0) break;
                if(nextVector.length() > 0.1 && rayLoc.getBlock().getType() != Material.AIR){
                    if(blastResist == -1)blockList.add(rayLoc.getBlock());
                    else damage++;
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
                return;
            }
            rayLoc.add(nextVector);
        }
        double blastDamage = event.getProjectile().getExplosionPower();

        if (pen > 0) {
            //penned
            blockList.forEach(Block::breakNaturally);
            if (Math.random() > 0.3)player.sendMessage(ChatColor.GREEN+"Penetration.");
            else if (Math.random() > 0.5) player.sendMessage(ChatColor.GREEN+"They're Hit!");
            else player.sendMessage(ChatColor.GREEN+"Good shot. Let's get them again!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 3, 3);
            Bukkit.broadcastMessage("damaged: " + damage);
//            cd.getTown().bankDeposit(-damage);
//            if(cd.getTown().getBank() == 0){
//            }
            Explosion explosion = new Explosion(((CraftWorld) Objects.requireNonNull(event.getImpactLocation().getWorld())).getHandle(),
                    null, null,  new CustomExplosionDamageCalculator(), event.getImpactLocation().getX(), event.getImpactLocation().getY(), event.getImpactLocation().getZ(),
                    event.getProjectile().getExplosionPower(), event.getProjectile().isProjectileOnFire(), Explosion.BlockInteraction.DESTROY, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
            explosion.explode();
            explosion.finalizeExplosion(true);
        }else{
            //did not Pen
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 3, 3);
            if (Math.random() > 0.3)player.sendMessage("We weren't able to penetrate them.");
            else if (Math.random() > 0.5) player.sendMessage("That one bounced right off.");
            else player.sendMessage("That one didn't go through.");
            Bukkit.broadcastMessage("damaged: "+blastDamage*0.15);
            Explosion explosion = new Explosion(((CraftWorld) Objects.requireNonNull(event.getImpactLocation().getWorld())).getHandle(),
                    null, null,  new CustomExplosionDamageCalculator(), event.getImpactLocation().getX(), event.getImpactLocation().getY(), event.getImpactLocation().getZ(),
                    event.getProjectile().getExplosionPower(), event.getProjectile().isProjectileOnFire(), Explosion.BlockInteraction.DESTROY, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
            explosion.explode();
            explosion.finalizeExplosion(true);
//            cd.getTown().bankDeposit(-blastDamage*0.15);
//            if(cd.getTown().getBank() == 0){
//            }
//            List<Block> blockBreaks = new ArrayList<>();
//            for(int i = -1; i <= 1; i++) {
//                for (int j = -1; j <= 1; j++) {
//                    for (int k = -1; k <= 1; k++) {
//                        Block block = new WrappedLocation(event.getImpactLocation()).add(i,j,k).getBlock();
//                        if(block.getType().getBlastResistance() < blastDamage){
//                            blockBreaks.add(block);
//                        }
//                    }
//                }
//            }
//            new EntityExplodeEvent(event.getProjectileEntity(),event.getImpactLocation(), blockBreaks,1f);
//            blockBreaks.forEach(Block::breakNaturally);


            // if block dura is less than blast then break block
        }

        event.setCancelled(true);
    }
}
