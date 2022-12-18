package us.teaminceptus.novaconomy.api.corporation;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Novaconomy Corporation
 */
public final class Corporation {

    private final List<Business> children = new ArrayList<>();

    private final String name;

    private final UUID id;

    private Corporation(UUID id, String name, List<Business> children) {
        this.id = id;
        this.name = name;

        this.children.addAll(children);
    }

    /**
     * Fetches all of the Businesses this Corporation is responsible for.
     * @return Business Children
     */
    @NotNull
    public List<Business> getChildren() {
        return children;
    }

    /**
     * Fetches the name of this Corporation.
     * @return Corporation Name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Fetches the ID of this Corporation.
     * @return Corporation ID
     */
    @NotNull
    public UUID getUniqueId() {
        return id;
    }
}
