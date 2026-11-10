package com.certainlyinstock.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Typed settings served by the config server (see config-repo/auth-service.properties). */
@Configuration
@EnableConfigurationProperties({ ClientProperties.Keycloak.class, ClientProperties.UserService.class })
public class ClientProperties {

    /** Keycloak admin access: the auth-service client uses the client-credentials grant. */
    @ConfigurationProperties(prefix = "keycloak")
    public record Keycloak(String baseUrl, String realm, String clientId, String clientSecret) {

        /** Keep the client secret out of logs. */
        @Override
        public String toString() {
            return "Keycloak[baseUrl=" + baseUrl + ", realm=" + realm + ", clientId=" + clientId + "]";
        }
    }

    @ConfigurationProperties(prefix = "user-service")
    public record UserService(String baseUrl) {
    }
}
