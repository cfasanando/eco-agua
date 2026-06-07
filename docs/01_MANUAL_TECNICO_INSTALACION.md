# Manual técnico de instalación V1

## Requisitos

- Java 17.
- Maven.
- MySQL 8 o MariaDB compatible.
- Git Bash en Windows, o terminal equivalente en Linux.
- Una base de datos por cliente.

## Estructura recomendada multi-cliente simple

La estrategia de V1 es simple:

```text
Mismo código / mismo JAR
Agua Eco               -> BD eco_agua_dev o eco_agua
Productos Selva Belén  -> BD productos_belen
Cliente nuevo          -> BD propia del cliente
```

Cada instancia se levanta con su propia conexión y puerto.

## `.properties` debe ser solo técnico

El archivo `.properties` debe contener conexión, puerto, perfil técnico y parámetros de infraestructura.

Ejemplo Agua Eco:

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/eco_agua_dev?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.thymeleaf.cache=false
```

Ejemplo Belén:

```properties
server.port=8082
spring.datasource.url=jdbc:mysql://localhost:3306/productos_belen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
spring.thymeleaf.cache=false
```

Los nombres del negocio, WhatsApp, etiquetas, textos visibles y módulos activos deben venir desde base de datos, no desde archivos.

## Variables externas

Las claves externas como Google Maps no deben quedar fijas en el repositorio. En ambiente real deben pasarse por variables de entorno o configuración segura.

## Compilar

```bash
mvn -DskipTests compile
```

## Ejecutar en desarrollo

```bash
./scripts/run-dev.sh
```

Esperar hasta ver:

```text
Tomcat started on port 8081
```

## Probar conexión básica

Usar `127.0.0.1` en Windows/Git Bash si `localhost` falla:

```bash
curl -s -o home.html -w "HTTP=%{http_code} SIZE=%{size_download}\n" http://127.0.0.1:8081/
```

Debe responder `HTTP=200` o redirigir correctamente según sesión.

## Login

Abrir:

```text
http://127.0.0.1:8081/login
```

Luego probar:

```text
http://127.0.0.1:8081/home
http://127.0.0.1:8081/dashboard/business
```
