package me.jacob.lifestealkit.data;

import me.jacob.lifestealkit.LifeStealKit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class Msgs {
	public static final String FILE_NAME = "messages.yml";
	public static final File FILE;

	private static final LifeStealKit plugin = LifeStealKit.getInstance();
	private static final Map<String, String> cachedMessages = new HashMap<>();

	private static FileConfiguration dataConfig;

	static {
		FILE = new File(plugin.getDataFolder(), FILE_NAME);

		if(!FILE.exists()) {
			plugin.saveResource(FILE_NAME, true);
		}

		reload();
	}

	private Msgs() {}

	public static void reload() {
		dataConfig = YamlConfiguration.loadConfiguration(FILE);

		InputStream defaultStream = plugin.getResource(FILE_NAME);
		if(defaultStream != null) {
			YamlConfiguration defaultConfig =
					YamlConfiguration.loadConfiguration(
							new InputStreamReader(defaultStream));

			dataConfig.setDefaults(defaultConfig);
		}

		cachedMessages.clear();

		for(String key : dataConfig.getKeys(true)) {
			Object obj = dataConfig.get(key);

			if(obj instanceof String && !((String) obj).contains("%")) {
				cachedMessages.put(key, format((String) obj));
			}
		}
	}

	private static String format(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static String get(String path, Object... args) {
		String cache = cachedMessages.get(path);

		if(cache != null) {
			return cache;
		}

		String result = dataConfig.getString(path);

		if(result == null) {
			plugin.getLogger().warning("Message " + path + " not found.");
			return path;
		}

		result = format(result).replaceAll("\\n", "\n");

		if(result.contains("%")) {
			try {
				result = String.format(result, args);
			} catch(Exception e) {
				plugin.getLogger().warning("Error formatting message " + path);
				plugin.getLogger().warning("Did you add another %?");
			}
		}

		return result;
	}
}
