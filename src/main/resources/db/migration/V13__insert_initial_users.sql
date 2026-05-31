

INSERT INTO users (email, password, role)
SELECT 'test@example.com', '$2a$10$aZdOoAFkxyKiXBIK8PvFkuKjqG6z/vlrh8zPxnZl6hHEq2pjqjFIW', 'ROLE_USER'
WHERE NOT EXISTS (
  SELECT 1 FROM users  WHERE email = 'test@example.com'
)