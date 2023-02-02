package us.teaminceptus.novaconomy.api.player;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerDepositEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerWithdrawEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a Player used in the Plugin
 *
 */
public final class NovaPlayer {

    private static final String LBW = "last_bank_withdraw";
    private static final String LBD = "last_bank_deposit";
    private final OfflinePlayer p;

    private final File pFile;
    private final FileConfiguration pConfig;

    final PlayerStatistics stats;

    /**
     * Creates a new NovaPlayer.
     * @param p OfflinePlayer to use
     * @throws IllegalArgumentException if player is null
     */
    public NovaPlayer(@NotNull OfflinePlayer p) throws IllegalArgumentException {
        if (p == null) throw new IllegalArgumentException("Player is null");
        this.p = p;

        if (!NovaConfig.getPlayerDirectory().exists()) NovaConfig.getPlayerDirectory().mkdir();

        this.pFile = new File(NovaConfig.getPlayerDirectory(), p.getUniqueId() + ".yml");

        if (!pFile.exists()) try {
            pFile.createNewFile();
        } catch (IOException e) {
            NovaConfig.print(e);
        }

        this.pConfig = YamlConfiguration.loadConfiguration(pFile);

        PlayerStatistics stats;
        try {
            stats = pConfig.contains("stats") ? (PlayerStatistics) pConfig.get("stats") : new PlayerStatistics(p);
        } catch (ClassCastException e) {
            NovaConfig.print(e);
            stats = new PlayerStatistics(p);
        }

        this.stats = stats;
    }

    /**
     * Fetch the Configuration file this player's information is stored in
     * @return {@link FileConfiguration} representing player config
     */
    @NotNull
    public FileConfiguration getPlayerConfig() {
        return pConfig;
    }

    /**
     * Fetch the File this player's information is stored in
     * @return {@link File} representing the file {@link #getPlayerConfig()} is stored in
     */
    @NotNull
    public File getPlayerFile() { return pFile; }

    /**
     * Fetch the player this class belongs to
     * @return OfflinePlayer of this object
     */
    @NotNull
    public OfflinePlayer getPlayer() {
        return p;
    }

    /**
     * Fetch the online player this class belongs to
     * @return Player if online, else null
     */
    @Nullable
    public Player getOnlinePlayer() {
        if (p.isOnline()) return p.getPlayer();
        else return null;
    }

    /**
     * Fetches this player's name.
     * @return Name of this player
     */
    @NotNull
    public String getPlayerName() {
        return this.p.getName();
    }

    /**
     * Fetches the balance of this player
     * @param econ Economy to use
     * @return Player Balance
     * @throws IllegalArgumentException if economy is null
     */
    public double getBalance(@NotNull Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        boolean zero = false;
        String econName = econ.getName().toLowerCase();

        if (!pConfig.isConfigurationSection("economies")) {
            pConfig.createSection("economies");
            zero = true;
        }
        if (!pConfig.isConfigurationSection("economies." + econName)) {
            pConfig.getConfigurationSection("economies").createSection(econName);
            zero = true;
        }

        if (zero) return 0;

        return pConfig.getDouble("economies." + econName + ".balance", 0);
    }

    /**
     * Sets the balance of this player
     * @param econ Economy to use
     * @param newBal Amount to set
     * @throws IllegalArgumentException if economy is null
     */
    public void setBalance(@NotNull Economy econ, double newBal) throws IllegalArgumentException {
        if (newBal < 0) throw new IllegalArgumentException("Balance cannot be negative");
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        pConfig.set("economies." + econ.getName().toLowerCase() + ".balance", newBal);

        if (newBal > stats.highestBalance) {
            stats.highestBalance = newBal;
            stats.highestBalanceEconomy = econ;
        }
        save();
    }

