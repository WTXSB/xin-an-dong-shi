CREATE TABLE IF NOT EXISTS user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  name VARCHAR(64),
  sex VARCHAR(16),
  email VARCHAR(128),
  tel VARCHAR(32),
  role VARCHAR(32),
  avatar VARCHAR(512),
  time TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS imgrecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  weight VARCHAR(128),
  input_img VARCHAR(1024),
  out_img VARCHAR(1024),
  confidence VARCHAR(512),
  all_time VARCHAR(64),
  conf VARCHAR(64),
  label VARCHAR(512),
  username VARCHAR(64),
  kind VARCHAR(64),
  start_time VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS videorecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  weight VARCHAR(128),
  input_video VARCHAR(1024),
  out_video VARCHAR(1024),
  conf VARCHAR(64),
  username VARCHAR(64),
  kind VARCHAR(64),
  start_time VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS camerarecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  weight VARCHAR(128),
  out_video VARCHAR(1024),
  conf VARCHAR(64),
  username VARCHAR(64),
  kind VARCHAR(64),
  start_time VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS emotionrecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  emotion_kind VARCHAR(64),
  txt VARCHAR(1024),
  start_time TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS awareness_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  source_type VARCHAR(32) NOT NULL,
  emotion_label VARCHAR(64),
  confidence VARCHAR(64),
  body_signal VARCHAR(255),
  gentle_summary VARCHAR(1024),
  suggested_practice VARCHAR(1024),
  input_media VARCHAR(1024),
  output_media VARCHAR(1024),
  keep_record TINYINT(1) DEFAULT 1,
  keep_media TINYINT(1) DEFAULT 0,
  privacy_note VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_awareness_username_created (username, created_at),
  INDEX idx_awareness_source_created (source_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detection_analysis_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL UNIQUE,
  awareness_record_id INT NULL,
  username VARCHAR(64),
  source_type VARCHAR(32) NOT NULL,
  analysis_mode VARCHAR(32) NOT NULL,
  model_configuration VARCHAR(512),
  emotion_type_count INT DEFAULT 0,
  bfrb_event_count INT DEFAULT 0,
  bfrb_total_duration_seconds DECIMAL(12, 3) DEFAULT 0,
  evidence_types VARCHAR(255),
  complaint VARCHAR(2048),
  additional_notes VARCHAR(2048),
  aggregation_rules_json LONGTEXT,
  input_media VARCHAR(1024),
  output_media VARCHAR(1024),
  keep_record TINYINT(1) DEFAULT 1,
  keep_media TINYINT(1) DEFAULT 0,
  detected_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_detection_username_created (username, created_at),
  INDEX idx_detection_awareness (awareness_record_id),
  INDEX idx_detection_source_created (source_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detection_emotion_results (
  id INT AUTO_INCREMENT PRIMARY KEY,
  analysis_record_id INT NOT NULL,
  emotion_type VARCHAR(64) NOT NULL,
  frame_count INT DEFAULT 0,
  average_confidence DECIMAL(8, 6) DEFAULT 0,
  max_confidence DECIMAL(8, 6) DEFAULT 0,
  INDEX idx_detection_emotion_record (analysis_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detection_bfrb_events (
  id INT AUTO_INCREMENT PRIMARY KEY,
  analysis_record_id INT NOT NULL,
  event_key VARCHAR(64),
  behavior_code VARCHAR(128),
  cue_type VARCHAR(128),
  start_seconds DECIMAL(12, 3) DEFAULT 0,
  end_seconds DECIMAL(12, 3) DEFAULT 0,
  duration_seconds DECIMAL(12, 3) DEFAULT 0,
  sample_count INT DEFAULT 0,
  average_confidence DECIMAL(8, 6) DEFAULT 0,
  max_confidence DECIMAL(8, 6) DEFAULT 0,
  key_frame_seconds DECIMAL(12, 3) DEFAULT 0,
  evidence_type VARCHAR(128),
  key_bbox_json VARCHAR(1024),
  geometry_json LONGTEXT,
  INDEX idx_detection_bfrb_record (analysis_record_id),
  INDEX idx_detection_bfrb_behavior (behavior_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS healing_conversations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  provider VARCHAR(64),
  user_message VARCHAR(2048),
  assistant_reply VARCHAR(4096),
  safety_note VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_healing_username_created (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS previsit_report_summaries (
  id INT AUTO_INCREMENT PRIMARY KEY,
  awareness_record_id INT NOT NULL UNIQUE,
  analysis_record_id INT NOT NULL,
  provider VARCHAR(64) NOT NULL,
  model_name VARCHAR(128),
  prompt_version VARCHAR(64) NOT NULL,
  objective_summary TEXT,
  clinician_questions_json TEXT,
  visit_preparation_json TEXT,
  safety_note VARCHAR(1024),
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_previsit_summary_analysis (analysis_record_id),
  INDEX idx_previsit_summary_generated (generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS care_letter_summaries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  awareness_record_id BIGINT NOT NULL UNIQUE,
  analysis_record_id BIGINT,
  provider VARCHAR(32),
  model_name VARCHAR(64),
  prompt_version VARCHAR(32),
  letter_text TEXT,
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_care_letter_analysis (analysis_record_id),
  INDEX idx_care_letter_generated (generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spa_identities (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  identity_type VARCHAR(16) NOT NULL,
  real_name VARCHAR(64),
  id_card_masked VARCHAR(32),
  license_no VARCHAR(64),
  hospital VARCHAR(128),
  department VARCHAR(128),
  title VARCHAR(64),
  bio VARCHAR(1024),
  audit_status VARCHAR(16) DEFAULT 'pending',
  audit_note VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_spa_identity_audit (identity_type, audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spa_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_username VARCHAR(64) NOT NULL,
  practitioner_username VARCHAR(64) NOT NULL,
  initial_message VARCHAR(1024),
  status VARCHAR(16) DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_spa_request_patient (patient_username, created_at),
  INDEX idx_spa_request_practitioner (practitioner_username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spa_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  sender_username VARCHAR(64) NOT NULL,
  sender_identity VARCHAR(16),
  content VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_spa_message_request (request_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS privacy_consents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  scene VARCHAR(64) NOT NULL,
  consent_type VARCHAR(64) NOT NULL,
  consent_text VARCHAR(1024),
  agreed TINYINT(1) DEFAULT 0,
  keep_record TINYINT(1) DEFAULT 0,
  keep_media TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_privacy_username_created (username, created_at),
  INDEX idx_privacy_scene_created (scene, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
