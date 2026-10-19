# Vintage Resourcify

`1.7.10` [Resourcify](https://github.com/DeDiamondPro/Resourcify) backport.

![logo](images/logo_small.png)

[![hub](images/badges/github.png)](https://github.com/AngelicIndustries/VintageResourcify/releases)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/vintage-resourcify)
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/vintage-resourcify)
[![67](images/badges/67.png)](https://67minecraft.com/mod/vintage-resourcify)
[![mcmodcn](images/badges/mcmodcn.png)](https://www.mcmod.cn/class/27359.html)
[![maven](images/badges/maven.png)](https://maven.angelic.industries/#/releases/industries/angelic/vintageresourcify/VintageResourcify)
![forge](images/badges/forge.png)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)

### ⚠️ This project's CurseForge API token has not been approved yet. Please set your own in the config.

### ⚠️ The CurseForge version disables user-configured Modrinth-compatible platforms. Provider addon mods are supported.

## Features

* Browse, download, remove, update resource packs and shaders ingame
* Added resource packs and shader file picker
* Drag and drop resource packs and shaders onto their respective screens ([lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify) required)
* Support for adding third party Modrinth instances
* Integration hooks for the separate VintagePackedPacks project (in development; a matching build is required)

Vintage Resourcify supports Angelica [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/angelica)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/angelica/versions) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/Angelica) and Swansong [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/swansong)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/swansong) [![git](images/icons/git.png)](https://github.com/vfx-dev/SwanSong). 

![search](images/screenshots/search.png)

## Platform addons

Separate Forge mods can register providers through the public API. See [Platform addons](https://github.com/AngelicIndustries/VintageResourcify/wiki/Platform-Addons) for the contract and examples. Addon providers are supported in both regular and CurseForge builds.

Starting with 1.0.6, the Java/Kotlin namespace and Maven group are `industries.angelic.vintageresourcify`. Addons compiled against `dev.dediamondpro.resourcify` must be rebuilt; use the matching updated VRAP release. The mod ID (`vintage-resourcify`), configuration directory and installed-pack update indexes have not changed.

## Dependencies

* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)
* [Forgelin: Legacy](https://modrinth.com/mod/forgelin-legacy) with Kotlin 2.4.10 or newer [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/forgelin-legacy) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/Forgelin). The tested runtime is `2.4.3-2.4.10`.
* [ModularUI2](https://github.com/GTNewHorizons/ModularUI2) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/modularui)  [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/modularui) [![git](images/icons/git.png)](https://github.com/GTNewHorizons/ModularUI2)
* [AngelLib](https://www.curseforge.com/minecraft/mc-mods/angellib) 1.0.0 or newer (replaces FentLib) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/angellib) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/angellib) [![67](images/icons/67.png)](https://67minecraft.com/mod/angellib) [![git](images/icons/git.png)](https://github.com/AngelicIndustries/AngelLib)


## Building

```sh
./gradlew build
```

For a local release-candidate build without creating a tag, use `VERSION=1.0.6 ./gradlew build`. This builds and tests both the regular and CurseForge distributions; it does not publish anything.

See the [1.0.6 release checklist](docs/release-1.0.6.md) for verification results, remaining manual checks and the matching VRAP release order.

Maven coordinates: `industries.angelic.vintageresourcify:VintageResourcify:<version>` at `https://maven.angelic.industries/releases`.

## Credits

* Resourcify by DeDiamondPro
* [Catalogue-Vintage](https://github.com/RuiXuqi/Catalogue-Vintage) for globe icon
* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10)
* Sounds: [Zapsplat](https://www.zapsplat.com) ([License](https://www.zapsplat.com/license-type/standard-license/))

## License

`LGPLv3`.

* [Resourcify assets and code are licensed under GPLv3](https://github.com/DeDiamondPro/Resourcify/blob/master/LICENSE).

## Buy me some creatine

* [ko-fi.com](https://ko-fi.com/jackisasubtlejoke)
* Monero: `893tQ56jWt7czBsqAGPq8J5BDnYVCg2tvKpvwTcMY1LS79iDabopdxoUzNLEZtRTH4ewAcKLJ4DM4V41fvrJGHgeKArxwmJ`

<br>

![license](images/license_small.png)
