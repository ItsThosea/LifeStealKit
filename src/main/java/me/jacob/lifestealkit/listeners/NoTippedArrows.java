package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import me.jacob.lifestealkit.data.Msgs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public final class NoTippedArrows implements Listener {

	private static final Module module = Module.getModule("no-tipped-arrows");

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if(event.getCurrentItem() == null)
			return;

		if(event.getCurrentItem().getType() != Material.TIPPED_ARROW)
			return;

		if(event.getInventory().getType() != InventoryType.MERCHANT)
			return;

		Player p = (Player) event.getWhoClicked();

		if(!module.affectsPlayer(p))
			return;

		event.setCancelled(true);
		p.sendMessage(Msgs.get("no-click"));
	}

	@EventHandler
	public void onCraft(PrepareItemCraftEvent event) {
		if(event.isRepair())
			return;

		CraftingInventory inv = event.getInventory();

		ItemStack item = inv.getResult();
		if(item == null || item.getType() != Material.TIPPED_ARROW)
			return;

		Player p = (Player) event.getView().getPlayer();

		if(!module.affectsPlayer(p))
			return;

		inv.setResult(null);
	}

	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		if(!(event.getEntity() instanceof Stray))
			return;

		if(module.isBlockedWorld(event.getEntity().getWorld()))
			return;

		event.getDrops().removeIf(item -> item.getType() == Material.TIPPED_ARROW);
	}

}
