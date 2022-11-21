package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import me.jacob.lifestealkit.data.Msgs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

import java.util.List;
import java.util.Locale;

public final class NoNetherite implements Listener {

	private final Module module = Module.getModule("no-netherite");

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if(event.getSlot() != 2)
			return;

		Inventory inv = event.getClickedInventory();

		if(!(inv instanceof SmithingInventory))
			return;

		SmithingInventory smithing = (SmithingInventory) inv;

		Player p = (Player) event.getWhoClicked();

		if(!module.affectsPlayer(p))
			return;

		if(event.getSlot() != 2)
			return;

		ItemStack item = smithing.getResult();

		if(item == null)
			return;

		String type = item.getType().name().toLowerCase(Locale.ENGLISH);
		List<String> disabled = module.getValue("disabled-items");

		if(disabled.contains(type)) {
			event.setCancelled(true);
			p.sendMessage(Msgs.get("no-netherite"));
		}
	}
}
