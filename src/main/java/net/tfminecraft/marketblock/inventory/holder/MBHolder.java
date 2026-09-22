package net.tfminecraft.marketblock.inventory.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MBHolder implements InventoryHolder {
    private final MBGUI type;
    private final String id;

    public MBHolder(MBGUI type, String id) {
        this.type = type;
        this.id = id;
    }

    public MBGUI getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
