# Restaurante - Fase 2E: roles operativos

## Objetivo

Separar el uso diario del módulo restaurante por usuario operativo, sin depender solo de `admin_demo`.

## Usuarios demo

Todos usan la clave temporal:

```text
Demo12345
```

| Usuario | Rol | Acceso esperado |
|---|---|---|
| `admin_demo` | Dueño / administrador | Acceso completo |
| `mozo_demo` | Mozo / salón | Panel restaurante, mesas y nueva comanda |
| `cocina_demo` | Cocina | Pantalla cocina y cambio de estados de preparación |
| `caja_demo` | Caja | Caja diaria, reporte diario y cobro de comandas |

## Aplicar SQL en cliente actual

```bash
mysql -u root -p < manual_sql/restaurant-operational-roles-phase2e-current-client.sql
```

## Validar usuarios y roles

```sql
USE restaurante_buen_sabor;

SELECT u.username, u.active, GROUP_CONCAT(r.variable ORDER BY r.variable) AS roles
FROM user u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.rol_id
WHERE u.username IN ('admin_demo', 'mozo_demo', 'cocina_demo', 'caja_demo')
GROUP BY u.id, u.username, u.active
ORDER BY u.username;
```

## Prueba funcional

1. Ingresar como `mozo_demo`.
   - Debe redirigir a `/admin/restaurant/dashboard`.
   - Debe poder crear comandas.
   - No debe ver caja diaria como opción principal.

2. Ingresar como `cocina_demo`.
   - Debe redirigir a `/admin/restaurant/kitchen`.
   - Debe poder cambiar estados de cocina.
   - No debe poder cobrar.

3. Ingresar como `caja_demo`.
   - Debe redirigir a `/admin/restaurant/cash`.
   - Debe poder ver comandas abiertas y cobrarlas.
   - Debe poder abrir el reporte diario.

4. Ingresar como `admin_demo`.
   - Debe mantener acceso completo.

## Reinicio recomendado

```bash
mvn clean -DskipTests package

JAR="$(find target -maxdepth 1 -type f -name "*.jar" ! -name "*sources*" | head -n 1)"

java -jar "$JAR" \
  --spring.config.additional-location="file:runtime-clients/demo_restaurante_buen_sabor/application.properties"
```
