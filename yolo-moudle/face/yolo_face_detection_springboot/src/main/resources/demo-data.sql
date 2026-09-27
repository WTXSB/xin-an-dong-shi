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
