# GastoClaro Personal — Fase 5C.1

## Objetivo

Convertir GastoClaro en un flujo mensual recurrente antes de cargar datos reales completos.

La regla principal queda así:

- Fuentes de ingreso = plantillas recurrentes.
- Ingresos del mes = ocurrencias generadas.
- Gastos fijos = plantillas recurrentes.
- Obligaciones del mes = ocurrencias generadas.
- Deudas = deuda principal.
- Cronograma = cuotas, intereses, pagos únicos o descuentos automáticos.
- Pagos = pagos reales, en una fase posterior.

## Rutas principales

- `/gasto-claro/income-sources`
- `/gasto-claro/fixed-expenses`
- `/gasto-claro/debts`
- `/gasto-claro/monthly-plan`

## Prueba rápida

1. Iniciar una instancia cliente.
2. Entrar con un usuario normal, por ejemplo `admin_demo`.
3. Crear una fuente recurrente en `/gasto-claro/income-sources`:
   - Nombre: `Beeznest Latino`
   - Monto mensual: `2500.00`
   - Día esperado: `30`
   - Generar cada mes: activo
4. Crear gastos recurrentes en `/gasto-claro/fixed-expenses`:
   - Alquiler `500.00`, día 1
   - Luz `250.00`, día 10
   - Agua `280.00`, día 10
   - Internet `150.00`, día 5
5. Crear una deuda con cronograma o pago mensual simple.
6. Abrir `/gasto-claro/monthly-plan?year=2026&month=7`.
7. Presionar **Generar / actualizar mes**.

## Resultado esperado

El sistema debe crear sin duplicar:

- ingresos del mes desde fuentes recurrentes;
- obligaciones de costo de vida desde gastos fijos;
- líneas de cronograma faltantes para deudas con auto-generación activa;
- obligaciones del mes desde cronogramas;
- obligaciones mensuales simples para deudas sin cronograma.

Si se presiona el botón por segunda vez, no debe duplicar registros del mismo mes.

## Consideraciones

No cargar datos personales en seeds versionados. Los datos reales deben ingresarse desde la interfaz y quedarse en la base local del usuario.
