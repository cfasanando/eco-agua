# Fase Plataforma 1 - Guía de prueba

## Objetivo

Validar el primer bloque de Super Admin para registrar negocios, plantillas por rubro y módulos activos por cliente.

Esta fase no crea bases de datos automáticamente. Solo prepara la configuración para la Fase Plataforma 2.

## SQL requerido

```bash
mysql -u root -p productos_selva_belen < manual_sql/platform-phase1.sql
```

## Rutas nuevas

- `/admin/platform/clients`
- `/admin/platform/clients/new`
- `/admin/platform/clients/{id}`
- `/admin/platform/templates`
- `/admin/platform/modules`

## Usuarios de prueba

- `admin_demo`: debe administrar todo.
- `gerencia_demo`: debe poder consultar/administrar si tiene permisos de admin principal.
- `readonly_demo`: puede consultar si se le asignó `ver_plataforma`, pero no debe ejecutar cambios críticos en fases futuras.

## Prueba rápida

1. Entrar con `admin_demo`.
2. Abrir `/admin/platform/clients`.
3. Validar los KPIs de negocios, plantillas y módulos.
4. Abrir `/admin/platform/templates`.
5. Revisar plantillas: Agua, Productos Selva, Academia, Restaurante, Courier y Tienda tipo Temu.
6. Abrir `/admin/platform/modules`.
7. Revisar módulos por área.
8. Crear un nuevo negocio con plantilla Restaurante.
9. Confirmar que carga módulos recomendados.
10. Guardar.
11. Abrir el detalle del negocio.
12. Activar/desactivar módulos y guardar.

## Resultado esperado

- El negocio queda en estado `DRAFT`.
- La base queda en estado `PENDING_STRUCTURE`.
- Los módulos seleccionados quedan registrados en `platform_client_module`.
- No se crea todavía una base de datos nueva.

## Próxima fase

Fase Plataforma 2: aprovisionamiento automático de base de datos.
