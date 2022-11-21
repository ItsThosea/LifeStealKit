package me.jacob.lifestealkit.data;

import me.jacob.lifestealkit.LifeStealKit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Module {

	private static final FileConfiguration config = LifeStealKit.getInstance().getConfig();
	private static final Map<String, Module> modules = new HashMap<>();

	private static final boolean worldBlackList = config.getBoolean("world-blacklist");

	private final String name;
	private final boolean enabled;
	private final List<String> worlds;
	private final boolean bypassWithOp;
	private final String bypassPermission;

	private Module(
			String name,
			boolean enabled,
			List<String> worlds,
			boolean bypassWithOp, String bypassPermission) {
		this.name = name;
		this.enabled = enabled;
		this.worlds = worlds;
		this.bypassWithOp = bypassWithOp;
		this.bypassPermission = bypassPermission;
	}

	public boolean affectsPlayer(Player player) {
		if(isBlockedWorld(player.getWorld()))
			return false;

		// CHeck permissions
		if(bypassWithOp && player.isOp())
			return false;

		if(bypassPermission != null && !bypassPermission.isEmpty() && player.hasPermission(
				bypassPermission
		))
			return false;

		return true;
	}

	public boolean isBlockedWorld(World world) {
		if(!enabled) return true;

		if(worldBlackList) {
			if(worlds.contains(world.getName()))
				return true;
		} else {
			if(!worlds.isEmpty() && !worlds.contains(world.getName()))
				return true;
		}

		return false;
	}

	public <T> T getValue(String key) {
		return (T) config.get(name + "." + key);
	}

	public static Module getModule(String name) {
		return modules.computeIfAbsent(name, ignored -> {
			ConfigurationSection section = config.getConfigurationSection(name);

			if(section == null)
				throw new IllegalStateException("Missing module: " + name);

			return new Module(
					name,
					section.getBoolean("enabled"),
					section.getStringList("worlds"),
					section.getBoolean("bypass-with-op"),
					section.getString("bypass-permission")
			);
		});
	}

}
