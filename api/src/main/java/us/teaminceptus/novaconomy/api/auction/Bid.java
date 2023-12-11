package us.teaminceptus.novaconomy.api.auction;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility class for representing a bid on an auction item.
 */
public final class Bid implements Serializable {

    private static final long serialVersionUID = 9021007490201553365L;

    private final UUID bidder;
    private final UUID economy;
    private final double amount;

    Bid(UUID bidder, UUID economy, double amount) {
        this.bidder = bidder;
        this.economy = economy;
        this.amount = amount;
    }

    /**
     * Gets the bidder of this bid.
     * @return The bidder that made this bid.
     */
    @NotNull
    public OfflinePlayer getBidder() {
        return Bukkit.getOfflinePlayer(bidder);
    }

    /**
     * Gets the economy this bid is for.
     * @return
     */
    @NotNull
    public Economy getEconomy() {
        return Economy.byId(economy);
    }

    /**
     * Gets the amount this bid is for.
     * @return The amount this bid is for.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Gets the price of this bid.
     * @return The price of this bid.
     */
    @NotNull
    public Price getPrice() {
        return new Price(getEconomy(), amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bid bid = (Bid) o;
        return Objects.equals(bidder, bid.bidder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bidder);
    }

    @Override
    public String toString() {
        return "Bid{" +
                "bidder=" + bidder +
                ", economy=" + economy +
                ", amount=" + amount +
                '}';
    }
}
