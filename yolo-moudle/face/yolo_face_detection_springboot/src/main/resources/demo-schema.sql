DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS imgrecords;
DROP TABLE IF EXISTS videorecords;
DROP TABLE IF EXISTS camerarecords;
DROP TABLE IF EXISTS emotionrecords;
DROP TABLE IF EXISTS awareness_records;
DROP TABLE IF EXISTS healing_conversations;
DROP TABLE IF EXISTS privacy_consents;

CREATE TABLE user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  name VARCHAR(64),
  sex VARCHAR(16),
  email VARCHAR(128),
  tel VARCHAR(32),
  role VARCHAR(32),
  avatar VARCHAR(512),
  time TIMESTAMP
);

CREATE TABLE imgrecords (
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
);

CREATE TABLE videorecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  weight VARCHAR(128),
  input_video VARCHAR(1024),
  out_video VARCHAR(1024),
  conf VARCHAR(64),
  username VARCHAR(64),
  kind VARCHAR(64),
  start_time VARCHAR(64)
);

CREATE TABLE camerarecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  weight VARCHAR(128),
  out_video VARCHAR(1024),
  conf VARCHAR(64),
  username VARCHAR(64),
  kind VARCHAR(64),
  start_time VARCHAR(64)
);

CREATE TABLE emotionrecords (
  id INT AUTO_INCREMENT PRIMARY KEY,
  emotion_kind VARCHAR(64),
  txt VARCHAR(1024),
  start_time TIMESTAMP
);

CREATE TABLE awareness_records (
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
  keep_record BOOLEAN DEFAULT TRUE,
  keep_media BOOLEAN DEFAULT FALSE,
  privacy_note VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE healing_conversations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  provider VARCHAR(64),
  user_message VARCHAR(2048),
  assistant_reply VARCHAR(4096),
  safety_note VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE privacy_consents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  scene VARCHAR(64) NOT NULL,
  consent_type VARCHAR(64) NOT NULL,
  consent_text VARCHAR(1024),
  agreed BOOLEAN DEFAULT FALSE,
  keep_record BOOLEAN DEFAULT FALSE,
  keep_media BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
