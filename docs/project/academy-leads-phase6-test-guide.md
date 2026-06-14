# Fase Academia 6 - Solicitudes e interesados

## Objetivo

Registrar solicitudes públicas de cursos, gestionarlas desde administración y convertir interesados en alumnos inscritos.

## Rutas principales

- `/academy/course/{slug}/request`
- `/admin/academy/leads`
- `/admin/academy/leads/{id}`

## Prueba pública

1. Abrir `/academy` sin iniciar sesión.
2. Entrar a un curso publicado.
3. Clic en **Solicitar inscripción**.
4. Registrar nombre, teléfono, correo, origen y mensaje.
5. Confirmar que aparece el mensaje de solicitud enviada.

## Prueba administrativa

1. Entrar con `admin_demo` o `mkt_demo`.
2. Abrir `/admin/academy/leads`.
3. Revisar KPIs: total, nuevos, contactados, inscritos y descartados.
4. Abrir el detalle de un interesado.
5. Cambiar estado a `Contactado`.
6. Guardar notas comerciales.
7. Abrir WhatsApp si el interesado tiene teléfono.
8. Convertir el lead seleccionando un usuario y curso.
9. Confirmar que aparece en `/admin/academy/enrollments`.

## Observaciones

- La conversión usa usuarios existentes del sistema.
- Si se necesita crear alumnos externos con clave propia, hacerlo en una fase posterior.
- Los leads públicos no requieren login.
