-- Add soft delete support to users table
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;

-- Create an index on deleted_at for soft delete queries
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
