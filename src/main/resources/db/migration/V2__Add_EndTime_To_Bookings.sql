-- Add end_time column to bookings table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='bookings' AND column_name='end_time') THEN
        ALTER TABLE bookings ADD COLUMN end_time TIMESTAMP;
    END IF;
END $$;

-- For existing bookings, set end_time to 2 hours after date_time as a default
UPDATE bookings SET end_time = date_time + INTERVAL '2 hours' WHERE end_time IS NULL;
