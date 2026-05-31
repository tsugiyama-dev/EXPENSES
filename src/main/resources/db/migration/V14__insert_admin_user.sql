

INSERT INTO users (email, password, role)
SELECT 'admin@example.com', '$2a$10$aZdOoAFkxyKiXBIK8PvFkuKjqG6z/vlrh8zPxnZl6hHEq2pjqjFIW', 'ROLE_ADMIN'
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE email = 'admin@example.com'
);

INSERT INTO users (email, password, role)
SELECT 'approver@example.com', '$2a$10$aZdOoAFkxyKiXBIK8PvFkuKjqG6z/vlrh8zPxnZl6hHEq2pjqjFIW', 'ROLE_APPROVER'
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE email = 'approver@example.com'
)