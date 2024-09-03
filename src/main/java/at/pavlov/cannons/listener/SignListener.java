package at.pavlov.cannons.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import at.pavlov.cannons.cannon.CannonManager;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.config.Config;
import at.pavlov.cannons.config.UserMessages;

public class SignListener implements Listener
{
	@SuppressWarnings("unused")
	private final Config config;
	@SuppressWarnings("unused")
	private final UserMessages userMessages;
	private final Cannons plugin;
	private final CannonManager cannonManager;

	
	public SignListener(Cannons plugin)
	{
		this.plugin = plugin;
		this.config = this.plugin.getMyConfig();
		this.userMessages = this.plugin.getMyConfig().getUserMessages();
		this.cannonManager = this.plugin.getCannonManager();
	}
	
	/**
	 * @param signChangeEvent fired upon sign change.
	 */
	@EventHandler
	public void signChange(SignChangeEvent signChangeEvent)
	{
		if (signChangeEvent.getBlock() instanceof WallSign)
		{
			Block block = signChangeEvent.getBlock();
			Sign s = (Sign) signChangeEvent.getBlock().getState();
			BlockData signBlockData = s.getBlockData();
			Block cannonBlock;
			if (signBlockData instanceof Directional directional) {
				cannonBlock = block.getRelative(directional.getFacing().getOppositeFace());
			} else {
				return;
			}

			//get cannon from location and creates a cannon if not existing
	        Cannon cannon = cannonManager.getCannon(cannonBlock.getLocation(), signChangeEvent.getPlayer().getUniqueId());
			
	        //get cannon from the sign
			//TODO - supply getCannon (Component)
			Cannon cannonFromSign = CannonManager.getCannon(String.valueOf(signChangeEvent.line(0)));
			
			//if the sign is placed against a cannon - no problem
			//if the sign has the name of other cannon - change it
			if(cannon == null && cannonFromSign  != null)
			{
				TextColor red = TextColor.fromHexString("#FF0000");
				final TextComponent inConflictSign = Component.text()
						.color(red).content("This sign is in conflict with cannons").build();
				signChangeEvent.getPlayer().sendMessage(inConflictSign);

				final TextComponent cannons = Component.text().content("[Cannons]").build();
				final TextComponent genericPlayer = Component.text().content("Player").build();
				signChangeEvent.line(0, cannons);
				signChangeEvent.line(1, genericPlayer);
			}

            //if there is a cannon and the sign is mounted on the sign interface
			if (cannon != null && cannon.isCannonSign(block.getLocation()))
			{
				final TextComponent line1 = Component.text().content(cannon.getSignString(0)).build();
				final TextComponent line2 = Component.text().content(cannon.getSignString(1)).build();
				final TextComponent line3 = Component.text().content(cannon.getSignString(2)).build();
				final TextComponent line4 = Component.text().content(cannon.getSignString(3)).build();

				signChangeEvent.line(0, line1);
				signChangeEvent.line(1, line2);
				signChangeEvent.line(2, line3);
				signChangeEvent.line(3, line4);
			}
		}
	}
}

