# Cloud-Native Store — Project Setup

## Overview
A modular e-commerce system built with a microservices architecture for SFWE 410/510. The system demonstrates bounded-context decomposition, containerized deployment, inter-service communication, and (in later phases) security and messaging.

## User Types

| Role | Capabilities |
|---|---|
| **Customer** | Sign up / log in, browse and search the catalog, manage cart, checkout, view own order history and status |
| **Admin** | All customer capabilities, plus: create/update/delete products, manage stock levels, view all orders across customers |

Role is established at signup/login (Auth/Account Service) and carried in the JWT; each service authorizes requests based on that role (e.g., only Admins can hit the catalog's write endpoints or view all orders).

## Services

| Service | Responsibility |
|---|---|
| **Config Server** | Centralized configuration for all services; maintains `dev` and `prod` profiles |
| **Auth/Account Service** | Signup, login, session/JWT issuance, user profile management, role (Customer/Admin) |
| **Product Catalog & Inventory Service** | Product listing, search/filter, product details, stock levels, and catalog administration |
| **Cart Service** | Add/remove items, view cart (per-user or per-session) |
| **Payment/Order Service** | Checkout flow, order history, order status, payment processing |
| **Notification Service** | Sends order confirmation emails / subscription notifications |

## Architecture

```
                      ┌─────────────────┐
                      │  Config Server   │
                      │ (dev / prod)     │
                      └────────▲─────────┘
                               │
                     ┌─────────┴─────────┐
                     │      Gateway       │  (Phase 2+)
                     └─────────▲─────────┘
                               │
   ┌───────────┬───────────────┼───────────────┬─────────────┐
   │           │               │               │             │
┌──▼───┐   ┌───▼──────┐   ┌────▼─────┐   ┌─────▼──────┐  ┌───▼───────────┐
│ Auth │   │ Catalog  │   │  Cart    │   │  Payment/  │  │ Notification  │
│      │   │+Inventory│   │ Service  │   │  Order     │  │ Service       │
└──┬───┘   └────┬─────┘   └────┬─────┘   └─────┬──────┘  └───────▲───────┘
   │            │              │               │                 │
   │            │              │               └─── order event ─┘
   ▼            ▼              ▼               ▼            (Phase 3: JMS/queue)
 [DB]         [DB]           [DB]             [DB]
```

Each service has its own 3-layer stack: **Controller (REST) → Service → Repository → Entity (relational DB)**, and its own database — no shared database access between services.

## Phase 1 Deliverables Checklist
- [ ] 3-layer stack (entity, repository, service) for each microservice
- [ ] CRUD REST controllers per service
- [ ] Config server with `dev` and `prod` profiles
- [ ] Dockerfile per service + docker-compose for full-stack local run
- [ ] Postman workspace covering all service endpoints
- [ ] Video: theme, canonical model/bounded context, deployment sketch, prototype demo, Docker/profile demo
- [ ] README to deploy and run the system (this doc, expanded with real commands)
- [ ] Submit as `LastnameFirstname_Phase1_Component` (e.g. `PotterTravis_Phase1_Presentation.mp4`)

## Tech Stack
- **Language/Framework:** Java + Spring Boot (Spring Cloud Config now; Eureka + Gateway added in Phase 2)
- **Database:** PostgreSQL (one schema/instance per service) or H2 for local dev
- **Containerization:** Docker + Docker Compose
- **API Testing:** Postman
- **Messaging (Phase 3):** RabbitMQ

## Prerequisites

Before running the application, make sure the following are installed:

- Docker Desktop
- Docker Compose
- Java 21
- Maven

Docker Desktop must be running before starting the application.

## Local Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/travispotterAZ/CertainlyInStock_AnOnlineStorefront.git

2. Navigate to the repository root:

   cd CertainlyInStock_AnOnlineStorefront

3. Make sure Docker Desktop is running.

4. Build and start the application:

   docker compose -f parent/docker-compose.yml up --build -d --wait

5. Verify that the containers are running:

   docker compose -f parent/docker-compose.yml ps

6. When finished, stop the containers:

   docker compose -f parent/docker-compose.yml down

## Service Ports

| Service | Port |
| --- | --- |
| Config Server | 8080 |
| Auth Service | 8081 |
| User Service | 8082 |
| Product Service | 8083 |
| Cart Service | 8084 |
| Payment/Order Service | 8085 |
| PostgreSQL | 5432 |

## Configuration Profiles

Spring Cloud Config provides centralized configuration for the
microservices.

Two environment profiles are maintained:

- `dev` - development configuration
- `prod` - production configuration

For example, the Cart Service configuration can be verified through
the Config Server:

    curl http://localhost:8080/cart-service/dev

    curl http://localhost:8080/cart-service/prod

## Repo Structure (suggested)
```
/config-server
/auth-service
/catalog-service        (includes inventory)
/cart-service
/payment-service
/notification-service
/postman
docker-compose.yml
README.md
```

## Looking Ahead
- **Phase 2:** Auth/Account becomes the identity provider issuing the bearer JWT (carrying the Customer/Admin role); Gateway fronts all services and handles auth + service discovery (Eureka/Consul); add a Gatling stress test.
- **Phase 3:** Notification Service consumes an "order placed" event off a queue (JMS) rather than being called synchronously — the natural home for the messaging requirement. Deploy to Heroku/AWS; add tracing (e.g., Zipkin/Jaeger) visualized against the stress test.
