package us.teaminceptus.novaconomy.api.player

import org.bukkit.OfflinePlayer
import us.teaminceptus.novaconomy.api.business.Business
import us.teaminceptus.novaconomy.api.corporation.Corporation
import us.teaminceptus.novaconomy.api.economy.Economy

// NovaPlayer / OfflinePlayer

inline val OfflinePlayer.novaPlayer
    /**
     * Gets the NovaPlayer instance of this player.
     * @return The NovaPlayer instance of this player.
     */
    get() = NovaPlayer(this)

inline val NovaPlayer.business: Business?
    /**
     * Gets the business this player currently owns.
     * @return The business this player currently owns, or null if the player does not own a business.
     * @see Business.byOwner
     */
    get() = Business.byOwner(player)

inline val NovaPlayer.corporation: Corporation?
    /**
     * Gets the corporation this player is currently apart of.
     * @return The corporation this player is in, or null if the player is not in a corporation.
     * @see Corporation.byMember
     */
    get() = Corporation.byMember(player)

/**
 * Gets the balance of the player in the specified economy.
 * @param economy The economy to get the balance from.
 * @return The balance of the player in the specified economy.
 * @see [NovaPlayer.getBalance]
 */
operator fun NovaPlayer.get(economy: Economy): Double = getBalance(economy)

/**
 * Sets the balance of the player in the specified economy.
 * @param economy The economy to set the balance in.
 * @param balance The balance to set.
 * @see [NovaPlayer.setBalance]
 */
operator fun NovaPlayer.set(economy: Economy, balance: Double) = setBalance(economy, balance)

