package pe.edu.nova.java.starters.notifications.micronaut;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.SlackWebhookUrl;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SlackConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsProvider;

/**
 * Micronaut factory that turns {@link NotificationsConfigurationProperties}
 * into the pure-library {@link NotificationConfiguration} and produces a
 * {@link NotificationFacade} bean.
 *
 * <p>This is a "colloquial module" per
 * {@code docs/java/10-micronaut-analisis-integracion.md}: a plain JAR with
 * {@code @Factory} and {@code @Bean} methods, discovered by Micronaut at
 * runtime via its annotation processor. No separate deployment/runtime
 * module is needed.
 */
@Factory
public class NotificationsFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsFactory.class);

    @Bean
    @Singleton
    @Primary
    public NotificationConfiguration notificationConfiguration(
            NotificationsConfigurationProperties properties,
            Optional<pe.edu.nova.java.libs.notifications.application.port.out.NotificationEventPublisherPort> customEventPublisher) {

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .resilience(toResilience(properties.getResilience()));

        customEventPublisher.ifPresent(builder::eventPublisher);

        applyConfigToBuilder(properties, builder);

        return builder.build();
    }

    @Bean
    @Singleton
    public NotificationFacade notificationFacade(
            NotificationsConfigurationProperties properties,
            NotificationConfiguration configuration) {
        if (!properties.isEnabled()) {
            LOGGER.warn("Nova Notifications (Micronaut) is disabled via nova.notifications.enabled=false; "
                    + "producing a no-op facade (every send returns FAILED with ErrorCode.DISABLED).");
            return NotificationFacade.createDisabled();
        }
        return NotificationFacade.create(configuration);
    }

    /**
     * Visible for unit tests: the channel-mapping logic, decoupled from the
     * Micronaut DI lookup of the optional event publisher.
     */
    void applyConfigToBuilder(
            NotificationsConfigurationProperties properties,
            NotificationConfiguration.Builder builder) {
        if (!properties.isEnabled()) {
            // Library is disabled at the starter level: skip all channel
            // assembly. The resilience config (set by the caller before this
            // method is invoked) is kept. The corresponding
            // {@link #notificationFacade} method returns a no-op facade so
            // consumers can still inject {@code NotificationFacade} without
            // a startup {@code ConfigurationException}.
            return;
        }
        NotificationsConfigurationProperties.Email email = properties.getEmail();
        if (email != null && allPresent(email.getProvider(), email.getApiKey(), email.getDefaultSender())) {
            builder.email(EmailConfiguration.builder()
                    .provider(EmailProvider.valueOf(email.getProvider().toUpperCase()))
                    .apiKey(email.getApiKey())
                    .defaultSender(new EmailAddress(email.getDefaultSender()))
                    .build());
        }
        if (properties.getSms() != null
                && allPresent(properties.getSms().getAccountSid(), properties.getSms().getAuthToken(),
                        properties.getSms().getFromNumber())) {
            builder.sms(SmsConfiguration.builder()
                    .provider(SmsProvider.valueOf(properties.getSms().getProvider().toUpperCase()))
                    .accountSid(properties.getSms().getAccountSid())
                    .authToken(properties.getSms().getAuthToken())
                    .fromNumber(properties.getSms().getFromNumber())
                    .build());
        }
        if (properties.getPush() != null
                && allPresent(properties.getPush().getProjectId(), properties.getPush().getServerKey())) {
            builder.push(PushConfiguration.builder()
                    .provider(PushProvider.valueOf(properties.getPush().getProvider().toUpperCase()))
                    .projectId(properties.getPush().getProjectId())
                    .serverKey(properties.getPush().getServerKey())
                    .build());
        }
        if (properties.getSlack() != null
                && properties.getSlack().getDefaultWebhookUrl() != null
                && !properties.getSlack().getDefaultWebhookUrl().isBlank()) {
            builder.slack(SlackConfiguration.builder()
                    .defaultWebhookUrl(SlackWebhookUrl.of(properties.getSlack().getDefaultWebhookUrl()))
                    .build());
        }
    }

    private static ResilienceConfiguration toResilience(NotificationsConfigurationProperties.Resilience r) {
        return new ResilienceConfiguration(
                r.getMaxAttempts(),
                NotificationsConfigurationProperties.durationOfMillis(r.getInitialBackoffMillis()),
                r.getCircuitFailureThreshold(),
                NotificationsConfigurationProperties.durationOfSeconds(r.getCircuitOpenDurationSeconds()),
                r.getRateLimitPermitsPerSecond());
    }

    private static boolean allPresent(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
