package at.pavlov.cannons.listener;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.container.ItemHolder;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;

public class EntityListener implements Listener
{
	private final Cannons plugin;
	
	public EntityListener(Cannons plugin)
	{
		this.plugin = plugin;
	}

	/**
	 * The projectile has hit an entity
	 * @param event
	 */
	@EventHandler
	public void onEntiyDeathEvent(EntityDeathEvent event) {
		plugin.getAiming().removeTarget(event.getEntity());
	}


    /**
     * The projectile has hit an entity
     * @param event
     */
	@EventHandler
	public void onProjectileHitEntity(EntityDamageByEntityEvent event)
	{
		Entity er = event.getDamager();
		if(event.getDamager() != null && er instanceof Projectile)
		{
			Projectile p = (Projectile) er;
			plugin.getProjectileManager().directHitProjectile(p, event.getEntity());
		}
	}

    /**
     * The projectile explosion has damaged an entity
     * @param event
     */
    @EventHandler
    public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent event)
    {
        //if (plugin.getProjectileManager().isFlyingProjectile(event.getDamager()))
        {
            //event.setCancelled(true);
            //plugin.logDebug("Explosion damage was canceled. Damage done: " + event.getDamage());
        }
    }

	/**
	 * Cannon snowball hits the ground
	 * 
	 * @param event
	 */
	@EventHandler
	public void ProjectileHit(ProjectileHitEvent event)
	{
        plugin.getProjectileManager().detonateProjectile(event.getEntity());
	}

	@EventHandler
	public void cannonBallHit(ProjectileImpactEvent event) {
		if (event.getImpactLocation() == null) {
			event.setCancelled(true);
			return;
		}
		battle.warnPlayers();
		if(cd.getTown().getBank() <= 0 || cd.getTown().isOnHeart(event.getImpactLocation())){
			if(event.getProjectile().getExplosionDamage()) {
				Explosion explosion = new Explosion(((CraftWorld) event.getImpactLocation().getWorld()).getHandle(),
						null, null, new CustomExplosionDamageCalculator(), event.getImpactLocation().getX(), event.getImpactLocation().getY(), event.getImpactLocation().getZ(),
						event.getProjectile().getExplosionPower(), event.getProjectile().isProjectileOnFire(), Explosion.BlockInteraction.DESTROY);
				explosion.explode();
				explosion.finalizeExplosion(true);
			}
			return;
		}


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
		double impactCos = Math.abs(Math.cos(projVelocity.angle(new Vector(normalVector.getEntry(0), normalVector.getEntry(1), normalVector.getEntry(2)))));
//        bc("impactCos" + impactCos);
		Player player = Bukkit.getPlayer(event.getShooterUID());
		assert player != null;
		if (impactCos < 0.34202014332){

			if (Math.random() > 0.5)player.sendMessage("Ricochet ~ !");
			else player.sendMessage("We didn't even scratch them.");
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 3);
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

				float blastResist = blast.getInt(-1,rayLoc.getBlock().getType().name());

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
			cd.getTown().bankDeposit(-damage);
			if(cd.getTown().getBank() == 0){
				player.sendMessage(ChatColor.GOLD+"Their protection is gone, this is our chance!");
			}

		}else{
			//did not Pen
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 3, 3);
			if (Math.random() > 0.3)player.sendMessage("We weren't able to penetrate them.");
			else if (Math.random() > 0.5) player.sendMessage("That one bounced right off.");
			else player.sendMessage("That one didn't go through.");
			Bukkit.broadcastMessage("damaged: "+blastDamage*0.15);
			cd.getTown().bankDeposit(-blastDamage*0.15);
			if(cd.getTown().getBank() == 0){
				player.sendMessage(ChatColor.GOLD+""+ ChatColor.BOLD + "Their protection is gone, this is our chance!");
			}
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

	
	/**
	 * handles the explosion event. Protects the buttons and torches of a cannon, because they break easily
	 * @param event
	 */
	@EventHandler
	public void EntityExplode(EntityExplodeEvent event)
	{
		plugin.logDebug("Explode event listener called");

		//do nothing if it is cancelled
		if (event.isCancelled())
			return;
		
		ExplosionEventHandler(event.blockList());
	}

    /**
     * searches for destroyed cannons in the explosion event and removes cannons parts which can't be destroyed in an explosion.
     * @param blocklist list of blocks involved in the event
     */
    public void ExplosionEventHandler(List<Block> blocklist){
        HashSet<UUID> remove = new HashSet<UUID>();

        // first search if a barrel block was destroyed.
        for (Block block : blocklist) {
            Cannon cannon = plugin.getCannonManager().getCannon(block.getLocation(), null);

            // if it is a cannon block
            if (cannon != null) {
                if (cannon.isDestructibleBlock(block.getLocation())) {
                    //this cannon is destroyed
                    remove.add(cannon.getUID());
                }
            }
        }

        //iterate again and remove all block of intact cannons
        for (int i = 0; i < blocklist.size(); i++)
        {
            Block block = blocklist.get(i);
            Cannon cannon = plugin.getCannonManager().getCannon(block.getLocation(), null);

            // if it is a cannon block and the cannon is not destroyed (see above)
            if (cannon != null && !remove.contains(cannon.getUID()))
            {
                if (cannon.isCannonBlock(block))
                {
                    blocklist.remove(i--);
                }
            }
        }

        //now remove all invalid cannons
        for (UUID id : remove)
            plugin.getCannonManager().removeCannon(id, false, true, BreakCause.Explosion);
    }
}
