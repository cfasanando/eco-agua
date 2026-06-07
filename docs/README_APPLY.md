# Eco Agua V1 - Manual final y checklist

Este ZIP no reemplaza código. Es documentación de cierre para la V1 del sistema.

## Qué contiene

- `00_ESTADO_FINAL_V1.md`: estado real del sistema y criterio de cierre.
- `01_MANUAL_TECNICO_INSTALACION.md`: instalación, ejecución local y estrategia multi-cliente simple.
- `02_CONFIGURACION_CLIENTE_BD.md`: cómo configurar cada cliente desde base de datos/pantallas.
- `03_CHECKLIST_SQL_MANUAL.md`: validaciones de columnas/tablas agregadas manualmente.
- `04_CHECKLIST_RUTAS_V1.md`: rutas principales para probar como administrador.
- `05_CHECKLIST_AGUA_ECO.md`: checklist recomendado para la instancia Agua Eco.
- `06_CHECKLIST_PRODUCTOS_BELEN.md`: checklist recomendado para la instancia Productos de la Selva Belén.
- `07_ROLES_PERMISOS_V1.md`: matriz práctica de roles y permisos.
- `08_NO_SUNAT_OFICIAL.md`: alcance contable real de la V1.
- `09_BACKLOG_POST_V1.md`: backlog recomendado después del cierre.
- `sql/verify_v1_manual_schema.sql`: SQL de verificación no destructivo.
- `sql/fix_platform_setting_mojibake_optional.sql`: SQL opcional para ubicar textos rotos.

## Qué hacer ahora

1. No copies este ZIP dentro del proyecto como código.
2. Guarda esta documentación fuera de `src`.
3. Ejecuta los checklists en ambas bases de datos: Agua Eco y Belén.
4. Si todo pasa, marca la V1 como cerrada para pruebas reales.

## Antes de commitear

El último contexto mostraba una carpeta de contexto sin seguimiento y la eliminación de un servicio antiguo. Revisa:

```bash
git status
```

No commitees carpetas tipo:

```bash
eco-agua-v1-manual-context-*/
eco-agua-v1-final-audit-context-*/
```

Si la eliminación del servicio hardcodeado ya fue aplicada correctamente, el commit recomendado es:

```bash
git add .
git commit -m "Add V1 installation and testing manual"
```
