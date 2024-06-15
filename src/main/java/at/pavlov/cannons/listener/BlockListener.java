package at.pavlov.cannons.listener;


import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.ProjectileProperties;
import at.pavlov.cannons.utils.Direction3D;
import at.pavlov.cannons.utils.RayTrace;
import at.pavlov.cannons.utils.Vector3D;
import at.pavlov.cannons.utils.WrappedLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockListener implements Listener
{
    private static Cannons plugin;

    public BlockListener(Cannons plugins)
    {
        plugin = plugins;
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
    static BukkitTask bt = null;
    public static void cannonBallHit(ProjectileImpactEvent event) {

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
                    totalX += x-2;
                    totalY += y-2;
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
//        Bukkit.broadcastMessage("Centroid: " + Arrays.toString(centroid));
        //============<Real Matrix Initialization>===============
        double[][] matrix = new double[entryCount][3];
        entryCount = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                Double value = locMap[x+2][y+2];
                if (value == null){
                    continue;
                }
                double[] row = direction.getDoubleArrayFromDirectionVector(x, y, value);
//                new Location(event.getImpactLocation().getWorld(), row[0], row[1], row[2]).add(event.getImpactLocation().getBlockX(), event.getImpactLocation().getBlockY(), event.getImpactLocation().getBlockZ() ).getBlock().setType(Material.BEDROCK);
                row[0]-=centroid[0];
                row[1]-=centroid[1];
                row[2]-=centroid[2];

                matrix[entryCount][0] = row[0];
                matrix[entryCount][1] = row[1];
                matrix[entryCount][2] = row[2];

//                    bc(Arrays.toString(row));
                entryCount++;
            }
        }

        // Convert to EJML matrix
        DMatrixRMaj X = new DMatrixRMaj(matrix);
//        Bukkit.broadcastMessage(X+"");
//        Bukkit.broadcastMessage("==================");

        // Compute the centroid
        // Center the points

        // Perform SVD
        org.ejml.interfaces.decomposition.SingularValueDecomposition<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(true, true, true);
        svd.decompose(X);

        // Extract the plane normal from the smallest singular value
        //U * Σ * V^T
        DMatrixRMaj V = svd.getV(null, false);
//        Bukkit.broadcastMessage(V+"");
//        Bukkit.broadcastMessage(svd.numberOfSingularValues() + "" + svd.getW(null));
        DMatrixRMaj normalVector = new DMatrixRMaj(3, 1);
        DMatrixRMaj W = svd.getW(null);
        double w0 = W.get(0,0), w1 = W.get(1,1), w2 = W.get(2,2);
        double wmin = Math.min(w0, Math.min(w1,w2));
        if(wmin == w0) CommonOps_DDRM.extract(V, 0, 3, 0, 1, normalVector, 0, 0);
        else if(wmin == w1) CommonOps_DDRM.extract(V, 0, 3, 1, 2, normalVector, 0, 0);
        else if(wmin == w2) CommonOps_DDRM.extract(V, 0, 3, 2, 3, normalVector, 0, 0);

        //====================<Penetration Calculation>===============
        double pen = event.getProjectile().getPenetration()*(new Random().nextGaussian()*0.15+1);
//        bc("starting pen" + pen);
//        Bukkit.broadcastMessage("Pen " + pen);
        Vector projVelocity = event.getProjectileEntity().getVelocity();
        Vector perp = new Vector(normalVector.get(0), normalVector.get(1), normalVector.get(2));
//        Bukkit.getPlayer("Arhke").teleport(event.getImpactLocation().clone().setDirection(perp));
//        Bukkit.broadcastMessage(perp.toString());
        Player player = Bukkit.getPlayer(event.getShooterUID());
        assert player != null;
        if(bt != null && !bt.isCancelled()){
            bt.cancel();
        }
        bt = new BukkitRunnable() {
            int i = 0;
            @Override
            public void run(){
                Location locc = event.getImpactLocation();
                player.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA,
                        new Location(event.getImpactLocation().getWorld(), locc.getX() + centroid[0],
                                locc.getY() + centroid[1], locc.getZ() + centroid[2]), 5);
                double d = locc.getX()*normalVector.get(0) + locc.getY()*normalVector.get(1) + locc.getZ()*normalVector.get(2);
                for(double x = -2; x < 2; x+=0.5){
                    for(double y = -2; y < 2; y+=0.5){
                        double z = (d-(locc.getX()+x)*normalVector.get(0) - (locc.getY()+y)*normalVector.get(1))/normalVector.get(2);
                        player.spawnParticle(Particle.DRIPPING_DRIPSTONE_WATER,
                                new Location(event.getImpactLocation().getWorld(), locc.getX() + x,
                                locc.getY() + y, z), 5);
                    }
                }


                if (i++ > 1200){
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
        double impactCos = Math.abs(Math.cos(projVelocity.angle(perp)));
        if (impactCos < 0.34202014332){

            if (Math.random() > 0.5)player.sendMessage("Ricochet ~ !");
            else player.sendMessage("We didn't even scratch them.");
            player.getWorld().playSound(event.getImpactLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, 5);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, 5);
            event.setCancelled(true);
            return;
        }
        else{
            pen *= impactCos;
        }




        //==================<RayCasting>==============
        RayTrace rt = new RayTrace(projVelocity);
        Location rayLoc = event.getProjectileEntity().getLocation().clone();
        List<Block> blockList = new ArrayList<>();
        boolean hasReachedWall = false;
        int yetToReachWall = 0;
        boolean superBreaker = event.getProjectile().hasProperty(ProjectileProperties.SUPERBREAKER);
        while(pen>=0){
            //Trace to the wall
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
                        return;
                    }
                }
            }else if(!hasReachedWall){
                hasReachedWall = true;
            }

            Vector nextVector = rt.nextInterSection(rayLoc);
            pen-=nextVector.length();
            if (pen <= 0 && !superBreaker)break; //didn't pen
            if(nextVector.length() > 0.1 && rayLoc.getBlock().getType() != Material.AIR){
                superBreaker = false;
                blockList.add(rayLoc.getBlock());
            }
            rayLoc.add(nextVector);
        }

        if (pen > 0) {
            //penned
            if(event.getProjectile().getPenetrationDamage()) {
                EntityExplodeEvent bee = new EntityExplodeEvent(event.getProjectileEntity(), event.getImpactLocation(), blockList,
                        0);
                Bukkit.getPluginManager().callEvent(bee);
                if(!bee.isCancelled())
                    blockList.forEach(Block::breakNaturally);
            }
            if (Math.random() > 0.3)player.sendMessage(ChatColor.GREEN+"Penetration~");
            else if (Math.random() > 0.5) player.sendMessage(ChatColor.GREEN+"They're Hit!");
            else player.sendMessage(ChatColor.GREEN+"Good shot! Let's get them again!");
//            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 3, 3);
        }else{
            //did not Pen
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 3, 3);
            if (Math.random() > 0.3)player.sendMessage("Exploded on Impact!");
            else if (Math.random() > 0.5) player.sendMessage("Kaboom!");
            else player.sendMessage("BOOM!");
        }

    }
}
