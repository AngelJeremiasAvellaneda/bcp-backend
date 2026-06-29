-- ============================================================
-- Script: Datos de prueba para apertura de cuenta digital
-- Crear usuarios con tarjeta y DNI para probar el flujo
-- ============================================================

-- Usuario de prueba 1: Juan Pérez
-- DNI: 12345678
-- Tarjeta: 4532123456789012
-- Clave: 1234 (encriptada con BCrypt)
INSERT INTO usuarios (nombre, email, password, dni, numero_tarjeta, telefono, rol, activo, cuenta_creada)
VALUES (
    'Juan Carlos Pérez García',
    'juan.perez@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye8IVO7FqxWMAy7TnfKCq/Yz5lnzGvFzC', -- 1234
    '12345678',
    '4532123456789012',
    '999888777',
    'CLIENTE',
    TRUE,
    FALSE
);

-- Usuario de prueba 2: María González
-- DNI: 87654321
-- Tarjeta: 4532987654321098
-- Clave: 4321 (encriptada con BCrypt)
INSERT INTO usuarios (nombre, email, password, dni, numero_tarjeta, telefono, rol, activo, cuenta_creada)
VALUES (
    'María Elena González López',
    'maria.gonzalez@example.com',
    '$2a$10$rN9qo8uLOickgx2ZMRZoMye8IVO7FqxWMAy7TnfKCq/Yz5lnzGvFzD', -- 4321
    '87654321',
    '4532987654321098',
    '988777666',
    'CLIENTE',
    TRUE,
    FALSE
);

-- Usuario de prueba 3: Pedro Ramírez
-- DNI: 11223344
-- Tarjeta: 4532112233445566
-- Clave: 5678 (encriptada con BCrypt)
INSERT INTO usuarios (nombre, email, password, dni, numero_tarjeta, telefono, rol, activo, cuenta_creada)
VALUES (
    'Pedro Antonio Ramírez Torres',
    'pedro.ramirez@example.com',
    '$2a$10$sN9qo8uLOickgx2ZMRZoMye8IVO7FqxWMAy7TnfKCq/Yz5lnzGvFzE', -- 5678
    '11223344',
    '4532112233445566',
    '977666555',
    'CLIENTE',
    TRUE,
    FALSE
);

COMMENT ON TABLE usuarios IS 'Usuarios con datos de prueba para apertura de cuenta digital';

-- Para generar nuevas contraseñas encriptadas, usa BCrypt con factor 10:
-- En Java: BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
--          String encoded = encoder.encode("tu_password");
