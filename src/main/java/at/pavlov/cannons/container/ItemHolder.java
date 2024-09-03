package at.pavlov.cannons.container;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;


//small class as at.pavlov.cannons.container for item id and data
public class ItemHolder
{
	public static final @NotNull TextComponent AIR = Component.text("Air");
	public static final @NotNull TextComponent EMPTY_STRING = Component.text("");
	private Material material;
	private Component displayName;
	private List<Component> lore;
	private boolean useTypeName;

	private static final Class localeClass = null;
	private static final Class craftItemStackClass = null;
    private static final Class nmsItemStackClass = null;
    private static final Class nmsItemClass = null;
	private static final String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
	private static final String NMS_PREFIX = OBC_PREFIX.replace("org.bukkit.craftbukkit", "net.minecraft.server");

	public ItemHolder(ItemStack item)
	{
		useTypeName = false;
        if (item == null){
            material=Material.AIR;
			displayName = Component.text().content("").build();
            lore = new ArrayList<>();
            return;
        }

		material = item.getType();

		if (item.hasItemMeta()){
            ItemMeta meta = item.getItemMeta();
			if (meta.hasDisplayName() && meta.displayName() != null)
				displayName = meta.displayName();
			else if (!meta.hasDisplayName()){
				useTypeName = true;
				displayName = getFriendlyName(item, true);
				//Cannons.getPlugin().logDebug("display name: " + displayName);
			}
			else
				displayName = EMPTY_STRING;
			if (meta.hasLore() && meta.lore() != null)
				lore = meta.lore();
			else
				lore = new ArrayList<Component>();
		}
	}

    @Deprecated
    public ItemHolder(int id)
    {
    	//not working
        this(Material.AIR);
    }

    public ItemHolder(Material material)
    {
        this(material, null, null);
    }

	public ItemHolder(Material material, String description, List<Component> lore)
	{
        if (material != null)
		    this.material = material;
        else
            this.material = Material.AIR;
		if (description != null)
			//TODO ComponentAPI: Replace with Mini Message formatting || ensure color is translated. -Flag
			this.displayName = Component.text(ChatColor.translateAlternateColorCodes('&',description));
		else
			this.displayName = EMPTY_STRING;
		if (lore != null)
			this.lore = lore;
		else
			this.lore = new ArrayList<>();
	}

	public ItemHolder(String str)
	{
        // data structure:
        // id;DESCRIPTION;LORE1;LORE2
        // HOE;COOL Item;Looks so cool;Fancy
        try
        {
        	material = Material.AIR;
            Scanner s = new Scanner(str).useDelimiter("\\s*;\\s*");
            if (s.hasNext()) {
                String next = s.next();
                if (next != null)
                    this.material = Material.matchMaterial(next);
                if (this.material == null) {
                    this.material = Material.AIR;
                }
            }

			if (s.hasNext())
				//TODO ComponentAPI: Replace with Mini Message formatting || ensure color is translated. -Flag
				displayName = Component.text(ChatColor.translateAlternateColorCodes('&', s.next()));
			else
				displayName = EMPTY_STRING;

			lore = new ArrayList<>();
			while (s.hasNext()){
                String nextStr = s.next();
                if (!nextStr.isEmpty())
				    lore.add(Component.text(nextStr));
			}
            s.close();
        }
        catch(Exception e)
        {
            System.out.println("[CANNONS] Error while converting " + str + ". Check formatting (minecraft:clock)");
        }
	}
	
	public SimpleBlock toSimpleBlock()
	{
		return new SimpleBlock(0, 0, 0, material);
	}
	
	public ItemStack toItemStack(int amount)
	{
		ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (this.hasDisplayName())
			meta.displayName(this.displayName);
        if (this.hasLore())
            meta.lore(this.lore);
        item.setItemMeta(meta);
        return item;
	}

	/**
	 * Creates a new BlockData instance for this Material, with all properties initialized to unspecified defaults.
	 * @return BlockData instance
	 */
	public BlockData toBlockData()
	{
		return this.material.createBlockData();
	}

