package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public final class NoTotems implements Listener {

	private final Module module = Module.getModule("no-totems");
	private final boolean onlyInCombat = module.getValue("only-in-combat");

	@EventHandler(ignoreCancelled = true)
	public void onResurrect(EntityResurrectEvent event) {
		LivingEntity entity = event.getEntity();

		if(!(entity instanceof Player))
			return;

		Player p = (Player) entity;

		if(onlyInCombat && !Combat.isInCombat(p))
			return;

		if(module.affectsPlayer(p))
			event.setCancelled(true);
	}
}
