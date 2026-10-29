"""Exercise release routing and SemVer RC allocation without publishing anything."""

import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("versions", Path(__file__).with_name("release-version.py"))
versions = importlib.util.module_from_spec(spec)
spec.loader.exec_module(versions)


class ReleaseVersionTest(unittest.TestCase):
    def resolve(self, tags=(), ref_type="branch", ref_name="main", **options):
        return versions.resolve(list(tags), ref_type, ref_name, **options)

    def test_snapshot_and_dry_run_ignore_prerelease_tags(self):
        tags = ["v1.9.0", "v1.10.0", "v2.0.0-rc.9"]
        self.assertEqual("1.10.1-SNAPSHOT", self.resolve(tags)["version"])
        self.assertEqual("1.10.1", self.resolve(tags, dry_run=True)["version"])
        self.assertEqual("0.0.1-rc.1", self.resolve(test_release=True)["version"])

    def test_rc_numbers_increment_numerically_and_only_for_the_selected_base(self):
        result = self.resolve(["v1.3.0", "v1.3.1-rc.2", "v1.3.1-rc.10", "v1.4.0-rc.99"], test_release=True)
        self.assertEqual("1.3.1-rc.11", result["version"])
        self.assertEqual("v1.3.1-rc.11", result["release_tag"])
        self.assertEqual("true", result["test_release"])
        self.assertEqual("false", result["is_snapshot"])
        self.assertEqual("2.0.0-rc.1", self.resolve(test_release=True, rc_base="2.0.0")["version"])

    def test_dry_run_exercises_the_rc_path(self):
        result = self.resolve(["v1.3.0"], test_release=True, dry_run=True)
        self.assertEqual("1.3.1-rc.1", result["version"])
        self.assertEqual("true", result["test_release"])
        self.assertEqual("true", result["dry_run"])

    def test_rc_tags_always_route_to_github(self):
        self.assertEqual("true", self.resolve(ref_type="tag", ref_name="v1.3.1-rc.1")["test_release"])
        stable = self.resolve(ref_type="tag", ref_name="v1.3.1")
        self.assertEqual("false", stable["test_release"])
        self.assertEqual("false", stable["is_prerelease"])
        self.assertEqual("1.3.1-rc.1", self.resolve(ref_type="tag", ref_name="v1.3.1-rc.1", dry_run=True)["version"])

    def test_invalid_versions_and_conflicting_options_fail(self):
        for tag in ["v01.3.1", "v1.3.1-rc.01", "v1.3.1-rc..1", "v1.3.1-", "1.3.1"]:
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                self.resolve(ref_type="tag", ref_name=tag)
        for options in [dict(rc_base="1.4.0"), dict(test_release=True, rc_base="01.4.0"),
                        dict(test_release=True, rc_base="1.3.0"),
                        dict(test_release=True, ref_type="tag", ref_name="v1.3.1")]:
            with self.subTest(options=options), self.assertRaises(ValueError):
                self.resolve(["v1.3.0"], **options)


if __name__ == "__main__":
    unittest.main()
