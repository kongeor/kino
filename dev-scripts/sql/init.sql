CREATE DATABASE kino;
CREATE USER kino WITH PASSWORD 'kino';
ALTER DATABASE kino owner to kino;
ALTER ROLE kino SET client_encoding TO 'utf8';
ALTER ROLE kino SET timezone TO 'UTC';
