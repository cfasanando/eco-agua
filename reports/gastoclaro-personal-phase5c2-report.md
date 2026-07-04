# GastoClaro Personal — Fase 5C.2 Report

## Resumen

Se agregó soporte específico para deudas en mora, cobranza y negociación sin cronograma vigente.

## Modelo

`personal_finance_debt` incorpora:

- `previous_monthly_payment`
- `last_payment_date`
- `delinquency_start_date`
- `collection_status`
- `negotiation_status`
- `next_review_date`

También se amplían los estados de deuda con cobranza, pendiente de negociación y reprogramada.

## Plan mensual

Se incorpora la sección **Deudas en mora / seguimiento** con:

- saldo total separado por PEN y USD;
- días aproximados de atraso;
- estado de cobranza;
- estado de negociación;
- fecha de último pago;
- fecha de próxima revisión.

Estos saldos son informativos y no se suman a las obligaciones exigibles del mes.

## Abonos voluntarios

Se agrega un flujo para crear una obligación puntual desde una deuda en seguimiento. Solo ese abono decidido por el usuario se incorpora al plan mensual.

## Protección de proyección

Cuando una deuda cambia a seguimiento de mora:

- se fuerza `TRACKING_ONLY`;
- se desactiva la generación automática;
- se cancelan obligaciones automáticas no pagadas asociadas a esa deuda;
- se preservan obligaciones pagadas o parciales.

## Seguridad y privacidad

No se incluyen nombres, acreedores ni montos personales reales en el código o en seeds.
