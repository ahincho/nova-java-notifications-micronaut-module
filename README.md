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

## What was built for this technical challenge

This repository is the **Micronaut 5 adapter** (Nivel 2) of the Nova
Platform notifications module. It exposes the pure library as a
Micronaut "colloquial" module (`@Factory` + `@ConfigurationProperties`),
ready to inject anywhere in a Micronaut 5 application.

### Role in the Nova Platform

- **Nivel**: 2 (framework adapter).
- **Depends on**: `pe.edu.nova.java.libs:nova-notifications:1.0.0`
  (Nivel 1, in the sibling repo).
- **Consumed by**: `examples/demo-notifications-micronaut` (Nivel 3,
  in `examples/`).

### What this repository delivers

- **Micronaut 5.0.4 "colloquial" module**: no deployment/runtime
  split, plain `@Factory` / `@ConfigurationProperties` /
  `@EachProperty` beans. Micronaut auto-discovers the beans via its
  AOT-time bean introspection.
- **`@Factory`-exposed `NotificationFacade` bean** wired by
  `NotificationsFactory`, with one method per library type.
- **`@ConfigurationProperties("nova.notifications")` binding** with
  nested `@ConfigurationProperties` per channel (Micronaut 5's
  idiom for nested config).
- **Micronaut AOT compatibility**: the module compiles cleanly with
  the Micronaut AOT processor; the demo uses
  `id("com.gradleup.shadow")` to produce a fat JAR.
- **`nova.notifications.enabled=false` no-op facade** — the factory
  returns `NotificationFacade.createDisabled()`.

### Quality gates verified

- **Gradle build + checkstyle + tests = green** in the CI/CD pipeline.
- **Published to GitHub Packages** with full sources + javadoc jars:
  `pe.edu.nova.java.starters:nova-java-notifications-micronaut-module:1.0.0`.

### How to reproduce the build

```bash
./gradlew clean build           # compile + test + jar
./gradlew publishToMavenLocal   # for the demo and other consumers
```

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

This repository was produced through human-AI collaboration. The human
author (Angel Eduardo Hincho Jove, `ahincho@unsa.edu.pe`, Universidad
Nacional de San Agustín de Arequipa — UNSA) retains full responsibility
for the final artifact and for every commit accepted into the repository.

### Challenge context

This work was produced in response to the technical challenge
described in `Reto-Tecnico-Backend.pdf`. Section 2.5 of the challenge
mandates an explicit AI disclosure in the README when AI is used. This
section fulfils that requirement (R-AI / **R**esponsible **AI**
disclosure) and is also aligned with:

- **Regulation (EU) 2024/1689** ("EU AI Act"), Article 3(1)
  (definition of "AI system") and Article 50 (transparency obligations
  for deployers of certain AI systems).
- **UNESCO Recommendation on the Ethics of Artificial Intelligence**
  (2021), adopted by 193 Member States, **Principle 6: Transparency and
  explainability**.

### AI tools used in this repository

| Tool | Provider | Model / Role | Access tier |
|---|---|---|---|
| GitHub Copilot | GitHub / Anthropic | Claude Opus 4.8, Claude Sonnet 5 (in-editor suggestions) | Licensed |
| MiniMax Token Plan | MiniMax | MiniMax-M3 (the model used for long-form generation and refactoring in the OpenCode session) | Paid (personal) |
| OpenCode | anomalyco (`opencode.ai`) | Interactive CLI harness — **not a model**, only the session/UI | Free (CLI) |
| OpenSpec | Fission AI | Spec-driven development framework (used for the meta-framework backlog) | Licensed |
| NotebookLM | Google | Gemini (cross-document synthesis of the challenge PDF and ADRs) | Free |
| Perplexity | Perplexity AI | Sonar / Pro Search (lookup of latest framework versions and release dates) | Free |

> *Important distinction*: OpenCode is the interactive CLI harness in
> which the AI-assisted development session took place (with MiniMax-M3
> as the underlying model). OpenCode is **not a model** and **not a
> license/subscription manager** — the subscription providing access to
> the model is the **MiniMax Token Plan** listed above. The two rows
> are kept deliberately separate so that anyone reading the disclosure
> can identify exactly which entity provides the model and which entity
> provides the session/UI.

