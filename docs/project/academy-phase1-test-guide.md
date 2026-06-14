# Fase Academia 1 - Guía rápida de prueba

## Objetivo
Validar el primer módulo de Academia: administración de cursos, catálogo público y detalle de curso con consulta por WhatsApp.

## Rutas

- Administración: `/admin/academy/courses`
- Catálogo público: `/academy`
- Detalle público: `/academy/course/{slug}`

## Usuarios sugeridos

- `admin_demo`: debe poder crear, editar, publicar, destacar y archivar cursos.
- `mkt_demo`: debe poder administrar cursos como parte de marketing/contenido.
- `gerencia_demo`: debe poder revisar el módulo.
- `readonly_demo`: debe tener vista de consulta si se le asigna `ver_academia`, sin acciones POST.

## Flujo de prueba

1. Ejecutar `manual_sql/academy-phase1.sql`.
2. Entrar con `admin_demo`.
3. Abrir `/admin/academy/courses`.
4. Crear un curso nuevo en borrador.
5. Publicarlo desde la tabla.
6. Marcarlo como destacado.
7. Abrir `/academy` en una pestaña pública.
8. Confirmar que el curso publicado aparece en el catálogo.
9. Abrir el detalle del curso.
10. Probar el botón de WhatsApp.
11. Archivar un curso y confirmar que desaparece del catálogo público.

## Alcance actual

Esta fase no incluye todavía lecciones, alumnos, progreso, evaluaciones ni certificados. Es solo la base comercial del módulo.

## Próximas fases sugeridas

- Fase 2: unidades y lecciones del curso.
- Fase 3: alumnos e inscripciones.
- Fase 4: avance del alumno.
- Fase 5: evaluaciones simples.
- Fase 6: certificados con código de validación.
