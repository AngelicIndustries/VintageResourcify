#!/usr/bin/env python3
"""Read-only checks of the two release jars and generated Maven POM."""
import argparse
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
from zipfile import ZipFile

PACKAGE = 'industries.angelic.vintageresourcify'
PREFIX = PACKAGE.replace('.', '/') + '/'
ROOT = Path(__file__).resolve().parents[1]


def check_jar(path, version, limited):
    with ZipFile(path) as jar:
        names = jar.namelist()
        assert len(names) == len(set(names)), 'Duplicate zip entries'
        assert not any(n.startswith('dev/dediamondpro/resourcify/') for n in names), 'Old package leaked'
        assert not any(n.startswith(('kotlin/', 'industries/angelic/angellib/')) for n in names), 'Bundled runtime dependency'
        for relative in ('VintageResourcify.class', 'core/EarlyMixinLoader.class',
                         'api/ProviderApi.class', 'config/ConfiguredPlatforms.class', 'services/DistributionPolicy.class'):
            assert PREFIX + relative in names, relative
        for name in names:
            if name.startswith(PREFIX) and name.endswith('.class'):
                assert struct.unpack('>H', jar.read(name)[6:8])[0] <= 52, 'Not Java 8 bytecode: ' + name
        manifest = jar.read('META-INF/MANIFEST.MF').decode().replace('\r\n ', '').replace('\n ', '')
        assert 'FMLCorePlugin: ' + PACKAGE + '.core.EarlyMixinLoader' in manifest
        assert 'MixinConfigs: mixins.vintage-resourcify.json' in manifest
        mod = json.loads(jar.read('mcmod.info'))['modList'][0]
        assert mod['modid'] == 'vintage-resourcify'
        assert mod['version'] == version, mod['version']
        assert mod['url'] == 'https://github.com/AngelicIndustries/VintageResourcify'
        assert ('disables user-configured platforms' in mod['description']) == limited
        for suffix, package in (('', 'mid'), ('.early', 'early'), ('.late', 'late')):
            config = json.loads(jar.read('mixins.vintage-resourcify' + suffix + '.json'))
            assert config['package'] == PACKAGE + '.mixins.' + package
            assert config['refmap'] in names
        refmap = jar.read('mixins.vintage-resourcify.refmap.json').decode()
        assert 'dev/dediamondpro/resourcify' not in refmap
        assert 'industries/angelic/vintageresourcify' in refmap
        assert any(n.startswith(PREFIX + 'libs/commonmark/') for n in names)
        assert not any(n.startswith('org/commonmark/') for n in names)
        if limited:
            assert not any(n.startswith(PREFIX + 'config/ConfiguredPlatforms$') for n in names)
        policy = jar.read(PREFIX + 'services/DistributionPolicy.class')
        print('PASS', path.name, '(CurseForge)' if limited else '(regular)')
        return policy


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', default='1.0.6')
    args = parser.parse_args()
    libs = ROOT / 'build/libs'
    regular = check_jar(libs / ('vintage-resourcify-' + args.version + '.jar'), args.version, False)
    limited = check_jar(libs / ('vintage-resourcify-' + args.version + '-curseforge.jar'), args.version, True)
    assert regular != limited, 'CurseForge replacement policy was not packaged'
    pom = ET.parse(ROOT / 'build/publications/maven/pom-default.xml')
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    assert pom.findtext('m:groupId', namespaces=ns) == PACKAGE
    assert pom.findtext('m:artifactId', namespaces=ns) == 'VintageResourcify'
    assert pom.findtext('m:version', namespaces=ns) == args.version
    dependencies = [(d.findtext('m:groupId', namespaces=ns), d.findtext('m:artifactId', namespaces=ns))
                    for d in pom.findall('m:dependencies/m:dependency', ns)]
    assert ('industries.angelic.angellib', 'AngelLib') in dependencies
    assert not any('fentlib' in group.lower() or artifact == 'FentLib' for group, artifact in dependencies)
    print('PASS Maven coordinates and AngelLib dependency')


if __name__ == '__main__':
    main()
