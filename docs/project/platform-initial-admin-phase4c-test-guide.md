# Plataforma 4C - Usuario administrador inicial por cliente

## Objetivo

Corregir el aprovisionamiento de clientes nuevos para que el bootstrap cree automáticamente el usuario administrador inicial del negocio.

## Credenciales demo iniciales

- Usuario: `admin_demo`
- Contraseña: `Demo12345`

Cambiar la contraseña después de validar la instancia demo.

## Caso actual: Restaurante El Buen Sabor

La instancia `demo_restaurante_buen_sabor` usa la base real `restaurante_buen_sabor` y el puerto `8084`.

Para reparar la base ya aprovisionada:

```bash
mysql -u root -p < manual_sql/platform-initial-admin-current-restaurant.sql
```

Luego entrar en:

```text
http://localhost:8084/login
```

## Validación SQL

```bash
mysql -u root -p restaurante_buen_sabor -e "
SELECT u.id, u.username, u.active, GROUP_CONCAT(r.variable ORDER BY r.variable) AS roles
FROM user u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.rol_id
WHERE u.username = 'admin_demo'
GROUP BY u.id, u.username, u.active;
"
```

Debe mostrar `admin_demo`, activo y con roles `ADMIN_PRINC` y `ROLE_OWNER`.

## Validación web

1. Mantener o levantar el runtime del restaurante:

```bash
bash runtime-clients/demo_restaurante_buen_sabor/run.sh
```

2. Entrar a:

```text
http://localhost:8084/login
```

3. Probar rutas internas:

```text
http://localhost:8084/home
http://localhost:8084/admin/restaurant/dashboard
http://localhost:8084/admin/restaurant/orders/new
http://localhost:8084/admin/restaurant/kitchen
```

## Nuevos clientes

Desde este cambio, el paso `3. Aplicar configuración inicial` genera el usuario inicial automáticamente en la base destino.
