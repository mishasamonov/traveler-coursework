-- V2. Extension of the TravelerAPI database for the web application (allowed by the task note):
-- accounts for Spring Security and the owner of a plan. Existing tables and data stay valid:
-- owner_id is nullable, so plans created through the REST API of the labs remain correct rows.

CREATE TABLE app_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(254) NOT NULL CHECK (email = lower(email) AND position('@' IN email) > 1),
  display_name VARCHAR(100) NOT NULL CHECK (length(trim(display_name)) >= 2),
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX ux_app_users_email ON app_users (email);

-- Who created the plan. ON DELETE SET NULL keeps the plan if an account is ever removed.
ALTER TABLE travel_plans
  ADD COLUMN owner_id UUID REFERENCES app_users (id) ON DELETE SET NULL;

CREATE INDEX idx_plans_owner ON travel_plans (owner_id);
-- The public catalogue filters by is_public and sorts by date most of the time.
CREATE INDEX idx_plans_public_start ON travel_plans (is_public, start_date);
