# nova-java-notifications-micronaut-module

Micronaut 5.0.4 "colloquial" module for the
[`nova-notifications`](../nova-java-notifications) library. When the
module is on the classpath, a `NotificationFacade` bean is exposed
via `@Factory` / `@Bean` and ready to inject anywhere in the
application.

This is the **Nivel 1 → Nivel 2** adapter for the Micronaut ecosystem
in Nova's meta-framework
(`docs/adrs/shared/ADR-001-arquitectura-meta-framework-cinco-niveles.md`).
The library is framework-agnostic; the module is the only piece that
knows about Micronaut.

## Colloquial module

This is a "colloquial" Micronaut module: a plain JAR with `@Factory`
and `@Bean` methods, discovered by Micronaut at runtime via its
annotation processor. **No separate deployment / runtime module** is
needed. This is the Micronaut 5 idiom for small libraries that don't
need AOT compilation or native-image integration metadata.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("pe.edu.nova.java.starters:nova-java-notifications-micronaut-module:1.0.0")
    annotationProcessor("io.micronaut:micronaut-inject-java")
    // (transitively brings io.micronaut:micronaut-core + micronaut-context)
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>pe.edu.nova.java.starters</groupId>
    <artifactId>nova-java-notifications-micronaut-module</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

`application.yml`:

```yaml
nova.notifications:
  enabled: true
  email:
    provider: sendgrid
    api-key: ${SENDGRID_API_KEY}
    default-sender: no-reply@example.com
  resilience:
    max-attempts: 3
```

`NotificationsController.java`:

```java
@Controller("/api/notifications")
public class NotificationsController {

    private final NotificationFacade facade;

    public NotificationsController(NotificationFacade facade) {
        this.facade = facade;
    }

    @Get("/email/welcome")
    public NotificationResult welcome() {
        return facade.send(EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("Welcome"))
                .body(new MessageBody("Thanks for signing up to Nova."))
                .build());
    }
}
```

The `NotificationFacade` is wired by `NotificationsFactory` (a
`@Factory` class with two `@Bean` methods:
`notificationConfiguration` and `notificationFacade`).

## Configuration reference

The Micronaut 5 property binder requires each nested configuration
class to declare its own `@ConfigurationProperties` annotation. The
top-level `nova.notifications` prefix is bound to the master `enabled`
switch; each channel is bound at its own relative prefix
(`nova.notifications.email`, `nova.notifications.sms`, etc.).

| Property | Type | Default | Description |
|---|---|---|---|
| `nova.notifications.enabled` | `boolean` | `true` | Master switch. When `false`, the factory returns a no-op `NotificationFacade` (every `send` returns `FAILED` with `ErrorCode.DISABLED`). |
| `nova.notifications.email.provider` | `String` | _(none)_ | `sendgrid` or `mailgun`. |
| `nova.notifications.email.api-key` | `String` | _(none)_ | |
| `nova.notifications.email.default-sender` | `String` | _(none)_ | |
| `nova.notifications.sms.provider` | `String` | `twilio` | |
| `nova.notifications.sms.account-sid` | `String` | _(none)_ | |
| `nova.notifications.sms.auth-token` | `String` | _(none)_ | |
| `nova.notifications.sms.from-number` | `String` | _(none)_ | |
| `nova.notifications.push.provider` | `String` | `firebase` | |
| `nova.notifications.push.project-id` | `String` | _(none)_ | |
| `nova.notifications.push.server-key` | `String` | _(none)_ | |
| `nova.notifications.slack.default-webhook-url` | `String` | _(none)_ | |
| `nova.notifications.resilience.max-attempts` | `int` | `3` | |
| `nova.notifications.resilience.initial-backoff-millis` | `long` | `200` | |
| `nova.notifications.resilience.circuit-failure-threshold` | `int` | `5` | |
| `nova.notifications.resilience.circuit-open-duration-seconds` | `long` | `30` | |
| `nova.notifications.resilience.rate-limit-permits-per-second` | `int` | `0` | `0` disables rate limiting. |

