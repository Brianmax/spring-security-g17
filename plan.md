# Plan de implementación de JWT, roles y permisos

## 1. Objetivo

Agregar autenticación y autorización al API de banca virtual existente usando Spring Security, JWT, roles y permisos, sin modificar las reglas financieras ya implementadas.

La solución debe:

- Mantener la arquitectura **Controller → Service → Repository**.
- Proteger los endpoints actuales con JWT Bearer.
- Aplicar autorización por roles, permisos y propiedad del recurso.
- Usar PostgreSQL y Liquibase como única fuente de verdad del esquema.
- Mantener toda la configuración de la aplicación en `application.properties`.
- Conservar las pruebas de depósitos, retiros, transferencias, conversión, idempotencia y concurrencia.
- Actualizar Swagger/OpenAPI, Postman, Bruno y el README en español.

## 2. Estado actual del proyecto

Repositorio analizado: `/Users/george/jwt-project/codigo`.

La aplicación usa:

- Java 21 y Spring Boot 3.5.
- Spring Web, Spring Data JPA y Bean Validation.
- PostgreSQL y Liquibase.
- Testcontainers para pruebas de integración.
- OpenAPI/Swagger.
- Paquetes separados para `controller`, `service`, `repository`, `entity`, `dto`, `mapper` y `exception`.
- Bloqueos de base de datos y transacciones de servicio para proteger saldos.
- Integración con Decolecta para transferencias entre monedas diferentes.

Actualmente no existen Spring Security, credenciales, roles, permisos ni endpoints de autenticación. Todos los endpoints bancarios son públicos.

## 3. Decisiones de diseño

### 3.1 Access token

- Formato: JWT firmado con RSA SHA-256 (`RS256`).
- Duración recomendada: 15 minutos.
- Transporte: encabezado `Authorization: Bearer <token>`.
- Validaciones: firma, algoritmo, `issuer`, `audience`, emisión, inicio de validez y expiración.
- El token no contendrá contraseñas, hashes, saldos ni datos sensibles.

### 3.2 Refresh token

- Será una cadena aleatoria opaca de al menos 256 bits; no será otro JWT.
- Duración recomendada: 7 días.
- PostgreSQL almacenará únicamente `SHA-256(token)`.
- Cada renovación rotará el refresh token.
- La reutilización de un token ya rotado revocará toda su familia.
- La lectura y rotación se ejecutarán en una transacción con bloqueo pesimista para que dos renovaciones concurrentes no puedan tener éxito.

### 3.3 Contraseñas

- Se almacenarán con `BCryptPasswordEncoder`.
- Nunca se devolverán en DTO, JWT, logs o errores.
- Se aplicarán límites de longitud antes de ejecutar BCrypt.
- Cambiar la contraseña incrementará `authVersion` y revocará todos los refresh tokens activos.

### 3.4 Revocación y estado del usuario

Cada JWT incluirá una versión de autenticación (`ver`). En cada solicitud protegida se comprobará que:

1. El usuario continúa existiendo.
2. Su estado es `ACTIVE`.
3. La versión del JWT coincide con `auth_credentials.auth_version`.

Este diseño hace una consulta de seguridad por solicitud, pero permite invalidar inmediatamente los tokens cuando se desactiva un usuario, cambia su contraseña o se modifican sus privilegios. Para esta aplicación educativa se prioriza claridad y revocación inmediata sobre una autenticación completamente sin estado.

## 4. Arquitectura objetivo

```text
HTTP request
    │
    ▼
SecurityFilterChain
    │  valida Bearer JWT, estado y authVersion
    ▼
Controller
    │  valida DTO y declara permisos
    ▼
Service
    │  aplica reglas, propiedad y transacciones
    ▼
Repository
    │
    ▼
PostgreSQL
```

No se creará un filtro JWT artesanal. Spring Security OAuth2 Resource Server validará el Bearer token mediante `JwtDecoder`.

### 4.1 Paquetes y componentes nuevos

