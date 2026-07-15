package pe.edu.nova.java.starters.notifications.micronaut;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.model.EmailNotification;
import pe.edu.nova.java.libs.notifications.domain.result.NotificationResult;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.MessageBody;
import pe.edu.nova.java.libs.notifications.domain.vo.Subject;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;

/**
 * Integration test for the Micronaut module when {@code nova.notifications}
 * is enabled (the default).
 *
 * <p>Unlike {@link MicronautModuleUnitTest}, this test boots a real Micronaut
 * {@link ApplicationContext}, exercises the actual {@link NotificationsFactory}
 * CDI bean production, and validates that the property binding
 * ({@code nova.notifications.*}) flows through Micronaut's
 * {@link io.micronaut.context.annotation.ConfigurationProperties} and ends
 * up in a working {@link NotificationFacade}.
 */
@MicronautTest
@Property(name = "nova.notifications.email.provider", value = "sendgrid")
@Property(name = "nova.notifications.email.api-key", value = "test-api-key-micronaut-it")
@Property(name = "nova.notifications.email.default-sender", value = "no-reply@example.com")
@Property(name = "nova.notifications.resilience.max-attempts", value = "1")
class MicronautExtensionIntegrationTest {

    @Inject
    NotificationFacade facade;

    @Inject
    NotificationConfiguration configuration;

    @Test
    void facadeBeanIsProducedFromMicronautContext() {
        assertThat(facade).isNotNull();
    }

    @Test
    void configurationBeanHasEmailChannelFromYmlProperties() {
        assertThat(configuration.email()).isPresent();
        assertThat(configuration.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(configuration.email().get().apiKey()).isEqualTo("test-api-key-micronaut-it");
    }

    @Test
    void otherChannelsAreNotConfigured() {
        assertThat(configuration.sms()).isEmpty();
        assertThat(configuration.push()).isEmpty();
        assertThat(configuration.slack()).isEmpty();
    }

    @Test
    void endToEndSendEmailReturnsSentResult() {
        EmailNotification email = EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("hi"))
                .body(new MessageBody("body"))
                .build();

        NotificationResult result = facade.send(email);

        assertThat(result.isSent()).isTrue();
        assertThat(result.providerMessageId()).isPresent();
    }
}