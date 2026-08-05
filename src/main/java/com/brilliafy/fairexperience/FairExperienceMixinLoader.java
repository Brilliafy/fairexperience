package com.brilliafy.fairexperience;

import zone.rong.mixinbooter.IEarlyMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

import java.util.Collections;
import java.util.List;

@MixinLoader
public class FairExperienceMixinLoader implements IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.fairexperience.json");
    }
}
