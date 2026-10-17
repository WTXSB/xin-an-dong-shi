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

-- 「心有灵犀」示例服务者账号（演示环境，非真实资质）
INSERT INTO user (username, password, name, sex, email, tel, role, avatar, time)
SELECT 'doctor.lin', '123456', '林安宁（演示）', '女', 'doctor.lin@mindease.local', '00000000000', 'common', '', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'doctor.lin');

-- 已完成模拟审核的示例服务者档案，不能作为真实身份或资质证明
INSERT INTO spa_identities (username, identity_type, real_name, license_no, hospital, department, title, bio, audit_status, audit_note, created_at, updated_at)
SELECT 'doctor.lin', 'practitioner', '林安宁（演示）', 'DEMO-NOT-A-CREDENTIAL', '心安情绪支持站（演示）', '情绪陪伴组', '陪伴服务者（演示）',
'这是用于体验协同支持流程的演示账号，不代表真实服务者或有效服务资质。',
'approved', '仅为演示审核，不代表真实资质认证', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM spa_identities WHERE username = 'doctor.lin');

-- Only update untouched legacy demo fixtures; never rewrite user-created profiles.
UPDATE user SET name = '林安宁（演示）'
WHERE username = 'doctor.lin' AND name = '林医生';
UPDATE spa_identities SET real_name = '林安宁（演示）', license_no = 'DEMO-NOT-A-CREDENTIAL',
hospital = '心安情绪支持站（演示）', department = '情绪陪伴组', title = '陪伴服务者（演示）',
bio = '这是用于体验协同支持流程的演示账号，不代表真实服务者或有效服务资质。',
audit_note = '仅为演示审核，不代表真实资质认证'
WHERE username = 'doctor.lin' AND real_name = '林安宁' AND license_no = '鄂2023-XA-0186'
AND hospital = '武汉心安医院' AND department = '临床心理科' AND title = '主治医师'
AND bio = '从事情绪陪伴与压力疏导工作多年，习惯先安静地倾听，再陪你一起把心事慢慢理清。愿这里成为你可以安心停靠的小小港湾。'
AND audit_status = 'approved' AND audit_note = '示例档案已完成认证';
