# 🚁 Drone Delivery Sim — Setup & Guia Rápido

Simulador didático de entregas por drones com **Spring Boot 3 / Java 17**.  
Principais recursos:

- 📦 Planejamento de entregas por **múltiplos drones**
- 🚫 **Zonas de exclusão** (obstáculos retangulares) desviando rotas
- ⏫ **Fila de prioridades** (HIGH → MEDIUM → LOW)
- 🏋️ Checagem de **capacidade/peso** (pedido pesado é rejeitado)
- 🔋 Simulação simples de **estados/bateria** em “tempo real”
- 🧭 **Swagger UI** para explorar **todos os endpoints**

---

## ✅ Requisitos

- **JDK 17**
- **Maven 3.9.11 via Maven Wrapper (recomendado)**  
  *(o projeto já inclui o wrapper: `mvnw`/`mvnw.cmd` que baixa e usa Maven 3.9.11)*
- **Ou Maven 3.9+ instalado globalmente** (opcional)

Verifique as versões:

**Windows (PowerShell/CMD):**
```powershell
java -version
.\mvnw -v
```

**Linux/macOS:**
```bash
java -version
./mvnw -v
```

---

## 📥 Clonar o repositório

```bash
git clone https://github.com/thiagobranc0/drone-delivery-sim.git
cd drone-delivery-sim
```

---

## ▶️ Como executar

### Windows
**PowerShell (Maven Wrapper):**
```powershell
.\mvnw spring-boot:run
```

**CMD (Maven Wrapper):**
```bat
mvnw.cmd spring-boot:run
```

**Com Maven instalado globalmente:**
```bat
mvn spring-boot:run
```

### Linux/macOS
**Maven Wrapper:**
```bash
./mvnw spring-boot:run
```

**Com Maven instalado globalmente:**
```bash
mvn spring-boot:run
```

- App: `http://localhost:8080`  
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

💡 **Use o Swagger para tudo:** clique no endpoint, **Try it out**, preencha os campos e **Execute**. Você vê a requisição e a resposta na hora.

---

## 🧭 Teste tudo pelo Swagger (passo a passo)

### 1) Criar um drone → `POST /drones`
**Corpo (JSON):**
```json
{
  "id": "D1",
  "capacidadeKg": 10.0,
  "alcanceKm": 100.0,
  "velocidadeKmh": 60.0,
  "consumoPercentPorKm": 1.5
}
```

**Respostas esperadas:**
- ✅ **201 Created** (criado)
- ❌ **409 Conflict** (id duplicado)
- ❌ **400 Bad Request** (regra de negócio violada, ex.: `capacidadeKg > 25.0`)

---

### 2) Criar pedidos → `POST /orders`
**Corpo (JSON):**
```json
{ "x": 3.0, "y": 4.0, "pesoKg": 2.5, "prioridade": "HIGH" }
```

---

### 3) (Opcional) Inserir obstáculo → `POST /obstacles`
Envie como **Request Params** (query string ou `application/x-www-form-urlencoded`). **Não envie JSON.**

**Parâmetros:** `x1`, `y1`, `x2`, `y2`  
**Exemplo (query):**
```
POST /obstacles?x1=1&y1=-2&x2=5&y2=2
```

Listar obstáculos: `GET /obstacles`

---

### 4) Gerar plano → `POST /plan`
Retorna `viagens`, `distanciaTotalKm`, `tempoTotalMin`, etc.  
Se o obstáculo cortar a rota direta, a distância/tempo **aumentam**.

---

### 5) Simulação em “tempo real”
- Iniciar: `POST /realtime/start`
- Avançar “tempo”: `POST /realtime/tick?droneId=D1&distanceKm=1.0` *(repita algumas vezes)*
- Acompanhar: `GET /drones/status` *(estado e bateria variando)*
- Parar: `POST /realtime/stop`

---

## 🗂️ Estrutura (visão geral)

```bash
src/main/java/br/com/dti/drone_delivery_sim/
├─ controller/
│  ├─ DroneController.java         # /drones (CRUD + status)
│  ├─ OrderController.java         # /orders (CRUD)
│  ├─ ObstacleController.java      # /obstacles (NoFlyZone)
│  ├─ PlanController.java          # /plan (gera plano)
│  └─ DroneTravelController.java   # simulação “tempo real” (start/stop/tick)
├─ service/
│  ├─ DroneService.java
│  ├─ OrderService.java
│  ├─ ObstacleService.java
│  ├─ DeliveryOptimizer.java       # rejeita pedido > capacidade; monta plano
│  ├─ RouteCalculator.java         # desvio de obstáculos
│  └─ RealTimeSimulator.java       # relatório do último plano (métricas/dashboard)
├─ model/
│  ├─ Drone.java / enums DroneState.java
│  ├─ Order.java / enums Priority.java
│  ├─ NoFlyZone.java
│  └─ Delivery.java
└─ dto/
   ├─ DroneDTO.java
   ├─ DroneCreateRequest.java
   └─ OrderDTO.java
```

🧠 **Armazenamento in-memory** (listas). **Não há banco**.

---

## 📏 Regras de negócio (resumo)

- 🏋️ **Capacidade x Peso:** pedido com `pesoKg` maior que a **capacidade do drone** é **rejeitado** no planejamento.  
- ⏫ **Prioridade:** `HIGH → MEDIUM → LOW` (não há “fura-fila”).  
- 🚫 **Zonas de exclusão:** retângulos `(x1,y1)–(x2,y2)` são evitados pelo `RouteCalculator`.  
- 🔄 **Estados (com simulação ativa):** `IDLE`, `DELIVERING`, `RETURNING`, `CHARGING`, etc.

---

## 🧪 Testes

Rodar testes:
```bash
# Linux/macOS
./mvnw test

# Windows (PowerShell)
.\mvnw test
```

Coberturas principais:

- **SimularBateriaTest** — *ticks* alteram **bateria/estado**
- **InserirObstaculoWebTest** — inserir zona **aumenta a distância** do plano
- **CalcularTempoEntregaTest** — **tempo total** bate com **distância/velocidade**
- **FilaPrioridadeTest** — `HIGH > MEDIUM > LOW`
- **Web tests** — Drone / Order / Simulation / Plan

---

## 🛠️ Solução de problemas

- **Swagger com erro** *(NoSuchMethodError ControllerAdviceBean)*  
  Use versão compatível do SpringDoc com Spring Boot 3.3.x:
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
  </dependency>
  ```

- **`POST /obstacles` retornando 400 Bad Request**  
  Envie `x1,y1,x2,y2` como **Request Params** (query/form), **não** em JSON.

- **`POST /drones` retornando 400 Bad Request**  
  Verifique a mensagem: ex. **`capacidadeKg > 25.0`** ou **`id` inválido**.

- **Plano vazio**  
  Garanta que exista **pelo menos 1 drone** e **pelo menos 1 pedido**.

---

## 📣 Reforçando:

Use a **Swagger UI** como “painel de controle” da aplicação.  
✅ Você cria dados, executa o plano e acompanha a simulação **sem sair da página**.
