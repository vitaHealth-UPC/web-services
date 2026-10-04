# API Contract Guidelines

The backend is the authority for the REST contract consumed by mobile-android and future clients.

## Rules

1. Define a route only in the controller owned by the responsible Bounded Context.
2. Document public operations through OpenAPI.
3. Use request/response Resources; never expose JPA entities directly.
4. Use assemblers/transformers at the HTTP boundary.
5. Make mutation semantics explicit and idempotent where the backlog requires it, especially intake confirmation and scheduled background processing.
6. Use stable identifiers and stable machine-readable error codes.
7. Keep base documentation in English; localizable human messages may use message bundles.
8. Do not expose another Bounded Context's persistence model.
9. When mobile depends on a new operation, establish/update the backend OpenAPI contract first or in the same coordinated work item.
10. TS-13 does not authorize undocumented "sync endpoints"; add only contracts justified by explicit backlog acceptance criteria.

Recommended flow:

~~~text
Controller
  → Request Resource
  → Command/Query assembler
  → Application handler/service
  → Domain
  → Repository/port
  → Response assembler
  → Resource
~~~

Exact route names are decided by the backend Technical Story and become authoritative only when captured in implementation and OpenAPI.
