package com.spaceres.service;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Google Drive 백업 서비스
 *
 * - 매일 새벽 2시 자동 백업 (Spring Scheduler)
 * - 수동 백업 API 제공
 * - 오래된 백업 파일 자동 정리 (30일 초과)
 * - 사용자 업로드/다운로드 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveBackupService {

    private final Drive driveService;

    @Value("${google.drive.backup-folder-id}")
    private String backupFolderId;

    private static final String MIME_TYPE_CSV  = "text/csv";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final int    MAX_BACKUP_FILES = 30; // 최대 보관 파일 수

    // ── 매일 새벽 2시 자동 백업 ──────────────────────────
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void scheduledBackup() {
        log.info("[자동 백업] 시작: {}", LocalDateTime.now());
        try {
            backupReservationLogs();
            cleanUpOldBackups();
            log.info("[자동 백업] 완료");
        } catch (Exception e) {
            log.error("[자동 백업] 실패: {}", e.getMessage(), e);
        }
    }

    // ── 예약 로그 백업 ────────────────────────────────────
    public String backupReservationLogs() throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "reservation_backup_" + timestamp + ".csv";

        // 실제 환경에서는 DB에서 데이터 조회 후 CSV 생성
        Path tempFile = createSampleCsvFile(fileName);

        String fileId = uploadToDrive(tempFile, fileName, MIME_TYPE_CSV);
        Files.deleteIfExists(tempFile); // 임시 파일 삭제

        log.info("백업 완료: {} (Drive ID: {})", fileName, fileId);
        return fileId;
    }

    // ── 특정 파일 업로드 ──────────────────────────────────
    public String uploadToDrive(Path filePath, String fileName, String mimeType) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(backupFolderId));

        java.io.File javaFile = filePath.toFile();
        FileContent mediaContent = new FileContent(mimeType, javaFile);

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, createdTime")
                .execute();

        log.info("Drive 업로드 완료: {} → ID: {}", fileName, uploadedFile.getId());
        return uploadedFile.getId();
    }

    // ── MultipartFile 업로드 ──────────────────────────────
    public String uploadMultipartFile(MultipartFile multipartFile) throws IOException {
        String fileName = multipartFile.getOriginalFilename();
        String mimeType = multipartFile.getContentType();

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(backupFolderId));

        java.io.File tempFile = java.io.File.createTempFile("upload_", "_" + fileName);
        multipartFile.transferTo(tempFile);

        FileContent mediaContent = new FileContent(mimeType, tempFile);

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, createdTime")
                .execute();

        tempFile.delete();

        log.info("Drive 업로드 완료: {} → ID: {}", fileName, uploadedFile.getId());
        return uploadedFile.getId();
    }

    // ── 파일 다운로드 ─────────────────────────────────────
    public void downloadFile(String fileId, Path destination) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(destination)) {
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            log.info("Drive 다운로드 완료: {} → {}", fileId, destination);
        }
    }

    // ── 백업 파일 목록 조회 ───────────────────────────────
    public List<File> listBackupFiles() throws IOException {
        FileList result = driveService.files().list()
                .setQ("'" + backupFolderId + "' in parents and trashed = false")
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, size, createdTime)")
                .execute();
        return result.getFiles();
    }

    // ── 특정 백업 파일 삭제 ───────────────────────────────
    public void deleteBackupFile(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
        log.info("백업 파일 삭제: {}", fileId);
    }

    // ── 오래된 백업 파일 정리 (30개 초과 시) ─────────────
    public void cleanUpOldBackups() throws IOException {
        List<File> files = listBackupFiles();

        if (files.size() > MAX_BACKUP_FILES) {
            List<File> toDelete = files.subList(MAX_BACKUP_FILES, files.size());
            for (File file : toDelete) {
                deleteBackupFile(file.getId());
                log.info("오래된 백업 삭제: {}", file.getName());
            }
        }
    }

    // ── 샘플 CSV 생성 (실제 환경에서는 DB 쿼리 결과 사용) ─
    private Path createSampleCsvFile(String fileName) throws IOException {
        Path tempFile = Files.createTempFile("backup_", ".csv");
        String csvContent = """
                reservation_id,user_email,space_name,start_time,end_time,status
                1,user1@example.com,회의실A,2024-01-01 09:00,2024-01-01 10:00,CONFIRMED
                2,user2@example.com,회의실B,2024-01-01 11:00,2024-01-01 12:00,CONFIRMED
                """;
        Files.writeString(tempFile, csvContent);
        return tempFile;
    }
}