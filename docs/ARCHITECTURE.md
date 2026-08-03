# AI Medication Safety Manager

## Architecture

```mermaid
flowchart LR
  Elder[老人端 React] --> API[Spring Boot REST API]
  Guardian[家属端 React] --> API
  API --> Auth[JWT + Spring Security]
  API --> MySQL[(MySQL)]
  API --> Redis[(Redis)]
  API --> WS[WebSocket]
  API --> OCR[Baidu OCR]
  API --> LLM[DeepSeek]
  API --> TTS[Baidu TTS]
```

The backend follows Controller -> Service -> Mapper -> MySQL layering. External AI/OCR/TTS calls stay behind service interfaces so that business flows can degrade gracefully when an external provider is unavailable.

## Core medication flow

```mermaid
sequenceDiagram
  participant U as Elder client
  participant A as API
  participant P as PlanService
  participant D as Database
  participant G as Guardian client
  U->>A: Confirm medication
  A->>P: Validate plan ownership
  P->>D: Insert medication log
  P->>D: Mark plan completed
  P->>D: Decrement medicine-box inventory
  P-->>U: Idempotent operation result
  P-->>G: Missed-dose/shortage notification
```

## Engineering decisions

- JWT identity is read from `SecurityContext`; client-provided user IDs are never trusted for authorization or rate limiting.
- Medication confirmation is transactional so the log, plan status, and inventory update commit together.
- Expensive AI endpoints use per-user/IP fixed-window limiting.
- Production responses use a global exception handler and do not return raw exception messages.
