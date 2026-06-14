# Fase Plataforma 1B - Prueba rápida

## Objetivo

Separar la administración global de plataforma en una sección propia del sidebar: **Plataforma / Super Admin**.

## SQL

Ejecutar:

```bash
mysql -u root -p productos_selva_belen < manual_sql/platform-superadmin-menu-phase1b.sql
```

## Pruebas con admin_demo

1. Iniciar sesión con `admin_demo`.
2. Confirmar que en el sidebar aparece la sección **Plataforma / Super Admin**.
3. Abrir **Administración global**.
4. Probar:
   - `/admin/platform/clients`
   - `/admin/platform/templates`
   - `/admin/platform/modules`
5. Confirmar que el menú **Sistema** ya no muestra los enlaces de negocios, plantillas ni catálogo global de módulos.

## Pruebas con readonly_demo / oper_demo / mkt_demo

1. Iniciar sesión con cada usuario.
2. Confirmar que no aparece la sección **Plataforma / Super Admin**.
3. Probar entrar manualmente a `/admin/platform/clients`.
4. Debe negar el acceso si el usuario no tiene permiso global de plataforma.

## Resultado esperado

- Configuración del negocio actual queda en **Sistema**.
- Configuración multi-negocio queda en **Plataforma / Super Admin**.
- La plataforma queda lista para la Fase Plataforma 2: aprovisionamiento automático de base de datos.
