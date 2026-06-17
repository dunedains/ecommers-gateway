# API Gateway

Punto de entrada unico al sistema e-commerce. Enruta las peticiones a los 9 microservicios,
valida los tokens JWT y expone un Swagger UI agregado.

## Informacion general

| Campo | Valor |
|-------|-------|
| Puerto | `8080` |
| Tecnologia | Spring Cloud Gateway (WebFlux) |
| Autenticacion | JWT (HS256) |

## Responsabilidades

1. **Enrutamiento** de `/api/**` hacia cada microservicio.
2. **Autenticacion**: valida el JWT en todas las rutas excepto las publicas y propaga el
   id del usuario al microservicio destino mediante la cabecera `X-User-Id`.
3. **Login / registro** en `/auth/**` (genera el JWT consultando al microservicio de usuarios).
4. **Swagger agregado** en `/swagger-ui.html` con la documentacion de los 9 servicios.

## Rutas publicas (no requieren token)

- `POST /auth/register`
- `POST /auth/login`
- `/swagger-ui/**`, `/v3/api-docs/**`

## Endpoints de autenticacion

**Registro:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Felipe Zapata",
    "email": "felipe@example.com",
    "address": "Av. Principal 123",
    "password": "secret123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "felipe@example.com", "password": "secret123" }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "felipe@example.com"
}
```

**Uso del token en una peticion protegida:**
```bash
curl http://localhost:8080/api/productos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Variables de entorno

| Variable | Descripcion | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Clave secreta del JWT (minimo 256 bits) | clave de desarrollo |
| `JWT_TTL_SECONDS` | Vigencia del token en segundos | `3600` |
| `PRODUCTOS_URI` ... `NOTIFICATIONS_URI` | URL de cada microservicio | `http://localhost:<puerto>` |
| `USERS_URI` | URL del servicio de usuarios (rutas y `/auth`) | `http://localhost:8082` |

## Notas de version (Spring Cloud 2025.1.1 / Spring Boot 4)

- El starter del gateway reactivo es `spring-cloud-starter-gateway-server-webflux`.
- Las rutas se configuran bajo `spring.cloud.gateway.server.webflux.routes`. Si tu version
  de Spring Cloud aun usa el namespace antiguo, muevelas a `spring.cloud.gateway.routes`.

## Tecnologias

- Java 25 - Spring Boot 4.0.6
- Spring Cloud Gateway 2025.1.1 (WebFlux)
- JJWT 0.12.6
- springdoc-openapi (WebFlux)
- Logback + Logstash encoder (logs JSON)
