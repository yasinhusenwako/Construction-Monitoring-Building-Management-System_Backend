-- Add endTime column to bookings table
ALTER TABLE bookings ADD COLUMN endTime TIMESTAMP;

-- For existing bookings, set endTime to 2 hours after dateTime as a default
UPDATE bookings SET endTime = dateTime + INTERVAL '2 hours' WHERE endTime IS NULL;
