package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import me.jacob.lifestealkit.data.Msgs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class NoElytras implements Listener {
	private static final Module module = Module.getModule("no-elytras");

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		ItemStack item = event.getItem();

		if(item == null)
			return;

		if(item.getType() != Material.ELYTRA)
			return;

		Player p = event.getPlayer();

		if(!module.affectsPlayer(p))
			return;

		event.setCancelled(true);
		p.sendMessage(Msgs.get("no-elytra"));
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();

		ItemStack item;

		if(event.isShiftClick()) {
			item = event.getCurrentItem();
		} else if(event.getRawSlot() != 6)
			return;
		else {
			if(event.getClick() == ClickType.NUMBER_KEY) {
				item = event.getView().getItem(36 + event.getHotbarButton());
			} else {
				item = event.getCursor();
			}
		}

		if(item == null)
			return;

		if(item.getType() != Material.ELYTRA)
			return;

		if(!module.affectsPlayer(p))
			return;

		event.setCancelled(true);
		p.sendMessage(Msgs.get("no-elytra"));
	}
}
