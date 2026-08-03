# API overview

All protected endpoints require `Authorization: Bearer <jwt>`.

The local-only scheduler trigger endpoints under `/test/**` are disabled by default and are not part of the production API.

| Module | Method | Endpoint | Purpose |
|---|---|---|---|
| Auth | POST | `/api/v1/user/login` | Login and issue JWT |
| Auth | POST | `/api/v1/user/register` | Register user |
| Medication | GET | `/api/v1/plan/today` | Read today's medication plan |
| Medication | PUT | `/api/v1/plan/{planId}/action` | Confirm, skip, or undo a dose |
| Medicine box | GET | `/api/v1/box/list` | Read current inventory |
| Guardian | GET | `/api/v1/guardian/dashboard` | Read linked elder summaries |
| Guardian | PUT | `/api/v1/guardian/events/{eventId}/resolve` | Resolve an emergency event |
| AI | POST | `/api/ai/chat` | AI medication conversation |
| OCR | POST | `/api/v1/drug/recognize` | Recognize medicine packaging |

Responses use:

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 0
}
```

Errors use the same envelope. Unexpected server errors return code `500` with a generic message; details are logged server-side.