```text
com.jwt.codigo
├── config
│   ├── SecurityConfig
│   └── JwtProperties
├── controller
│   ├── AuthController
│   └── RoleController
├── dto
│   └── auth
│       ├── RegisterRequest
│       ├── LoginRequest
│       ├── RefreshTokenRequest
│       ├── LogoutRequest
│       ├── ChangePasswordRequest
│       ├── TokenResponse
│       └── CurrentUserResponse
├── entity
│   ├── UserCredentialEntity
│   ├── RoleEntity
│   ├── PermissionEntity
│   └── RefreshTokenEntity
├── repository
│   ├── UserCredentialRepository
│   ├── RoleRepository
│   ├── PermissionRepository
│   └── RefreshTokenRepository
├── security
│   ├── JwtTokenService
│   ├── JwtUserAuthenticationConverter
│   ├── BankingAuthorization
│   ├── RestAuthenticationEntryPoint
│   └── RestAccessDeniedHandler
└── service
    ├── AuthenticationService
    ├── RefreshTokenService
    └── RoleService
```

Los controladores solo manejarán HTTP; los servicios contendrán autenticación, rotación, revocación y asignación de roles; los repositorios manejarán la persistencia.

## 5. Dependencias Maven

Agregar al `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Las versiones quedarán administradas por el BOM de Spring Boot para evitar incompatibilidades.

## 6. Modelo de datos

Crear un nuevo changeset Liquibase, sin modificar `001-initial-schema.sql` ni `002-cross-currency-transfers.sql`.

### 6.1 `auth_credentials`

| Campo | Descripción |
|---|---|
| `user_id` | PK y FK a `app_users.id` |
| `password_hash` | Hash BCrypt, nunca contraseña plana |
| `auth_version` | Versión usada para revocar JWT |
| `password_changed_at` | Fecha del último cambio |
| `created_at` | Auditoría |
| `updated_at` | Auditoría |

### 6.2 `security_roles`

| Campo | Descripción |
|---|---|
| `id` | UUID PK |
| `code` | Código único: `CUSTOMER`, `SUPPORT`, `ADMIN` |
| `description` | Descripción legible |

### 6.3 `security_permissions`

| Campo | Descripción |
|---|---|
| `id` | UUID PK |
| `code` | Código único con formato `dominio:acción:alcance` |
| `description` | Descripción legible |

### 6.4 Tablas de asociación

- `user_roles(user_id, role_id)`: PK compuesta y FK a usuario y rol.
- `role_permissions(role_id, permission_id)`: PK compuesta y FK a rol y permiso.
- Índices adicionales por `role_id`, `permission_id` y `user_id`.

### 6.5 `refresh_tokens`

| Campo | Descripción |
|---|---|
| `id` | UUID PK |
| `user_id` | FK a `app_users` |
| `token_hash` | SHA-256 único |
| `family_id` | Agrupa todas las rotaciones de una sesión |
| `expires_at` | Expiración absoluta |
| `revoked_at` | Fecha de revocación |
| `replaced_by_id` | Token que reemplazó al actual |
| `created_at` | Auditoría |

Agregar índices para `user_id`, `family_id`, `expires_at` y `token_hash`.

### 6.6 Datos iniciales

Liquibase insertará roles, permisos y sus asociaciones de manera idempotente. No se insertará una contraseña administrativa predeterminada.

## 7. Roles y permisos

### 7.1 Roles iniciales

| Rol | Responsabilidad |
|---|---|
| `CUSTOMER` | Gestionar su perfil, sus cuentas y sus operaciones financieras |
| `SUPPORT` | Consultar usuarios/cuentas y congelar o descongelar cuentas |
| `ADMIN` | Administración completa, cierre de cuentas y asignación de roles |

### 7.2 Catálogo inicial de permisos

| Dominio | Permisos |
|---|---|
| Usuario | `user:create:any`, `user:read:self`, `user:read:any`, `user:update:self`, `user:update:any`, `user:deactivate:any` |
| Cuenta | `account:create:self`, `account:read:self`, `account:read:any`, `account:freeze:any`, `account:unfreeze:any`, `account:close:self`, `account:close:any` |
| Operación | `deposit:create:self`, `withdrawal:create:self` |
| Transferencia | `transfer:create:self`, `transfer:read:self`, `transfer:read:any` |
| Movimiento | `transaction:read:self`, `transaction:read:any` |
| Seguridad | `role:read:any`, `role:assign:any`, `permission:read:any` |

Los roles simplifican la administración; las decisiones HTTP usarán authorities derivadas de permisos, no comparaciones rígidas de nombres de rol.

## 8. Endpoints de autenticación

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público | Crea usuario, credenciales y rol `CUSTOMER`; devuelve 201 |
| `POST` | `/api/v1/auth/login` | Público | Valida credenciales y devuelve access/refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Público con refresh token | Rota el token y devuelve un nuevo par |
| `POST` | `/api/v1/auth/logout` | Autenticado | Revoca el refresh token presentado; devuelve 204 |
| `GET` | `/api/v1/auth/me` | Autenticado | Devuelve identidad, roles y permisos efectivos |
| `PUT` | `/api/v1/auth/password` | Autenticado | Cambia contraseña y revoca sesiones |

### 8.1 Administración de roles

| Método | Ruta | Permiso |
|---|---|---|
| `GET` | `/api/v1/security/roles` | `role:read:any` |
| `GET` | `/api/v1/security/permissions` | `permission:read:any` |
| `PUT` | `/api/v1/security/users/{userId}/roles` | `role:assign:any` |

La asignación de roles reemplazará el conjunto completo dentro de una transacción y aumentará `authVersion` para invalidar tokens anteriores.

## 9. Contenido del JWT

Ejemplo de claims:

```json
{
  "iss": "virtual-banking-api",
  "aud": ["virtual-banking-clients"],
  "sub": "UUID-del-usuario",
  "jti": "UUID-del-token",
  "iat": 1785771000,
  "nbf": 1785771000,
  "exp": 1785771900,
  "roles": ["CUSTOMER"],
  "permissions": [
    "account:read:self",
    "transfer:create:self"
  ],
  "ver": 1
}
```

La respuesta de login y refresh seguirá este contrato:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "token-opaco...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

## 10. Matriz de autorización para endpoints bancarios

| Endpoint | CUSTOMER | SUPPORT | ADMIN |
|---|---|---|---|
| `POST /api/v1/users` | No | No | Crear cualquier usuario |
| `GET /api/v1/users` | No | Consultar | Consultar |
| `GET /api/v1/users/{id}` | Solo su usuario | Cualquiera | Cualquiera |
| `PUT /api/v1/users/{id}` | Solo su usuario | No | Cualquiera |
| `DELETE /api/v1/users/{id}` | No | No | Desactivar/eliminar |
| `POST /api/v1/users/{id}/accounts` | Solo para sí mismo | No | Cualquier usuario |
| `GET /api/v1/users/{id}/accounts` | Solo propias | Cualquiera | Cualquiera |
| `GET /api/v1/accounts/{id}` | Solo propia | Cualquiera | Cualquiera |
| `PATCH .../freeze` | No | Cualquiera | Cualquiera |
| `PATCH .../unfreeze` | No | Cualquiera | Cualquiera |
| `PATCH .../close` | Solo propia | No | Cualquiera |
| `POST .../deposits` | Solo propia | No | Cualquiera |
| `POST .../withdrawals` | Solo propia | No | Cualquiera |
| `POST /api/v1/transfers` | La cuenta origen debe ser propia | No | Cualquiera |
| `GET /api/v1/transfers/{id}` | Solo si participa una cuenta propia | Consulta | Consulta |
| `GET .../transactions` | Solo cuenta propia | Consulta | Consulta |
| `GET /api/v1/transactions/{id}` | Solo movimiento propio | Consulta | Consulta |

La cuenta destino de una transferencia no necesita pertenecer al usuario autenticado. La validación de propiedad se aplica a la cuenta origen.

## 11. Protección de propiedad e IDOR

Un permiso con alcance `self` no será suficiente por sí solo. Antes de invocar la operación se comprobará la relación real en PostgreSQL.

Ejemplos:

```java
@PreAuthorize("hasAuthority('account:read:any') or " +
              "@bankingAuthorization.ownsAccount(authentication, #accountId)")

