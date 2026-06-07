# Roles y permisos V1

## Roles base recomendados

- Dueño / Administrador general.
- Gerencia.
- Ventas.
- Logística / almacén.
- Producción.
- Marketing.
- Finanzas.
- Contabilidad.
- RRHH.
- Solo lectura / auditor.

## Matriz práctica V1

| Área | Puede ver | Puede modificar |
|---|---|---|
| Dashboard general | Todos los roles internos | No aplica |
| Dashboard gerencial | Dueño, Gerencia, Finanzas, Solo lectura | No aplica |
| Ventas | Dueño, Gerencia, Ventas, Finanzas, Logística, Solo lectura | Dueño, Ventas |
| Cobranzas | Ventas, Finanzas, Gerencia | Ventas, Finanzas |
| Productos | Ventas, Logística, Producción, Gerencia, Solo lectura | Logística, Dueño |
| Rentabilidad | Dueño, Gerencia, Finanzas, Solo lectura | No aplica |
| Inventario | Logística, Producción, Gerencia, Solo lectura | Logística, Producción |
| Producción | Producción, Gerencia, Solo lectura | Producción, Dueño |
| Marketing | Marketing, Gerencia, Solo lectura | Marketing, Dueño |
| RRHH | RRHH, Gerencia, Solo lectura | RRHH, Dueño |
| Finanzas | Dueño, Gerencia, Finanzas, Solo lectura | Finanzas, Dueño |
| Contabilidad interna | Dueño, Gerencia, Finanzas, Solo lectura | Finanzas/Contabilidad, Dueño |
| Usuarios | Dueño/Admin | Dueño/Admin |
| Módulos/settings | Dueño/Admin | Dueño/Admin |

## Validaciones mínimas

- Un usuario operativo no debe ver `/dashboard/business` si no corresponde.
- Un usuario solo lectura no debe poder hacer `POST` en `/accounting/**`.
- Un usuario de ventas no debe modificar contabilidad.
- Un usuario de marketing no debe modificar stock ni caja.
- Un usuario de almacén no debe modificar settings globales.

## Pendiente post V1

Separar de forma más granular:

- Permisos por acción.
- Permisos por botón.
- Auditoría de cambios críticos.
- Panel para administrar permisos sin tocar código.