	/**
	 * Creates a new BlockData instance for this Material, with all properties initialized to unspecified defaults, except for those provided in data.
	 * @return BlockData instance
	 */
	public BlockData toBlockData(String string)
	{
		return this.material.createBlockData(string);
	}

    /**
     * compares the id of two Materials
     * @param material material to compare
     * @return true if both material are equal
     */
	public boolean equals(Material material)
	{
        return material != null && material.equals(this.material);
    }
	
	/**
	 * compares id and data, but skips data comparison if one is -1
	 * @param item item to compare
	 * @return true if both items are equal in data and id or only the id if one data = -1
	 */
	public boolean equalsFuzzy(ItemStack item)
	{
		ItemHolder itemHolder = new ItemHolder(item);
		return equalsFuzzy(itemHolder);
	}
	
	
	/**
	 * compares id and data, but skips data comparison if one is -1
	 * @param item the item to compare
	 * @return true if both items are equal in data and id or only the id if one data = -1
	 */	
	public boolean equalsFuzzy(ItemHolder item)
	{
		if (item != null)
		{
			//System.out.println("item: " + item.getDisplayName() + " cannons " + this.getDisplayName());
            //Item does not have the required display name
            if ((this.hasDisplayName() && !item.hasDisplayName()) || (!this.hasDisplayName() && item.hasDisplayName()))
                return false;
            //Display name do not match
			if (!item.displayName.equals(this.displayName))
				return false;

            if (this.hasLore()) {
                //does Item have a Lore
                if (!item.hasLore())
                    return false;

                Collection<Component> similar = new HashSet<>(this.lore);

                int size = similar.size();
				similar.retainAll(item.lore);
                if (similar.size() < size)
                    return false;
            }
			return item.getType().equals(this.material);
		}	
		return false;
	}
	
	/**
	 * compares id and data, but skips data comparison if one is -1
	 * @param block item to compare
	 * @return true if both items are equal in data and id or only the id if one data = -1
	 */
	public boolean equalsFuzzy(Block block)
	{
		//System.out.println("id:" + item.getId() + "-" + id + " data:" + item.getData() + "-" + data);
		if (block != null)
		{
            return block.getType().equals(this.material);
		}	
		return false;
	}
	
	public String toString()
	{
		return this.material + ":" + this.displayName + ":" + StringUtils.join(this.lore, ":");
	}

	public Material getType()
	{
		return this.material;
	}
	public void setType(Material material)
	{
		this.material = material;
	}

	public Component getDisplayName() {
		return displayName;
	}

    public boolean hasDisplayName(){
        return this.displayName!= null && !this.displayName.equals(EMPTY_STRING);
    }

	public List<Component> getLore() {
		return lore;
	}

    public boolean hasLore(){
		return !this.lore.isEmpty();
    }

	private static String capitalizeFully(String name) {
		if (name != null) {
			if (name.length() > 1) {
				if (name.contains("_")) {
					StringBuilder sbName = new StringBuilder();
					for (String subName : name.split("_"))
						sbName.append(subName.substring(0, 1).toUpperCase())
								.append(subName.substring(1).toLowerCase())
								.append(" ");
					return sbName.substring(0, sbName.length() - 1);
				} else {
					return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
				}
			} else {
				return name.toUpperCase();
			}
		} else {
			return "";
		}
	}

	private static Component getFriendlyName(Material material) {
		return material == null ? AIR : getFriendlyName(new ItemStack(material), false);
	}

	private static Component getFriendlyName(ItemStack itemStack, boolean checkDisplayName) {
		if (itemStack == null || itemStack.getType() == Material.AIR) return AIR;

		if (checkDisplayName && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
			return itemStack.getItemMeta().displayName();
		}
		return Component.text(capitalizeFully(itemStack.getType().name().replace("_", " ").toLowerCase()));
	}
}