@PreAuthorize("hasAuthority('transfer:create:any') or " +
              "@bankingAuthorization.ownsAccount(authentication, #request.sourceAccountId())")
```

`BankingAuthorization` consultará repositorios mediante operaciones `exists` específicas y no cargará entidades completas cuando no sea necesario.

Cuando un recurso exista pero no pertenezca al usuario, se devolverá `404` en endpoints de lectura sensibles para reducir enumeración de identificadores. Las denegaciones de capacidades administrativas devolverán `403`.

## 12. Configuración de Spring Security

`SecurityConfig` deberá:

- Habilitar `@EnableMethodSecurity`.
- Usar sesiones `STATELESS`.
- Deshabilitar login por formulario y HTTP Basic.
- Deshabilitar CSRF porque el Bearer token se enviará en encabezado y no se usarán cookies en esta fase.
- Permitir únicamente `register`, `login`, `refresh`, Swagger y OpenAPI sin autenticación.
- Exigir autenticación para cualquier otro endpoint.
- Configurar OAuth2 Resource Server con `JwtDecoder`.
- Convertir los claims `permissions` a `GrantedAuthority` sin agregar prefijo.
- Usar respuestas JSON consistentes para 401 y 403.
- No registrar `Authorization`, contraseñas ni refresh tokens.

## 13. Configuración en `application.properties`

No se crearán archivos YAML, `.env` ni Docker Compose. Las propiedades nuevas serán:

```properties
security.jwt.issuer=virtual-banking-api
security.jwt.audience=virtual-banking-clients
security.jwt.access-token-ttl=15m
security.jwt.refresh-token-ttl=7d
security.jwt.public-key-location=classpath:keys/jwt-public.pem
security.jwt.private-key-location=classpath:keys/jwt-private.pem
security.jwt.clock-skew=30s

