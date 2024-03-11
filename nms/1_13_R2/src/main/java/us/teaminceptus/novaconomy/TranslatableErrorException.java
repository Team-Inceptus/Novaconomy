package us.teaminceptus.novaconomy;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.exception.SendableException;

import static us.teaminceptus.novaconomy.messages.MessageHandler.*;

class TranslatableErrorException extends SendableException {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final Object[] args;

    public TranslatableErrorException(ServerOperator sender, String key, Object... args) {
        super(format(sender, get(sender, key), args));

        this.key = key;
        this.args = args;
    }

    @Override
    public void sendTo(@NotNull CommandActor actor) {
        CommandSender sender = actor.as(BukkitCommandActor.class).getSender();
        messages.sendError(sender, key, args);
    }
}
