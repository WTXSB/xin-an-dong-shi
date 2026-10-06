-- Apply once to an existing production MySQL database before deploying the diary feature.
-- Demo/test H2 uses demo-schema.sql automatically. No existing data is deleted.
CREATE TABLE IF NOT EXISTS diary_moods (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  entry_date VARCHAR(10) NOT NULL,
  period VARCHAR(16) NOT NULL,
  score INT,
  prompt_shown BOOLEAN DEFAULT FALSE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (username, entry_date, period)
);
CREATE TABLE IF NOT EXISTS emotion_diaries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  entry_date VARCHAR(10) NOT NULL,
  content VARCHAR(10000) NOT NULL DEFAULT '',
  revision INT NOT NULL DEFAULT 0,
  analysis_json TEXT,
  analysis_fingerprint VARCHAR(64),
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (username, entry_date)
);
