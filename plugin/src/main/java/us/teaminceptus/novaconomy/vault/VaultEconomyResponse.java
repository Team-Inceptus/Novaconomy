package us.teaminceptus.novaconomy.vault;

import net.milkbowl.vault.economy.EconomyResponse;

class VaultEconomyResponse extends EconomyResponse {
    /**
     * Constructor for EconomyResponse
     *
     * @param amount       Amount modified during operation
     * @param balance      New balance of account
     * @param type         Success or failure type of the operation
     * @param errorMessage Error message if necessary (commonly null)
     */
    public VaultEconomyResponse(double amount, double balance) {
        super(amount, balance, ResponseType.SUCCESS, null);
    }
}
