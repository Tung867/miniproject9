package com.spaceres.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Collections;

@Slf4j
@Configuration
public class GoogleDriveServiceAccountConfig {

    @Value("${google.drive.credentials-file}")
    private Resource credentialsFile;

    @Value("${google.drive.application-name}")
    private String applicationName;

    @Bean
    public Drive googleDriveService() throws Exception {
        // Load Service Account JSON
        try (InputStream inputStream = credentialsFile.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList(DriveScopes.DRIVE_FILE));

            // Tạo Drive service với Service Account
            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName(applicationName).build();
        }
    }
}
