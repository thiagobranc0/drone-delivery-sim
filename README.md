# 🚁 Drone Delivery Sim

> Simulador de entregas por drones com **Spring Boot 3 / Java 17** em arquitetura **em camadas com API RESTful**.

> Desenvolvido como case técnico para **DTI Digital!**

---

## 📚 Índice
- 🔎 [Visão Geral](#-visão-geral)
- 🏗️ [Arquitetura](#-arquitetura-em-camadas)
- 🤖 [Ajuda da IA (regras de uso)](#-ajuda-da-ia-regras-de-uso)
- 🧰 [Stack & Requisitos](#-stack--requisitos)
- 📥 [Instalação](#-instalação)
- ▶️ [Execução](#-execução)
- 🧭 [API & Swagger](#-api--swagger)
- 🔌 [Endpoints](#-endpoints)
- ✅ [Funcionalidades implementadas](#-funcionalidades-implementadas)
- ⏱️ [Simulação em Tempo Real](#-simulação-em-tempo-real)
- 🗂️ [Estrutura do Projeto](#-estrutura-do-projeto)
- 🧪 [Testes](#-testes)
- 🛠️ [Solução de Problemas](#-solução-de-problemas)
- 📜 [Licença e Créditos](#-licença-e-créditos)

---

## 🔎 Visão Geral
Simulador de entregas por drones com:
- 📦 **Múltiplos drones**
- ⏫ **Fila de prioridades** `HIGH → MEDIUM → LOW`
- 🏋️ **Checagem de capacidade/peso**
- 🚫 **Zonas de exclusão** (retângulos) com **desvio de rotas**
- 🔋 **Simulação simples** de estados/bateria em “tempo real”
- 🧭 **Swagger UI** para explorar endpoints

---

## 🏗️ Arquitetura (em camadas)

Camadas bem definidas, focadas em simplicidade e testabilidade:

- **Controller (Web/API)** — expõe a API REST, faz *binding*/validação básica e traduz requests/responses.
- **Service (Casos de uso)** — orquestra regras e coordenadas entre componentes do domínio.
  - `DeliveryOptimizer` (planejamento/validações)
  - `RouteCalculator` (desvios por zonas de exclusão)
  - `RealTimeSimulator` (ticks, estados e telemetria)
- **Domain (Regra de negócio)** — modelos e *enums*: `Drone`, `Order`, `NoFlyZone`, `Delivery`, `DroneState`, `Priority`.
- **Persistência** — **in-memory** (listas) para fins didáticos; facilmente substituível por repositórios no futuro.

---

## 🤖 Ajuda da IA (regras de uso)
- **Autoria de arquitetura:** a **arquitetura foi totalmente definida pelo autor do projeto** (Camadas com API RESTful).
- **Apoio da IA**:
  - 🔍 **Heurísticas**: refinamento de estratégias de priorização, checagens de capacidade e decisões de montagem de plano.
  - 🧪 **Geração de testes**: sugestões de cenários (bateria/estado por *ticks*, impacto de obstáculos em distância, fila de prioridade).
  - 🧭 **Otimização de rotas**: definição do fluxo básico de cálculo — rota direta (euclidiana) **com desvio por contorno de retângulos**, escolhendo o menor entre caminhos alternativos (ex.: superior/inferior ou esquerdo/direito).
  - 🗺️ **Mapeamento da malha de atuação**: padronização do plano cartesiano 2D `(x, y)` e das **No-Fly Zones** como retângulos `[(x1,y1) – (x2,y2)]`.

---

## 🧰 Stack & Requisitos
- **Java 17**
- **Spring Boot 3.x**
- **Maven 3.9.11 (via Maven Wrapper incluído)**  
  *(o projeto inclui `mvnw`/`mvnw.cmd` que baixa e usa Maven 3.9.11)*
- **springdoc-openapi** para Swagger UI

Verificar versões:

```bash
# Windows (PowerShell)
java -version
.\mvnw -v

# Linux/macOS
java -version
./mvnw -v
```

---

## 📥 Instalação
```bash
git clone https://github.com/thiagobranc0/drone-delivery-sim.git
cd drone-delivery-sim
```

---

## ▶️ Execução

### Windows
**PowerShell (Maven Wrapper)**
```powershell
.\mvnw spring-boot:run
```

**CMD (Maven Wrapper)**
```bat
mvnw.cmd spring-boot:run
```

**Com Maven global**
```bat
mvn spring-boot:run
```

### Linux/macOS
**Maven Wrapper**
```bash
./mvnw spring-boot:run
```

**Com Maven global**
```bash
mvn spring-boot:run
```

- App: `http://localhost:8080`  
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 🧭 API & Swagger
Use o **Try it out** no Swagger para criar drones, pedidos, obstáculos e gerar o plano.

Exemplos de payloads:

**POST /drones**
```json
{
  "id": "D1",
  "capacidadeKg": 10.0,
  "alcanceKm": 100.0,
  "velocidadeKmh": 60.0,
  "consumoPercentPorKm": 1.5
}
```

**POST /orders**
```json
{ "x": 3.0, "y": 4.0, "pesoKg": 2.5, "prioridade": "HIGH" }
```

**POST /obstacles (query params)**
```
/obstacles?x1=1&y1=-2&x2=5&y2=2
```

---

## 🔌 Endpoints

### ✈️ Drones
| Método | Endpoint                  | O que faz                                                  | Body/Parâmetros principais                                                |
|-------:|---------------------------|------------------------------------------------------------|---------------------------------------------------------------------------|
| POST   | `/drones`                 | Cria um **drone**                                          | JSON: `id`, `capacidadeKg`, `alcanceKm`, `velocidadeKmh`, `consumoPercentPorKm` |
| GET    | `/drones`                 | Lista **drones**                                           | —                                                                         |
| GET    | `/drones/{id}`            | Busca **drone por id**                                     | `id` (path)                                                               |
| PUT    | `/drones/{id}`            | Atualiza **drone** (campos principais + estado)            | JSON: `capacidadeKg`, `alcanceKm`, `velocidadeKmh`, `consumoPercentPorKm`, `estado` |
| PATCH  | `/drones/{id}/state`      | Atualiza **apenas o estado** do drone                      | Query: `estado` (enum `DroneState`)                                       |
| DELETE | `/drones/{id}`            | Remove **um drone**                                        | `id` (path)                                                               |
| DELETE | `/drones`                 | Remove **todos os drones**                                 | —                                                                         |
| GET    | `/drones/status`          | Snapshot de **estado** e **bateria** dos drones            | —                                                                         |

### 🧾 Pedidos (Orders)
| Método | Endpoint     | O que faz                      | Body/Parâmetros principais                          |
|-------:|--------------|--------------------------------|-----------------------------------------------------|
| POST   | `/orders`    | Cria **pedido**                | JSON: `x`, `y`, `pesoKg`, `prioridade`              |
| GET    | `/orders`    | Lista **pedidos**              | —                                                   |
| DELETE | `/orders`    | Remove **todos os pedidos**    | —                                                   |

### ⛔ Obstáculos (No-Fly Zones)
| Método | Endpoint        | O que faz                                   | Body/Parâmetros principais                    |
|-------:|-----------------|---------------------------------------------|-----------------------------------------------|
| POST   | `/obstacles`    | Adiciona **zona de exclusão** (retângulo)   | **Query/Form**: `x1`, `y1`, `x2`, `y2`        |
| GET    | `/obstacles`    | Lista **zonas de exclusão**                 | —                                             |
| DELETE | `/obstacles`    | Remove **todas as zonas**                   | —                                             |

### 🧠 Planejamento & Dashboard
| Método | Endpoint      | O que faz                                                      | Parâmetros                              |
|-------:|---------------|----------------------------------------------------------------|-----------------------------------------|
| POST   | `/plan`       | Gera **plano de entregas** (viagens, `distanciaTotalKm`, `tempoTotalMin`) | Query: `batteryPolicy` = `STRICT` (default) \| `SMART` |
| GET    | `/dashboard`  | **Relatório do último plano** (métricas agregadas)            | —                                       |

### ⏱️ Simulação em Tempo Real (`/realtime`)
| Método | Endpoint                      | O que faz                                           | Parâmetros                                                |
|-------:|-------------------------------|-----------------------------------------------------|-----------------------------------------------------------|
| POST   | `/realtime/queue-last-plan`   | Enfileira **último plano** como missões             | —                                                         |
| POST   | `/realtime/start`             | Inicia **simulação** (AUTO/MANUAL)                  | Query: `mode` = `MANUAL`\|`AUTO`, `tickMillis`?           |
| POST   | `/realtime/stop`              | Encerra **simulação**                               | —                                                         |
| POST   | `/realtime/tick`              | Avança simulação manualmente                        | Query: `secs` (default `60`)                              |
| GET    | `/realtime/status`            | Status atual do simulador                           | —                                                         |
| GET    | `/realtime/telemetry`         | Telemetria de **todos os drones**                   | —                                                         |
| GET    | `/realtime/telemetry/{droneId}` | Telemetria de **um drone** específico             | `droneId` (path)                                          |

> Observação: `POST /obstacles` **não aceita JSON**; envie **query string** ou **form-url-encoded**.

---

## ✅ Funcionalidades implementadas
- 🏋️ **Validação de capacidade x peso** — pedidos com `pesoKg` maior que `capacidadeKg` do drone são **rejeitados** no planejamento pelo `DeliveryOptimizer`.
- ⏫ **Fila de prioridade** — pedidos são ordenados por `HIGH → MEDIUM → LOW`; o `DeliveryOptimizer` consome nessa ordem sem “fura-fila”.
- 🚫 **Zonas de exclusão (No-Fly Zones)** — definidas como retângulos `(x1,y1)-(x2,y2)`; o `RouteCalculator` evita a interseção com a rota direta, contornando o retângulo pelo menor caminho.
- 🧭 **Planejamento de entregas** — gera viagens com `distanciaTotalKm` e `tempoTotalMin` considerando velocidade do drone e desvios por obstáculos.
- 🔋 **Consumo de bateria** — decremento proporcional à distância percorrida; impede deslocamentos que violariam política de bateria (ex.: `STRICT`), forçando retorno/carga quando necessário.
- 🔄 **Estados do drone** — transições entre `IDLE`, `DELIVERING`, `RETURNING`, `CHARGING` durante a simulação; `RealTimeSimulator` atualiza estados a cada *tick*.
- 📊 **Dashboard do último plano** — expõe métricas agregadas (quantidade de viagens, distância e tempo totais) para inspeção rápida.
- 📡 **Telemetria** — fornece posição/estado/bateria em tempo real para todos os drones ou um drone específico.
- 🧼 **Limpeza rápida de dados** — endpoints para limpar drones, pedidos e obstáculos facilitam reexecuções de cenários no case.

---

## ⏱️ Simulação em Tempo Real
```text
POST /realtime/queue-last-plan             # carrega último plano como missões
POST /realtime/start?mode=MANUAL           # ou AUTO; tickMillis opcional
POST /realtime/tick?secs=60                # avance manualmente N segundos
GET  /realtime/status                      # status do simulador
GET  /realtime/telemetry                   # telemetria geral
GET  /realtime/telemetry/{droneId}         # telemetria de um drone
POST /realtime/stop
```

---

## 🗂️ Estrutura do Projeto
```text
src/main/java/br/com/dti/drone_delivery_sim/
├─ controller/
│  ├─ DroneController.java         # /drones (CRUD + status/state)
│  ├─ OrderController.java         # /orders (criar/listar/limpar)
│  ├─ DroneTravelController.java   # /plan, /dashboard, /obstacles (CRUD simples)
│  └─ SimulationController.java    # /realtime (start/stop/tick/status/telemetria/queue-last-plan)
├─ service/
│  ├─ DroneService.java
│  ├─ OrderService.java
│  ├─ DeliveryOptimizer.java       # rejeita > capacidade; monta plano
│  ├─ RouteCalculator.java         # desvio de obstáculos retangulares
│  └─ RealTimeSimulator.java       # telemetria, estados, agendamento
├─ model/
│  ├─ Drone.java / enums DroneState.java
│  ├─ Order.java / enums Priority.java
│  ├─ NoFlyZone.java
│  └─ Delivery.java
└─ dto/
   ├─ DroneDTO.java
   ├─ DroneCreateRequest.java
   └─ DroneUpdateRequest.java
```

---

## 🧪 Testes
```bash
# Linux/macOS
./mvnw test

# Windows (PowerShell)
.\mvnw test
```
Alguns exemplos de testes implementados:
- **SimularBateriaTest** — *ticks* alteram **bateria/estado**
- **ObstaculoTest** — inserir zona **aumenta a distância** do plano
- **CalcularTempoEntregaTest** — **tempo total** = f(distância, velocidade)
- **FilaPrioridadeTest** — `HIGH > MEDIUM > LOW`
- **Web tests** — Drone / Order / Simulation / Dashboard

---

## 🛠️ Solução de Problemas
- **Erro springdoc** *(NoSuchMethodError ControllerAdviceBean)* → usar:
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
  </dependency>
  ```
- **`POST /obstacles` 400** → envie `x1,y1,x2,y2` como **query/form**, **não JSON**.
- **`POST /drones` 400** → mensagens comuns: `capacidadeKg > 25.0` ou `id` inválido.
- **Plano vazio** → garanta pelo menos **1 drone** e **1 pedido**.

---

## 📜 Licença e Créditos
- Desenvolvido por **Thiago Branco de Oliveira** para case técnico da **DTI Digital** 💪
- **Apoio da IA:** heurísticas, testes, definição do fluxo de otimização de rotas e mapeamento da malha de atuação dos drones.
