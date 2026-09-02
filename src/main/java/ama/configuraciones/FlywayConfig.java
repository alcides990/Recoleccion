package ama.configuraciones;

import java.nio.charset.Charset;
import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion compatible entre Spring Boot 3.0 y Flyway moderno.
 *
 * Spring Boot 3.0 incorpora una autoconfiguracion para Flyway 9 que invoca una
 * API eliminada en versiones posteriores. Esta configuracion mantiene el
 * inicializador de Spring (y por lo tanto la migracion antes de JPA), pero
 * construye Flyway con su API actual para poder trabajar con MySQL 8.4.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Bean
    Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:0}") String baselineVersion,
            @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate,
            @Value("${spring.flyway.clean-disabled:true}") boolean cleanDisabled,
            @Value("${spring.flyway.encoding:UTF-8}") String encoding,
            @Value("${spring.flyway.ignore-migration-patterns:*:future}") String ignoreMigrationPatterns) {

        String[] ubicaciones = Arrays.stream(locations.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .toArray(String[]::new);
        String[] patronesIgnorados = Arrays.stream(ignoreMigrationPatterns.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .toArray(String[]::new);

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(ubicaciones)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .validateOnMigrate(validateOnMigrate)
                .cleanDisabled(cleanDisabled)
                .encoding(Charset.forName(encoding))
                .ignoreMigrationPatterns(patronesIgnorados)
                .load();
    }

    @Bean
    FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }
}
