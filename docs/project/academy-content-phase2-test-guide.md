# Guía de prueba - Academia Fase 2

## Objetivo

Validar que el módulo Academia permite organizar un curso por unidades y lecciones, publicar temario y mostrar una clase gratuita en el portal público.

## Usuarios recomendados

- `admin_demo`: administración completa.
- `mkt_demo`: administración de contenido y cursos.
- `gerencia_demo`: revisión general.

## Flujo rápido

1. Entrar con `admin_demo`.
2. Abrir `/admin/academy/courses`.
3. Elegir un curso y hacer clic en el icono de contenido.
4. Crear una unidad.
5. Crear una lección dentro de esa unidad.
6. Marcar la lección como `Publicado`.
7. Marcar una lección como `Vista previa gratuita`.
8. Abrir `/academy`.
9. Entrar al detalle del curso.
10. Verificar que aparece el temario.
11. Abrir la clase gratuita desde `/academy/course/{slug}/learn`.

## Pruebas principales

| Prueba | Resultado esperado |
|---|---|
| Crear unidad | La unidad aparece en el temario admin. |
| Crear lección | La lección queda dentro de la unidad seleccionada. |
| Publicar lección | La lección aparece en el detalle público del curso. |
| Marcar preview | La lección aparece como gratuita y abre en la vista de clase. |
| Archivar lección | Ya no aparece en el temario público. |
| Curso sin temario | Muestra mensaje claro de temario pendiente. |

## Rutas nuevas

- `/admin/academy/courses/{courseId}/content`
- `/academy/course/{slug}/learn`

## Notas

- Los videos se guardan por URL, no como archivo pesado en el servidor.
- Para embeber video directamente, usar una URL tipo `https://www.youtube.com/embed/...`.
- Si se usa YouTube normal, Drive, Vimeo u otro enlace, se mostrará como botón externo.
