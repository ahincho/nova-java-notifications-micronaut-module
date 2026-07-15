package pe.edu.nova.java.starters.notifications.micronaut;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.error.ErrorCode;
import pe.edu.nova.java.libs.notifications.domain.model.EmailNotification;
import pe.edu.nova.java.libs.notifications.domain.result.NotificationResult;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.MessageBody;
import pe.edu.nova.java.libs.notifications.domain.vo.Subject;

/**
 * Integration test for the Micronaut module when
 * {@code nova.notifications.enabled=false}.
 *
 * <p>Boots a real Micronaut {@link ApplicationContext} with the master switch
 * flipped off and verifies that the no-op facade production path works
 * end-to-end through real Micronaut DI. The unit test in
 * {@link MicronautModuleUnitTest} only exercises the factory methods directly,
 * so this integration coverage is the real safety net for the disabled-state
 * wiring.
 */
@MicronautTest
@Property(name = "nova.notifications.enabled", value = "false")
class MicronautExtensionDisabledIntegrationTest {

    @Inject
    NotificationFacade facade;

    @Test
    void facadeBeanIsStillInjectedWhenDisabled() {
        assertThat(facade).isNotNull();
    }

    @Test
    void everySendReturnsFailedWithDisabledErrorCode() {
        EmailNotification email = EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("hi"))
                .body(new MessageBody("body"))
                .build();

        NotificationResult result = facade.send(email);

        assertThat(result.isSent()).isFalse();
        assertThat(result.errorCode()).contains(ErrorCode.DISABLED);
    }
}