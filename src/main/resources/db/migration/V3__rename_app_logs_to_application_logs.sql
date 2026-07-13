-- ERD의 테이블명(application_logs)과 맞춘다.
ALTER TABLE app_logs RENAME TO application_logs;
ALTER TABLE application_logs RENAME INDEX idx_app_logs_created TO idx_application_logs_created;
