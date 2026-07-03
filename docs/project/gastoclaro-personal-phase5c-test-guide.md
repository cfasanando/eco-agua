# GastoClaro Personal - Fase 5C - Guía de prueba

## Objetivo

Agregar cronogramas de deuda y generación de obligaciones mensuales para que GastoClaro pueda manejar:

- bancos con cronograma de cuotas;
- prestamistas con interés mensual;
- pagos únicos;
- descuentos automáticos;
- obligaciones generadas para el plan mensual.

Esta fase no incluye datos personales reales en código ni seeds versionados.

## Rutas principales

- `/gasto-claro/debts`
- `/gasto-claro/debts/{id}/schedule`
- `/gasto-claro/monthly-plan`

## Prueba 1: deuda bancaria con cronograma

1. Ir a `/gasto-claro/debts`.
2. Crear o editar una deuda tipo `Préstamo bancario propio` o `Banco por tercero`.
3. Seleccionar modo `Cronograma bancario`.
4. Completar cuota mensual, día de vencimiento, fecha de inicio y número de cuotas.
5. Guardar.
6. Entrar al botón de cronograma.
7. Generar 6 o 12 cuotas.
8. Confirmar que se crean filas con tipo `Cuota`.

## Prueba 2: prestamista con interés mensual

1. Crear deuda tipo `Prestamista`.
2. Completar saldo actual e interés mensual, por ejemplo 10 o 15.
3. Seleccionar modo `Prestamista con interés mensual`.
4. Entrar al cronograma.
5. Generar 6 meses.
6. Confirmar que cada fila genera el monto de interés mensual.

## Prueba 3: pago único

1. Crear deuda con modo `Pago único`.
2. Registrar saldo actual o cuota mensual y fecha de inicio.
3. Generar cronograma.
4. Confirmar que se crea una sola fila.

## Prueba 4: cargar obligaciones del mes

1. Ir a `/gasto-claro/monthly-plan`.
2. Seleccionar el mes donde existen filas de cronograma.
3. Presionar `Generar obligaciones del mes`.
4. Confirmar que aparecen en `Deudas x pagar`.
5. Repetir la acción y confirmar que no duplica obligaciones ya generadas.

## Resultado esperado

El plan mensual ya puede recibir obligaciones desde cronogramas y separarlas del registro simple de deudas.
