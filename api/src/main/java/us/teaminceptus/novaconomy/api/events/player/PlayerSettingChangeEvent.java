package us.teaminceptus.novaconomy.api.events.player;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.events.settings.SettingEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;

/**
 * Called when a Player changes their personal settings
 */
public class PlayerSettingChangeEvent extends SettingEvent {

    private final Player player;

    private final boolean oldValue;

    private final boolean newValue;

    private final Settings.Personal setting;

    /**
     * Constructs a BusinessSettingChangeEvent.
     * @param setting    Setting involved
     * @param player   Business involved
     * @param oldValue   Old value of the setting
     * @param newValue   New value of the setting
     * @throws IllegalArgumentException if player or setting is null
     */
    public PlayerSettingChangeEvent(@NotNull Player player, boolean oldValue, boolean newValue, @NotNull Settings.Personal setting) throws IllegalArgumentException {
        super(setting);

        Preconditions.checkNotNull(player, "Player cannot be null");
        this.player = player;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.setting = setting;
    }

    /**
     * Fetches the player involved in this event.
     * @return Player involved
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Fetches the old value of the setting.
     * @return Old value of the setting
     */
    public boolean getOldValue() {
        return oldValue;
    }

    /**
     * Fetches the new value of the setting.
     * @return New value of the setting
     */
    public boolean getNewValue() {
        return newValue;
    }

    @Override
    public Settings.Personal getSetting() {
        return setting;
    }
}
