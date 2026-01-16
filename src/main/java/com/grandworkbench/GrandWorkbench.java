package com.grandworkbench;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GrandWorkbench extends JavaPlugin {

    private static final String TARGET_BENCH_ID = "GrandWorkbench";

    // GrandWorkbench tabs
    private static final String WORKBENCH_GENERAL  = "workbench_general";
    private static final String WEAPON_GENERAL     = "weapon_general";
    private static final String ARMOR_GENERAL      = "armor_general";
    private static final String ALCHEMY_GENERAL    = "alchemy_general";
    private static final String ARCANE_GENERAL     = "arcane_general";
    private static final String FARMING_GENERAL    = "farming_general";
    private static final String FURNITURE_GENERAL  = "furniture_general";
    private static final String COOKING_GENERAL    = "cooking_general";

    // Vanilla categories (detectar de onde veio)
    private static final Set<String> WORKBENCH_CATEGORIES = Set.of(
            "Workbench_Survival", "Workbench_Tools", "Workbench_Crafting", "Workbench_Tinkering"
    );
    private static final Set<String> WEAPON_CATEGORIES = Set.of(
            "Weapon_Sword", "Weapon_Mace", "Weapon_Battleaxe", "Weapon_Daggers", "Weapon_Bow"
    );
    private static final Set<String> ARMOR_CATEGORIES = Set.of(
            "Armor_Head", "Armor_Chest", "Armor_Hands", "Armor_Legs", "Weapon_Shield"
    );
    private static final Set<String> ALCHEMY_CATEGORIES = Set.of(
            "Alchemy_Potions", "Alchemy_Potions_Misc", "Alchemy_Bombs"
    );
    private static final Set<String> ARCANE_CATEGORIES = Set.of(
            "Arcane_Portals", "Arcane_Misc"
    );
    private static final Set<String> FARMING_CATEGORIES = Set.of(
            "Farming", "Seeds", "Saplings", "Essence", "Planters", "Decorative"
    );
    private static final Set<String> FURNITURE_CATEGORIES = Set.of(
            "Furniture_Storage", "Furniture_Beds", "Furniture_Lighting", "Furniture_Pottery",
            "Furniture_Textiles", "Furniture_Village_Walls", "Furniture_Misc", "Furniture_Seasonal"
    );
    private static final Set<String> COOKING_CATEGORIES = Set.of(
            "Prepared", "Baked", "Ingredients"
    );

    // evita recursion/loop quando a gente mesmo carrega recipes novas
    private static volatile boolean generating = false;

    public GrandWorkbench(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        System.out.println("[GrandWorkbench] setup() called");

        getEventRegistry().register(
                LoadedAssetsEvent.class,
                CraftingRecipe.class,
                (LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) -> {
                    if (generating) return;
                    generateGrandWorkbenchRecipes(event.getLoadedAssets().values().stream().toList());
                }
        );
    }

    private void generateGrandWorkbenchRecipes(List<CraftingRecipe> loaded) {
        generating = true;
        try {
            List<CraftingRecipe> generated = new ArrayList<>();

            for (CraftingRecipe recipe : loaded) {
                String id = recipe.getId();

                // não processa recipes que a gente mesmo gerou
                if (id != null && id.startsWith("GrandWorkbench_")) continue;

                BenchRequirement[] reqs = recipe.getBenchRequirement();
                if (reqs == null) continue;

                // Para cada BenchRequirement relevante, cria uma cópia da recipe
                for (BenchRequirement req : reqs) {
                    // só vamos espelhar recipes de crafting (não fieldcraft)
                    if (req.type != BenchType.Crafting) continue;

                    String condensed = condensedCategoryFor(req.id, req.categories);
                    if (condensed == null) continue; // não é uma bench que queremos absorver

                    // clone do recipe (copy constructor existe!)
                    CraftingRecipe clone = new CraftingRecipe(recipe);

                    // novo id único (um por requirement)
                    String newId = "GrandWorkbench_" + id + "_" + req.id;

                    // requirement novo: agora exige GrandWorkbench + categoria general correspondente
                    int clampedTier = Math.min(req.requiredTierLevel, 3);

                    BenchRequirement grandWorkbenchReq = new BenchRequirement(
                            BenchType.Crafting,
                            TARGET_BENCH_ID,
                            new String[]{condensed},
                            clampedTier
                    );

                    setField(clone, "id", newId);
                    setField(clone, "benchRequirement", new BenchRequirement[]{grandWorkbenchReq});

                    generated.add(clone);
                }
            }

            if (!generated.isEmpty()) {
                System.out.println("[GrandWorkbench] Generating " + generated.size() + " GrandWorkbench_* recipes...");
                // carrega como assets novos
                CraftingRecipe.getAssetStore().loadAssets("GrandWorkbench:GrandWorkbench", generated);
            } else {
                System.out.println("[GrandWorkbench] No recipes to generate this pass.");
            }
        } finally {
            generating = false;
        }
    }

    private String condensedCategoryFor(String benchId, String[] categories) {
        if (benchId == null) return null;

        // Workbench
        if ("Workbench".equals(benchId) && hasAny(categories, WORKBENCH_CATEGORIES)) return WORKBENCH_GENERAL;

        // Weapon bench
        if ("Weapon_Bench".equals(benchId) && hasAny(categories, WEAPON_CATEGORIES)) return WEAPON_GENERAL;

        // Armor bench
        if ("Armor_Bench".equals(benchId) && hasAny(categories, ARMOR_CATEGORIES)) return ARMOR_GENERAL;

        // Alchemy
        if ("Alchemybench".equals(benchId) && hasAny(categories, ALCHEMY_CATEGORIES)) return ALCHEMY_GENERAL;

        // Arcane
        if ("Arcanebench".equals(benchId) && hasAny(categories, ARCANE_CATEGORIES)) return ARCANE_GENERAL;

        // Farming
        if ("Farmingbench".equals(benchId) && hasAny(categories, FARMING_CATEGORIES)) return FARMING_GENERAL;

        // Furniture
        if ("Furniture_Bench".equals(benchId) && hasAny(categories, FURNITURE_CATEGORIES)) return FURNITURE_GENERAL;

        // Cooking
        if ("Cookingbench".equals(benchId) && hasAny(categories, COOKING_CATEGORIES)) return COOKING_GENERAL;

        return null;
    }

    private boolean hasAny(String[] arr, Set<String> set) {
        if (arr == null) return false;
        for (String s : arr) {
            if (set.contains(s)) return true;
        }
        return false;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("[GrandWorkbench] Failed setting field '" + fieldName + "'", e);
        }
    }
}
