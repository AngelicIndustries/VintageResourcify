package dev.dediamondpro.resourcify.gui.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import cpw.mods.fml.client.config.ConfigGuiType;
import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import dev.dediamondpro.resourcify.VintageResourcify;
import dev.dediamondpro.resourcify.config.Config;

/**
 * Vanilla-Forge-style settings UI. Each element subclasses
 * {@link DummyConfigElement} so {@code set()} writes through to our JSON-backed
 * {@link Config} and triggers a save, instead of just mutating in-memory state.
 * This keeps upstream Resourcify's JSON file the single source of truth while
 * still letting us use Forge's built-in GuiConfig (ESC to close, "Done" button,
 * etc).
 */
public class ResourcifyConfigGui extends GuiConfig {

    public ResourcifyConfigGui(GuiScreen parent) {
        super(parent, buildElements(), VintageResourcify.MODID, false, false, I18n.format("resourcify.config.title"));
    }

    private static List<IConfigElement> buildElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(
            new DummyConfigElement<Boolean>(
                "debugMode",
                Config.Companion.getInstance()
                    .getDebugMode(),
                ConfigGuiType.BOOLEAN,
                "resourcify.config.debug-mode") {

                @Override
                public void set(Boolean value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setDebugMode(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new JsonStringListElement(
                "additionalResourcePackFolders",
                Config.Companion.getInstance()
                    .getAdditionalResourcePackFolders(),
                "resourcify.config.additional-resource-pack-folders") {

                @Override
                protected void saveValues(List<String> values) {
                    Config.Companion.getInstance()
                        .setAdditionalResourcePackFolders(new ArrayList<>(values));
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new JsonStringListElement(
                "additionalShaderPackFolders",
                Config.Companion.getInstance()
                    .getAdditionalShaderPackFolders(),
                "resourcify.config.additional-shader-pack-folders") {

                @Override
                protected void saveValues(List<String> values) {
                    Config.Companion.getInstance()
                        .setAdditionalShaderPackFolders(new ArrayList<>(values));
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new DummyConfigElement<Boolean>(
                "autoUpdateChecks",
                Config.Companion.getInstance()
                    .getAutoUpdateChecks(),
                ConfigGuiType.BOOLEAN,
                "resourcify.config.auto-update-checks") {

                @Override
                public void set(Boolean value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setAutoUpdateChecks(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new DummyConfigElement<Boolean>(
                "enableSounds",
                Config.Companion.getInstance()
                    .getEnableSounds(),
                ConfigGuiType.BOOLEAN,
                "resourcify.config.enable-sounds") {

                @Override
                public void set(Boolean value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setEnableSounds(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new DummyConfigElement<String>(
                "markdownTheme",
                Config.Companion.getInstance()
                    .getMarkdownTheme(),
                ConfigGuiType.STRING,
                "resourcify.config.markdownTheme",
                new String[] { "dark", "light" }) {

                @Override
                public void set(String value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setMarkdownTheme(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new DummyConfigElement<String>(
                "curseUserAgent",
                Config.Companion.getInstance()
                    .getCurseUserAgent(),
                ConfigGuiType.STRING,
                "resourcify.config.curse-user-agent") {

                @Override
                public void set(String value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setCurseUserAgent(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        list.add(
            new DummyConfigElement<String>(
                "curseApiToken",
                Config.Companion.getInstance()
                    .getCurseApiToken(),
                ConfigGuiType.STRING,
                "resourcify.config.curse-api-token") {

                @Override
                public void set(String value) {
                    super.set(value);
                    Config.Companion.getInstance()
                        .setCurseApiToken(value);
                    Config.Companion.save(Config.Companion.getInstance());
                }
            });
        return list;
    }

    /** Forge's array editor passes Object[], even for STRING lists. Avoid the String[] bridge cast. */
    private abstract static class JsonStringListElement extends DummyConfigElement.DummyListElement<Object> {

        JsonStringListElement(String name, List<String> values, String langKey) {
            super(name, values.toArray(), ConfigGuiType.STRING, langKey);
        }

        @Override
        public void set(Object[] values) {
            Object[] copy = Arrays.copyOf(values, values.length);
            super.set(copy);
            this.values = copy;
            List<String> strings = new ArrayList<>(copy.length);
            for (Object value : copy) {
                if (value != null) strings.add(value.toString());
            }
            saveValues(strings);
        }

        protected abstract void saveValues(List<String> values);
    }
}
