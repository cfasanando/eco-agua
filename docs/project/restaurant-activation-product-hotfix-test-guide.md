# Hotfix Restaurante 1C - instalación sin tabla product

## Objetivo
Evitar el error `Failed to open the referenced table 'product'` al instalar Restaurante desde Sistema → Módulos del sistema.

## Prueba
1. Compilar y reiniciar la aplicación.
2. Entrar a `/admin/system-modules`.
3. Clic en **Instalar y activar Restaurante**.
4. Verificar que ya no aparezca error de FK.
5. Abrir:
   - `/admin/restaurant/dashboard`
   - `/admin/restaurant/tables`
   - `/admin/restaurant/orders/new`
   - `/admin/restaurant/kitchen`
   - `/restaurant/menu`

## Validación SQL
```sql
SHOW TABLES LIKE 'product';
SHOW TABLES LIKE 'restaurant_%';
SELECT variable, value FROM platform_setting WHERE variable = 'module.restaurant.enabled';
```
