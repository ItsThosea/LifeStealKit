package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.LifeStealKit;
import me.jacob.lifestealkit.data.Module;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class NoExplosives implements Listener {
	private static final Module module = Module.getModule("no-explosives");

	// Crystals
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if(!(event.getEntity() instanceof Player))
			return;

		if(event.getDamager().getType() != EntityType.ENDER_CRYSTAL)
			return;

		if(!module.affectsPlayer((Player) event.getEntity()))
			return;

		event.setCancelled(true);
	}

	// Respawn anchors
	private final List<Player> invul = new ArrayList<>();
	@EventHandler
	public void onDamage(EntityDamageByBlockEvent event) {
		if(event.getCause() != DamageCause.BLOCK_EXPLOSION)
			return;

		if(!(event.getEntity() instanceof Player))
			return;

		if(!invul.remove((Player) event.getEntity()))
			return;

		event.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		// Is it exploding?
		if(event.getClickedBlock() == null)
			return;

		World world = event.getClickedBlock().getWorld();

		if(world.getEnvironment() == Environment.NETHER)
			return;

		BlockData data = event.getClickedBlock().getBlockData();

		if(!(data instanceof RespawnAnchor))
			return;

		// Are we exploding the anchor
		RespawnAnchor anchor = (RespawnAnchor) data;

		int charges = anchor.getCharges();

		if(charges == 0)
			return;

		if(anchor.getCharges() < 4) {
			if(event.getItem() != null && event.getItem().getType() == Material.GLOWSTONE)
				return;
		}

		// Add invulnerable players
		Location source = event.getClickedBlock().getLocation();
		List<Player> added = new ArrayList<>();

		world.getPlayers().forEach(p -> {
			if(p.getLocation().distance(source) <= 15) {
				added.add(p);
				invul.add(p);
			}
		});

		new BukkitRunnable() {
			public void run() {
				invul.removeAll(added);
			}
		}.runTask(LifeStealKit.getInstance());
	}
}
