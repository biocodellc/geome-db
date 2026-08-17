\set ON_ERROR_STOP on

BEGIN;

TRUNCATE public.ezid_queue;

UPDATE public.client_identifiers
SET client_id = 'geome-dev-client';

DELETE FROM public.clients;

INSERT INTO public.clients (id, secret, access_token, token_expiration)
VALUES ('geome-dev-client', 'geome-dev-secret', NULL, NULL);

COMMIT;

SELECT 'bcid clients' AS table_name, count(*) AS rows FROM public.clients
UNION ALL
SELECT 'bcid client_identifiers', count(*) FROM public.client_identifiers
UNION ALL
SELECT 'bcid ezid_queue', count(*) FROM public.ezid_queue
ORDER BY table_name;
