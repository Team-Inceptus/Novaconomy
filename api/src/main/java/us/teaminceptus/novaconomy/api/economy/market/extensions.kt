package us.teaminceptus.novaconomy.api.economy.market

import org.bukkit.Material
import us.teaminceptus.novaconomy.api.economy.Economy

// NovaMarket

/**
 * Gets the price of an item.
 * @param item The item to get the price of.
 * @return The price of the item.
 */
operator fun NovaMarket.get(item: Material): Double = getPrice(item)

/**
 * Gets the price of an item.
 * @param item The item to get the price of.
 * @param economy The economy to get the price in.
 * @return The price of the item.
 */
operator fun NovaMarket.get(item: Material, economy: Economy?): Double = getPrice(item, economy)

/**
 * Gets the price of an item.
 * @param item The item to get the price of.
 * @param scale The conversion scale to get the price in.
 * @return The price of the item.
 */
operator fun NovaMarket.get(item: Material, scale: Double): Double = getPrice(item, scale)

/**
 * Sets the override price of an item.
 * @param item The item to get the price of.
 * @param price The price to set the item to.
 */
operator fun NovaMarket.set(item: Material, price: Double) = setPriceOverrides(item, price)