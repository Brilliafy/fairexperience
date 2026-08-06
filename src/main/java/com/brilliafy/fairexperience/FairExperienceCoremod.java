package com.brilliafy.fairexperience;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FML coremod that doubles as MixinBooter's early mixin loader.
 *
 * <p>MixinBooter only discovers {@code IEarlyMixinLoader}s from the FML coremod list
 * (MixinBooterPlugin#gatherEarlyLoaders). An annotation-based loader class is silently
 * ignored on both client and server, so without this coremod the mixins never apply and
 * the mod has no effect (vanilla XP costs are used).</p>
 *
 * <p>Loading the mixins in the early phase is required because the targets
 * ({@code EntityPlayer}, {@code ContainerEnchantment}) load during FML container
 * construction, which is too early for a late mixin config.</p>
 *
 * <p>The jar manifest must declare {@code FMLCorePlugin} pointing at this class.
 * Do <em>not</em> add the {@code MixinConfigs} manifest attribute: MixinBooter then
 * pre-adds the jar to the LaunchClassLoader and FML sees the same mod twice
 * (DuplicateModsFoundException) on servers with clean paths.</p>
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class FairExperienceCoremod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.fairexperience.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    @Nullable
    public String getModContainerClass() {
        return null;
    }

    @Override
    @Nullable
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    @Nullable
    public String getAccessTransformerClass() {
        return null;
    }
}
