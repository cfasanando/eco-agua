# Fase Academia 7 - Panel de control y cierre técnico

## Objetivo

Centralizar en una sola pantalla los principales indicadores del módulo Academia: cursos, alumnos, avance, evaluaciones, certificados e interesados.

## Rutas principales

- `/admin/academy/dashboard`: panel de control de Academia.
- `/admin/academy/courses`: administración de cursos.
- `/admin/academy/enrollments`: inscripciones y avance.
- `/admin/academy/leads`: interesados y solicitudes.
- `/admin/academy/assessment-results`: resultados de evaluaciones.
- `/admin/academy/certificates`: certificados emitidos.
- `/academy`: catálogo público.
- `/my-courses`: cursos del alumno.

## Prueba rápida

1. Entrar como `admin_demo`.
2. Abrir `/admin/academy/dashboard`.
3. Validar que se muestran KPIs de cursos, inscritos, interesados y certificados.
4. Revisar la tabla de cursos con movimiento.
5. Revisar actividad reciente.
6. Usar accesos rápidos hacia Cursos, Inscripciones, Interesados, Evaluaciones y Certificados.
7. Entrar como `mkt_demo` y repetir la validación.
8. Entrar como `oper_demo` y confirmar que no puede administrar el panel, pero sí puede usar `/my-courses`.

## Resultado esperado

- El panel carga dentro del layout normal con sidebar y topbar.
- Los KPIs reflejan la información ya registrada en Academia.
- No se crean tablas nuevas ni se modifica la base de datos.
- Los accesos rápidos redirigen correctamente.

## Observación

Esta fase cierra el módulo Academia como versión presentable. Las siguientes mejoras deberían enfocarse en diseño comercial, landing de cursos o integración con pagos solo cuando haga falta.
