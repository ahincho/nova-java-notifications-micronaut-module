package pe.edu.nova.java.starters.notifications.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import java.time.Duration;

/**
 * Micronaut configuration properties for the notifications module.
 *
 * <p>Maps the {@code nova.notifications.*} properties from
 * {@code application.yml} or {@code application.properties} — same prefix
 * as the Spring Boot starter and Quarkus extension, following the same
 * convention as the newer Nova starters
 * (e.g. {@code nova-observability-spring-boot-starter} uses
 * {@code nova.observability.*}).
 *
 * <p>The outer class is bound to {@code nova.notifications.*} via the top-level
 * {@code enabled} field. Each nested channel configuration (Email, Sms, Push,
 * Slack, Resilience) is bound to its own relative prefix through its own
 * {@link ConfigurationProperties} annotation. The outer class is still
 * a POJO with default field values so unit tests can instantiate it directly
 * with {@code new NotificationsConfigurationProperties()} and then populate
 * each channel via its {@code setX(...)} methods; Micronaut runtime wires the
 * nested configs via constructor injection (see
 * {@code MicronautExtensionIntegrationTest} in the test source set for the
 * integration test that exercises the runtime path; javadoc references the
 * test class by name to avoid linking to a symbol that is not on the
 * production classpath).
 */
@ConfigurationProperties("nova.notifications")
public class NotificationsConfigurationProperties {

    private boolean enabled = true;
    private Email email = new Email();
    private Sms sms = new Sms();
    private Push push = new Push();
    private Slack slack = new Slack();
    private Resilience resilience = new Resilience();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    public Push getPush() {
        return push;
    }

    public void setPush(Push push) {
        this.push = push;
    }

    public Slack getSlack() {
        return slack;
    }

    public void setSlack(Slack slack) {
        this.slack = slack;
    }

    public Resilience getResilience() {
        return resilience;
    }

    public void setResilience(Resilience resilience) {
        this.resilience = resilience;
    }

    @ConfigurationProperties("email")
    @Introspected
    public static class Email {
        private String provider;
        private String apiKey;
        private String defaultSender;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getDefaultSender() {
            return defaultSender;
        }

        public void setDefaultSender(String defaultSender) {
            this.defaultSender = defaultSender;
        }
    }

    @ConfigurationProperties("sms")
    @Introspected
    public static class Sms {
        private String provider = "twilio";
        private String accountSid;
        private String authToken;
        private String fromNumber;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }
    }

    @ConfigurationProperties("push")
    @Introspected
    public static class Push {
        private String provider = "firebase";
        private String projectId;
        private String serverKey;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getServerKey() {
            return serverKey;
        }

        public void setServerKey(String serverKey) {
            this.serverKey = serverKey;
        }
    }

    @ConfigurationProperties("slack")
    @Introspected
    public static class Slack {
        private String defaultWebhookUrl;

        public String getDefaultWebhookUrl() {
            return defaultWebhookUrl;
        }

        public void setDefaultWebhookUrl(String defaultWebhookUrl) {
            this.defaultWebhookUrl = defaultWebhookUrl;
        }
    }

    @ConfigurationProperties("resilience")
    @Introspected
    public static class Resilience {
        private int maxAttempts = 3;
        private long initialBackoffMillis = 200;
        private int circuitFailureThreshold = 5;
        private long circuitOpenDurationSeconds = 30;
        private int rateLimitPermitsPerSecond = 0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMillis() {
            return initialBackoffMillis;
        }

        public void setInitialBackoffMillis(long initialBackoffMillis) {
            this.initialBackoffMillis = initialBackoffMillis;
        }

        public int getCircuitFailureThreshold() {
            return circuitFailureThreshold;
        }

        public void setCircuitFailureThreshold(int circuitFailureThreshold) {
            this.circuitFailureThreshold = circuitFailureThreshold;
        }

        public long getCircuitOpenDurationSeconds() {
            return circuitOpenDurationSeconds;
        }

        public void setCircuitOpenDurationSeconds(long s) {
            this.circuitOpenDurationSeconds = s;
        }

        public int getRateLimitPermitsPerSecond() {
            return rateLimitPermitsPerSecond;
        }

        public void setRateLimitPermitsPerSecond(int rateLimitPermitsPerSecond) {
            this.rateLimitPermitsPerSecond = rateLimitPermitsPerSecond;
        }
    }

    /** Hidden helper to convert millis to a {@link Duration} (used by the factory). */
    public static Duration durationOfMillis(long millis) {
        return Duration.ofMillis(millis);
    }

    /** Hidden helper to convert seconds to a {@link Duration} (used by the factory). */
    public static Duration durationOfSeconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }
}