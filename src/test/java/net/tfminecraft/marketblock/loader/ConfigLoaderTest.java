package net.tfminecraft.marketblock.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.util.DemandSchedule;

class ConfigLoaderTest {
    private final ConfigLoader loader = new ConfigLoader();

    @AfterEach
    void restoreDefault() {
        Cache.demandRecoveryHours = DemandSchedule.DEFAULT_HOURS;
    }

    @Test
    void loadsDemandRecoveryHours(@TempDir File dir) throws Exception {
        File config = write(dir, "demand-recovery-hours: 4\n");

        loader.load(config);

        assertEquals(4.0, Cache.demandRecoveryHours);
    }

    @Test
    void missingKeyDefaultsToFourHours(@TempDir File dir) throws Exception {
        File config = write(dir, "market-block: iaf(tfmc:market_block)\n");

        loader.load(config);

        assertEquals(DemandSchedule.DEFAULT_HOURS, Cache.demandRecoveryHours);
    }

    @Test
    void nonPositiveHoursFallBackToFour(@TempDir File dir) throws Exception {
        loader.load(write(dir, "demand-recovery-hours: 0\n"));
        assertEquals(4.0, Cache.demandRecoveryHours);

        loader.load(write(dir, "demand-recovery-hours: -1\n"));
        assertEquals(4.0, Cache.demandRecoveryHours);
    }

    private static File write(File dir, String yaml) throws Exception {
        File config = new File(dir, "config.yml");
        Files.writeString(config.toPath(), yaml);
        return config;
    }
}
