# Fase Plataforma 3C - Prueba de run.sh y run-client.sh en Windows/Git Bash

## Objetivo

Corregir la ejecución de clientes generados en `runtime-clients/{perfil}` cuando se usa Git Bash en Windows.

El problema corregido era que Spring recibía rutas tipo `/c/Users/...` y podía fallar buscando `application.properties`. Ahora los scripts convierten la ruta con `cygpath -m` cuando está disponible, dejando la ruta como `C:/Users/...`.

## Prueba rápida

1. Generar archivos runtime desde:

```text
/admin/platform/clients/{id}/provisioning
```

2. Validar que exista:

```bash
ls -la runtime-clients/demo_tienda_china_temu
cat runtime-clients/demo_tienda_china_temu/application.properties
```

3. Ejecutar desde el script global:

```bash
bash scripts/run-client.sh demo_tienda_china_temu 8083
```

4. Abrir:

```text
http://localhost:8083
```

5. Ejecutar desde el script generado:

```bash
bash runtime-clients/demo_tienda_china_temu/run.sh
```

6. Confirmar que el log muestra una ruta compatible con Windows:

```text
[INFO] Config: C:/Users/PC/Projects/eco-agua-workspace/eco-agua/runtime-clients/demo_tienda_china_temu/application.properties
```

## Validación esperada

- La aplicación arranca con el puerto del cliente.
- Ya no aparece el error `Config data resource ... does not exist` con ruta `/c/Users/...`.
- `runtime-clients/` queda ignorado por Git.
