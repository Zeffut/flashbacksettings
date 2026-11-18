"""Guard the manual-update-only distribution; not a general network sandbox."""
from pathlib import Path
import re
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java'


class NoEmbeddedUpdater(unittest.TestCase):
    def test_no_updater_sources_or_calls(self):
        files = list(JAVA.rglob('*.java'))
        self.assertTrue(files)
        for file in files:
            text = file.read_text()
            self.assertNotRegex(text, r'\b(?:UpdateService|ModrinthApi|JanitorMain)\b', str(file))
            self.assertNotIn('fr.zeffut.flashbacksettings.update', text, str(file))

    def test_network_source_is_telemetry_only(self):
        network = re.compile(r'\b(?:java\.net|HttpClient|URLConnection|openStream|Socket)\b')
        found = [p.relative_to(JAVA).as_posix() for p in JAVA.rglob('*.java')
                 if network.search(p.read_text())]
        self.assertEqual(found, ['fr/zeffut/flashbacksettings/telemetry/Telemetry.java'])
        telemetry = (JAVA / found[0]).read_text()
        self.assertIn('BodyPublishers.ofString(body)', telemetry)
        self.assertIn('BodyHandlers.discarding()', telemetry)

    def test_built_jars_have_no_updater(self):
        import os
        path = os.environ.get('VERIFY_JAR')
        if not path:
            self.skipTest('VERIFY_JAR is set in CI after build')
        with zipfile.ZipFile(path) as jar:
            bad = [name for name in jar.namelist()
                   if name.startswith('fr/zeffut/flashbacksettings/update/')]
        self.assertEqual(bad, [])


if __name__ == '__main__':
    unittest.main()
