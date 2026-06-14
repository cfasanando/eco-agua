# Fase Academia 3 - Inscripciones y avance

## Objetivo

Esta fase convierte el catálogo de cursos en un LMS básico: permite inscribir usuarios, mostrar sus cursos, abrir lecciones y marcar avance.

## Rutas nuevas

- `/admin/academy/enrollments`: administración de inscripciones.
- `/my-courses`: cursos inscritos del usuario autenticado.
- `/my-courses/{courseSlug}`: temario del curso inscrito.
- `/my-courses/{courseSlug}/lesson/{lessonId}`: visor interno de lección.

## Prueba como administrador

1. Entrar con `admin_demo` o `mkt_demo`.
2. Abrir `/admin/academy/enrollments`.
3. Seleccionar un usuario activo.
4. Seleccionar un curso publicado.
5. Guardar inscripción.
6. Confirmar que aparece en la tabla de alumnos inscritos.
7. Cambiar el estado a `En progreso`, `Completado` o `Cancelado`.

## Prueba como alumno

1. Entrar con un usuario inscrito, por ejemplo `oper_demo` si cargaste el SQL demo.
2. Abrir `/my-courses`.
3. Entrar a un curso.
4. Abrir una lección.
5. Marcar la lección como completada.
6. Volver al temario y validar que sube el porcentaje.

## Prueba de seguridad

- Un usuario no inscrito que entra a `/my-courses/{slug}` debe volver a la ficha pública del curso.
- Las lecciones internas completas solo deben abrirse desde `/my-courses` si existe inscripción activa.
- Las clases gratuitas siguen disponibles desde `/academy/course/{slug}/learn`.

## Datos demo incluidos

El SQL `manual_sql/academy-enrollment-phase3.sql` intenta crear inscripciones demo para:

- `oper_demo` en el curso de Eco Agua.
- `mkt_demo` en el curso de Productos de la Selva Belén.
- `admin_demo` en ambos cursos.

Si esos usuarios o cursos no existen, simplemente no se insertan esas filas.