    /**
     * <p>Saves this player's information to the file.</p>
     * <p>Information will be automatically saved with wrapper methods, so this method does not need to be called again.</p>
     */
    public void save() {
        pConfig.set("stats", this.stats);

        try {
            pConfig.save(pFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    /**
     * Fetches the statistics of this Player.
     * @return PlayerStatistics of this player
     */
    @NotNull
    public PlayerStatistics getStatistics() {
        return this.stats;
    }

    /**
     * Adds to the balance of this player
     * @param econ Economy to use
     * @param add Amount to add
     * @throws IllegalArgumentException if economy is null
     */
    public void add(@NotNull Economy econ, double add) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        stats.moneyAdded += add;
        setBalance(econ, getBalance(econ) + add);
    }

    /**
     * Removes from the balance of this player
     * @param econ Economy to use
     * @param remove Amount to remove
     * @throws IllegalArgumentException if economy is null
     */
    public void remove(@NotNull Economy econ, double remove) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        stats.totalMoneySpent += remove;
        setBalance(econ, getBalance(econ) - remove);
    }

    /**
     * Removes a Price from the balance of this player
     * @param price Price to remove
     */
    public void remove(@NotNull Price price) {
        if (price == null) throw new IllegalArgumentException("Price cannot be null");
        remove(price.getEconomy(), price.getAmount());
    }

    /**
     * Adds a Price to the balance of this player
     * @param price Price to use
     * @throws IllegalArgumentException if price is null
     */
    public void add(@NotNull Price price) throws IllegalArgumentException {
        if (price == null) throw new IllegalArgumentException("Price cannot be null");
        add(price.getEconomy(), price.getAmount());
    }

    /**
     * Whether this Nova Player can afford a Product.
     * @param p Product to buy
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Product p) {
        if (p == null) return false;
        return getBalance(p.getEconomy()) >= p.getPrice().getAmount();
    }

    /**
     * Fetches an Event Representation of the last bank withdraw of this player
     * @return {@link PlayerWithdrawEvent} representing last withdraw; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerWithdrawEvent getLastBankWithdraw() {
        return new PlayerWithdrawEvent(getOnlinePlayer(), pConfig.getDouble(LBW + ".amount", 0), Economy.getEconomy(pConfig.getString(LBW + ".economy", "")), pConfig.getLong(LBW + ".timestamp", 0));
    }

    /**
     * Fetches an Event Representation of the last bank deposit of this player
     * @return {@link PlayerDepositEvent} representing last deposit; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerDepositEvent getLastBankDeposit() {
        return new PlayerDepositEvent(getOnlinePlayer(), pConfig.getDouble(LBD + ".amount", 0), Economy.getEconomy(pConfig.getString(LBD + ".economy", "")), pConfig.getLong(LBD + ".timestamp", 0));
    }

    /**
     * Withdraws an amount from the global bank
     * @param econ Economy to withdraw from
     * @param amount Amount to withdraw
     * @throws IllegalArgumentException if economy is null, or amount is negative or greater than the current bank balance
     * @throws UnsupportedOperationException if amount is greater than the max withdraw or already withdrawn in the last 24 hours
     */
    public void withdraw(@NotNull Economy econ, double amount) throws IllegalArgumentException, UnsupportedOperationException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount < 0 || amount > Bank.getBalance(econ)) throw new IllegalArgumentException("Amount cannot be negative or greater than current bank balance");

        if (!NovaConfig.getConfiguration().canBypassWithdraw(p) && NovaConfig.getConfiguration().getMaxWithdrawAmount(econ) < amount) throw new UnsupportedOperationException("Amount exceeds max withdraw amount");
        if (System.currentTimeMillis() - 86400000 < pConfig.getLong(LBW + ".timestamp", 0)) throw new UnsupportedOperationException("Last withdraw was less than 24 hours ago");

        Bank.removeBalance(econ, amount);
        add(econ, amount);

        pConfig.set(LBW + ".amount", amount);
        pConfig.set(LBW + ".economy", econ.getName());
        pConfig.set(LBW + ".timestamp", System.currentTimeMillis());

        stats.totalWithdrawn += amount;
        save();

