# API de banca virtual con JWT

API educativa construida con Java 21, Spring Boot, PostgreSQL, Liquibase y Spring Security. Conserva las operaciones de usuarios, cuentas, depósitos, retiros, transferencias e historial, y las protege con JWT RS256, roles, permisos y propiedad del recurso.

> La aplicación simula dinero virtual. Las claves RSA incluidas son **solo para desarrollo** y no deben usarse en producción.

## Requisitos

- Java 21
- PostgreSQL 15 o superior
- Docker compatible con Testcontainers para las pruebas de integración

## Configuración

Toda la configuración está en `src/main/resources/application.properties`. Ajuste como mínimo:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/banking
spring.datasource.username=postgres
spring.datasource.password=admin
```

Las propiedades `security.jwt.*` controlan issuer, audience, vigencias, tolerancia de reloj y ubicaciones de las claves. `security.password.bcrypt-strength` controla el costo BCrypt. En producción, inyecte la clave privada desde un gestor de secretos y configure TLS.

Liquibase es la única fuente del esquema. Al iniciar aplica `001`, `002` y `003-security.sql`; Hibernate se limita a validar con `ddl-auto=validate`. No se crea un administrador ni una contraseña predeterminada.

## Ejecución

```bash
./mvnw spring-boot:run
```

Swagger UI queda disponible en `http://localhost:8080/swagger-ui.html`. Ejecute login, copie `accessToken`, pulse **Authorize** y escriba el token Bearer.

## Flujo de autenticación

Registrar un cliente (la contraseña debe tener entre 12 y 128 caracteres, mayúscula, minúscula y número):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","password":"SecurePassword1"}'
```

Iniciar sesión:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"SecurePassword1"}'
```

La respuesta contiene un access token JWT de 15 minutos y un refresh token opaco de 7 días. Envíe el JWT con `Authorization: Bearer <accessToken>`. Use `/api/v1/auth/refresh` para rotar el refresh token, `/logout` para revocar su familia, `/me` para consultar la identidad y `PUT /password` para cambiar la contraseña y revocar sesiones.

Los refresh tokens se guardan exclusivamente como SHA-256. Reutilizar uno ya rotado revoca toda su familia. Cada solicitud JWT comprueba que el usuario siga activo y que `authVersion` continúe vigente.

## Autorización

- `CUSTOMER`: opera únicamente sus recursos.
- `SUPPORT`: consulta recursos y congela/descongela cuentas.
- `ADMIN`: administración completa y asignación de roles.

Las decisiones usan permisos (`account:read:self`, `role:assign:any`, etc.) y consultas de propiedad en PostgreSQL para evitar IDOR. La cuenta destino de una transferencia puede ser ajena; la cuenta origen debe pertenecer al cliente.

Los endpoints administrativos están bajo `/api/v1/security`. Como no existe administrador inicial, el primer rol administrativo debe aprovisionarse mediante un procedimiento operativo seguro en base de datos; nunca mediante una credencial conocida incluida en el repositorio.

## Pruebas

```bash
./mvnw clean verify
```

Las pruebas usan PostgreSQL real mediante Testcontainers e incluyen el ciclo registro → login → refresh, reutilización de refresh token, respuestas 401/403 y la regresión financiera existente.

Las colecciones están en `api-clients/postman` y `api-clients/bruno`. No guarde tokens ni contraseñas reales en esos archivos; use variables locales de entorno.

## Errores

Los errores mantienen un contrato uniforme con `timestamp`, `status`, `code`, `message`, `path`, `requestId` y `fieldErrors`. Las respuestas de seguridad no exponen detalles criptográficos, SQL, contraseñas ni tokens.
