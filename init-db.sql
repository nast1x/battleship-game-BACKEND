-- Создаем ENUM типы перед созданием таблиц
CREATE TYPE game_status_enum AS ENUM ('WAITING', 'ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE game_type_enum AS ENUM ('SINGLEPLAYER', 'MULTIPLAYER');
CREATE TYPE cell_type_enum AS ENUM ('SHIP', 'SEA', 'MISS', 'HIT');

-- Создаем пользователя для компьютера, если его нет
INSERT INTO player (player_id, nickname, password_hash, status, avatar_url)
SELECT 0, 'Computer', '$2a$10$N.zMD/aQFbEJp3v5YhXQguP7aZwKvWJ0r1WlXJ2QH0y9JY7Y7qL8a', true, 'computer-avatar.png'
WHERE NOT EXISTS (SELECT 1 FROM player WHERE player_id = 0);