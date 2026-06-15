# Fase Restaurante 1B - Activación desde interfaz

## Objetivo

Permitir instalar y activar el módulo Restaurante desde la interfaz del negocio actual, sin ejecutar SQL manualmente.

## Ruta principal

```text
/admin/system-modules
```

## Prueba rápida

1. Iniciar sesión como `admin_demo` o `gerencia_demo`.
2. Ir a **Sistema -> Módulos del sistema**.
3. En la tarjeta **Restaurante / carta, mesas y cocina**, hacer clic en **Instalar y activar Restaurante**.
4. Verificar que aparezcan los badges:
   - `Tablas instaladas`
   - `Módulo activo`
5. Abrir:
   - `/admin/restaurant/dashboard`
   - `/admin/restaurant/tables`
   - `/admin/restaurant/orders/new`
   - `/admin/restaurant/kitchen`
   - `/restaurant/menu`

## Validación SQL opcional

```sql
SHOW TABLES LIKE 'restaurant_%';
SELECT variable, value FROM platform_setting WHERE variable = 'module.restaurant.enabled';
```

## Nota

La instalación desde UI crea las tablas necesarias, activa el flag `module.restaurant.enabled` y carga datos demo básicos de restaurante. No elimina tablas cuando se desactiva; solo oculta/deshabilita el módulo.
