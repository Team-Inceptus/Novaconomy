package us.teaminceptus.novaconomy.api.util;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a message to be sent to a mail receiver, such as a Business or Corporation.
 */
public final class Mail implements ConfigurationSerializable, Serializable {

    /**
     * The maximum length of a mail subject.
     */
    public static final int MAX_SUBJECT_LENGTH = 16;

    /**
     * The maximum length of a mail message.
     */
    public static final int MAX_MESSAGE_LENGTH = 102_400;

    /**
     * The date format for mail timestamps.
     */
    public static final String DATE_FORMAT = "MMMM d, yyyy '|' h:mm a";

    private static final long serialVersionUID = 8486787868388524648L;

    private final UUID uniqueId;
    private final UUID sender;
    private final UUID recipient;
    private final String subject;
    private final String message;
    private final long timestamp;

    private boolean anonymous;
    private boolean read;

    private Mail(@NotNull UUID uniqueId, @NotNull UUID sender, @NotNull UUID recipient, @NotNull String subject, @NotNull String message, long timestamp, boolean anonymous) {
        this.uniqueId = uniqueId;
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.timestamp = timestamp;
        this.anonymous = anonymous;
    }

    /**
     * Creates a new mail object for a business.
     * @param sender The sender of the mail.
     * @param recipient The business recipient of the mail.
     * @param subject The subject of the mail.
     * @param message The message to be sent.
     */
    public Mail(@NotNull OfflinePlayer sender, @NotNull Business recipient, @NotNull String subject, @NotNull String message) {
        this(sender, recipient, subject, message, new Date());
    }

