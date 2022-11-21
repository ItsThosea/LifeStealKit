package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.data.Module;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class TntMinecartNerf implements Listener {

	private static final Module module = Module.getModule("tnt-minecart-nerf");
	private static final double multiplier = Double.parseDouble(
			module.getValue("multiplier").toString()
	);

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if(!(event.getEntity() instanceof Player))
			return;

		if(!(event.getDamager() instanceof ExplosiveMinecart))
			return;

		if(!module.affectsPlayer((Player) event.getEntity()))
			return;

		event.setDamage(event.getDamage() * multiplier);
	}
}
