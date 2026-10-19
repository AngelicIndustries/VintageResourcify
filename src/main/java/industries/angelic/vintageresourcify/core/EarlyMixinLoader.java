package industries.angelic.vintageresourcify.core;

import java.util.List;
import java.util.Set;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.angellib.core.AngelEarlyMixinLoader;
import industries.angelic.vintageresourcify.VintageResourcify;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader extends AngelEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + VintageResourcify.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Mixins.getEarlyMixinsForLoader();
    }
}