Each channel is independent. An empty or partial config for a channel
is treated as "channel not enabled" and the corresponding
`SendNotificationPort` is not registered.

## Disabling the library

```yaml
nova.notifications:
  enabled: false
```

The factory returns a no-op `NotificationFacade` (every `send` returns
`FAILED` with `ErrorCode.DISABLED`). Consumers can still inject
`NotificationFacade` without a startup `ConfigurationException`; the
no-op contract is documented in
[`nova-java-notifications/README.md`](../nova-java-notifications/README.md).

## API reference

The module does NOT add new public types beyond the `@Factory` and the
`@ConfigurationProperties` interface. The injected `NotificationFacade`
and `NotificationConfiguration` are pure-library types; see
[`nova-java-notifications/README.md`](../nova-java-notifications/README.md)
for the full API reference.

## Testing

```bash
./gradlew check
```

The module ships with **13 tests** (pure JUnit + `@MicronautTest`):

- **6 unit tests** in `MicronautModuleUnitTest` covering the factory
  methods with a manually-constructed properties object.
- **4 integration tests** in `MicronautExtensionIntegrationTest` that
  boot a real Micronaut `ApplicationContext` via `@MicronautTest` and
  exercise the CDI bean production path (end-to-end send through the
  injected `NotificationFacade`).
- **3 disabled-state tests** in
  `MicronautExtensionDisabledIntegrationTest` that flip
  `nova.notifications.enabled=false` and verify the no-op facade
  contract (every send returns `FAILED` with `ErrorCode.DISABLED`).

## Build

```bash
./gradlew build              # compile + test + jar
./gradlew publishToMavenLocal  # for the demo / other consumers
```

Micronaut types are at `compileOnly` scope (the consumer provides the
runtime). The Micronaut 5.0.4 BOM is the current pin.

### Micronaut test-junit5 version note

`micronaut-test-junit5:5.0.1` is the latest patch available on Maven
Central for the 5.0.x line (the test artifact trails the core
artifact). 5.0.1 is binary-compatible with the 5.0.x core used here
(`micronaut-core:5.0.4`).

## Versioning

- `1.0.0` — initial release aligned with `nova-notifications:1.0.0`.
- Property prefix: `nova.notifications.*` (Nova convention; older
  legacy starters still use `galaxy-training.*` — migration tracked
  in the meta-framework backlog).
- Java 25 toolchain (the workspace's standard pin).
- Micronaut 5.0.4.

## Related

- [`nova-java-notifications`](../nova-java-notifications) — pure library.
- [`nova-java-notifications-spring-boot-starter`](../nova-java-notifications-spring-boot-starter) — Spring Boot auto-config.
- [`nova-java-notifications-quarkus-extension`](../nova-java-notifications-quarkus-extension) — Quarkus colloquial extension.
- [`examples/demo-notifications-micronaut`](../../examples/demo-notifications-micronaut) — example app consuming this module.

---

## AI Assistance Attribution

This work was created through human-AI collaboration. The human author
(Angel Eduardo Hincho Jove, `ahincho@unsa.edu.pe`, UNSA) retains full
responsibility for the final artifact.

**AI tools used**: GitHub Copilot (Claude Opus 4.8, Sonnet 5), MiniMax
(MiniMax-M3 via paid Token Plan), OpenCode (the interactive CLI
harness used to host the session), NotebookLM, Perplexity.
Methodology: OpenSpec spec-driven development.

**Important legal note**: this artifact is **not an "AI system"** under
Article 3(1) of Regulation (EU) 2024/1689 (the EU AI Act). Article 50
transparency obligations therefore do not directly apply. This
disclosure is made voluntarily, aligned with UNESCO Principle 6
(transparency and explainability) and the R-AI requirement of the
originating challenge.

The canonical, full AI-ATTRIBUTION.md (covering the entire Nova
Platform workspace) lives at the workspace root:
[`../../AI-ATTRIBUTION.md`](../../AI-ATTRIBUTION.md).
