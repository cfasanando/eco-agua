CREATE TABLE IF NOT EXISTS password_reset_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_token_hash (token_hash),
  KEY idx_password_reset_user (user_id),
  KEY idx_password_reset_validity (token_hash, used_at, expires_at),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO platform_setting (variable, value, type, category, description) VALUES
('login.background_image', '/uploads/belen/portal/public.hero.background_image-20260527215046.jpg', 'image', 'public_site', 'Imagen de fondo de la pantalla de acceso'),
('login.logo', '/uploads/belen/portal/platform.logo-20260527214351.png', 'image', 'public_site', 'Logo de la pantalla de acceso'),
('login.title', 'Acceso colaboradores', 'string', 'public_site', 'Título de la pantalla de acceso'),
('login.subtitle', 'Ingresa con tu usuario para administrar productos, ventas y contenidos.', 'text', 'public_site', 'Subtítulo de la pantalla de acceso'),
('login.username_label', 'Usuario', 'string', 'public_site', 'Etiqueta del campo usuario'),
('login.username_placeholder', 'Ingresa tu usuario', 'string', 'public_site', 'Placeholder del campo usuario'),
('login.password_label', 'Contraseña', 'string', 'public_site', 'Etiqueta del campo contraseña'),
('login.password_placeholder', 'Ingresa tu contraseña', 'string', 'public_site', 'Placeholder del campo contraseña'),
('login.remember_label', 'Recordarme', 'string', 'public_site', 'Etiqueta de recordar sesión'),
('login.submit_label', 'Acceder', 'string', 'public_site', 'Texto del botón de acceso'),
('login.forgot_label', '¿Olvidaste tu contraseña?', 'string', 'public_site', 'Texto del enlace de recuperación'),
('login.back_to_public_label', 'Volver al portal', 'string', 'public_site', 'Texto del enlace para volver al portal público'),
('login.primary_color', '#166534', 'string', 'public_site', 'Color principal de la pantalla de acceso'),
('login.primary_hover_color', '#14532d', 'string', 'public_site', 'Color hover de la pantalla de acceso'),
('login.password_reset.request_title', 'Recuperar contraseña', 'string', 'public_site', 'Título de recuperación de contraseña'),
('login.password_reset.request_text', 'Ingresa tu usuario. Si existe y está activo, se generará un enlace temporal de recuperación.', 'text', 'public_site', 'Texto de recuperación de contraseña'),
('login.password_reset.reset_title', 'Crear nueva contraseña', 'string', 'public_site', 'Título para crear nueva contraseña'),
('login.password_reset.submit_label', 'Generar enlace de recuperación', 'string', 'public_site', 'Botón para solicitar recuperación'),
('login.password_reset.save_label', 'Guardar nueva contraseña', 'string', 'public_site', 'Botón para guardar nueva contraseña'),
('login.password_reset.show_dev_link', 'true', 'boolean', 'public_site', 'Mostrar enlace temporal en desarrollo local')
ON DUPLICATE KEY UPDATE
  value = VALUES(value),
  type = VALUES(type),
  category = VALUES(category),
  description = VALUES(description);
