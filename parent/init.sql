-- PostgreSQL runs this script once when initializing an empty data volume.
-- Each application service owns a separate database.
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE cart_db;
CREATE DATABASE order_db;
