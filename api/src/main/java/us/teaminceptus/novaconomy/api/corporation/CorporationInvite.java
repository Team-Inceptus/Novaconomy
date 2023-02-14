package us.teaminceptus.novaconomy.api.corporation;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

import java.util.*;

/**
 * Utility class for a Corporation Invite
 */
public final class CorporationInvite implements ConfigurationSerializable {

    private final Corporation from;
    private final Business to;
    private final long invitedTimestamp;

    /**
     * Constructs a CorporationInvite with a Date of {@link System#currentTimeMillis()}.
     * @param from Corporation that sent the invite
     * @param to Business that was invited
     */
    public CorporationInvite(@NotNull Corporation from, @NotNull Business to) {
        this(from, to, new Date());
    }

    /**
     * Constructs a CorporationInvite.
     * @param from Corporation that sent the invite
     * @param to Business that was invited
     * @param invited Date of when the invite was sent
     */
    public CorporationInvite(@NotNull Corporation from, @NotNull Business to, @NotNull Date invited) {
        this.from = from;
        this.to = to;
        this.invitedTimestamp = invited.getTime();
    }

    /**
     * Fetches the Corporation that sent the invite.
     * @return Corporation that the invite is from
     */
    @NotNull
    public Corporation getFrom() {
        return from;
    }

    /**
     * Fetches the Business that was invited.
     * @return Business that was invited
     */
    @NotNull
    public Business getTo() {
        return to;
    }

    /**
     * Fetches the timestamp of when the invite was sent.
     * @return Date of when the invite was sent
     */
    @NotNull
    public Date getInvitedTimestamp() {
        return new Date(invitedTimestamp);
    }

    /**
     * Accepts this invite, adding the Business to the Corporation.
     * @throws IllegalStateException if the Business already has a parent corporation or is no longer invited
     */
    public void accept() throws IllegalStateException {
        if (to.getParentCorporation() != null) throw new IllegalStateException("Business already has a parent corporation");
        if (!from.isInvited(to)) throw new IllegalStateException("Business is no longer invited to this corporation");
        if (from.getChildren().size() >= from.getMaxChildren()) throw new IllegalStateException("Corporation is full");

        from.removeInvite(to);
        from.addChild(to);
    }

    /**
     * Declines this invite, removing the Business from the Corporation's invite list.
     * @throws IllegalStateException if the Business is no longer invited
     */
    public void decline() throws IllegalStateException {
        if (!from.isInvited(to)) throw new IllegalStateException("Business is no longer invited to this corporation");

        from.removeInvite(to);
    }

    @Override
    @NotNull
    public String toString() {
        return "CorporationInvite{" +
                "from=" + from +
                ", to=" + to +
                ", invitedTimestamp=" + new Date(invitedTimestamp) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorporationInvite that = (CorporationInvite) o;
        return invitedTimestamp == that.invitedTimestamp && from.equals(that.from) && to.equals(that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, invitedTimestamp);
    }

    // Serialization

    /**
     * Deserializes a CorporationInvite from a Map.
     * @param serial Serialized CorporationInvite
     * @return Deserialized CorporationInvite
     * @throws IllegalArgumentException if the serial map is null
     */
    @NotNull
    public static CorporationInvite deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) throw new IllegalArgumentException("Serial cannot be null");

        return new CorporationInvite(
                Corporation.byId(UUID.fromString(serial.get("from").toString())),
                Business.byId(UUID.fromString(serial.get("to").toString())),
                new Date((long) serial.get("invitedTimestamp"))
        );
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("from", from.getUniqueId().toString());
            put("to", to.getUniqueId().toString());
            put("invitedTimestamp", invitedTimestamp);
        }};
    }
}
