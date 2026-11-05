package dev.mylauncher.core.mixin;

import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to title / action bar state for the Titles and Action Bar mods. */
@Mixin(GuiIngame.class)
public interface GuiIngameAccessor {
    @Accessor("displayedTitle")
    String ml$getTitle();

    @Accessor("displayedSubTitle")
    String ml$getSubTitle();

    @Accessor("titlesTimer")
    int ml$getTitlesTimer();

    @Accessor("titleFadeIn")
    int ml$getTitleFadeIn();

    @Accessor("titleDisplayTime")
    int ml$getTitleDisplayTime();

    @Accessor("titleFadeOut")
    int ml$getTitleFadeOut();

    @Accessor("recordPlaying")
    String ml$getRecordPlaying();

    @Accessor("recordPlayingUpFor")
    int ml$getRecordPlayingUpFor();

    @Accessor("recordIsPlaying")
    boolean ml$getRecordIsPlaying();
}
