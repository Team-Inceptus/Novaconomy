package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.economy.account.NonPlayerAccount;
import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.account.accessor.AccountAccessor;
import me.lokka30.treasury.api.economy.account.accessor.NonPlayerAccountAccessor;
import me.lokka30.treasury.api.economy.account.accessor.PlayerAccountAccessor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.util.concurrent.CompletableFuture;

enum TreasuryAccountAccessor implements AccountAccessor {

    INSTANCE

    ;

    TreasuryAccountAccessor() {}

    private static final class TreasuryPlayerAccessor extends PlayerAccountAccessor {

        @Override
        protected @NotNull CompletableFuture<PlayerAccount> getOrCreate(@NotNull PlayerAccountCreateContext ctx) {
            return CompletableFuture.completedFuture(new TreasuryPlayerAccount(new NovaPlayer(Bukkit.getOfflinePlayer(ctx.getUniqueId()))));
        }

    }

    @Override
    public @NotNull PlayerAccountAccessor player() {
        return new TreasuryPlayerAccessor();
    }

    private static final class TreasuryNonPlayerAccessor extends NonPlayerAccountAccessor {

        @Override
        protected @NotNull CompletableFuture<NonPlayerAccount> getOrCreate(@NotNull NonPlayerAccountCreateContext ctx) {
            return CompletableFuture.completedFuture(new TreasuryAccount(ctx.getIdentifier(), ctx.getName()));
        }
    }

    @Override
    public @NotNull NonPlayerAccountAccessor nonPlayer() {
        return new TreasuryNonPlayerAccessor();
    }
}
