package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.LifeStealKit;
import me.jacob.lifestealkit.data.Module;
import me.jacob.lifestealkit.data.Msgs;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Combat implements Listener {
	// Settings
	private static final Module module = Module.getModule("combat-settings");
	private static final int combatTicks = (int) module.getValue("combat-duration") * 20;
	private static final boolean killPlayer = module.getValue("kill-player");
	private static final boolean blockEchests = module.getValue("block-echest");

	private static final LifeStealKit plugin = LifeStealKit.getInstance();
	private static final Logger logger = plugin.getLogger();
	private static final Map<UUID, Integer> inCombat = new HashMap<>();

	// Reflection methods, for retaining kill credit
	private static final Method getHandleMethod;
	private static final Method getLastDamageSourceMethod;
	private static final Method setHealthMethod;
	private static final Method dieMethod;

	static {
		// Reflection
		Method resultGetHandle;
		Method resultGetLastDamageSource = null;
		Method resultSetHealth = null;
		Method resultDie = null;
		try {
			resultGetHandle = Class.forName("org.bukkit.craftbukkit." +
					Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]
					+ ".entity.CraftPlayer").getMethod("getHandle");

			Class<?> livingEntityClass = resultGetHandle.getReturnType().getSuperclass().getSuperclass();

			for(Method m : livingEntityClass.getDeclaredMethods()) {
				if(m.getReturnType().getSimpleName().equals("DamageSource")) {
					resultGetLastDamageSource = m;
				} else {
					Parameter[] params = m.getParameters();

					if(params.length != 1)
						continue;

					if(params[0].getType() == float.class) {
						resultSetHealth = m;
					} else if(params[0].getType().getSimpleName().equals("DamageSource") &&
							Modifier.isPublic(m.getModifiers()) &&
							m.getReturnType() != boolean.class) {
						resultDie = m;
					}
				}
			}

			Objects.requireNonNull(resultGetLastDamageSource, "setLastDamageSource");
			Objects.requireNonNull(resultSetHealth, "setHealth");
			Objects.requireNonNull(resultSetHealth, "die");
		} catch(Exception e) {
			LifeStealKit.getInstance().getLogger().log(
					Level.SEVERE,
					"Error with reflection",
					e
			);

			resultGetHandle = null;
			resultGetLastDamageSource = null;
			resultSetHealth = null;
			resultDie = null;
		}

		getHandleMethod = resultGetHandle;
		getLastDamageSourceMethod = resultGetLastDamageSource;
		setHealthMethod = resultSetHealth;
		dieMethod = resultDie;
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if(!blockEchests)
			return;

		if(event.getInventory().getType() == InventoryType.ENDER_CHEST &&
				isInCombat((Player) event.getWhoClicked())) {
			event.setCancelled(true);
			event.getWhoClicked().sendMessage(Msgs.get("no-click-combat"));
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if(!(event.getEntity() instanceof Player))
			return;

		Player p = (Player) event.getEntity();
		Player damager = null;

		if(event.getDamager() instanceof Projectile) {
			Projectile proj = (Projectile) event.getDamager();
			ProjectileSource shooter = proj.getShooter();

			if(shooter instanceof Player)
				damager = (Player) shooter;
		} else if(event.getDamager() instanceof Player) {
			damager = (Player) event.getDamager();
		}

		if(damager == null)
			return;

		startCombat(p);
		startCombat(damager);
	}

	private void startCombat(Player p) {
		if(!module.affectsPlayer(p))
			return;

		Integer taskId = inCombat.remove(p.getUniqueId());

		if(taskId != null)
			Bukkit.getScheduler().cancelTask(taskId);
		else
			p.sendMessage(Msgs.get("combat-start"));

		if(combatTicks < 1)
			return;

		inCombat.put(p.getUniqueId(), new BukkitRunnable() {

			private int ticksRemaining = combatTicks;

			@Override
			public void run() {
				if((ticksRemaining--) == 0)
					endCombat(p);

				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
						Msgs.get("combat-tick")
				));
			}
		}.runTaskTimer(plugin, 0, 0).getTaskId());
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		endCombat(event.getEntity());
	}

	private boolean endCombat(Player p) {
		Integer taskId = inCombat.remove(p.getUniqueId());

		if(taskId == null)
			return false;

		Bukkit.getScheduler().cancelTask(taskId);
		p.sendMessage(Msgs.get("combat-end"));

		return true;
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		Player p = event.getPlayer();

		if(!endCombat(p))
			return;

		if(!killPlayer)
			return;

		if(getHandleMethod == null) {
			p.setHealth(0);
		} else {
			try {
				Object serverPlayer = getHandleMethod.invoke(p);
				Object lastDamageSource = getLastDamageSourceMethod.invoke(serverPlayer);

				if(lastDamageSource == null) {
					// Don't wanna use more reflection to get DamageSource.GENERIC
					p.setHealth(0.0);
				} else {
					setHealthMethod.invoke(serverPlayer, 0.0f);
					dieMethod.invoke(serverPlayer, lastDamageSource);
				}
			} catch(Exception e) {
				logger.log(
						Level.SEVERE,
						"Error with reflection",
						e
				);
			}
		}
	}

	public static boolean isInCombat(Player p) {
		return inCombat.containsKey(p.getUniqueId());
	}
}
