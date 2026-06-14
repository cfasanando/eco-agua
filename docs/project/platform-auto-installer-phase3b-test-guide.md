# Fase Plataforma 3B - Asistente automático de instalación

## Objetivo

Permitir que el Super Admin complete la instalación de un negocio desde la plataforma, sin tener que copiar manualmente todos los comandos.

## Ruta principal

```text
/admin/platform/clients/{id}/provisioning
```

## Flujo recomendado

1. Crear o abrir un negocio desde `/admin/platform/clients`.
2. Entrar a **Aprovisionamiento**.
3. Ejecutar los pasos en orden:
   - Crear base de datos.
   - Copiar estructura automáticamente.
   - Aplicar configuración inicial.
   - Activar negocio.
   - Generar archivos runtime.
4. Revisar el historial de aprovisionamiento.
5. Ejecutar el negocio con:

```bash
bash scripts/run-client.sh tienda_china_express 8083
```

También se puede ejecutar directamente:

```bash
bash runtime-clients/tienda_china_express/run.sh
```

## Resultado esperado

La carpeta generada debe quedar así:

```text
runtime-clients/
└── tienda_china_express/
    ├── application.properties
    ├── run.sh
    ├── create-database-tienda_china_express.sql
    ├── bootstrap-tienda_china_express.sql
    └── README.txt
```

## Notas técnicas

- La copia automática usa `SHOW CREATE TABLE` sobre la base modelo y crea tablas en la base destino.
- Los SQL manuales siguen disponibles como respaldo.
- Los archivos runtime se generan fuera de `src/main/resources` para no tocar código fuente.
- `runtime-clients/` debe quedar ignorado por Git porque contiene configuraciones locales por cliente.