### Scope of AI assistance in this repository

- Drafting the **initial code skeletons** (sealed interfaces, value
  objects, port interfaces, provider stubs).
- Drafting **unit tests** for value objects, the error hierarchy, the
  template resolver, the rate-limiter, the circuit-breaker state
  machine, and the i18n message bundle (Spanish / English).
- **Documentation drafts** of this README and the inline Javadoc.
- **Build infrastructure** snippets for the reusable CI/CD workflows
  in `ahincho/nova-devops`.
- **Cross-checking** the published provider documentation (SendGrid,
  Mailgun, Twilio, Firebase) for the authentication-header / payload
  shape used in the per-provider adapters (no live API calls are made).

### Human contributions (author: Angel Eduardo Hincho Jove)

The following decisions and artifacts are **authored and approved by
the human**, not delegated to AI:

- **Architecture**: hexagonal / ports-and-adapters layout, framework
  isolation in the core library, the five-level meta-framework
  (Nivel 1 = pure library, Nivel 2 = starter/extension,
  Nivel 3 = application) per `ADR-001` and `ADR-015`.
- **Scope**: which channels and providers are in scope for the
  challenge (Email / SMS / Push mandatory, Slack optional) and which
  features are deferred.
- **Version pinning**: Java 25, Spring Boot 4.1.0, Quarkus 3.33.2.1
  LTS, Micronaut 5.0.4, Gradle 9.5.1, Maven 3.9.x. Each pin was
  cross-checked against the latest stable release and the framework
  vendor's LTS roadmap.
- **Quality gates**: 80 % JaCoCo coverage, Checkstyle Nova style,
  ArchUnit test enforcing zero framework leakage in the core library.
- **Build infrastructure**: Maven for the core (T-02 of the challenge
  mandates Maven), Gradle 9.5.1 for the framework starters and demos
  (consistency with the rest of the Nova Platform).
- **Final review and approval** of every commit, including a final
  end-to-end run of `./mvnw verify` and `./gradlew build` against
  JDK 25 before tagging the release.
- **Legal classification**: the determination that the artifacts
  shipped here (a deterministic Java library + framework adapters +
  example apps) are **not "AI systems"** under EU AI Act Article 3(1)
  and therefore do not directly attract Article 50 obligations (see
  the legal clarification below).

### Methodology

The work followed a **Spec-Driven Development** approach using
OpenSpec:

1. Requirements were captured as structured specifications before any
   code was written (`CHALLENGE.md`, `REQUIREMENTS.md`, the ADRs).
2. AI assistance operated against those specifications, not in the
   abstract.
3. The human author reviewed and approved each artifact (build, test,
   commit) before it was accepted into the repository.

### Legal clarification (EU AI Act)

A deterministic Java notifications library does not "infer" outputs,
does not generate predictions / recommendations / decisions, and does
not exhibit autonomy or adaptiveness after deployment. Therefore the
artifacts shipped in this repository are **not "AI systems"** within
the meaning of Article 3(1) of Regulation (EU) 2024/1689 (the EU AI
Act), and Article 50 does not directly impose obligations on them.

This disclosure is nevertheless made:

- **By contractual / academic requirement**: per the R-AI requirement
  of the originating technical challenge (challenge PDF §2.5).
- **Voluntarily**, in alignment with the spirit of the EU AI Act
  transparency principles and UNESCO Principle 6.
- **In the interest of authorship transparency** for the open-source
  community.

### Canonical disclosure

The full Nova Platform AI attribution (covering every repository in
the workspace — pure libraries, framework adapters, demos, tooling
and documentation) lives in a single canonical file at the workspace
root:

[`../../AI-ATTRIBUTION.md`](../../AI-ATTRIBUTION.md)

This per-repository section is a compact summary that points back to
that canonical file as the source of truth for the full disclosure;
the legal analysis and the human-contributions audit are not
duplicated in every repository on purpose.

### Change log

- **2026-07-15** — Initial disclosure created.