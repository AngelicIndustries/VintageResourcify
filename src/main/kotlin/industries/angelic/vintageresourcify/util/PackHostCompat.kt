package industries.angelic.vintageresourcify.util

import cpw.mods.fml.common.Loader
import industries.angelic.vintageresourcify.VintageResourcify
import industries.angelic.vintageresourcify.services.ProjectType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/** Optional pack-host integration. Discovery and management belong to the host mod. */
object PackHostCompat {
    private val methods = ConcurrentHashMap<String, Method>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    private fun call(name: String, vararg args: Any): Any? {
        if (!Loader.isModLoaded("vintagepackedpacks") || name in missing) return null
        return try {
            val method = methods[name] ?: Class.forName("org.fentanylsolutions.vintagepackedpacks.api.PackIntegration")
                .methods.first { it.name == name && it.parameterTypes.size == args.size }
                .also { methods[name] = it }
            method.invoke(null, *args)
        } catch (e: ReflectiveOperationException) {
            if (missing.add(name)) VintageResourcify.LOG.warn("Optional pack host integration unavailable: {}", name, e)
            null
        } catch (e: NoSuchElementException) {
            missing.add(name)
            null
        } catch (e: LinkageError) {
            if (missing.add(name)) VintageResourcify.LOG.warn("Optional pack host integration unavailable: {}", name, e)
            null
        }
    }

    @JvmStatic fun isScreen(screen: GuiScreen?): Boolean = screen != null && call("isScreen", screen) == true
    @JvmStatic fun refreshCurrent(): Boolean = call("refreshCurrent") == true
    @JvmStatic fun replace(old: File, new: File) { call("replace", old, new) }
    @JvmStatic fun enable(screen: GuiScreen?, file: File) { if (screen != null) call("enable", screen, file) }
    @JvmStatic fun stableName(file: File): String = call("stableName", file) as? String ?: file.name
    @JvmStatic fun owner(file: File): File? = call("owner", file) as? File
        ?: Minecraft.getMinecraft().resourcePackRepository.dirResourcepacks.takeIf { same(file.parentFile, it) }

    @JvmStatic fun roots(type: ProjectType, primary: File): List<File> {
        val shaders = type == ProjectType.IRIS_SHADER || type == ProjectType.OPTIFINE_SHADER
        val result = call("roots", primary, shaders) as? List<*> ?: return listOf(primary)
        return result.filterIsInstance<File>().ifEmpty { listOf(primary) }
    }

    @JvmStatic fun shader(primary: File, name: String): File? = call("shader", primary, name) as? File
        ?: listOf(File(primary, name), File(primary, "$name.zip")).firstOrNull { it.exists() }

    private fun same(first: File?, second: File): Boolean = first != null &&
        runCatching { first.canonicalFile == second.canonicalFile }.getOrDefault(first.absoluteFile == second.absoluteFile)
}
