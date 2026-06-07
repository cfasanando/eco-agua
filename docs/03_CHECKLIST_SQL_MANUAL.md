# Checklist de SQL manual V1

En el contexto final no aparece una carpeta `manual_sql` versionada. Aun así, durante el desarrollo se agregaron campos/tablas manuales. Antes de cerrar V1, validar que existan en cada base de datos cliente.

## Bases que deben validarse

- Agua Eco.
- Productos de la Selva Belén.
- Cualquier base nueva creada para cliente.

## Validación rápida

Ejecutar el archivo:

```text
sql/verify_v1_manual_schema.sql
```

## Elementos esperados

### RRHH asistencia

Tabla esperada:

```text
employee_attendance
```

Debe permitir asistencia por trabajador y fecha.

### Producción lote/costo/merma

Tabla esperada:

```text
production_order
```

Columnas esperadas:

```text
batch_code
quantity_expected
quantity_produced
quantity_loss
loss_reason
unit_cost_estimated
real_unit_cost
```

### Producción calidad

Columnas esperadas:

```text
quality_status
quality_checked_at
quality_checked_by
quality_cleaning_ok
quality_packaging_ok
quality_labeling_ok
quality_product_ok
quality_observation
```

### Producción vencimiento

Columnas esperadas:

```text
expiry_date
expiry_observation
```

## Reglas

- No ejecutar `ALTER TABLE` a ciegas si la columna ya existe.
- Validar primero con `SHOW COLUMNS`.
- Aplicar los cambios en Agua Eco y Belén por separado.
- Hacer backup antes de tocar una base con datos reales.
