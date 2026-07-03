-- ============================================================================
--  AzTU Support — seed data
--  NOTE: seed passwords are BCrypt-hashed here (plain text: "Sonoma89!&").
-- ============================================================================

-- ── Roles ───────────────────────────────────────────────────────────────────
INSERT INTO roles (name, description) VALUES
    ('USER',         'Can open tickets to a platform or "Additional".'),
    ('SUPPORT_TEAM', 'Can open tickets and resolve / change ticket status.'),
    ('ADMIN',        'Support duties + approve registrations + manage platforms/categories.'),
    ('SUPER_ADMIN',  'Full access; sees everything and can change any user''s role.');

-- ── Platforms ───────────────────────────────────────────────────────────────
INSERT INTO platforms (name, description, active, is_additional) VALUES
    ('Student Information System', 'AzTU student portal (grades, enrolment, transcripts).', TRUE, FALSE),
    ('Moodle (LMS)',               'Learning management system for courses and assignments.',        TRUE, FALSE),
    ('Corporate Email & Office',   'AzTU e-mail accounts and Office 365 services.',                  TRUE, FALSE),
    ('Wi-Fi & Network',            'Campus Wi-Fi, VPN and network connectivity.',                    TRUE, FALSE),
    ('AzTU Website',               'Public university website (aztu.edu.az) and sub-sites.',         TRUE, FALSE),
    ('Additional / Other',         'Requests that are not tied to a specific named platform.',       TRUE, TRUE);

-- ── Categories ──────────────────────────────────────────────────────────────
-- Generic categories for each named platform.
INSERT INTO categories (platform_id, name, description)
SELECT p.id, c.name, c.description
FROM platforms p
CROSS JOIN (VALUES
        ('Login / Access Issue', 'Cannot sign in or access the platform.'),
        ('Bug / Error',          'The platform is showing an error or behaving incorrectly.'),
        ('Feature Request',      'Request for a new capability or change.'),
        ('General Question',     'A question about how to use the platform.')
    ) AS c(name, description)
WHERE p.is_additional = FALSE;

-- Configurable sub-categories for the "Additional / Other" option.
INSERT INTO categories (platform_id, name, description)
SELECT p.id, c.name, c.description
FROM platforms p
CROSS JOIN (VALUES
        ('Hardware Issue',       'Desktop, laptop, printer or peripheral problems.'),
        ('Software Request',     'Install or license a piece of software.'),
        ('Account / Access',     'New account, permissions or access request.'),
        ('Other',                'Anything else not covered above.')
    ) AS c(name, description)
WHERE p.is_additional = TRUE;

-- ── Pre-approved (ACTIVE) accounts ──────────────────────────────────────────
INSERT INTO users (first_name, last_name, email, password_hash, role_id, status, email_verified, approved_at)
VALUES
    ('Firdovsi', 'Rzaev',
     'firdovsi.rzaev@aztu.edu.az',
     '$2b$10$kdVK1pA9NhLFmc0AVebZy.0WI3EOroQFDHQbLohWRXhDfOXcH8iV.',
     (SELECT id FROM roles WHERE name = 'SUPPORT_TEAM'),
     'ACTIVE', TRUE, now()),
    ('Bakhtiyar', 'Badalov',
     'bakhtiyar.badalov@aztu.edu.az',
     '$2b$10$0w.YupYHLb0J8pz5k4ccCOPUuExIQ6aSkhKiuAihHBEBiQjnA7Ei.',
     (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'),
     'ACTIVE', TRUE, now());

-- ── Keep identity sequences in sync after explicit inserts ──────────────────
SELECT setval(pg_get_serial_sequence('roles', 'id'),      (SELECT MAX(id) FROM roles));
SELECT setval(pg_get_serial_sequence('platforms', 'id'),  (SELECT MAX(id) FROM platforms));
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('users', 'id'),      (SELECT MAX(id) FROM users));
