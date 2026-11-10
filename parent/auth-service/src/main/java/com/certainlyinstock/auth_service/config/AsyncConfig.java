package com.certainlyinstock.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async (used so password-reset response time does not depend on whether the account exists). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
