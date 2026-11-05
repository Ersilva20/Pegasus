package dev.mylauncher.core;

import java.util.List;

/** Implemented by a mod's {@code @Mod} class when one jar contains several features. */
public interface FeatureProvider {
    List<? extends Feature> features();
}
