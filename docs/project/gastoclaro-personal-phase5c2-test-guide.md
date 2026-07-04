# GastoClaro Personal — Fase 5C.2

## Objetivo

Manejar deudas que ya no siguen un cronograma vigente, por ejemplo préstamos o tarjetas que se dejaron de pagar y están en mora, cobranza o negociación.

Estas deudas deben:

- conservar su saldo pendiente total;
- mostrar fecha de último pago e inicio de mora;
- calcular días aproximados de atraso;
- registrar situación de cobranza y negociación;
- aparecer en una sección separada del plan mensual;
- no aumentar el pago exigible ni el déficit del mes;
- permitir crear un abono voluntario cuando el usuario decida pagar algo.

## Rutas

- `/gasto-claro/debts`
- `/gasto-claro/monthly-plan`
- `/gasto-claro/debts/{id}/voluntary-payment`

## Prueba de deuda en mora

1. Abrir `/gasto-claro/debts`.
2. Crear o editar una deuda con:
   - Estado: `Dejé de pagar`, `En cobranza`, `Pendiente de negociación` o `En negociación`.
   - Modo: `Solo seguimiento / mora`.
   - Saldo actual: cualquier monto de prueba.
   - Cuota anterior referencial: monto histórico opcional.
   - Fecha del último pago.
   - Inicio de mora.
   - Estado de cobranza.
   - Estado de negociación.
   - Próxima revisión.
3. Guardar y volver a editar.
4. Confirmar que los campos persisten.
5. Abrir `/gasto-claro/monthly-plan`.
6. Confirmar que la deuda aparece en **Deudas en mora / seguimiento**.
7. Confirmar que el saldo en seguimiento no cambia `Deudas exigibles`, `Pendiente` ni `Saldo proyectado`.

## Prueba de abono voluntario

1. Desde la deuda en seguimiento, presionar el icono de moneda.
2. Registrar monto, moneda, fecha y observación.
3. Guardar.
4. Confirmar que se abre el plan del mes correspondiente.
5. Confirmar que el abono sí aparece en **Deudas x pagar** y sí forma parte del total exigible del mes.

## Cambio de deuda activa a seguimiento

Al guardar una deuda como seguimiento de mora:

- se desactiva la generación mensual automática;
- no se crean nuevas cuotas ni intereses automáticos;
- obligaciones generadas no pagadas se cancelan para no distorsionar el plan;
- obligaciones pagadas o parciales se conservan como evidencia.

## Datos personales

Los nombres, bancos y montos reales deben ingresarse desde la interfaz en la base local. No deben agregarse a código, scripts ni seeds versionados.
