package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map.Entry;

public final class EnchantLimits implements Listener {

	private final Module module = Module.getModule("enchant-limits");
	private final ConfigurationSection section = module.getValue("limits");

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if(event.getClick() == ClickType.CREATIVE)
			return;

		Inventory inv = event.getClickedInventory();

		if(inv == null)
			return;

		ItemStack item = inv.getItem(event.getSlot());

		if(item == null) {
			return;
		}

		Player p = (Player) event.getWhoClicked();

		if(!module.affectsPlayer(p))
			return;

		ItemStack fixed = fixItem(item);

		if(fixed != null)
			inv.setItem(event.getSlot(), fixed);
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if(!(event.getEntity() instanceof Player))
			return;

		if(!module.affectsPlayer((Player) event.getEntity()))
			return;

		Item entity = event.getItem();
		ItemStack item = fixItem(entity.getItemStack());

		if(item != null)
			entity.setItemStack(item);
	}

	private ItemStack fixItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();

		if(meta == null)
			return null;

		boolean changed = false;

		for(Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
			String id = entry.getKey().getKey().getKey();
			Object obj = section.get(id);

			if(!(obj instanceof Integer)) {
				continue;
			}

			int limit = (int) obj;

			if(entry.getValue() > limit) {
				// Revert
				changed = true;
				meta.removeEnchant(entry.getKey());

				if(limit > 0) {
					meta.addEnchant(entry.getKey(), limit, true);
				}
			}
		}

		if(changed) {
			item.setItemMeta(meta);

			return item;
		} else {
			return null;
		}
	}
}
