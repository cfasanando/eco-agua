# Platform installer state hotfix - test guide

## Objetivo

Corregir la pantalla de aprovisionamiento cuando un negocio queda en estados intermedios como `STRUCTURE_READY` pero todavía muestra archivos runtime anteriores.

## Prueba

1. Entrar como `admin_demo` o `gerencia_demo`.
2. Abrir `/admin/platform/clients`.
3. Entrar al negocio demo de Tienda China.
4. Abrir `Aprovisionamiento`.
5. Si el estado BD es `STRUCTURE_READY`, el botón `3. Aplicar configuración inicial` debe verse habilitado y destacado.
6. Ejecutar `3. Aplicar configuración inicial`.
7. Ejecutar `4. Activar negocio`.
8. Ejecutar `5. Generar / regenerar archivos runtime`.
9. Levantar el cliente:

```bash
bash scripts/run-client.sh demo_tienda_china_temu 8083
```

10. Abrir `http://localhost:8083`.

## Validaciones esperadas

- No debe aparecer error 500 si una acción falla.
- El error debe volver a la misma pantalla como mensaje visible.
- Los archivos runtime anteriores no deben confundir la etapa actual si el negocio está en `STRUCTURE_READY`.
- El botón de runtime debe permitir regenerar archivos cuando el negocio ya está listo.
