package us.teaminceptus.novaconomy.api.events.business;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.events.settings.SettingEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;

/**
 * Called after a setting for a business is changed
 */
public class BusinessSettingChangeEvent extends SettingEvent {

    private final Business business;

    private final boolean oldValue;

    private final boolean newValue;

    private final Settings.Business<?> setting;

    /**
     * Constructs a BusinessSettingChangeEvent.
     * @param setting    Setting involved
     * @param business   Business involved
     * @param oldValue   Old value of the setting
     * @param newValue   New value of the setting
     * @throws IllegalArgumentException if business or setting is null
     */
    public BusinessSettingChangeEvent(@NotNull Business business, boolean oldValue, boolean newValue, @NotNull Settings.Business<?> setting) throws IllegalArgumentException {
        super(setting);

        Preconditions.checkNotNull(business, "Business cannot be null");
        this.business = business;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.setting = setting;
    }

    /**
     * Fetches the Business involved in this event.
     * @return Business involved
     */
    @NotNull
    public Business getBusiness() {
        return business;
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
    public Settings.Business<?> getSetting() {
        return setting;
    }
}
