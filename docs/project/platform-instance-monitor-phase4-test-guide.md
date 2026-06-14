# Fase Plataforma 4 - Monitor de instancias

## Objetivo

Agregar una pantalla de Super Admin para ver qué clientes/negocios están instalados, si tienen runtime generado y si su URL local responde.

## Ruta

- `/admin/platform/instances`

## Prueba rápida

1. Iniciar la plataforma principal en `http://localhost:8081`.
2. Entrar con `admin_demo` o `gerencia_demo`.
3. Abrir `/admin/platform/instances`.
4. Validar KPIs: negocios, listos, en línea, detenidos, por instalar y sin runtime.
5. Para un negocio listo, ejecutar en otra terminal:

```bash
bash scripts/run-client.sh demo_tienda_china_temu 8083
```

6. Volver a `/admin/platform/instances` y presionar **Actualizar estado**.
7. La instancia debe pasar de `DETENIDO` a `EN LÍNEA` si `http://localhost:8083` responde.
8. Usar el botón **Abrir** para entrar al negocio.

## Notas

- Esta pantalla no inicia ni detiene procesos. Solo verifica si la URL responde.
- Si una instancia sale como `DETENIDO`, copiar el comando mostrado en una terminal Git Bash.
- Si sale `SIN RUNTIME`, volver a aprovisionamiento y generar archivos runtime.
- Si sale `PENDIENTE`, completar primero el aprovisionamiento.
