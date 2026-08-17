\set ON_ERROR_STOP on
\pset pager off

\if :{?dev_seed_project_ids}
\else
  \set dev_seed_project_ids ''
\endif

\if :{?dev_seed_project_limit}
\else
  \set dev_seed_project_limit 5
\endif

\if :{?dev_seed_expeditions_per_project}
\else
  \set dev_seed_expeditions_per_project 3
\endif

BEGIN;

CREATE TEMP TABLE dev_seed_requested_projects AS
SELECT DISTINCT btrim(project_id_text)::integer AS id
FROM regexp_split_to_table(:'dev_seed_project_ids', ',') AS project_id_text
WHERE btrim(project_id_text) <> '';

CREATE TEMP TABLE dev_seed_event_counts AS
SELECT expedition_id, count(*)::bigint AS rows
FROM network_1.event
GROUP BY expedition_id;

CREATE TEMP TABLE dev_seed_sample_counts AS
SELECT expedition_id, count(*)::bigint AS rows
FROM network_1.sample
GROUP BY expedition_id;

CREATE TEMP TABLE dev_seed_tissue_counts AS
SELECT expedition_id, count(*)::bigint AS rows
FROM network_1.tissue
GROUP BY expedition_id;

CREATE TEMP TABLE dev_seed_diagnostics_counts AS
SELECT expedition_id, count(*)::bigint AS rows
FROM network_1.diagnostics
GROUP BY expedition_id;

CREATE TEMP TABLE dev_seed_expedition_counts AS
SELECT
    e.id,
    e.project_id,
    COALESCE(event_counts.rows, 0) AS event_rows,
    COALESCE(sample_counts.rows, 0) AS sample_rows,
    COALESCE(tissue_counts.rows, 0) AS tissue_rows,
    COALESCE(diagnostics_counts.rows, 0) AS diagnostics_rows,
    COALESCE(event_counts.rows, 0)
      + COALESCE(sample_counts.rows, 0)
      + COALESCE(tissue_counts.rows, 0)
      + COALESCE(diagnostics_counts.rows, 0) AS core_rows
FROM public.expeditions e
LEFT JOIN dev_seed_event_counts event_counts ON event_counts.expedition_id = e.id
LEFT JOIN dev_seed_sample_counts sample_counts ON sample_counts.expedition_id = e.id
LEFT JOIN dev_seed_tissue_counts tissue_counts ON tissue_counts.expedition_id = e.id
LEFT JOIN dev_seed_diagnostics_counts diagnostics_counts ON diagnostics_counts.expedition_id = e.id;

CREATE TEMP TABLE dev_seed_project_counts AS
SELECT
    p.id,
    COALESCE(sum(ec.core_rows), 0)::bigint AS core_rows
FROM public.projects p
LEFT JOIN dev_seed_expedition_counts ec ON ec.project_id = p.id
GROUP BY p.id;

CREATE TEMP TABLE dev_seed_keep_projects AS
WITH automatic_projects AS (
    SELECT pc.id
    FROM dev_seed_project_counts pc
    JOIN public.projects p ON p.id = pc.id
    WHERE NOT EXISTS (SELECT 1 FROM dev_seed_requested_projects)
      AND p.public
      AND p.discoverable
      AND pc.core_rows > 0
    ORDER BY pc.core_rows ASC, p.id
    LIMIT :dev_seed_project_limit
)
SELECT p.id
FROM public.projects p
JOIN dev_seed_requested_projects requested ON requested.id = p.id
UNION
SELECT id
FROM automatic_projects;

CREATE UNIQUE INDEX dev_seed_keep_projects_id_idx ON dev_seed_keep_projects (id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM dev_seed_keep_projects) THEN
    RAISE EXCEPTION 'dev seed selected no projects. Check dev_seed_project_ids or dev_seed_project_limit.';
  END IF;
END
$$;

CREATE TEMP TABLE dev_seed_keep_expeditions AS
WITH ranked AS (
    SELECT
        ec.id,
        ec.project_id,
        row_number() OVER (
            PARTITION BY ec.project_id
            ORDER BY ec.core_rows ASC, ec.id ASC
        ) AS project_rank
    FROM dev_seed_expedition_counts ec
    JOIN dev_seed_keep_projects kp ON kp.id = ec.project_id
    WHERE ec.core_rows > 0
)
SELECT id, project_id
FROM ranked
WHERE project_rank <= :dev_seed_expeditions_per_project;

CREATE UNIQUE INDEX dev_seed_keep_expeditions_id_idx ON dev_seed_keep_expeditions (id);
CREATE INDEX dev_seed_keep_expeditions_project_id_idx ON dev_seed_keep_expeditions (project_id);

SET LOCAL session_replication_role = replica;

DROP TABLE IF EXISTS network_1.event_photo_bkp_20250809_141820;
DROP TABLE IF EXISTS network_1.sample_photo_bkp_20250809_141820;

TRUNCATE network_1.audit_table;
TRUNCATE public.network_config_history;
TRUNCATE public.project_config_history;
TRUNCATE public.oauth_tokens;
TRUNCATE public.oauth_nonces;
TRUNCATE public.sra_submissions;
TRUNCATE public.user_invite;

