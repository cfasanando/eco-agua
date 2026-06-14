# Fase Plataforma 2 - Guía de prueba rápida

## Objetivo

Validar el flujo seguro de aprovisionamiento para un negocio creado desde Super Admin.

## Requisitos

- Haber ejecutado `manual_sql/platform-phase1.sql`.
- Haber ejecutado `manual_sql/platform-superadmin-menu-phase1b.sql`.
- Iniciar sesión con `admin_demo` o `gerencia_demo`.

## SQL de esta fase

```bash
mysql -u root -p productos_selva_belen < manual_sql/platform-provisioning-phase2.sql
```

## Pantallas

- `/admin/platform/clients`
- `/admin/platform/clients/{id}`
- `/admin/platform/clients/{id}/provisioning`

## Prueba recomendada

1. Entrar a `/admin/platform/clients`.
2. Abrir un negocio demo, por ejemplo restaurante o tienda tipo Temu.
3. En el detalle, hacer clic en **Abrir aprovisionamiento**.
4. Revisar el plan de instalación.
5. Validar que aparezca el SQL `CREATE DATABASE IF NOT EXISTS`.
6. Validar que aparezcan los comandos manuales de `mysqldump` y `mysql`.
7. Hacer clic en **Crear base de datos vacía**.
8. Confirmar que el estado de BD cambie a `DATABASE_CREATED`.
9. Revisar que se registre historial.
10. Si copiaste la estructura manualmente, hacer clic en **Marcar estructura copiada**.
11. Después de aplicar el bootstrap SQL, hacer clic en **Marcar negocio activo**.

## Comportamiento esperado

- El botón de crear base ejecuta solo `CREATE DATABASE IF NOT EXISTS`.
- No borra bases existentes.
- No modifica la base actual, salvo el estado del cliente y el log.
- Si MySQL no permite crear bases, muestra error y deja el SQL listo para copiar/pegar.
- El bootstrap SQL se genera con branding, WhatsApp, ciudad, color y módulos activos.

## Nota técnica

Esta fase todavía no copia automáticamente la estructura ni cambia dinámicamente el datasource de la aplicación.
Eso queda preparado para una fase posterior, cuando se defina si cada cliente correrá como:

- instancia separada + base separada, o
- una sola app multi-tenant con router de datasource.
