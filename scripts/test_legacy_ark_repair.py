import unittest
import tempfile
from pathlib import Path
from unittest.mock import patch

import legacy_r2_ark_repair as repair


class LegacyArkRepairTests(unittest.TestCase):
    def apply_args(self):
        with patch("sys.argv", ["repair", "--legacy-prefix", "Q2", "--delay", "0"]):
            return repair.parse_args()

    def row(self):
        return repair.build_mapping_rows([{
            "tissueCatalogNumber": "ark:/21547/Q2MBIO3418.1",
            "tissueID": "MBIO3418.1", "projectId": "75",
            "bcid": "ark:/21547/CVL2MBIO3418.1",
        }], "Tissue", "Q2")[0]

    def test_chris_tissue_maps_to_its_own_record(self):
        record = {
            "tissueCatalogNumber": "http://n2t.net/ark:/21547/Q2MBIO3418.1",
            "tissueOtherCatalogNumbers": "ark:/21547/R2MBIO3418",
            "tissueID": "MBIO3418.1",
            "materialSampleID": "BMOO_01424",
            "bcid": "ark:/21547/CVL2MBIO3418.1",
            "projectId": "75",
            "expeditionCode": "MINV_2006",
        }
        rows = repair.build_mapping_rows([record], "Tissue", "Q2")
        self.assertEqual(len(rows), 1)
        row = rows[0]
        self.assertEqual(row["old_ark"], "ark:/21547/Q2MBIO3418.1")
        self.assertEqual(row["target_url"], "https://geome-db.org/record/ark:/21547/CVL2MBIO3418.1")
        self.assertEqual(row["source_field"], "tissueCatalogNumber")
        self.assertEqual(row["tissue_id"], "MBIO3418.1")
        self.assertIn("dc.title: Legacy Moorea Biocode tissue ARK\n", repair.ezid_payload(row))
        self.assertIn("dc.relation: ark:/21547/CVL2MBIO3418.1\n", repair.ezid_payload(row))

    def test_r2_default_is_preserved(self):
        rows = repair.build_mapping_rows([{
            "catalogNumber": "http://n2t.net/ark:/21547/R2CMPI38181",
            "bcid": "ark:/21547/CYA2Reedy01",
        }], "Sample")
        self.assertEqual(rows[0]["old_ark"], "ark:/21547/R2CMPI38181")
        self.assertIn("specimen ARK", repair.ezid_payload(rows[0]))
        with patch("sys.argv", ["repair"]):
            args = repair.parse_args()
        self.assertEqual(args.entity, "Sample")
        self.assertEqual(args.output_dir, repair.DEFAULT_OUTPUT_DIR)

    def test_q2_defaults_and_entity_guard(self):
        with patch("sys.argv", ["repair", "--legacy-prefix", "Q2"]):
            args = repair.parse_args()
        self.assertEqual(args.entity, "Tissue")
        self.assertIn("tissueCatalogNumber", args.source_fields)
        self.assertEqual(args.output_dir.name, "legacy-q2-ark-repair")
        with patch("sys.argv", ["repair", "--legacy-prefix", "Q2", "--entity", "Sample"]):
            with patch("sys.stderr"), self.assertRaises(SystemExit):
                repair.parse_args()

    def test_identifier_formats_and_tissue_suffix(self):
        for text in (
            "https://n2t.net/ark:/21547/Q2MBIO3418.1",
            "https://ezid.cdlib.org/id/ark:/21547/Q2MBIO3418.1",
            "https://arks.org/ark:21547/Q2MBIO3418.1",
            "https://arks.org/ark:/21547/Q2MBIO3418.1",
            "ark:21547/Q2MBIO3418.1",
            "(ark:/21547/Q2MBIO3418.1).",
        ):
            with self.subTest(text=text):
                self.assertEqual(repair.extract_legacy_arks(text, "Q2"), ["ark:/21547/Q2MBIO3418.1"])
        self.assertEqual(repair.extract_legacy_arks("ark:/21547/Q2MBIO3418.1"), [])

    def test_ambiguous_arks_are_excluded_and_duplicates_collapsed(self):
        rows = repair.build_mapping_rows([
            {"bcid": "ark:/21547/CVL2one", "tissueCatalogNumber": "ark:/21547/Q2shared"},
            {"bcid": "ark:/21547/CVL2two", "tissueCatalogNumber": "ark:/21547/Q2shared"},
            {"bcid": "ark:/21547/CVL2three", "tissueCatalogNumber": "ark:/21547/Q2unique",
             "tissueOtherCatalogNumbers": "ark:/21547/Q2unique"},
        ], "Tissue", "Q2")
        accepted, conflicts = repair.split_conflicts(rows)
        self.assertEqual([r["old_ark"] for r in accepted], ["ark:/21547/Q2unique"])
        self.assertEqual(len(conflicts), 2)
        self.assertTrue(all(r["conflict_reason"] == "old_ark_maps_to_multiple_current_bcids" for r in conflicts))

    def test_authentication_failure_prevents_writes(self):
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(401, "error", {})), \
                patch.object(repair, "put_ezid") as put:
            with self.assertRaises(repair.ScriptError):
                repair.apply_ezid(self.apply_args(), [self.row()])
            put.assert_not_called()

    def test_existing_other_target_is_never_overwritten_by_default(self):
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(200, "success:", {})), \
                patch.object(repair, "ezid_exact_lookup", return_value=("exists", "https://example.org/other", "")), \
                patch.object(repair, "put_ezid") as put, patch("sys.stderr"):
            results = repair.apply_ezid(self.apply_args(), [self.row()])
            self.assertEqual(results[0]["action"], "skipped")
            put.assert_not_called()

    def test_publication_records_redirect_mismatch_in_checkpoint(self):
        args = self.apply_args()
        args.verify_after = True
        checkpoint = []
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(200, "success:", {})), \
                patch.object(repair, "ezid_exact_lookup", return_value=("missing", "", "")), \
                patch.object(repair, "put_ezid", return_value=(201, "success: ark:/21547/Q2MBIO3418.1")), \
                patch.object(repair, "verify_n2t", return_value=("200", "https://example.org/project")), \
                patch("sys.stderr"):
            results = repair.apply_ezid(args, [self.row()], checkpoint.append)
        self.assertEqual(results[0]["action"], "published_verify_mismatch")
        self.assertEqual(checkpoint, results)

    def test_network_failure_is_recorded_for_resume(self):
        checkpoint = []
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(200, "success:", {})), \
                patch.object(repair, "ezid_exact_lookup", side_effect=repair.ScriptError("connection lost")), \
                patch("sys.stderr"):
            results = repair.apply_ezid(self.apply_args(), [self.row()], checkpoint.append)
        self.assertEqual(results[0]["action"], "failed")
        self.assertEqual(checkpoint, results)

    def test_verification_rejects_error_even_with_correct_url(self):
        row = self.row()
        result = {"action": "published", "message": ""}
        with patch.object(repair, "verify_n2t", return_value=("404", row["target_url"])):
            repair.record_verification(result, row, 30)
        self.assertEqual(result["action"], "published_verify_mismatch")

    def test_resume_verifies_matching_existing_record_without_writing(self):
        row = self.row()
        args = self.apply_args()
        args.skip_existing_exact = args.verify_after = True
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(200, "success:", {})), \
                patch.object(repair, "ezid_exact_lookup", return_value=("exists", row["target_url"], "")), \
                patch.object(repair, "put_ezid") as put, \
                patch.object(repair, "verify_n2t", return_value=("200", row["target_url"])), \
                patch("sys.stderr"):
            results = repair.apply_ezid(args, [row])
        put.assert_not_called()
        self.assertEqual(results[0]["action"], "skipped_existing_exact")
        self.assertEqual(results[0]["n2t_status"], "200")

    def test_resolver_403_pauses_further_verification(self):
        row = self.row()
        args = self.apply_args()
        args.verify_after = True
        with patch.object(repair, "ezid_credentials", return_value=("user", "pass")), \
                patch.object(repair, "request_text", return_value=(200, "success:", {})), \
                patch.object(repair, "ezid_exact_lookup", return_value=("missing", "", "")), \
                patch.object(repair, "put_ezid", return_value=(201, "success:")), \
                patch.object(repair, "verify_n2t", return_value=("403", "")) as verify, \
                patch("sys.stderr"):
            results = repair.apply_ezid(args, [row, {**row, "old_ark": "ark:/21547/Q2another"}])
        self.assertEqual(verify.call_count, 1)
        self.assertEqual(results[0]["action"], "published_verify_mismatch")
        self.assertEqual(results[1]["action"], "published_verify_deferred")
        self.assertEqual(results[1]["n2t_status"], "deferred")

    def test_reviewed_csv_rejects_wrong_destination_and_entity(self):
        args = self.apply_args()
        with tempfile.TemporaryDirectory() as tmp:
            args.input_mapping = Path(tmp) / "mapping.csv"
            row = self.row()
            repair.write_csv(args.input_mapping, [row], list(row))
            self.assertEqual(repair.read_mapping(args), [row])
            for key, value in (("target_url", "https://example.org"), ("entity", "Sample"),
                               ("project_id", "1"), ("source_value", "ark:/21547/Q2another")):
                with self.subTest(key=key):
                    repair.write_csv(args.input_mapping, [{**row, key: value}], list(row))
                    with self.assertRaises(repair.ScriptError):
                        repair.read_mapping(args)


if __name__ == "__main__":
    unittest.main()
