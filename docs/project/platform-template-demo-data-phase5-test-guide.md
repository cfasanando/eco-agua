# Fase Plataforma 5 - Datos demo automáticos por plantilla

## Objetivo

Agregar al aprovisionamiento un paso para cargar datos demo según el rubro del negocio.

## Flujo esperado

1. Crear base de datos.
2. Copiar estructura automáticamente.
3. Aplicar configuración inicial.
4. Cargar datos demo de plantilla.
5. Activar negocio.
6. Generar archivos runtime.

## Plantillas cubiertas inicialmente

- Tienda tipo Temu / e-commerce.
- Restaurante.
- Academia.
- Courier / RutaPack.
- Productos regionales / catálogo.
- Agua de mesa.

## Prueba recomendada

Entrar como `admin_demo`:

```text
/admin/platform/clients/{id}/provisioning
```

Para un negocio con `Demo = Sí`, después del bootstrap debe habilitarse el botón:

```text
4. Cargar datos demo de plantilla
```

Luego validar en la base destino:

```bash
mysql -u root -p tienda_china_express -e "SELECT COUNT(*) FROM product; SELECT COUNT(*) FROM client; SELECT COUNT(*) FROM marketing_content_idea;"
```

Después activar y regenerar runtime:

```bash
bash scripts/run-client.sh demo_tienda_china_temu 8083
```

Abrir:

```text
http://localhost:8083
```

## Resultado esperado

El cliente debe mostrar productos, clientes y datos operativos acordes con su plantilla, evitando que una tienda/restaurante nuevo parezca Agua Eco.
