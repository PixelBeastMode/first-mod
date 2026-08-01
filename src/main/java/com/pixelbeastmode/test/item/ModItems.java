package com.pixelbeastmode.test.item;

import com.pixelbeastmode.test.TestMod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/**
 * Holds every item this mod adds, and registers them with the game.
 */
public final class ModItems {

	// Each item is a static field. Creating the field is what registers it,
	// because register(...) below runs as this class is loaded.
	//
	// Item.Properties is where you configure behaviour. Some things to try later:
	//   new Item.Properties().stacksTo(16)              -> max stack size of 16
	//   new Item.Properties().rarity(Rarity.UNCOMMON)   -> yellow name in tooltips
	//   new Item.Properties().fireResistant()           -> survives lava
	public static final Item PIXEL_SHARD = register("pixel_shard", Item::new, new Item.Properties());

	/**
	 * Registers a single item.
	 *
	 * @param name    the item's path, e.g. "pixel_shard" -> "test-mod:pixel_shard"
	 * @param factory constructor for the item class, usually Item::new
	 * @param props   configuration for the item
	 */
	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		// A ResourceKey is a typed, namespaced ID: "which registry" + "which entry".
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TestMod.id(name));

		// Since 1.21.2 the item must know its own ID before it is constructed,
		// so the key goes into the properties first.
		Item item = factory.apply(props.setId(key));

		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/**
	 * Called from {@link TestMod#onInitialize()}.
	 * <p>
	 * Java only loads a class the first time something touches it. Without this
	 * call, the static fields above would never run and no items would exist.
	 */
	public static void initialize() {
		// Put the item in the "Ingredients" tab of the creative inventory.
		// Other options: CreativeModeTabs.TOOLS_AND_UTILITIES, COMBAT, FOOD_AND_DRINKS, ...
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
			.register(tab -> tab.accept(PIXEL_SHARD));
	}

	private ModItems() {
	}
}
