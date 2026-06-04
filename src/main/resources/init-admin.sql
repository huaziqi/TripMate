-- Run this once on your MySQL server to create the first super admin.
-- Password is: admin123  (change after first login!)
-- To generate a different password hash: new BCryptPasswordEncoder().encode("yourpassword")
INSERT INTO admin_user (username, password, role, status)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SUPER_ADMIN', 1);
