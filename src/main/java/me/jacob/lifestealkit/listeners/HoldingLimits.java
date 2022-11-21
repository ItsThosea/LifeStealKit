package me.jacob.lifestealkit.listeners;

import me.jacob.lifestealkit.LifeStealKit;
import me.jacob.lifestealkit.data.Module;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public final class HoldingLimits implements Listener {

	private static final Map<Material, Integer> limits = new HashMap<>();

	private static final Module module = Module.getModule(
			"holding-limits"
	);

	static {
		Map<String, Object> stringObjectMap = ((ConfigurationSection)
				module.getValue("limits")).getValues(false);

		for(Entry<String, Object> entry : stringObjectMap.entrySet()) {
			try {
				assert entry.getValue() instanceof Integer;

				limits.put(
						Objects.requireNonNull(Material.matchMaterial(entry.getKey())),
						(int) entry.getValue()
				);
			} catch(Exception e) {
				LifeStealKit
						.getInstance()
						.getLogger()
						.warning("Invalid item or limit (" + entry.getKey() + ") in holding-limits");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPickup(EntityPickupItemEvent event) {
		LivingEntity entity = event.getEntity();

		if(!(entity instanceof Player))
			return;

		handlePickup((Player) entity, event.getItem(), event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onArmorStandUse(PlayerArmorStandManipulateEvent event) {
		ItemStack item = event.getArmorStandItem();

		if(item == null)
			return;

		int max = getMax(item.getType());

		if(max == -1)
			return;

		Player p = event.getPlayer();
		CountResult result = getCount(p, item.getType());

		if((result.count + item.getAmount()) > max) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPickupArrow(PlayerPickupArrowEvent event) {
		handlePickup(event.getPlayer(), event.getItem(), event);
	}

	private void handlePickup(Player p, Item entity, Cancellable event) {
		if(!module.affectsPlayer(p))
			return;

		ItemStack item = entity.getItemStack();
		Material type = item.getType();

		int max = getMax(type);

		if(max < 0)
			return;

		int count = getCount(p, type).count;

		if((count + item.getAmount()) > max) {
			event.setCancelled(true);

			ItemStack give = item.clone();
			give.setAmount(Math.min(max - count, item.getAmount()));

			item.setAmount(item.getAmount() - give.getAmount());

			Map<Integer, ItemStack> leftover = p.getInventory().addItem(give);

			if(!leftover.isEmpty())
				item.setAmount(item.getAmount() + Math.min(0, leftover.get(0).getAmount()));

			entity.setItemStack(item);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClose(InventoryCloseEvent event) {
		Player p = (Player) event.getPlayer();

		if(!module.affectsPlayer(p))
			return;

		limits.forEach((type, max) -> {
			CountResult result = getCount(p, type);

			if(result.count > max) {
				List<ItemStack> drops = new ArrayList<>();
				int toRemove = result.count - max;

				for(ItemStack item : result.items) {
					int removed = Math.min(toRemove, item.getAmount());
					toRemove -= removed;

					ItemStack clone = item.clone();

					clone.setAmount(removed);
					drops.add(clone);

					item.setAmount(item.getAmount() - removed);

					if(toRemove <= 0)
						break;
				}

				World w = p.getWorld();
				Location loc = p.getLocation();

				for(ItemStack item : drops) {
					w.dropItem(loc, item);
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if(event.getItem() == null)
			return;

		ItemStack item = event.getItem();
		int max = getMax(item.getType());

		if(max < 0)
			return;

		if(item.getAmount() > max) {
			// Only possible with a hacked client
			event.setCancelled(true);
		}
	}

	private int getMax(Material type) {
		return limits.getOrDefault(type, -1);
	}

	private CountResult getCount(Player p, Material type) {
		List<ItemStack> items = getItems(p)
				.stream()
				.filter(item -> item.getType() == type)
				.collect(Collectors.toList());

		int amount = 0;

		for(ItemStack item : items) {
			amount += item.getAmount();
		}

		return new CountResult(amount, items);
	}

	private static final class CountResult {
		private final int count;
		private final List<ItemStack> items;

		public CountResult(int count, List<ItemStack> items) {
			this.count = count;
			this.items = items;
		}
	}

	private List<ItemStack> getItems(Player p) {
		PlayerInventory inv = p.getInventory();
		List<ItemStack> result = new ArrayList<>();

		for(int i = 0; i < 36; i++) {
			ItemStack item = inv.getItem(i);

			if(item != null)
				result.add(item);
		}

		for(ItemStack item : inv.getArmorContents()) {
			if(item != null)
				result.add(item);
		}

		result.add(inv.getItemInOffHand());
		result.add(p.getItemOnCursor());

		return result;
	}

}
