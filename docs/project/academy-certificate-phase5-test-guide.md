# Fase Academia 5 - Certificados y verificación pública

## Objetivo

Agregar emisión de certificados para alumnos que completan un curso y aprueban las evaluaciones publicadas.

## Rutas principales

- `/admin/academy/certificates`: panel administrativo de certificados.
- `/my-courses/{courseSlug}/certificate`: certificado del alumno.
- `/academy/certificate/verify/{code}`: verificación pública por código.

## Reglas de emisión

Un certificado puede emitirse cuando:

1. La inscripción está activa.
2. El avance del curso es 100%.
3. Todas las evaluaciones publicadas del curso están aprobadas.

Si un curso no tiene evaluaciones publicadas, se valida solo el avance completo.

## Prueba rápida

1. Ejecutar `manual_sql/academy-certificate-phase5.sql`.
2. Entrar con `admin_demo`.
3. Abrir `/admin/academy/certificates`.
4. Revisar certificados emitidos y candidatos.
5. Entrar con `oper_demo`.
6. Abrir `/my-courses/eco-agua-operacion-reparto-cobranza`.
7. Revisar la sección Certificado.
8. Abrir `/my-courses/eco-agua-operacion-reparto-cobranza/certificate`.
9. Probar el botón imprimir.
10. Abrir `/academy/certificate/verify/ACA-DEMO-ECO-AGUA-001` sin sesión.

## Validación esperada

- El certificado debe verse con diseño imprimible.
- La URL pública debe indicar si el certificado es válido.
- El panel admin debe permitir anular certificados activos.
- Los certificados anulados deben aparecer como no válidos en la verificación pública.
