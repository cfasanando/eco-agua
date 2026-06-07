# Comandos útiles V1

## Ver estado Git

```bash
git status
```

## Compilar

```bash
mvn -DskipTests compile
```

## Ejecutar local

```bash
./scripts/run-dev.sh
```

## Probar home con curl

```bash
curl -s -o home.html -w "HTTP=%{http_code} SIZE=%{size_download}\n" http://127.0.0.1:8081/
```

## Probar dashboard gerencial

```bash
curl -i http://127.0.0.1:8081/dashboard/business
```

Si no hay sesión, puede devolver `302` hacia login. Eso es normal.

## Buscar textos rotos en base de datos

```sql
SELECT variable, value
FROM platform_setting
WHERE value LIKE '%Ã%'
   OR value LIKE '%Â%'
   OR value LIKE '%�%';
```

## Validar módulos

```sql
SELECT variable, value
FROM platform_setting
WHERE variable LIKE 'module.%'
ORDER BY variable;
```

## No commitear contextos

```bash
rm -rf eco-agua-*-context-*
```