security.password.bcrypt-strength=12
```

Para el proyecto educativo pueden existir claves RSA de desarrollo claramente identificadas. No deben reutilizarse en producción. En un despliegue real, la clave privada debe inyectarse desde un gestor de secretos aunque la referencia continúe declarada en `application.properties`.

## 14. Errores HTTP

`RestAuthenticationEntryPoint` y `RestAccessDeniedHandler` reutilizarán el contrato actual:

```json
{
  "timestamp": "2026-08-03T15:30:00Z",
  "status": 401,
  "code": "INVALID_ACCESS_TOKEN",
  "message": "Authentication is required",
  "path": "/api/v1/accounts/UUID",
  "requestId": "..."
}
```

Códigos nuevos:

- `INVALID_CREDENTIALS` — 401.
- `INVALID_ACCESS_TOKEN` — 401.
- `ACCESS_TOKEN_EXPIRED` — 401.
- `INVALID_REFRESH_TOKEN` — 401.
- `REFRESH_TOKEN_EXPIRED` — 401.
- `REFRESH_TOKEN_REUSED` — 401.
- `ACCOUNT_DISABLED` — 403.
- `ACCESS_DENIED` — 403.
- `ROLE_NOT_FOUND` — 404.
- `ROLE_ASSIGNMENT_CONFLICT` — 409.
- `PASSWORD_POLICY_VIOLATION` — 422.

No se expondrán causas criptográficas, consultas SQL ni stack traces.

## 15. Etapas de implementación

Cada etapa termina con compilación y pruebas. No se avanza si la puerta de salida falla.

### Etapa 0 — Línea base

1. Ejecutar `./mvnw clean verify`.
2. Guardar el inventario de endpoints y pruebas existentes.
3. Confirmar que Liquibase aplica `001` y `002` sobre PostgreSQL limpio.

**Puerta de salida:** todas las pruebas actuales pasan sin cambios.

### Etapa 1 — Dependencias y configuración

1. Agregar dependencias Spring Security, Resource Server y Security Test.
2. Crear `JwtProperties` con validación al iniciar.
3. Agregar propiedades JWT y BCrypt.
4. Crear claves RSA exclusivas para desarrollo/pruebas.

**Puerta de salida:** la aplicación compila y el contexto carga.

### Etapa 2 — Migración y persistencia

1. Crear `003-security.sql`.
2. Crear entidades y repositorios de credenciales, roles, permisos y refresh tokens.
3. Insertar catálogo inicial de roles/permisos.
4. Probar restricciones únicas, FKs y consultas con bloqueo.

**Puerta de salida:** Liquibase actualiza una base vacía y una base con `001`/`002`; `ddl-auto=validate` pasa.

### Etapa 3 — Registro y contraseñas

1. Implementar `RegisterRequest` y política de contraseña.
2. Crear usuario, credencial y rol CUSTOMER en una sola transacción.
3. Implementar cambio de contraseña y revocación de sesiones.
4. Mantener `POST /users` como operación administrativa.

**Puerta de salida:** pruebas de registro, correo duplicado, contraseña inválida y rollback pasan.

### Etapa 4 — Emisión y validación JWT

1. Configurar `JwtEncoder` y `JwtDecoder` RSA.
2. Implementar claims, issuer, audience, expiración y clock skew.
3. Implementar `JwtUserAuthenticationConverter` para estado y `authVersion`.
4. Probar firma alterada, algoritmo incorrecto, expiración, issuer y audience.

**Puerta de salida:** solo un JWT válido autentica una solicitud protegida.

### Etapa 5 — Login, refresh y logout

1. Implementar autenticación por correo normalizado y contraseña.
2. Emitir access token y refresh token opaco.
3. Implementar rotación transaccional y detección de reutilización.
4. Implementar logout y revocación por familia.
5. Implementar `/auth/me`.

**Puerta de salida:** ciclo register → login → refresh → logout verificado, incluyendo dos refresh concurrentes.

### Etapa 6 — Roles, permisos y propiedad

1. Crear `BankingAuthorization`.
2. Agregar `@PreAuthorize` a todos los controladores.
3. Implementar endpoints administrativos de roles.
4. Incrementar `authVersion` al cambiar roles o desactivar usuarios.
5. Comprobar que no existan rutas bancarias sin regla explícita.

**Puerta de salida:** toda la matriz de autorización tiene pruebas positivas y negativas.

### Etapa 7 — Errores y documentación

1. Integrar 401/403 con `ApiErrorResponse` y request ID.
2. Configurar Bearer JWT en OpenAPI.
3. Documentar endpoints públicos/protegidos y ejemplos.
4. Actualizar README en español.

**Puerta de salida:** Swagger permite login y autorización Bearer; los errores no filtran información interna.

### Etapa 8 — Postman y Bruno

1. Agregar solicitudes de registro, login, refresh, logout y consulta de identidad.
2. Guardar `accessToken` y `refreshToken` como variables de entorno.
3. Enviar automáticamente Bearer token en operaciones protegidas.
4. Agregar escenarios CUSTOMER, SUPPORT y ADMIN.
5. Agregar casos 401, 403, token expirado y refresh reutilizado.
6. No guardar contraseñas, claves privadas ni tokens reales en las colecciones.

**Puerta de salida:** ambas colecciones ejecutan el flujo bancario completo autenticado.

### Etapa 9 — Regresión final

1. Ejecutar `./mvnw clean verify` con Testcontainers.
2. Levantar la aplicación con PostgreSQL limpio.
3. Ejecutar register, login y todos los endpoints bancarios.
4. Verificar transferencias con misma moneda y con conversión.
5. Verificar idempotencia, rollback y concurrencia bajo seguridad.
6. Ejecutar Postman/Newman y Bruno.

**Puerta de salida:** compilación verde, migraciones aplicadas, pruebas automatizadas y colecciones completas sin fallos.

## 16. Estrategia de pruebas

### 16.1 Unitarias

- Codificación y verificación BCrypt.
- Construcción de claims y authorities.
- Validación de issuer, audience y expiración.
- Registro, login y contraseña incorrecta.
- Rotación, expiración y reutilización del refresh token.
- Asignación de roles y aumento de `authVersion`.
- Reglas de propiedad para usuario, cuenta, transferencia y movimiento.

### 16.2 Integración con PostgreSQL/Testcontainers

- Liquibase desde base vacía.
- Restricciones únicas de credencial, roles, permisos y token hash.
- Registro atómico.
- Login con JWT RSA válido.
- Refresh y logout persistidos.
- Dos refresh concurrentes: solo uno puede rotar.
- Usuario inactivo no puede usar ni renovar tokens.
- Cambio de roles invalida access tokens anteriores.

### 16.3 Seguridad HTTP

- Endpoints públicos sin token.
- Endpoint protegido sin token devuelve 401.
- JWT inválido o expirado devuelve 401.
- JWT válido sin permiso devuelve 403.
- CUSTOMER no puede leer recursos ajenos.
- SUPPORT puede consultar y congelar, pero no transferir.
- ADMIN puede ejecutar operaciones administrativas.
- IDs ajenos no filtran existencia en lecturas sensibles.

### 16.4 Regresión bancaria

Reejecutar, ahora con JWT:

- Creación y actualización de usuarios.
- Creación y consulta de cuentas.
- Depósito y retiro.
- Fondos insuficientes.
- Cuenta congelada o cerrada.
- Transferencia normal y con conversión de moneda.
- Monedas no soportadas por la integración.
- Idempotencia.
- Rollback tras fallo.
- Retiros y transferencias concurrentes.
- Paginación de movimientos.

Una falla de autenticación o autorización nunca debe crear transferencias, movimientos ni cambios de saldo.

## 17. Criterios de aceptación

- La aplicación inicia con PostgreSQL y todas las migraciones Liquibase.
- No se crea un administrador con contraseña conocida automáticamente.
- Registro, login, refresh, logout, `me` y cambio de contraseña funcionan.
- Todos los endpoints no públicos requieren un JWT Bearer válido.
- Los roles y permisos se cargan desde PostgreSQL.
- La propiedad de cada recurso se comprueba en el servidor.
- Un usuario inactivo no puede autenticarse ni usar tokens emitidos anteriormente.
- Cambiar contraseña o roles invalida tokens anteriores.
- La rotación evita el uso duplicado de refresh tokens bajo concurrencia.
- Los errores 401 y 403 usan el contrato uniforme del API.
- Las reglas financieras, bloqueos y transacciones siguen funcionando.
- Swagger documenta JWT y permite probar endpoints protegidos.
- Postman y Bruno prueban el flujo autenticado completo.
- `./mvnw clean verify` termina correctamente.
- El README en español explica configuración, migraciones, ejecución y pruebas.

## 18. Limitaciones y trabajo futuro

- Esta fase no implementará OAuth social, MFA, recuperación por correo ni un proveedor externo de identidad.
- Los access tokens no se almacenarán en PostgreSQL; su revocación inmediata dependerá de `authVersion` y del estado del usuario.
- Un frontend web futuro deberá decidir entre Bearer token y cookies `HttpOnly`; si usa cookies, deberá diseñarse protección CSRF.
- Producción requerirá un gestor de secretos, rotación formal de claves RSA, TLS obligatorio, métricas y alertas de seguridad.
- La administración de roles podrá evolucionar a políticas más complejas si el proyecto lo necesita, sin cambiar la separación Controller–Service–Repository.

## 19. Referencias técnicas

- [Spring Security OAuth2 Resource Server y JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Authorization HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
