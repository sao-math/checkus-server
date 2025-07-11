-- Add deleted_at column to users table for soft delete functionality
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;

-- Add index for better query performance when filtering non-deleted users
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
