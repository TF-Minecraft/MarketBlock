package net.tfminecraft.marketblock.trade;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;

import net.tfminecraft.denareconomy.api.Denars;
import net.tfminecraft.denareconomy.api.Result;
import net.tfminecraft.marketblock.util.SaleTake;
import net.tfminecraft.tlibs.character.CharacterRef;

/**
 * One sale to the market. Its operation ID is fixed when the sale is planned, so settling it again
 * returns the ledger's first result instead of paying twice, and the goods are taken once, only
 * after the payment is applied.
 */
public final class MarketSale {
    private final String opId;
    private final CharacterRef seller;
    private final SaleTake take;
    private final long cents;
    private final Path intent;
    private boolean recorded;
    private boolean taken;

    public MarketSale(Path folder, CharacterRef seller, SaleTake take, long cents) {
        this.opId = "market:" + UUID.randomUUID();
        this.seller = seller;
        this.take = take;
        this.cents = cents;
        this.intent = folder.resolve(seller.ownerId() + ".properties");
    }

    public String opId() {
        return opId;
    }

    /** Pays the seller's pouch from the mint, taxed, then takes the goods if the payment applied. */
    public Result settle() throws IOException {
        if (taken) return Result.APPLIED;
        if (!recorded && !Denars.taxReady()) return Result.UNAVAILABLE;
        if (!recorded) record();
        Result result = Denars.status(opId).orElseGet(
                () -> Denars.earn(opId, seller, Denars.pouch(seller), cents, true));
        if (result == Result.APPLIED) {
            take.apply();
            taken = true;
        }
        if (result == Result.APPLIED || result == Result.REFUSED) {
            Files.deleteIfExists(intent);
            syncDirectory();
        }
        return result;
    }

    private void record() throws IOException {
        Files.createDirectories(intent.getParent());
        try (FileChannel parent = FileChannel.open(intent.getParent().getParent(), StandardOpenOption.READ)) {
            parent.force(true);
        }
        Properties data = new Properties();
        data.setProperty("operation", opId);
        data.setProperty("owner", seller.ownerId().toString());
        data.setProperty("character", seller.characterId().toString());
        data.setProperty("grossCents", Long.toString(cents));
        data.setProperty("items", take.description());
        // CREATE_NEW also blocks another payment when an interrupted sale needs reconciliation.
        try (FileChannel channel = FileChannel.open(intent,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            data.store(Channels.newOutputStream(channel), "Pending market sale; reconcile before removing");
            channel.force(true);
        }
        syncDirectory();
        recorded = true;
    }

    private void syncDirectory() throws IOException {
        try (FileChannel directory = FileChannel.open(intent.getParent(), StandardOpenOption.READ)) {
            directory.force(true);
        }
    }
}
