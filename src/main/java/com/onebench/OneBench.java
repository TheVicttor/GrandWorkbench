package com.onebench;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class OneBench extends JavaPlugin {

    public OneBench(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // 1) Log para sabermos que o plugin carregou
        System.out.println("[OneBench] setup() called");

        // 2) Quando recipes carregarem, injeta.
        //    (LoadedAssetsEvent dispara quando o AssetStore carrega CraftingRecipe assets)
        getEventRegistry().register(
                LoadedAssetsEvent.class,
                CraftingRecipe.class,
                (LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) -> {
                    // Esse evento pode disparar múltiplas vezes; tudo bem.
                    System.out.println("[OneBench] CraftingRecipe assets loaded: " + event.getLoadedAssets().size());
                    injectRecipes();
                }
        );

        // 3) Tentativa inicial também (caso já esteja carregado)
        injectRecipes();
    }

    private void injectRecipes() {
        final String TARGET = "OneBench";

        BenchRecipeRegistry merged = new BenchRecipeRegistry(TARGET);

        int countWorkbench = copyBenchInto(merged, "Workbench", null, TARGET);
        int countSword = copyBenchInto(merged, "Weapon_Bench", "Weapon_Sword", TARGET);

        merged.recompute();
        injectRegistry(TARGET, merged);

        System.out.println("[OneBench] Injected recipes into '" + TARGET + "': " +
                "from Workbench=" + countWorkbench + ", from Weapon_Bench(Sword)=" + countSword);
    }

    private int copyBenchInto(BenchRecipeRegistry targetRegistry, String sourceBenchId, String onlyCategoryOrNull, String targetBenchId) {
        List<CraftingRecipe> recipes = CraftingPlugin.getBenchRecipes(BenchType.Crafting, sourceBenchId, onlyCategoryOrNull);

        int added = 0;

        for (CraftingRecipe recipe : recipes) {
            BenchRequirement[] reqs = recipe.getBenchRequirement();
            if (reqs == null) continue;

            for (BenchRequirement req : reqs) {
                if (req.type != BenchType.Crafting) continue;
                if (!sourceBenchId.equals(req.id)) continue;

                if (onlyCategoryOrNull != null) {
                    if (req.categories == null) continue;
                    boolean ok = false;
                    for (String c : req.categories) {
                        if (onlyCategoryOrNull.equals(c)) { ok = true; break; }
                    }
                    if (!ok) continue;
                }

                BenchRequirement aliased = new BenchRequirement(
                        BenchType.Crafting,
                        targetBenchId,
                        req.categories,
                        req.requiredTierLevel
                );

                targetRegistry.addRecipe(aliased, recipe);
                added++;
            }
        }

        return added;
    }

    @SuppressWarnings("unchecked")
    private void injectRegistry(String benchId, BenchRecipeRegistry registry) {
        try {
            Field f = CraftingPlugin.class.getDeclaredField("registries");
            f.setAccessible(true);
            Map<String, BenchRecipeRegistry> map = (Map<String, BenchRecipeRegistry>) f.get(null);
            map.put(benchId, registry);
        } catch (Exception e) {
            throw new RuntimeException("OneBench: failed to inject recipes", e);
        }
    }
}