        PlayerWithdrawEvent event = new PlayerWithdrawEvent(getOnlinePlayer(), amount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Deposits an amount to the global bank
     * @param econ Economy to deposit to
     * @param firstAmount Amount to deposit
     * @throws IllegalArgumentException if economy is null, or amount is negative
     */
    public void deposit(@NotNull Economy econ, double firstAmount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (firstAmount < 0) throw new IllegalArgumentException("Amount cannot be negative");

        PlayerDepositEvent event = new PlayerDepositEvent(getOnlinePlayer(), firstAmount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
        double amount = event.getAmount();

        Bank.addBalance(econ, amount);
        remove(econ, amount);

        pConfig.set(LBW + ".amount", amount);
        pConfig.set(LBW + ".economy", econ.getName());
        pConfig.set(LBW + ".timestamp", System.currentTimeMillis());
        pConfig.set("donated." + econ.getName(), getDonatedAmount(econ) + amount);
        save();
    }

    /**
     * Fetches a Map of Economies to how much they have donated to the global bank.
     * @return Donation Map
     */
    @NotNull
    public Map<Economy, Double> getAllDonatedAmounts() {
        Map<Economy, Double> amounts = new HashMap<>();
        if (!pConfig.isConfigurationSection("donated")) {
            pConfig.createSection("donated");
            return amounts;
        }

        pConfig.getConfigurationSection("donated").getValues(false).forEach((k, v) -> amounts.put(Economy.getEconomy(k), v instanceof Double ? (double) v : 0));
        return amounts;
    }

    /**
     * Fetches the total amount of money this player has donated to the global bank.
     * @param econ Economy to check
     * @return Total amount donated, or 0 if not found or economy is null
     */
    public double getDonatedAmount(@Nullable Economy econ) {
        if (econ == null) return 0;
        return getAllDonatedAmounts().getOrDefault(econ, 0D);
    }

    /**
     * Fetches a sorted list of the top donators of this Economy to the global bank, descending.
     * @param econ Economy donated to
     * @param max Amount of top donators to fetch
     * @return List of top donators
     */
    @NotNull
    public static List<NovaPlayer> getTopDonators(Economy econ, int max) {
        List<NovaPlayer> top = Arrays.stream(Bukkit.getOfflinePlayers())
                .map(NovaPlayer::new)
                .sorted(Collections.reverseOrder(Comparator.comparingDouble(o -> o.getDonatedAmount(econ))))
                .collect(Collectors.toList());
        return max <= 0 ? top : top.subList(0, Math.min(max, top.size()));
    }

    /**
     * Fetches and adds all of the balances from {@link #getBalance(Economy)}.
     * @return total balance of all economies
     */
    public double getTotalBalance() {
        AtomicDouble bal = new AtomicDouble();
        Economy.getEconomies().forEach(e -> bal.addAndGet(getBalance(e)));
        return bal.get();
    }

    /**
     * Fetches a sorted list of the top donators of this Economy to the global bank, descending.
     * @param econ Economy donated to
     * @return List of top donators
     */
    @NotNull
    public static List<NovaPlayer> getTopDonators(Economy econ) {
        return getTopDonators(econ, -1);
    }

    /**
     * Whether this Nova Player is online.
     * @return true if online, else false
     */
    public boolean isOnline() {
        return p.isOnline();
    }

    /**
     * Fetches all of the Bounties this player owns.
     * @return List of Owned Bounties
     */
    @NotNull
    public Map<OfflinePlayer, Bounty> getOwnedBounties() {
        Map<OfflinePlayer, Bounty> bounties = new HashMap<>();
        if (!pConfig.isConfigurationSection("bounties")) {
            pConfig.createSection("bounties");
            return bounties;
        }

        pConfig.getConfigurationSection("bounties").getValues(false).forEach((k, v) -> bounties.put(Bukkit.getOfflinePlayer(UUID.fromString(k)), (Bounty) v));
        return bounties;
    }

    /**
     * Fetches a sorted list of Bounties this player owns, descending, by their amount.
     * @param max Amount of top bounties to fetch
     * @return List of top bounties
     */
    @NotNull
    public List<Map.Entry<OfflinePlayer, Bounty>> getTopBounties(int max) {
        List<Map.Entry<OfflinePlayer, Bounty>> bounties = new ArrayList<>(
                getOwnedBounties()
                        .entrySet()
                        .stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(Collectors.toList()));
        return max <= 0 ? bounties : bounties.subList(0, Math.min(max, bounties.size()));
    }

    /**
     * Fetches a sorted list of Bounties this player owns, descending, by their amount, with no maximum.
     * @return List of top bounties
     */
    @NotNull
    public List<Map.Entry<OfflinePlayer, Bounty>> getTopBounties() {
        return getTopBounties(-1);
    }

    /**
     * Fetches a sorted list of Bounties that have this player as their target, descending, by their amount.
     * @param max Amount of top bounties to fetch
     * @return List of top bounties
     */
    @NotNull
    public List<Bounty> getTopSelfBounties(int max) {
        List<Bounty> bounties = new ArrayList<>(
                getSelfBounties()
                        .stream()
                        .sorted(Collections.reverseOrder(Comparator.comparingDouble(Bounty::getAmount)))
                        .collect(Collectors.toList()));
        return max <= 0 ? bounties : bounties.subList(0, Math.min(max, bounties.size()));
    }

    /**
     * Fetches a sorted list of Bounties that have this player as their target, descending, by their amount, with no maximum.
     * @return List of top bounties
     */
    @NotNull
    public List<Bounty> getTopSelfBounties() {
        return getTopSelfBounties(-1);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NovaPlayer)) return false;
        NovaPlayer that = (NovaPlayer) o;
        if (that == this) return true;

        return p.equals(that.p);
    }

    @Override
    public int hashCode() {
        return Objects.hash(p.getUniqueId());
    }

    /**
     * Fetches all of the Bounties that this player is the target of.
     * @return List of Bounties wanted by this player
     */
    @NotNull
    public Set<Bounty> getSelfBounties() {
        Set<Bounty> bounties = new HashSet<>();

        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            NovaPlayer np = new NovaPlayer(p);
            bounties.addAll(np.getOwnedBounties()
                    .values()
                    .stream()
                    .filter(b -> b.isOwner(p))
                    .collect(Collectors.toSet()));
        }