    /**
     * Creates a new mail object for a business.
     * @param sender The sender of the mail.
     * @param recipient The business recipient of the mail.
     * @param subject The subject of the mail.
     * @param message The message to be sent.
     * @param timestamp The timestamp of the mail's sent date.
     */
    public Mail(@NotNull OfflinePlayer sender, @NotNull Business recipient, @NotNull String subject, @NotNull String message, @Nullable Date timestamp) {
        if (sender == null) throw new IllegalArgumentException("Sender cannot be null");
        if (recipient == null) throw new IllegalArgumentException("Recipient cannot be null");
        if (subject == null) throw new IllegalArgumentException("Subject cannot be null");
        if (subject.length() > MAX_SUBJECT_LENGTH) throw new IllegalArgumentException("Subject length exceeds maximum length of " + MAX_SUBJECT_LENGTH + " characters");
        if (message == null) throw new IllegalArgumentException("Message cannot be null");
        if (message.length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("Message length exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");

        this.uniqueId = UUID.randomUUID();
        this.sender = sender.getUniqueId();
        this.recipient = recipient.getUniqueId();
        this.subject = subject;
        this.message = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        this.timestamp = timestamp == null ? System.currentTimeMillis() : timestamp.getTime();
    }

    /**
     * Creates a new mail object for a corporation.
     * @param sender The sender of the mail.
     * @param recipient The corporation recipient of the mail.
     * @param message The message to be sent.
     */
    public Mail(@NotNull OfflinePlayer sender, @NotNull Corporation recipient, @NotNull String subject, @NotNull String message) {
        this(sender, recipient, subject, message, new Date());
    }

    /**
     * Creates a new mail object for a corporation.
     * @param sender The sender of the mail.
     * @param recipient The corporation recipient of the mail.
     * @param subject The subject of the mail.
     * @param message The message to be sent.
     * @param timestamp The timestamp of the mail's sent date.
     */
    public Mail(@NotNull OfflinePlayer sender, @NotNull Corporation recipient, @NotNull String subject, @NotNull String message, @Nullable Date timestamp) {
        if (sender == null) throw new IllegalArgumentException("Sender cannot be null");
        if (recipient == null) throw new IllegalArgumentException("Recipient cannot be null");
        if (subject == null) throw new IllegalArgumentException("Subject cannot be null");
        if (subject.length() > MAX_SUBJECT_LENGTH) throw new IllegalArgumentException("Subject length exceeds maximum length of " + MAX_SUBJECT_LENGTH + " characters");
        if (message == null) throw new IllegalArgumentException("Message cannot be null");
        if (message.length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("Message length exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");

        this.uniqueId = UUID.randomUUID();
        this.sender = sender.getUniqueId();
        this.recipient = recipient.getUniqueId();
        this.subject = subject;
        this.message = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        this.timestamp = timestamp == null ? System.currentTimeMillis() : timestamp.getTime();
    }

    /**
     * Gets the ID of the Mail message.
     * @return Mail ID
     */
    @NotNull
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the sender of the mail message.
     * @return sender
     */
    @NotNull
    public OfflinePlayer getSender() {
        return Bukkit.getOfflinePlayer(sender);
    }

    /**
     * Gets the ID of the recipient of the mail message.
     * @return mail recipient
     */
    @NotNull
    public UUID getRecipient() {
        return recipient;
    }

    /**
     * Gets the subject of the mail message.
     * @return Mail Subject
     */
    @NotNull
    public String getSubject() {
        return subject;
    }

    /**
     * Gets the decoded message of the mail.
     * @return message
     */
    public String getMessage() {
        return new String(Base64.getDecoder().decode(message));
    }

    /**
     * Gets the Base64-Encoded message of the mail.
     * @return Mail Message
     */
    @NotNull
    public String getEncodedMessage() {
        return message;
    }

    /**
     * Gets the timestamp of the mail message.
     * @return Mail Timestamp
     */
    @NotNull
    public Date getTimestamp() {
        return new Date(timestamp);
    }

    /**
     * Gets whether this mail is anonymous or not.
     * @return Whether this mail is anonymous or not
     */
    public boolean isAnonymous() {
        return anonymous;
    }

    /**
     * Sets whether this mail is anonymous or not.
     * @param anonymous Whether this mail is anonymous or not
     */
    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    /**
     * Gets whether this Mail object was already opened by the recipient.
     * @return Whether mail was already opened
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets whether this Mail object was already opened by the recipient.
     * @param read Whether mail was already opened
     */
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Gets the display name for the sender. It will return {@code "???"} if the mail is anonymous.
     * @return Sender Name
     */
    @NotNull
    public String getSenderName() {
        if (anonymous) return getSender().getName();
        else return "???";
    }

    /**
     * Generates the display item for this mail object.
     * @return Mail ItemStack
     */
    @NotNull
    public ItemStack generateBook() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.setAuthor(getSenderName());
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + getSubject());
        meta.setTitle(getSubject());

        List<String> pages = Arrays.stream(toString().split("(?<=\\G.{798})"))
                .map(p -> ChatColor.translateAlternateColorCodes('&', p))
                .collect(Collectors.toList());
        meta.setPages(pages);

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Language.getCurrentLocale());
        meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + format.format(getTimestamp())));

        item.setItemMeta(meta);

        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mail mail = (Mail) o;
        return uniqueId == mail.uniqueId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public String toString() {
        return "<" + getSenderName() + "> " + subject + "\n\n" + getMessage();
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .put("id", uniqueId.toString())
                .put("sender", sender.toString())
                .put("recipient", recipient.toString())
                .put("message", message)
                .put("subject", subject)
                .put("timestamp", timestamp)
                .put("anonymous", anonymous)
                .build();

    }

    /**
     * Deserializes a mail object from a serialized map.
     * @param serialized The serialized map to deserialize from.
     * @return Deserialized Mail Object
     */
    @NotNull
    public static Mail deserialize(@NotNull Map<String, Object> serialized) {
        UUID id = UUID.fromString((String) serialized.get("id"));
        UUID sender = UUID.fromString((String) serialized.get("sender"));
        UUID recipient = UUID.fromString((String) serialized.get("recipient"));
        String message = (String) serialized.get("message");
        String subject = (String) serialized.get("subject");
        long timestamp = (long) serialized.get("timestamp");
        boolean anonymous = (boolean) serialized.get("anonymous");

        return new Mail(id, sender, recipient, message, subject, timestamp, anonymous);
    }
}
