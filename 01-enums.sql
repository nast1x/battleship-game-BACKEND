-- Создаем ENUM типы перед созданием таблиц
CREATE TYPE game_status_enum AS ENUM ('WAITING', 'ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE game_type_enum AS ENUM ('SINGLEPLAYER', 'MULTIPLAYER');
CREATE TYPE cell_type_enum AS ENUM ('SHIP', 'SEA', 'MISS', 'HIT');
CREATE TYPE ship_type_enum AS ENUM ('CARRIER', 'BATTLESHIP', 'CRUISER', 'SUBMARINE', 'DESTROYER');

-- Создаем пользователя для компьютера
INSERT INTO player (nickname, password_hash, status, avatar_url)
SELECT 'COMPUTER', '$2a$10$dummyhash', true, 'computer-avatar.png'
WHERE NOT EXISTS (SELECT 1 FROM player WHERE nickname = 'COMPUTER');