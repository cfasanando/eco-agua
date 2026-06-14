# Fase Plataforma 3 - Perfiles de ejecución por negocio

## Objetivo

Permitir que cada negocio configurado desde Super Admin tenga un perfil de ejecución independiente:

- `application-{perfil}.properties`
- `run-{perfil}.sh`
- puerto local sugerido
- URL local/pública
- comandos para ejecutar la instancia

Esta fase no hace multitenant en caliente. Mantiene el enfoque seguro: una app base, una base por cliente y una instancia por puerto.

## SQL

Ejecutar:

```bash
mysql -u root -p productos_selva_belen < manual_sql/platform-client-runtime-phase3.sql
```

## Pruebas

1. Iniciar sesión con `admin_demo` o `gerencia_demo`.
2. Ir a `/admin/platform/clients`.
3. Abrir un negocio activo/listo.
4. Entrar a `Perfil ejecución`.
5. Confirmar que se muestran:
   - perfil Spring,
   - puerto,
   - URL,
   - application properties,
   - script de ejecución,
   - comandos.
6. Guardar un puerto nuevo, por ejemplo `8083`.
7. Descargar `application-{perfil}.properties`.
8. Descargar `run-{perfil}.sh`.
9. Copiar los archivos según los comandos mostrados.
10. Ejecutar:

```bash
bash scripts/run-client.sh tienda_china_express 8083
```

## Resultado esperado

La app debe iniciar con el perfil del negocio en el puerto indicado, usando la base de datos del cliente.
