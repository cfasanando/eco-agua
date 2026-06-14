# Academia - Cierre técnico y prueba final

## Objetivo

Validar que el módulo Academia quede usable, seguro y presentable después de las fases de cursos, contenidos, inscripciones, evaluaciones, certificados, leads y dashboard.

## Usuarios de prueba

- `admin_demo`: administración completa de Academia.
- `mkt_demo`: administración comercial y de contenidos.
- `oper_demo`: alumno inscrito / vista de aprendizaje.
- `readonly_demo`: modo consulta, sin acciones de escritura.

## Rutas públicas

- `/academy`
- `/academy/course/eco-agua-operacion-reparto-cobranza`
- `/academy/course/eco-agua-operacion-reparto-cobranza/request`
- `/academy/certificate/verify/ACA-DEMO-ECO-AGUA-001`

## Rutas administrativas

- `/admin/academy/dashboard`
- `/admin/academy/courses`
- `/admin/academy/enrollments`
- `/admin/academy/leads`
- `/admin/academy/assessment-results`
- `/admin/academy/certificates`

## Checklist de cierre

1. Entrar como `admin_demo` y revisar todas las rutas administrativas.
2. Crear o editar un curso de prueba.
3. Revisar unidades/lecciones de un curso.
4. Revisar inscripciones y avance.
5. Revisar resultados de evaluaciones.
6. Revisar certificados emitidos y verificación pública.
7. Crear un lead desde el formulario público y verlo en el panel de interesados.
8. Entrar como `oper_demo`, abrir `Mis cursos`, completar una lección y revisar evaluación/certificado.
9. Entrar como `readonly_demo` y confirmar que puede consultar pero no ve formularios POST ni botones de edición.
10. Confirmar que el sidebar muestra Academia de forma coherente según el rol.

## Criterio de aceptación

El módulo queda cerrado cuando los usuarios de administración pueden gestionar Academia, los alumnos pueden estudiar y avanzar, los visitantes pueden solicitar inscripción y el usuario solo lectura no puede ejecutar acciones de escritura.
