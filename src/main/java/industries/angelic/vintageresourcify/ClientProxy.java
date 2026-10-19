package industries.angelic.vintageresourcify;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import industries.angelic.vintageresourcify.config.Config;
import industries.angelic.vintageresourcify.gui.ResourcifyTooltipStyle;
import industries.angelic.vintageresourcify.gui.pack.PackDropHandler;
import industries.angelic.vintageresourcify.services.ServiceRegistry;
import industries.angelic.vintageresourcify.util.ClientGuiTasks;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        // Touching Config.Companion.getInstance() runs the companion's init block,
        // which reads config/vintageresourcify/resourcify.json (or writes defaults if missing).
        // ServiceRegistry's static init registers built-in and configured services.
        Config.Companion.getInstance();
        ServiceRegistry.INSTANCE.getAllServices();

        VintageResourcify.LOG.info(
            "VintageResourcify {} initialized with {} service(s).",
            Tags.VERSION,
            ServiceRegistry.INSTANCE.getAllServices()
                .size());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance()
            .bus()
            .register(ClientGuiTasks.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ResourcifyTooltipStyle.INSTANCE);
        PackDropHandler.INSTANCE.register();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
