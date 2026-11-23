package com.autotuning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Configuration
public class CryptoConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(preHacher(rawPassword));
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return bcrypt.matches(preHacher(rawPassword), encodedPassword)
                        || bcrypt.matches(rawPassword, encodedPassword);
            }

            @Override
            public boolean upgradeEncoding(String encodedPassword) {
                return bcrypt.upgradeEncoding(encodedPassword);
            }

            private String preHacher(CharSequence rawPassword) {
                Objects.requireNonNull(rawPassword, "Le mot de passe ne peut pas être nul");
                try {
                    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8)));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("SHA-256 n'est pas disponible", e);
                }
            }
        };
    }
}
