package net.tfminecraft.marketblock.inventory;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.tlibs.objects.api.subapi.StringFormatter;
import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.trade.Category;

public class CategoryView {
    // Keep the existing legacy text representation, formatting, and exact-string comparisons.
    @SuppressWarnings("deprecation")
    public void categoryView(Inventory inv, Player p) {
        boolean open = false;
        if(inv == null) {
            inv = MarketBlock.plugin.getServer().createInventory(new MBHolder(MBGUI.CATEGORY, "none"), 54, "§7Market Categories");
            open = true;
        }
        List<Category> categories = CategoryLoader.getAsList();
		for(int i = 0; i < Cache.slots.size() && i < categories.size(); i++) {
            int slot = Cache.slots.get(i);
            Category cat = categories.get(i);
            inv.setItem(slot, createCategoryItem(cat));
        }
		int slotn = 0;
		while(slotn < inv.getSize()) {
			if(inv.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				inv.setItem(slotn, fill);
			}
			slotn++;
		}
		if(open) p.openInventory(inv);
	}

    // Keep the existing legacy text representation, formatting, and exact-string comparisons.
    @SuppressWarnings("deprecation")
    public ItemStack createCategoryItem(Category cat) {
        ItemStack i = cat.getItem();
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#e6ca40§lCategory§e: "+cat.getName()));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#50d990"+cat.getTrades().size()+" #b8906eTrades"));
        lore.add(StringFormatter.formatHex("#c9c14fClick to View"));
        m.setLore(lore);
        NamespacedKey key = new NamespacedKey(MarketBlock.plugin, "category_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, cat.getId());
        i.setItemMeta(m);
        return i;
    }
}
