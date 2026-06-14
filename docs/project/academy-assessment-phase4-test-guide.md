# Academia - Fase 4: evaluaciones simples

## Objetivo

Agregar evaluaciones básicas al módulo Academia para validar aprendizajes del alumno.

## Rutas nuevas

- `/admin/academy/courses/{courseId}/assessments`
- `/admin/academy/assessment-results`
- `/my-courses/{courseSlug}/assessment`
- `/my-courses/{courseSlug}/assessment/{assessmentId}`

## Prueba admin

1. Entrar con `admin_demo` o `mkt_demo`.
2. Abrir `/admin/academy/courses`.
3. Entrar al botón de evaluaciones de un curso.
4. Crear una evaluación en borrador.
5. Crear preguntas de opción múltiple o verdadero/falso.
6. Marcar la opción correcta usando `*` al inicio de la línea.
7. Publicar la evaluación.
8. Abrir `/admin/academy/assessment-results` para revisar intentos.

## Prueba alumno

1. Entrar con `oper_demo`.
2. Abrir `/my-courses`.
3. Entrar al curso inscrito.
4. Abrir la sección Evaluaciones.
5. Rendir una evaluación.
6. Confirmar que muestra puntaje, porcentaje y aprobado/desaprobado.
7. Volver a intentar hasta el máximo configurado.

## Datos demo

El SQL `manual_sql/academy-assessment-phase4.sql` crea evaluaciones para:

- Eco Agua: operación, reparto y cobranza.
- Productos de la Selva Belén: catálogo, promoción y WhatsApp.

## Validaciones esperadas

- El alumno solo puede rendir evaluaciones de cursos donde está inscrito.
- El alumno no puede superar el número máximo de intentos.
- Las respuestas correctas suman puntos.
- El porcentaje se calcula como `score / max_score * 100`.
- Admin puede ver resultados de todos los intentos.
