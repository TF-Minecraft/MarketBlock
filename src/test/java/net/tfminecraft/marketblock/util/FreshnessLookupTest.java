package net.tfminecraft.marketblock.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import net.tfminecraft.cooking.item.FoodItem;
import net.tfminecraft.cooking.item.tag.TagStep;
import net.tfminecraft.cooking.item.tag.TagTrack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;

class FreshnessLookupTest {
    private ItemStack item;
    private MockedStatic<FoodItem> foods;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        item = new ItemStack(Material.CARROT);
        foods = mockStatic(FoodItem.class);
    }

    @AfterEach
    void tearDown() {
        if (foods != null) foods.close();
        MockBukkit.unmock();
    }

    @Test
    void missingCookingPluginUsesFreshWithoutLookingUpFoodMetadata() {
        assertNotNull(new FreshnessLookup());
        assertFalse(FreshnessLookup.cookingLoaded());
        assertEquals("fresh", FreshnessLookup.stepId(item));
        foods.verifyNoInteractions();
    }

    @Test
    void nullItemsNeverReachCookingEvenWhenThePluginIsLoaded() {
        MockBukkit.createMockPlugin("Cooking");
        assertTrue(FreshnessLookup.cookingLoaded());
        assertEquals("fresh", FreshnessLookup.stepId(null));
        foods.verifyNoInteractions();
    }

    @Test
    void ordinaryItemsWithoutFoodDataUseFresh() {
        MockBukkit.createMockPlugin("Cooking");
        foods.when(() -> FoodItem.fromItem(item)).thenReturn(null);

        assertEquals("fresh", FreshnessLookup.stepId(item));

        foods.verify(() -> FoodItem.fromItem(item));
    }

    @Test
    void foodWithoutAFreshnessTrackUsesFresh() {
        MockBukkit.createMockPlugin("Cooking");
        FoodItem food = mock(FoodItem.class);
        foods.when(() -> FoodItem.fromItem(item)).thenReturn(food);

        assertEquals("fresh", FreshnessLookup.stepId(item));

        verify(food).getTagTrack("freshness");
    }

    @Test
    void foodWithoutACurrentFreshnessStepUsesFresh() {
        MockBukkit.createMockPlugin("Cooking");
        FoodItem food = mock(FoodItem.class);
        TagTrack track = mock(TagTrack.class);
        foods.when(() -> FoodItem.fromItem(item)).thenReturn(food);
        when(food.getTagTrack("freshness")).thenReturn(track);

        assertEquals("fresh", FreshnessLookup.stepId(item));

        verify(track).getCurrentStep();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void absentOrBlankStepIdsUseFresh(String id) {
        installFoodStep(id);
        assertEquals("fresh", FreshnessLookup.stepId(item));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fresh", "STALE", "Rotten", "custom_step"})
    void recognizedAndCustomFreshnessIdsUseNormalizedKeys(String id) {
        installFoodStep(id);
        assertEquals(id.toLowerCase(Locale.ROOT), FreshnessLookup.stepId(item));
    }

    @Test
    void freshnessIdsMatchConfiguredKeysUnderATurkishServerLocale() {
        installFoodStep("RIPE");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("ripe", FreshnessLookup.stepId(item));
        } finally {
            Locale.setDefault(previous);
        }
    }

    private void installFoodStep(String id) {
        MockBukkit.createMockPlugin("Cooking");
        FoodItem food = mock(FoodItem.class);
        TagTrack track = mock(TagTrack.class);
        TagStep step = mock(TagStep.class);
        foods.when(() -> FoodItem.fromItem(item)).thenReturn(food);
        when(food.getTagTrack("freshness")).thenReturn(track);
        when(track.getCurrentStep()).thenReturn(step);
        when(step.getId()).thenReturn(id);
    }
}
