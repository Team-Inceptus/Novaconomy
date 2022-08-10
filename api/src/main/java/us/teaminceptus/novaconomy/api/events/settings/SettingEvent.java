package us.teaminceptus.novaconomy.api.events.settings;

import com.google.common.base.Preconditions;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.settings.Settings;

/**
 * Represents an event involving a Setting
 */
public abstract class SettingEvent extends Event {

    private final Settings.NovaSetting<?> setting;

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a SettingEvent.
     * @param setting Setting involved
     * @throws IllegalArgumentException if setting is null
     */
    public SettingEvent(@NotNull Settings.NovaSetting<?> setting) throws IllegalArgumentException {
        Preconditions.checkNotNull(setting, "Setting cannot be null");
        this.setting = setting;
    }

    /**
     * Fetches the setting involved in this event.
     * @return {@link Settings.NovaSetting} involved
     */
    @NotNull
    public Settings.NovaSetting<?> getSetting() {
        return setting;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the Event's Handlers.
     * @return Event Handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
