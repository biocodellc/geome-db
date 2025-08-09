DO $$
DECLARE
  ts text := to_char(clock_timestamp(), 'YYYYMMDD_HH24MISS');
  s  text := 'network_1';
BEGIN
  -- event_photo
  EXECUTE format(
    'CREATE TABLE %I.%I (LIKE %I.%I INCLUDING ALL)',
    s, 'event_photo_bkp_'||ts,
    s, 'event_photo'
  );
  EXECUTE format(
    'INSERT INTO %I.%I SELECT * FROM %I.%I',
    s, 'event_photo_bkp_'||ts,
    s, 'event_photo'
  );

  -- sample_photo
  EXECUTE format(
    'CREATE TABLE %I.%I (LIKE %I.%I INCLUDING ALL)',
    s, 'sample_photo_bkp_'||ts,
    s, 'sample_photo'
  );
  EXECUTE format(
    'INSERT INTO %I.%I SELECT * FROM %I.%I',
    s, 'sample_photo_bkp_'||ts,
    s, 'sample_photo'
  );
END$$;

