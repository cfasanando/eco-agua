# GastoClaro Personal - 5C.1A Checkbox Persistence Hotfix

## Objetivo

Corregir persistencia de checkboxes en formularios recurrentes de GastoClaro:

- Gastos fijos: `Generar cada mes`, `Obligatorio`, `Activo`
- Fuentes de ingreso: `Generar cada mes`, `Activa`
- Deudas: `Tiene cuota fija`, `Generar obligaciones desde cronograma`

## Problema

Los formularios incluían campos hidden manuales con el mismo nombre y valor `false` antes del checkbox. En algunos bindings, el valor `false` ganaba aunque el checkbox estuviera marcado.

## Prueba

1. Abrir `/gasto-claro/fixed-expenses`.
2. Crear o editar `Alquiler`.
3. Marcar:
   - Generar cada mes
   - Obligatorio
   - Activo
4. Guardar.
5. Volver a editar el registro.
6. Verificar que los checks sigan marcados.
7. Repetir la prueba en `/gasto-claro/income-sources`.
8. Repetir en `/gasto-claro/debts` con `Tiene cuota fija` y `Generar obligaciones desde cronograma`.

## Nota

Si un registro ya fue guardado antes del hotfix como inactivo/no automático, volver a editarlo, marcar los checks correctos y guardar una vez más.
