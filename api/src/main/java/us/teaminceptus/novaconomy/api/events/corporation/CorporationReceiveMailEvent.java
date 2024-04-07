package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.util.Mail;

import java.util.Date;

/**
 * Called when a corporation receives mail.
 */
public class CorporationReceiveMailEvent extends CorporationEvent {

    private final Mail mail;

    /**
     * Constructs a new CorporationReceiveMailEvent.
     * @param c Corporation that received the mail
     * @param mail The {@linkplain Mail mail} that was received
     */
    public CorporationReceiveMailEvent(@NotNull Corporation c, @NotNull Mail mail) {
        super(c);
        this.mail = mail;
    }

    /**
     * Gets the {@linkplain Mail mail} that was received.
     * @return Mail Received
     */
    @NotNull
    public Mail getMail() {
        return mail;
    }

    /**
     * Gets the timestamp fo the mail that was received.
     * @return Mail Timestamp
     */
    @NotNull
    public Date getTimestamp() {
        return mail.getTimestamp();
    }

    /**
     * Gets the player sender of the mail.
     * @return Mail Sender
     */
    @NotNull
    public OfflinePlayer getPlayer() {
        return mail.getSender();
    }

    /**
     * Gets whether this mail is anonymous.
     * @return true if anonymous, false otherwise
     */
    public boolean isAnonymous() {
        return mail.isAnonymous();
    }

}
