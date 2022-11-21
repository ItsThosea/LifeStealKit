package me.jacob.lifestealkit.listeners;

import com.google.common.collect.ImmutableList;
import me.jacob.lifestealkit.LifeStealKit;
import me.jacob.lifestealkit.data.Module;
import me.jacob.lifestealkit.data.Msgs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class Effects implements Listener {
	private static final Module module = Module.getModule("effects");
	private static final List<PotionType> noBrew;
	private static final List<PotionEffectType> completelyDisabled;

	private static final boolean playerOnly = module.getValue("only-disable-for-player");

	static {
		Logger logger = LifeStealKit.getInstance().getLogger();

		noBrew = parsePotions(logger, "disabled-effects", false);
		completelyDisabled = parsePotions(logger, "completely-disabled", true);
	}

	private static <T> List<T> parsePotions(Logger logger, String section, boolean convertToEffect) {
		ImmutableList.Builder<T> builder = ImmutableList.builder();

		for(String potionId : ((List<String>) module.getValue(section))) {
			if(potionId.equalsIgnoreCase("jump_boost"))
				potionId = "jump";
			else if(potionId.equalsIgnoreCase("instant_health"))
				potionId = "instant_heal";
			else if(potionId.equalsIgnoreCase("regeneration"))
				potionId = "regen";

			try {
				PotionType type = PotionType.valueOf(potionId.toUpperCase(Locale.ENGLISH));

				if(convertToEffect)
					builder.add((T) type.getEffectType());
				else
					builder.add((T) type);
			} catch(Exception e) {
				logger.warning("Invalid potion effect: " + potionId);
			}
		}

		return builder.build();
	}

	@EventHandler
	public void onBrew(BrewEvent event) {
		if(module.isBlockedWorld(event.getBlock().getWorld()))
			return;

		for(ItemStack item : event.getResults()) {
			if(item == null)
				continue;

			if(item.getItemMeta() == null)
				return;

			ItemMeta meta = item.getItemMeta();

			if(!(meta instanceof PotionMeta))
				return;

			if(noBrew.contains(((PotionMeta) meta).getBasePotionData().getType())) {
				event.setCancelled(true);

				String msg = Msgs.get("no-brew");
				event.getContents().getViewers().forEach(e -> e.sendMessage(msg));
				return;
			}
		}
	}

	@EventHandler
	public void onEffect(EntityPotionEffectEvent event) {
		if(event.getAction() != Action.ADDED)
			return;

		if(event.getCause() == Cause.COMMAND || event.getCause() == Cause.PLUGIN)
			return;

		Entity entity = event.getEntity();
		boolean isPlayer = entity instanceof Player;

		if(!isPlayer && playerOnly)
			return;

		if(isPlayer) {
			if(!module.affectsPlayer((Player) entity))
				return;
		} else {
			if(module.isBlockedWorld(entity.getWorld()))
				return;
		}

		PotionEffect effect = event.getNewEffect();

		if(effect == null)
			return;

		if(completelyDisabled.contains(effect.getType()))
			event.setCancelled(true);
	}
}
