package me.jacob.lifestealkit;

import me.jacob.lifestealkit.data.Msgs;
import me.jacob.lifestealkit.listeners.Combat;
import me.jacob.lifestealkit.listeners.Effects;
import me.jacob.lifestealkit.listeners.EnchantLimits;
import me.jacob.lifestealkit.listeners.HoldingLimits;
import me.jacob.lifestealkit.listeners.NoElytras;
import me.jacob.lifestealkit.listeners.NoExplosives;
import me.jacob.lifestealkit.listeners.NoNetherite;
import me.jacob.lifestealkit.listeners.NoTippedArrows;
import me.jacob.lifestealkit.listeners.NoTotems;
import me.jacob.lifestealkit.listeners.TntMinecartNerf;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LifeStealKit extends JavaPlugin {

	private static LifeStealKit instance;

	public static LifeStealKit getInstance() {
		return instance;
	}

	@Override
	public void onEnable() {
		instance = this;

		// Load config and messages
		this.saveDefaultConfig();
		Msgs.get("placeholder");

		registerEvents();
	}

	private void registerEvents() {
		registerEvent(EnchantLimits.class);
		registerEvent(NoNetherite.class);
		registerEvent(NoTotems.class);
		registerEvent(HoldingLimits.class);
		registerEvent(NoElytras.class);
		registerEvent(NoTippedArrows.class);
		registerEvent(NoExplosives.class);
		registerEvent(Effects.class);
		registerEvent(Combat.class);
		registerEvent(TntMinecartNerf.class);
	}

	private void registerEvent(Class<? extends Listener> clazz) {
		try {
			this.getServer().getPluginManager().registerEvents(clazz.getConstructor().newInstance(), this);
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Failed to register listener " + clazz, e);
		}
	}

}
