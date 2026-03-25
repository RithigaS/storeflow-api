DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name='users' AND column_name='avatar_path'
    ) THEN
ALTER TABLE users RENAME COLUMN avatar_path TO avatar_url;
END IF;
END $$;