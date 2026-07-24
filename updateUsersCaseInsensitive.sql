-- Resolve existing username case collisions and enforce case-insensitive usernames.
-- Review the explicit merge and rename list before running this against production.

BEGIN;

DO
$$
DECLARE
    r RECORD;
    actual_username TEXT;
BEGIN
    FOR r IN
        SELECT *
        FROM (VALUES
            (899, 952, 'Akropf', 'akropf'),
            (265, 537, 'avdr', 'AVDR'),
            (603, 602, 'ceschraidt', 'CESCHRAIDT'),
            (267, 350, 'clare', 'Clare'),
            (529, 518, 'GouldingT', 'gouldingT'),
            (533, 518, 'gouldingt', 'gouldingT')
        ) AS merge_plan(source_user_id, target_user_id, source_username, target_username)
    LOOP
        SELECT username
        INTO actual_username
        FROM users
        WHERE id = r.source_user_id;

        IF actual_username IS DISTINCT FROM r.source_username THEN
            RAISE EXCEPTION 'Expected source user id % to have username %, found %',
                r.source_user_id, r.source_username, actual_username;
        END IF;

        SELECT username
        INTO actual_username
        FROM users
        WHERE id = r.target_user_id;

        IF actual_username IS DISTINCT FROM r.target_username THEN
            RAISE EXCEPTION 'Expected target user id % to have username %, found %',
                r.target_user_id, r.target_username, actual_username;
        END IF;

        DELETE FROM user_projects source_membership
        USING user_projects target_membership
        WHERE source_membership.user_id = r.source_user_id
          AND target_membership.user_id = r.target_user_id
          AND source_membership.project_id = target_membership.project_id;

        UPDATE expeditions
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE networks
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE oauth_nonces
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE oauth_tokens
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE project_configurations
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE projects
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE sra_submissions
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE user_invite
        SET invited_by_id = r.target_user_id
        WHERE invited_by_id = r.source_user_id;

        UPDATE user_projects
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        UPDATE worksheet_templates
        SET user_id = r.target_user_id
        WHERE user_id = r.source_user_id;

        DELETE FROM users
        WHERE id = r.source_user_id;
    END LOOP;
END;
$$;

DO
$$
DECLARE
    r RECORD;
    actual_username TEXT;
BEGIN
    FOR r IN
        SELECT *
        FROM (VALUES
            (704, 'Deco313', 'deco313_704'),
            (822, 'Demo', 'demo_822'),
            (614, 'Hiepnd', 'hiepnd_614'),
            (825, 'Test', 'test_825'),
            (714, 'TestTest', 'testtest_714')
        ) AS rename_plan(user_id, expected_username, replacement_username)
    LOOP
        SELECT username
        INTO actual_username
        FROM users
        WHERE id = r.user_id;

        IF actual_username IS DISTINCT FROM r.expected_username THEN
            RAISE EXCEPTION 'Expected user id % to have username %, found %',
                r.user_id, r.expected_username, actual_username;
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1
        FROM users u
        JOIN (VALUES
            (704, 'Deco313', 'deco313_704'),
            (822, 'Demo', 'demo_822'),
            (614, 'Hiepnd', 'hiepnd_614'),
            (825, 'Test', 'test_825'),
            (714, 'TestTest', 'testtest_714')
        ) AS rename_plan(user_id, expected_username, replacement_username)
            ON lower(u.username) = lower(rename_plan.replacement_username)
        WHERE u.id <> rename_plan.user_id
    ) THEN
        RAISE EXCEPTION 'One or more replacement usernames are already in use';
    END IF;

    UPDATE users u
    SET username = rename_plan.replacement_username
    FROM (VALUES
        (704, 'Deco313', 'deco313_704'),
        (822, 'Demo', 'demo_822'),
        (614, 'Hiepnd', 'hiepnd_614'),
        (825, 'Test', 'test_825'),
        (714, 'TestTest', 'testtest_714')
    ) AS rename_plan(user_id, expected_username, replacement_username)
    WHERE u.id = rename_plan.user_id;
END;
$$;

DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY lower(username)
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Username case collisions remain';
    END IF;
END;
$$;

UPDATE users
SET username = lower(username)
WHERE username <> lower(username);

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_username_lowercase_check;

ALTER TABLE users
    ADD CONSTRAINT users_username_lowercase_check CHECK (username = lower(username));

COMMIT;
