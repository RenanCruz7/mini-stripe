# 💳 MiniStripe - Payment Ecosystem Microservices

O **MiniStripe** é uma plataforma distribuída de pagamentos inspirada na arquitetura da Stripe. O projeto demonstra a implementação de um ecossistema escalável de microserviços utilizando Java 21, Spring Boot 3 e arquitetura orientada a eventos (Event-Driven Architecture).

---

### 🏛️ Arquitetura do Sistema

O sistema é composto por 5 microserviços independentes que se comunicam de forma síncrona (REST) e assíncrona (Kafka):

1.  **API Gateway**: Ponto de entrada único, responsável pelo roteamento e segurança.
2.  **Auth Service**: Central de identidade (IAM) com JWT, roles (RBAC) e OAuth2.
3.  **Account Service**: Gestão de contas de usuários, carteiras digitais e saldos.
4.  **Payment Service**: Core do sistema. Processa transações com garantia de idempotência e resiliência.
5.  **Notification Service**: Consumidor de eventos para disparo de comunicações e Webhooks.

---

### 🛠️ Tecnologias & Ferramentas

- **Back-end:** Java 21, Spring Boot 3.2
- **Data:** PostgreSQL (Bancos isolados), Redis (Cache & Rate Limiting)
- **Mensageria:** Apache Kafka
- **Segurança:** Spring Security, JWT (Json Web Token)
- **Resiliência:** Resilience4j (Circuit Breaker, Retry, Fallback)
- **Observabilidade:** Spring Actuator, Prometheus, Grafana
- **Infra/DevOps:** Docker, Docker Compose, GitHub Actions (CI/CD)
- **Testes:** JUnit 5, Mockito, Testcontainers

---

### 🚀 Diferenciais Técnicos Implementados

- **Idempotência de Transações**: Garantia de que uma mesma transação não seja processada mais de uma vez através de chaves únicas.
- **Consistência Eventual & Saga Pattern**: Coordenação de transações distribuídas entre os serviços de Account e Payment via Kafka.
- **Race Condition Prevention**: Uso de *Optimistic Locking* no saldo da carteira para evitar problemas de concorrência em saques/depósitos simultâneos.
- **Observabilidade**: Dashboards para monitoramento de saúde dos serviços, latência e taxa de erro.
- **Isolamento de Dados**: Database-per-service para garantir total independência entre os microserviços.

---

### 📦 Como Executar

**Pré-requisitos:** Docker e Docker Compose instalados.

1. Clone o repositório:
   ```bash
   git clone https://github.com/seu-usuario/mini-stripe.git