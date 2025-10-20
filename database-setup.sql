-- Create table for notes
CREATE TABLE IF NOT EXISTS notes (
    id SERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster text search
CREATE INDEX IF NOT EXISTS idx_notes_text ON notes(text);

-- Function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at, only if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_trigger 
        WHERE tgname = 'update_notes_updated_at'
        AND tgrelid = 'notes'::regclass
    ) THEN
        CREATE TRIGGER update_notes_updated_at
        BEFORE UPDATE ON notes
        FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- Insert test data, only if they don't exist
INSERT INTO notes (text)
SELECT 'First note'
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE text = 'First note')
UNION ALL
SELECT 'Second note for testing'
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE text = 'Second note for testing')
UNION ALL
SELECT 'Example of editing a note'
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE text = 'Example of editing a note');