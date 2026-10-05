INSERT INTO user (username, password, name, sex, email, tel, role, avatar, time)
SELECT 'demo', '123456', '体验者', '未知', 'demo@mindease.local', '00000000000', 'admin', 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'demo');

INSERT INTO emotionrecords (emotion_kind, txt, start_time)
SELECT '平稳', '今天愿意停下来观察自己，就是一个温柔的开始。', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM emotionrecords WHERE txt = '今天愿意停下来观察自己，就是一个温柔的开始。');

INSERT INTO emotionrecords (emotion_kind, txt, start_time)
SELECT '明亮', '把轻松的时刻记下来，它会成为下一次继续的力量。', DATEADD('DAY', -1, CURRENT_TIMESTAMP)
WHERE NOT EXISTS (SELECT 1 FROM emotionrecords WHERE txt = '把轻松的时刻记下来，它会成为下一次继续的力量。');

INSERT INTO emotionrecords (emotion_kind, txt, start_time)
SELECT '低落', '低落不是退步，只是身心在提醒你需要被照顾。', DATEADD('DAY', -2, CURRENT_TIMESTAMP)
WHERE NOT EXISTS (SELECT 1 FROM emotionrecords WHERE txt = '低落不是退步，只是身心在提醒你需要被照顾。');

-- 「心有灵犀」示例心灵SPA师账号（演示环境）
INSERT INTO user (username, password, name, sex, email, tel, role, avatar, time)
SELECT 'doctor.lin', '123456', '林医生', '女', 'doctor.lin@mindease.local', '00000000000', 'common', '', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'doctor.lin');

-- 已完成认证的示例心灵SPA师档案
INSERT INTO spa_identities (username, identity_type, real_name, license_no, hospital, department, title, bio, audit_status, audit_note, created_at, updated_at)
SELECT 'doctor.lin', 'practitioner', '林安宁', '鄂2023-XA-0186', '武汉心安医院', '临床心理科', '主治医师',
'从事情绪陪伴与压力疏导工作多年，习惯先安静地倾听，再陪你一起把心事慢慢理清。愿这里成为你可以安心停靠的小小港湾。',
'approved', '示例档案已完成认证', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM spa_identities WHERE username = 'doctor.lin');
