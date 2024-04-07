package us.teaminceptus.novaconomy.api.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationPermission;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.NovaMarket;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.market.player.PlayerMembershipChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerDepositEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerWithdrawEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a Player used in the Plugin
 */
@SuppressWarnings("unchecked")
public final class NovaPlayer {

    private static final String LBW = "last_bank_withdraw";
    private static final String LBD = "last_bank_deposit";
    private final OfflinePlayer p;

    private final Set<AuctionProduct> wonAuctions = new HashSet<>();

    private final Map<String, Object> pConfig = new HashMap<>();

    PlayerStatistics stats;

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS players (" +
                "id CHAR(36) NOT NULL," +
                "data MEDIUMBLOB NOT NULL," +
                "stats BLOB(65535) NOT NULL," +
                "won_auctions MEDIUMBLOB NOT NULL," +
                "PRIMARY KEY (id))"
        );
    }

    /**
     * Creates a new NovaPlayer.
     * @param p OfflinePlayer to use
     * @throws IllegalArgumentException if player is null
     */
    public NovaPlayer(@NotNull OfflinePlayer p) throws IllegalArgumentException {
        if (p == null) throw new IllegalArgumentException("Player is null");
        this.p = p;

        PlayerStatistics stats = new PlayerStatistics(p);

        if (NovaConfig.getConfiguration().isDatabaseEnabled())
            try {
                checkTable();
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT * FROM players WHERE id = ?");
                ps.setString(1, p.getUniqueId().toString());
                ps.execute();
                ResultSet rs = ps.getResultSet();

                if (rs.next()) {
                    ByteArrayInputStream is = new ByteArrayInputStream(rs.getBytes("data"));
                    BukkitObjectInputStream bIs = new BukkitObjectInputStream(is);
                    this.pConfig.putAll((Map<String, Object>) bIs.readObject());
                    bIs.close();

                    ByteArrayInputStream statsIs = new ByteArrayInputStream(rs.getBytes("stats"));
                    BukkitObjectInputStream statsBIs = new BukkitObjectInputStream(statsIs);
                    stats = (PlayerStatistics) statsBIs.readObject();
                    statsBIs.close();

                    ByteArrayInputStream wonAuctionsIs = new ByteArrayInputStream(rs.getBytes("won_auctions"));
                    BukkitObjectInputStream wonAuctionsBIs = new BukkitObjectInputStream(wonAuctionsIs);
                    wonAuctions.addAll((Set<AuctionProduct>) wonAuctionsBIs.readObject());
                    wonAuctionsBIs.close();
                }

                rs.close();
            } catch (Exception e) {
                NovaConfig.print(e);
            }
        else {
            if (!NovaConfig.getPlayerDirectory().exists()) NovaConfig.getPlayerDirectory().mkdir();

            File pFile = new File(NovaConfig.getPlayerDirectory(), p.getUniqueId().toString() + ".yml");
            if (!pFile.exists()) try {
                pFile.createNewFile();
            } catch (IOException e) {
                NovaConfig.print(e);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(pFile);
            pConfig.putAll(toMap(config));

            stats = pConfig.containsKey("stats") ? (PlayerStatistics) pConfig.get("stats") : new PlayerStatistics(p);
            wonAuctions.addAll((Collection<AuctionProduct>) pConfig.getOrDefault("won_auctions", new ArrayList<>()));
        }

        this.stats = stats;
    }

    private static Map<String, Object> toMap(Configuration config) {
        Map<String, Object> map = new HashMap<>();

        for (String key : config.getKeys(true)) {
            Object o = config.get(key);
            if (o instanceof ConfigurationSection) continue;
            map.put(key, o);
        }

        return map;
    }

    /**
     * Fetches a mutable Map Representation of the player's data.
     * @return Map of Player Data
     */
    @NotNull
    public Map<String, Object> getPlayerData() {
        return pConfig;
    }

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

        return (double) pConfig.getOrDefault("economies." + econ.getName().toLowerCase() + ".balance", 0D);
    }

    /**
     * Sets the balance of this player
     * @param econ Economy to use
     * @param newBal Amount to set
     * @throws IllegalArgumentException if economy is null
     */
    public void setBalance(@NotNull Economy econ, double newBal) throws IllegalArgumentException {
        if (newBal < 0 && !NovaConfig.getConfiguration().isNegativeBalancesEnabled()) throw new IllegalArgumentException("Balance cannot be negative");
        if (newBal < NovaConfig.getConfiguration().getMaxNegativeBalance()) throw new IllegalArgumentException("Balance cannot be less than the max negative balance (" + NovaConfig.getConfiguration().getMaxNegativeBalance() + ")");
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        checkStats();
        pConfig.put("economies." + econ.getName().toLowerCase() + ".balance", newBal);

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
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                String sql;
                try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM players WHERE id = \"" + p.getUniqueId() + "\"")) {
                    if (rs.next())
                        sql = "UPDATE players SET " +
                                "id = ?, " +
                                "data = ?, " +
                                "stats = ? " +
                                "WHERE id = \"" + p.getUniqueId() + "\"";
                    else
                        sql = "INSERT INTO players VALUES (?, ?, ?)";
                }

                PreparedStatement ps = db.prepareStatement(sql);
                ps.setString(1, p.getUniqueId().toString());

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                BukkitObjectOutputStream bOs = new BukkitObjectOutputStream(os);
                bOs.writeObject(pConfig);
                bOs.close();
                ps.setBytes(2, os.toByteArray());

                ByteArrayOutputStream statsOs = new ByteArrayOutputStream();
                BukkitObjectOutputStream statsBos = new BukkitObjectOutputStream(statsOs);
                statsBos.writeObject(this.stats);
                statsBos.close();
                ps.setBytes(3, statsOs.toByteArray());

                ByteArrayOutputStream wonAuctionsOs = new ByteArrayOutputStream();
                BukkitObjectOutputStream wonAuctionsBos = new BukkitObjectOutputStream(wonAuctionsOs);
                wonAuctionsBos.writeObject(this.wonAuctions);
                wonAuctionsBos.close();
                ps.setBytes(4, wonAuctionsOs.toByteArray());

                ps.executeUpdate();
                ps.close();
            } else {
                pConfig.put("stats", this.stats);
                pConfig.put("won_auctions", new ArrayList<>(this.wonAuctions));

                File pFile = new File(NovaConfig.getPlayerDirectory(), p.getUniqueId().toString() + ".yml");
                if (!pFile.exists()) pFile.createNewFile();

                FileConfiguration pConfig = YamlConfiguration.loadConfiguration(pFile);
                for (Map.Entry<String, Object> entry : this.pConfig.entrySet())
                    pConfig.set(entry.getKey(), entry.getValue());

                pConfig.save(pFile);
            }

        } catch (Exception e) {
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
        checkStats();

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
        checkStats();

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
     * Whether this NovaPlayer can afford a Product.
     * @param p Product to buy
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Product p) {
        return canAfford(p, false);
    }

    /**
     * Whether this NovaPlayer can afford a Product.
     * @param p Product to buy
     * @param allowDebt Whether to allow debt. This will be ignored if {@link NovaConfig#isNegativeBalancesEnabled()} returns false.
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Product p, boolean allowDebt) {
        return canAfford(p == null ? null : p.getPrice(), allowDebt);
    }

    /**
     * Whether this NovaPlayer can afford a Price.
     * @param p Price to buy at
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Price p) {
        return canAfford(p, false);
    }

    /**
     * Whether this NovaPlayer can afford a Price.
     * @param p Price to buy at
     * @param allowDebt Whether to allow debt. This will be ignored if {@link NovaConfig#isNegativeBalancesEnabled()} returns false.
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Price p, boolean allowDebt) {
        if (p == null) return false;
        return canAfford(p.getEconomy(), p.getAmount(), allowDebt);
    }

    /**
     * Whether this NovaPlayer can afford an amount.
     * @param econ Economy to use
     * @param amount Amount to buy at
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Economy econ, double amount) {
        return canAfford(econ, amount, false);
    }

    /**
     * Whether this NovaPlayer can afford an amount.
     * @param econ Economy to use
     * @param amount Amount to buy at
     * @param allowDebt Whether to allow debt. This will be ignored if {@link NovaConfig#isNegativeBalancesEnabled()} returns false.
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Economy econ, double amount, boolean allowDebt) {
        if (econ == null) return false;
        if (amount <= 0) return true;

        double bal = getBalance(econ);
        double result = bal - amount;

        if (!NovaConfig.getConfiguration().isNegativeBalancesEnabled()) return result >= 0;

        if (!allowDebt) {
            if (isInDebt(econ)) return false;
            return result >= 0;
        } else {
            double max = canBypassMaxNegativeBalance() ? Double.MIN_VALUE : NovaConfig.getConfiguration().getMaxNegativeBalance();
            return result >= max;
        }
    }

    /**
     * Gets whether this NovaPlayer is currently in debt.
     * @param econ Economy to use
     * @return true if in debt, else false
     */
    public boolean isInDebt(@Nullable Economy econ) {
        if (econ == null) return false;
        if (!NovaConfig.getConfiguration().isNegativeBalancesEnabled()) return false;

        if (NovaConfig.getConfiguration().isNegativeBalancesIncludeZero())
            return getBalance(econ) <= 0;
        else
            return getBalance(econ) < 0;
    }

    /**
     * Fetches an Event Representation of the last bank withdraw of this player
     * @return {@link PlayerWithdrawEvent} representing last withdraw; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerWithdrawEvent getLastBankWithdraw() {
        return new PlayerWithdrawEvent(getOnlinePlayer(), (double) pConfig.getOrDefault(LBW + ".amount", 0D), Economy.byName((String) pConfig.getOrDefault(LBW + ".economy", "")), (long) pConfig.getOrDefault(LBW + ".timestamp", 0));
    }

    /**
     * Fetches an Event Representation of the last bank deposit of this player
     * @return {@link PlayerDepositEvent} representing last deposit; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerDepositEvent getLastBankDeposit() {
        return new PlayerDepositEvent(getOnlinePlayer(), (double) pConfig.getOrDefault(LBD + ".amount", 0D), Economy.byName((String) pConfig.getOrDefault(LBD + ".economy", "")), (long) pConfig.getOrDefault(LBD + ".timestamp", 0));
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
        if (amount < 0 || amount > Bank.getBalance(econ))
            throw new IllegalArgumentException("Amount cannot be negative or greater than current bank balance");

        if (!NovaConfig.getConfiguration().canBypassWithdraw(p) && NovaConfig.getConfiguration().getMaxWithdrawAmount(econ) < amount)
            throw new UnsupportedOperationException("Amount exceeds max withdraw amount");
        if (System.currentTimeMillis() - 86400000 < (long) pConfig.getOrDefault(LBW + ".timestamp", 0))
            throw new UnsupportedOperationException("Last withdraw was less than 24 hours ago");

        checkStats();
        Bank.removeBalance(econ, amount);
        add(econ, amount);

        pConfig.put(LBW + ".amount", amount);
        pConfig.put(LBW + ".economy", econ.getName());
        pConfig.put(LBW + ".timestamp", System.currentTimeMillis());

        stats.totalWithdrawn += amount;
        save();

        PlayerWithdrawEvent event = new PlayerWithdrawEvent(getOnlinePlayer(), amount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Deposits an amount to the global bank
     * @param econ Economy to deposit to
     * @param amount Amount to deposit
     * @throws IllegalArgumentException if economy is null, amount is negative, or cannot afford
     */
    public void deposit(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");

        if (!canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowPayBanks()))
            throw new IllegalArgumentException("Player cannot afford this amount");

        PlayerDepositEvent event = new PlayerDepositEvent(getOnlinePlayer(), amount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
        double amount0 = event.getAmount();

        Bank.addBalance(econ, amount0);
        remove(econ, amount0);

        pConfig.put(LBW + ".amount", amount0);
        pConfig.put(LBW + ".economy", econ.getName());
        pConfig.put(LBW + ".timestamp", System.currentTimeMillis());
        pConfig.put("donated." + econ.getName(), getDonatedAmount(econ) + amount0);
        save();
    }

    /**
     * Fetches a Map of Economies to how much they have donated to the global bank.
     * @return Donation Map
     */
    @NotNull
    public Map<Economy, Double> getAllDonatedAmounts() {
        Map<Economy, Double> amounts = new HashMap<>();

        for (Map.Entry<String, Object> entry : pConfig.entrySet()) {
            if (!entry.getKey().startsWith("donated.")) continue;

            Economy econ = Economy.byName(entry.getKey().split("\\.")[1].toLowerCase());
            if (econ == null) continue;

            amounts.put(econ, (double) entry.getValue());
        }

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
     * @return Map of Owned Bounties
     */
    @NotNull
    public Map<OfflinePlayer, Bounty> getOwnedBounties() {
        Map<OfflinePlayer, Bounty> bounties = new HashMap<>();

        for (Map.Entry<String, Object> entry : pConfig.entrySet()) {
            if (!entry.getKey().startsWith("bounties.")) continue;

            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey().split("\\.")[1]));
            if (target == null) continue;

            bounties.put(target, (Bounty) entry.getValue());
        }

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
     * @return Set of Bounties with {@link #getPlayer()} as their target
     */
    @NotNull
    public Set<Bounty> getSelfBounties() {
        Set<Bounty> bounties = new HashSet<>();

        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            NovaPlayer np = new NovaPlayer(p);
            bounties.addAll(np.getOwnedBounties()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equals(this.p))
                    .map(Map.Entry::getValue)
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
        return (boolean) pConfig.getOrDefault("settings." + setting.name().toLowerCase(), setting.getDefaultValue());
    }

    /**
     * Sets a Personal Setting for this Player.
     * @param setting Setting to set
     * @param value Value for the setting
     * @throws IllegalArgumentException if setting is null
     */
    public void setSetting(@NotNull Settings.Personal setting, boolean value) throws IllegalArgumentException {
        if (setting == null) throw new IllegalArgumentException("Setting cannot be null");
        pConfig.put("settings." + setting.name().toLowerCase(), value);
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

        pConfig.put("ratings." + id + ".rating", rating);
        pConfig.put("ratings." + id + ".last_rating", System.currentTimeMillis());
        pConfig.put("ratings." + id + ".comment", comm);

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
        if (!(pConfig.get("ratings." + id + ".last_rating") instanceof Long)) return null;

        long time = (long) pConfig.getOrDefault("ratings." + id + ".last_rating", 0);
        int level = (int) pConfig.getOrDefault("ratings." + id + ".rating", 0);
        String comment = (String) pConfig.getOrDefault("ratings." + id + ".comment", "");

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
        return new Date((long) pConfig.getOrDefault("ratings." + b.getUniqueId() + ".last_rating", 0));
    }

    /**
     * Fetches the rating value for this Business.
     * @param b Business to fetch the rating for
     * @return Rating value, or 0 if not found
     * @throws IllegalArgumentException if business is null
     */
    public int getRatingLevel(@NotNull Business b) throws IllegalArgumentException {
        Preconditions.checkNotNull(b, "Business cannot be null");
        return (int) pConfig.getOrDefault("ratings." + b.getUniqueId() + ".rating", 0);
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
        return (String) pConfig.getOrDefault("ratings." + b.getUniqueId() + ".comment", "");
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

    /**
     * <p>Whether this player can access the Novaconomy Market.</p>
     * <p>If {@link NovaMarket#isMarketMembershipEnabled()} returns false or Player has {@code novaconomy.admin.market.bypass_membership}, this method will always return true.</p>
     * @return true if this player can access the market, else false
     */
    public boolean hasMarketAccess() {
        if (!NovaConfig.getMarket().isMarketMembershipEnabled()) return true;
        if (p.isOp() || (p.isOnline() && p.getPlayer().hasPermission("novaconomy.admin.market.bypass_membership")))
            return true;

        return (Boolean) pConfig.getOrDefault("market.membership", false);
    }

    /**
     * <p>Sets whether this player can access the Novaconomy Market.</p>
     * <p>If {@link NovaMarket#isMarketMembershipEnabled()} returns false or Player has {@code novaconomy.admin.market.bypass_membership}, this method will do nothing.</p>
     * @param value true if this player can access the market, else false
     */
    public void setMarketAccess(boolean value) {
        if (!NovaConfig.getMarket().isMarketMembershipEnabled()) return;
        if (p.isOp() || (p.isOnline() && p.getPlayer().hasPermission("novaconomy.admin.market.bypass_membership")))
            return;

        boolean old = hasMarketAccess();
        if (old == value) return;

        PlayerMembershipChangeEvent event = new PlayerMembershipChangeEvent(p, old, value);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            pConfig.put("market.membership", event.getNewStatus());
            save();
        }
    }

    /**
     * Fetches an immutable copy of all of the purchases this player has made.
     * @return All Purchases
     */
    @NotNull
    public Set<Receipt> getPurchases() {
        return ImmutableSet.copyOf(NovaConfig.getMarket().getAllPurchases().stream()
                .filter(r -> r.getPurchaser().equals(p))
                .collect(Collectors.toList())
        );
    }

    /**
     * Fetches an immutable copy of all of the purchases this player has made for a specific Material.
     * @param m Material to search for
     * @return All Purchases for this Material
     */
    @NotNull
    public Set<Receipt> getPurchases(@NotNull Material m) {
        return ImmutableSet.copyOf(getPurchases().stream()
                .filter(r -> r.getPurchased() == m)
                .collect(Collectors.toList())
        );
    }

    /**
     * <p>Checks if this player has a specific corporation permission.</p>
     * <p>This will return {@code false} if the player does not own a corporation, business, or if the permission is null.</p>
     * @param permission
     * @return true if this player has the corporation permission, else false
     */
    public boolean hasPermission(@Nullable CorporationPermission permission) {
        if (permission == null) return false;

        Business b = Business.byOwner(p);
        if (b == null) return false;

        Corporation c = Corporation.byMember(p);
        if (c == null) return false;

        return c.hasPermission(b, permission);
    }

    /**
     * <p>Fetches an immutable copy of all of the Auctions this player has won.</p>
     * <p>Expired Auctions are no longer stored in the Auction House. Use {@link #getWonAuction(UUID)} for retrieval by ID.</p>
     * @return All Won Auctions
     */
    @NotNull
    public Set<AuctionProduct> getWonAuctions() {
        return ImmutableSet.copyOf(wonAuctions);
    }

    /**
     * Adds an Auction to this player's won auctions.
     * @param product Auction to add
     */
    public void addWonAuction(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Auction cannot be null");
        wonAuctions.add(product);
        save();
    }

    /**
     * Awards an auction to this player, adding it to their inventory. The player must be online.
     * @param product Auction from {@link #getWonAuctions()} to Award
     */
    public void awardAuction(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Auction cannot be null");
        if (!p.isOnline()) throw new IllegalStateException("Player is not online");

        Player player = p.getPlayer();
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), product.getItem());
        } else {
            player.getInventory().addItem(product.getItem());
        }

        if (wonAuctions.contains(product)) {
            wonAuctions.remove(product);
            save();
        }
    }

    /**
     * Gets a won auction by its ID.
     * @param id ID of the Auction
     * @return Auction, or null if not found
     */
    @Nullable
    public AuctionProduct getWonAuction(@NotNull UUID id) {
        return wonAuctions.stream().filter(a -> a.getUUID().equals(id)).findFirst().orElse(null);
    }

    /**
     * Fetches whether this player can bypass the max negative balance. This will return {@code false} if {@link NovaConfig#isNegativeBalancesEnabled()} return false.
     * @return true if this player can bypass, else false
     */
    public boolean canBypassMaxNegativeBalance() {
        if (!NovaConfig.getConfiguration().isNegativeBalancesEnabled()) return false;

        return NovaConfig.getConfiguration().canBypassMaxNegativeAmount(p);
    }

    /**
     * Gets the language of this player.
     * @return Language of this player
     */
    @NotNull
    public Language getLanguage() {
        return Language.getById((String) pConfig.getOrDefault("language", Language.getCurrentLanguage().getIdentifier()));
    }

    /**
     * Sets the language of this player.
     * @param lang Language to set
     * @throws IllegalArgumentException if language is null
     */
    public void setLanguage(@NotNull Language lang) throws IllegalArgumentException {
        if (lang == null) throw new IllegalArgumentException("Language cannot be null");
        pConfig.put("language", lang.getIdentifier());
        save();
    }

    private void checkStats() {
        if (stats == null)
            stats = new PlayerStatistics(p);
    }
}