        return bounties;
    }

    /**
     * Fetches a Personal Setting for this Player.
     * @param setting Setting to fetch
     * @return Setting value, default if not found, or false if null
     */
    public boolean getSetting(@Nullable Settings.Personal setting) {
        if (setting == null) return false;
        return pConfig.getBoolean("settings." + setting.name().toLowerCase(), setting.getDefaultValue());
    }

    /**
     * Sets a Personal Setting for this Player.
     * @param setting Setting to set
     * @param value Value for the setting
     * @throws IllegalArgumentException if setting is null
     */
    public void setSetting(@NotNull Settings.Personal setting, boolean value) throws IllegalArgumentException {
        if (setting == null) throw new IllegalArgumentException("Setting cannot be null");
        pConfig.set("settings." + setting.name().toLowerCase(), value);
        save();
    }

    /**
     * Utility setting for {@link Settings.Personal#NOTIFICATIONS}
     * <br><br>
     * This method is not overridden by {@link NovaConfig#hasNotifications()}.
     * @return true if notifications are enabled, else false
     */
    public boolean hasNotifications() {
        return getSetting(Settings.Personal.NOTIFICATIONS);
    }

    /**
     * Sets the rating value for this Business.
     * @param id ID of the Business to set the rating for
     * @param rating Rating to set
     * @param comment Comment on the rating
     * @throws IllegalArgumentException if rating is less than 0 (no rating) / greater than 5, or business ID is null
     */
    public void setRating(@NotNull UUID id, int rating, @Nullable String comment) throws IllegalArgumentException {
        Preconditions.checkNotNull(id, "Business ID cannot be null");
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Rating must be between 0 and 5");

        String comm = comment == null ? "" : comment;

        pConfig.set("ratings." + id + ".rating", rating);
        pConfig.set("ratings." + id + ".last_rating", System.currentTimeMillis());
        pConfig.set("ratings." + id + ".comment", comm);

        save();
    }

    /**
     * Sets the rating for this Business by this player.
     * @param business Business to set the rating for
     * @param rating Rating to set
     * @param comment Comment on the rating
     * @throws IllegalArgumentException if rating is less than 0 (no rating) / greater than 5, or business is null
     */
    public void setRating(@NotNull Business business, int rating, @Nullable String comment) throws IllegalArgumentException {
        Preconditions.checkNotNull(business, "Business cannot be null");
        setRating(business.getUniqueId(), rating, comment);
    }

    /**
     * Sets the rating for this Business by this player.
     * @param r Rating to set
     */
    public void setRating(@NotNull Rating r) {
        Preconditions.checkNotNull(r, "Rating cannot be null");
        setRating(r.getBusinessId(), r.getRatingLevel(), r.getComment());
    }

    /**
     * Fetches the Rating for this Business.
     * @param b Business to fetch the rating for
     * @return Rating for this Business, or null if not found / business is null
     */
    @Nullable
    public Rating getRating(@Nullable Business b) {
        if (b == null) return null;

        UUID id = b.getUniqueId();
        if (!pConfig.isLong("ratings." + id + ".last_rating")) return null;

        long time = pConfig.getLong("ratings." + id + ".last_rating", 0);
        int level = pConfig.getInt("ratings." + id + ".rating", 0);
        String comment = pConfig.getString("ratings." + id + ".comment", "");

        return new Rating(p, id, level, time, comment);
    }

    /**
     * Fetches a Date representation of when this Player last rated
     * @param b Business to search
     * @return Date of when this Player last rated, or current time if not found / Business is null
     */
    @NotNull
    public Date getLastRating(@Nullable Business b) {
        if (b == null) return new Date();
        return new Date(pConfig.getLong("ratings." + b.getUniqueId() + ".last_rating", 0));
    }

    /**
     * Fetches the rating value for this Business.
     * @param b Business to fetch the rating for
     * @return Rating value, or 0 if not found
     * @throws IllegalArgumentException if business is null
     */
    public int getRatingLevel(@NotNull Business b) throws IllegalArgumentException {
        Preconditions.checkNotNull(b, "Business cannot be null");
        return pConfig.getInt("ratings." + b.getUniqueId() + ".rating", 0);
    }

    /**
     * Fetches the comment on the rating for this Business.
     * @param b Business to fetch the comment for
     * @return Comment on the rating, may be empty
     * @throws IllegalArgumentException if business is null
     */
    @NotNull
    public String getRatingComment(@NotNull Business b) throws IllegalArgumentException {
        Preconditions.checkNotNull(b, "Business cannot be null");
        return pConfig.getString("ratings." + b.getUniqueId() + ".comment", "");
    }

    /**
     * Whether this player has a rating for this Business.
     * @param b Business to check
     * @return true if this player has a rating for this Business, else false
     */
    public boolean hasRating(@Nullable Business b) {
        if (b == null) return false;
        return getRatingLevel(b) != 0;
    }
}