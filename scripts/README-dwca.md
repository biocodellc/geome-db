# DwC-A Generator (Biocode/GEOME)

This script builds a Darwin Core Archive zip from GEOME project data.

Script: `scripts/generate_dwca_biocode.js`

## What it generates

- `occurrence.txt`
- `multimedia.txt` (from `Sample_Photo`)
- `meta.xml`
- `eml.xml`
- zip archive containing the 4 files

The output structure and field order are fixed to match your reference DwC-A package.
By default, the archive name is exactly `DWCA_OUTPUT_BASENAME.zip`.
Set `DWCA_APPEND_TIMESTAMP=true` if you want timestamped archives.

## Dependencies

- Node.js 18+
- npm package: `pg`
- system `zip` command available in PATH

Install dependency:

```bash
npm install pg
```

## .env settings

Create/update `.env` at repo root:

```dotenv
# GEOME auth (required)
GEOME_API_BASE=https://api.geome-db.org
GEOME_USERNAME=biocode
GEOME_PASSWORD=YOUR_PASSWORD
GEOME_CLIENT_ID=YOUR_CLIENT_ID
GEOME_CLIENT_SECRET=YOUR_CLIENT_SECRET

# DWCA generation options
DWCA_PROJECT_ID=75
GEOME_NETWORK_SCHEMA=network_1
DWCA_OUTPUT_DIR=/Users/jdeck/Downloads
DWCA_OUTPUT_BASENAME=dwca-biocode
DWCA_APPEND_TIMESTAMP=false

# Optional: custom EML content JSON
# DWCA_EML_OVERRIDES_JSON=/absolute/path/to/eml-overrides.json

# Optional: DB settings (if omitted, script falls back to
# src/main/environment/local/biocode-fims-database.properties)
# DB_HOST=149.165.170.158
# DB_PORT=5432
# DB_NAME=biscicol
# DB_USER=biscicol
# DB_PASSWORD=...
# DB_SSLMODE=require
```

## Run

```bash
node scripts/generate_dwca_biocode.js --verbose
```

Optional flags:

- `--env-file /path/to/.env`
- `--project-id 75`
- `--network-schema network_1`
- `--db-props-file src/main/environment/local/biocode-fims-database.properties`
- `--eml-overrides-json /path/to/eml-overrides.json`

## Output

On success, the script prints:

- generated zip path
- staging directory
- occurrence row count
- multimedia row count

Example output zip:

- `/Users/jdeck/Downloads/dwca-biocode.zip`

If `DWCA_APPEND_TIMESTAMP=true`, filename becomes timestamped.

## Dynamic properties behavior

`dynamicProperties` is automatically populated when project fields exist:

- `localcontexts_id` -> `local_contexts_project_id`
- `permit_guid` -> `access_and_benefits_sharing`

Example:

```json
{"local_contexts_project_id":"https://localcontextshub.org/projects/71b32571-0176-4627-8e01-4d78818432a7/","access_and_benefits_sharing":"https://doi.org/10.5281/zenodo.7150710"}
```

## Notes on `references`

`occurrence.txt` `references` is set from **Sample BCID** (`entity_identifiers` concept alias `Sample`) as requested.

## Notes on `catalogNumber`

`occurrence.txt` `catalogNumber` is set from Sample `materialSampleID` (`materialSampleID` / `urn:materialSampleID`), matching the original mapping behavior.

## EML placeholder template

`eml.xml` uses a legacy-style placeholder template derived from the previous Moorea Biocode EML output.
It includes XML TODO comments indicating where to map fields from GEOME project metadata in the future.
Use `DWCA_EML_OVERRIDES_JSON` to replace default template values as needed.

Example overrides file:

```json
{
  "dataset_title": "Moorea Biocode Project",
  "abstract": "Dataset abstract text.",
  "rights": "Rights statement text.",
  "methods": "Methods summary text.",
  "geographic_description": "Moorea, French Polynesia",
  "creator": {
    "givenName": "Chris",
    "surName": "Meyer",
    "organizationName": "Smithsonian Institution",
    "electronicMailAddress": "meyerc@si.edu"
  },
  "package_id": "8b8c78fb-53d7-434c-b800-61f38b30d0c9/v62.434",
  "alternate_identifier": "https://bnhmipt.berkeley.edu/resource?r=biocode"
}
```
