# Fase Plataforma 2B - Guía de prueba

## Objetivo

Pulir la pantalla de aprovisionamiento para que sea más clara en demo y más segura para uso operativo.

## Rutas a probar

- `/admin/platform/clients`
- `/admin/platform/clients/{id}`
- `/admin/platform/clients/{id}/provisioning`
- `/admin/platform/clients/{id}/provisioning/create-database.sql`
- `/admin/platform/clients/{id}/provisioning/bootstrap.sql`

## Prueba con negocio nuevo

1. Crear un negocio desde `/admin/platform/clients/new`.
2. Entrar al detalle del negocio.
3. Abrir `Aprovisionamiento`.
4. Confirmar que el estado indica pendiente de aprovisionamiento.
5. Confirmar que el botón `Crear base de datos vacía` está habilitado.
6. Copiar comandos con el botón `Copiar comandos`.
7. Descargar `SQL bootstrap`.
8. Descargar `SQL para crear base`.

## Prueba con negocio ya listo

1. Abrir un negocio con `Estado BD = READY`.
2. Entrar a `Aprovisionamiento`.
3. Confirmar que aparece el mensaje `Negocio listo`.
4. Confirmar que las acciones que ya no aplican están desactivadas.
5. Confirmar que sigue disponible `Reiniciar estado` para pruebas controladas.
6. Confirmar que el botón `Abrir negocio` aparece cuando el negocio está activo.

## Resultado esperado

- La pantalla muestra un estado general claro.
- Los comandos se pueden copiar.
- Los SQL se pueden copiar o descargar.
- No se repite accidentalmente la creación de base cuando ya está lista.
- No hay cambios de base de datos requeridos por esta fase.
