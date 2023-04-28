package us.teaminceptus.novaconomy.api.events.corporation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.settings.Settings;

/**
 * Called when a Corporation's Setting chanes
 */
public class CorporationSettingChangeEvent extends CorporationEvent {
    
    private final Object oldValue;
    private final Object newValue;
    private final Settings.Corporation<?> setting;

    /**
     * Creates a new CorporationSettingChangeEvent.
     * @param c Corporation involved
     * @param oldValue Old value of Setting
     * @param newValue New value of Setting
     * @param setting Setting that was changed
     */
    public CorporationSettingChangeEvent(@NotNull Corporation c, @Nullable Object oldValue, @Nullable Object newValue, @NotNull Settings.Corporation<?> setting) {
        super(c);
        this.setting = setting;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Fetches the old value of the Setting.
     * @return Old Value of Setting
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Fetches the new value of the Setting.
     * @return New Value of Setting
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Fetches the Setting that was changed.
     * @return Setting that Changed
     */
    public Settings.Corporation<?> getSetting() {
        return setting;
    }

}