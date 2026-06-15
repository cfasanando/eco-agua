# Fase Plataforma 4B - Instancias protegidas y monitor comercial

## Objetivo

Registrar Eco Agua y Productos de la Selva como instancias existentes protegidas, mover Restaurante demo al puerto 8084 y ocultar Tienda China Express del monitor principal mientras no se use como demo comercial.

## Resultado esperado

En `/admin/platform/instances` deben verse por defecto:

- Eco Agua del Amazonas - protegido - puerto 8081.
- Productos de la Selva Belén - protegido - puerto 8082.
- Restaurante El Buen Sabor - demo - puerto 8084.

Tienda China Express queda pausada/oculta. Se puede ver usando el botón **Todas**.

## Prueba rápida

1. Ejecutar `manual_sql/platform-protected-instances-phase4b.sql` sobre la base principal del Super Admin.
2. Compilar y reiniciar la app principal.
3. Abrir `/admin/platform/instances`.
4. Confirmar que Eco Agua y Productos de la Selva tienen badge `Protegido`.
5. Confirmar que los protegidos muestran comando habitual, pero no requieren runtime generado.
6. Abrir `/admin/platform/instances?show=all` y confirmar que Tienda China aparece como oculta/pausada.
7. Abrir el aprovisionamiento de Eco Agua o Productos de la Selva y confirmar que la pantalla advierte que es protegida y bloquea acciones de instalación.
8. Usar Restaurante El Buen Sabor para aprovisionar la demo en puerto 8084.

## Puertos recomendados

- 8081: Eco Agua / Super Admin.
- 8082: Productos de la Selva Belén.
- 8084: Restaurante El Buen Sabor.
- 8083: Tienda China Express, pausado por ahora.

## Comandos útiles

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

./scripts/run-dev.sh
./scripts/run-belen.sh
bash scripts/run-client.sh demo_restaurante_buen_sabor 8084
```

## Validación SQL

```sql
SELECT code, business_name, status, database_status, runtime_port, public_url,
       management_mode, monitor_visible, protected_instance
FROM platform_business_client
ORDER BY runtime_port;
```