DELETE FROM network_1.diagnostics
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.environmentalpackage
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.event_photo
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.extraction
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.fastasequence
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.fastqmetadata
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.sample_photo
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.tissue
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.sample
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM network_1.event
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM public.entity_identifiers
WHERE expedition_id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM public.expeditions
WHERE id NOT IN (SELECT id FROM dev_seed_keep_expeditions);

DELETE FROM public.worksheet_templates
WHERE project_id NOT IN (SELECT id FROM dev_seed_keep_projects);

DELETE FROM public.user_projects
WHERE project_id NOT IN (SELECT id FROM dev_seed_keep_projects);

DELETE FROM public.projects
WHERE id NOT IN (SELECT id FROM dev_seed_keep_projects);

DELETE FROM public.project_configurations pc
WHERE NOT EXISTS (
    SELECT 1
    FROM public.projects p
    WHERE p.config_id = pc.id
);

DELETE FROM public.networks n
WHERE NOT EXISTS (
    SELECT 1
    FROM public.projects p
    WHERE p.network_id = n.id
)
AND NOT EXISTS (
    SELECT 1
    FROM public.project_configurations pc
    WHERE pc.network_id = n.id
);

DELETE FROM public.oauth_clients;
INSERT INTO public.oauth_clients (id, client_secret, callback)
VALUES ('geome-dev-client', 'geome-dev-secret', 'http://localhost:8081/geome-db/oauth/callback');

DELETE FROM public.users
WHERE id = -1 OR username = 'dev';

INSERT INTO public.users (
    id,
    username,
    password,
    email,
    first_name,
    last_name,
    institution,
    password_reset_token,
    password_reset_expiration,
    subscription_expiration_date,
    date_joined,
    last_login,
    sra_username,
    sra_email,
    sra_first_name,
    sra_last_name
)
VALUES (
    -1,
    'dev',
    '1000:00112233445566778899aabbccddeeff0011223344556677:e0b5018945ae29721ee9ba474b20c75e44ca2425555f8ff7',
    'dev@example.test',
    'Dev',
    'User',
    'GeOMe local development',
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
);

UPDATE public.networks SET user_id = -1;
UPDATE public.project_configurations SET user_id = -1;
UPDATE public.projects
SET
    user_id = -1,
    project_contact = 'GeOMe Dev',
    project_contact_email = 'dev@example.test',
    principal_investigator = 'GeOMe Dev',
    principal_investigator_affiliation = 'GeOMe local development';
UPDATE public.expeditions SET user_id = -1;
UPDATE public.worksheet_templates SET user_id = -1;

DELETE FROM public.user_projects;
INSERT INTO public.user_projects (project_id, user_id)
SELECT id, -1
FROM public.projects;

DELETE FROM public.users
WHERE id <> -1;

SELECT setval('public.users_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.users), 0)), true);
SELECT setval('public.projects_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.projects), 0)), true);
SELECT setval('public.expeditions_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.expeditions), 0)), true);
SELECT setval('public.project_configurations_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.project_configurations), 0)), true);
SELECT setval('public.project_templates_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.worksheet_templates), 0)), true);
SELECT setval('public.entity_identifiers_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM public.entity_identifiers), 0)), true);
SELECT setval('network_1.event_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.event), 0)), true);
SELECT setval('network_1.sample_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.sample), 0)), true);
SELECT setval('network_1.tissue_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.tissue), 0)), true);
SELECT setval('network_1.diagnostics_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.diagnostics), 0)), true);
SELECT setval('network_1.sample_photo_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.sample_photo), 0)), true);
SELECT setval('network_1.event_photo_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.event_photo), 0)), true);
SELECT setval('network_1.extraction_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.extraction), 0)), true);
SELECT setval('network_1.fastasequence_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.fastasequence), 0)), true);
SELECT setval('network_1.fastqmetadata_id_seq', GREATEST(1, COALESCE((SELECT max(id) FROM network_1.fastqmetadata), 0)), true);

COMMIT;

SELECT 'projects' AS table_name, count(*) AS rows FROM public.projects
UNION ALL SELECT 'expeditions', count(*) FROM public.expeditions
UNION ALL SELECT 'users', count(*) FROM public.users
UNION ALL SELECT 'events', count(*) FROM network_1.event
UNION ALL SELECT 'samples', count(*) FROM network_1.sample
UNION ALL SELECT 'tissues', count(*) FROM network_1.tissue
UNION ALL SELECT 'diagnostics', count(*) FROM network_1.diagnostics
UNION ALL SELECT 'sample_photo', count(*) FROM network_1.sample_photo
UNION ALL SELECT 'fastqmetadata', count(*) FROM network_1.fastqmetadata
ORDER BY table_name;

SELECT
    p.id AS project_id,
    p.project_title,
    e.id AS expedition_id,
    e.expedition_code
FROM public.projects p
JOIN public.expeditions e ON e.project_id = p.id
ORDER BY p.id, e.id;
