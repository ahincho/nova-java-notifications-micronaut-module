package pe.edu.nova.java.starters.notifications.micronaut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.error.ConfigurationException;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;

/**
 * Unit tests for the Micronaut module. Per the Nova convention (see
 * docs/java/10-micronaut-analisis-integracion.md), this module ships
 * with pure JUnit tests only; the full @MicronautTest integration coverage
 * lives in the examples/ module that consumes it.
 */
class MicronautModuleUnitTest {

    private final NotificationsFactory factory = new NotificationsFactory();

    @Test
    void factoryIsInstantiableAndHasNoRequiredDependencies() {
        assertThat(factory).isNotNull();
    }

    @Test
    void applyConfigToBuilderMapsEmailChannelFromProperties() {
        NotificationsConfigurationProperties properties = new NotificationsConfigurationProperties();
        properties.getEmail().setProvider("sendgrid");
        properties.getEmail().setApiKey("test-api-key");
        properties.getEmail().setDefaultSender("no-reply@example.com");

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        factory.applyConfigToBuilder(properties, builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    @Test
    void applyConfigToBuilderMapsAllFourChannelsIndependently() {
        NotificationsConfigurationProperties properties = new NotificationsConfigurationProperties();
        properties.getEmail().setProvider("mailgun");
        properties.getEmail().setApiKey("key-1");
        properties.getEmail().setDefaultSender("noreply@example.com");
        properties.getSms().setProvider("twilio");
        properties.getSms().setAccountSid("AC1");
        properties.getSms().setAuthToken("token-1");
        properties.getSms().setFromNumber("+15005550006");
        properties.getPush().setProvider("firebase");
        properties.getPush().setProjectId("project-1");
        properties.getPush().setServerKey("server-key-1");
        properties.getSlack().setDefaultWebhookUrl("https://hooks.slack.com/services/T0/B0/secret");

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        factory.applyConfigToBuilder(properties, builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.sms()).isPresent();
        assertThat(built.push()).isPresent();
        assertThat(built.slack()).isPresent();
    }

    @Test
    void applyConfigToBuilderSkipsChannelsWithMissingFields() {
        NotificationsConfigurationProperties properties = new NotificationsConfigurationProperties();
        properties.getEmail().setProvider(null);
        properties.getEmail().setApiKey("key");
        properties.getEmail().setDefaultSender("noreply@example.com");

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        factory.applyConfigToBuilder(properties, builder);
        NotificationConfiguration built = builder.build();

        // null provider → email channel must NOT be configured.
        assertThat(built.email()).isEmpty();
    }

    @Test
    void facadeThrowsConfigurationExceptionWhenNoChannelIsConfigured() {
        NotificationConfiguration emptyConfiguration = NotificationConfiguration.builder().build();
        assertThatThrownBy(() -> NotificationFacade.create(emptyConfiguration))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void resilienceConfigurationBuilderProducesValidLibraryConfiguration() {
        ResilienceConfiguration resilience = new ResilienceConfiguration(
                3,
                java.time.Duration.ofMillis(200),
                5,
                java.time.Duration.ofSeconds(30),
                0);
        assertThat(resilience.maxAttempts()).isEqualTo(3);
        assertThat(resilience.retryEnabled()).isTrue();
        assertThat(resilience.rateLimitEnabled()).isFalse();
    }
}
