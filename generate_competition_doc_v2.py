"""
生成「心安动识」初赛作品说明文档 V2
- 内容精炼，贴近模板字数要求
- 复用原PDF美化的品牌背景
- 专业排版
"""
import docx
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import parse_xml, OxmlElement
import copy
import os

# ============================================================
# 创建文档
# ============================================================
doc = Document()

# ============================================================
# 页面设置
# ============================================================
for section in doc.sections:
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.0)
    section.left_margin = Cm(2.8)
    section.right_margin = Cm(2.8)

# ============================================================
# 定义颜色常量 (品牌色)
# ============================================================
DARK_GREEN = RGBColor(0x17, 0x3D, 0x36)
TEAL = RGBColor(0x2F, 0x9F, 0x91)
DEEP_BLUE = RGBColor(0x2C, 0x5F, 0x8A)
CALM_BLUE = RGBColor(0x4A, 0x90, 0xD9)
WARM_ORANGE = RGBColor(0xF5, 0xA6, 0x23)
BODY_TEXT = RGBColor(0x33, 0x33, 0x33)
MUTED_TEXT = RGBColor(0x99, 0x99, 0x99)
HEALING_GREEN = RGBColor(0x7B, 0xC8, 0xA4)

# ============================================================
# 定义样式
# ============================================================
# Normal
style = doc.styles['Normal']
font = style.font
font.name = '微软雅黑'
font.size = Pt(10.5)
font.color.rgb = BODY_TEXT
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
style.paragraph_format.line_spacing = 1.6
style.paragraph_format.space_after = Pt(4)
style.paragraph_format.first_line_indent = Cm(0.7)

# Heading 1 - 大题号
h1 = doc.styles['Heading 1']
h1.font.name = '微软雅黑'
h1.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
h1.font.size = Pt(18)
h1.font.color.rgb = DARK_GREEN
h1.font.bold = True
h1.paragraph_format.space_before = Pt(24)
h1.paragraph_format.space_after = Pt(12)
h1.paragraph_format.first_line_indent = Cm(0)

# Heading 2 - 二级标题
h2 = doc.styles['Heading 2']
h2.font.name = '微软雅黑'
h2.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
h2.font.size = Pt(13)
h2.font.color.rgb = DEEP_BLUE
h2.font.bold = True
h2.paragraph_format.space_before = Pt(16)
h2.paragraph_format.space_after = Pt(8)
h2.paragraph_format.first_line_indent = Cm(0)

# ============================================================
# 辅助函数
# ============================================================
def add_centered_text(doc, text, size=12, color=BODY_TEXT, bold=False, italic=False, space_after=6):
    """添加居中文本"""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_after = Pt(space_after)
    run = p.add_run(text)
    run.font.size = Pt(size)
    run.font.color.rgb = color
    run.font.bold = bold
    run.font.italic = italic
    run.font.name = '微软雅黑'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    return p

def add_body_text(doc, text):
    """添加正文段落"""
    p = doc.add_paragraph(text)
    return p

def add_colored_line(doc, color=TEAL, width=Cm(4), height=Pt(2)):
    """添加装饰线"""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(6)
    # 添加形状
    run = p.add_run('─' * 30)
    run.font.size = Pt(8)
    run.font.color.rgb = color
    return p

def add_section_heading(doc, number, title):
    """添加带序号的章节标题"""
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_before = Pt(30)
    p.paragraph_format.space_after = Pt(10)
    
    # 序号圆圈
    run_num = p.add_run(f' {number} ')
    run_num.font.size = Pt(16)
    run_num.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
    run_num.font.bold = True
    run_num.font.name = '微软雅黑'
    run_num._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    
    # 标题
    run_title = p.add_run(f'  {title}')
    run_title.font.size = Pt(16)
    run_title.font.color.rgb = DARK_GREEN
    run_title.font.bold = True
    run_title.font.name = '微软雅黑'
    run_title._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    
    # 下划线
    p2 = doc.add_paragraph()
    p2.paragraph_format.first_line_indent = Cm(0)
    p2.paragraph_format.space_after = Pt(12)
    run_line = p2.add_run('─' * 50)
    run_line.font.size = Pt(6)
    run_line.font.color.rgb = TEAL
    
    return p

# ============================================================
# 设置页面背景色 (浅蓝绿渐变无法直接实现，用纯色背景代替)
# ============================================================
def set_page_background(doc, hex_color='EDF8F2'):
    """设置所有节的页面背景色"""
    for section in doc.sections:
        sectPr = section._sectPr
        if sectPr is None:
            continue
        
        # 创建背景元素
        background = OxmlElement('w:background')
        background.set(qn('w:color'), hex_color)
        background.set(qn('w:themeColor'), 'background2')
        background.set(qn('w:shade'), '05')
        
        # 插入到sectPr中
        sectPr.insert(0, background)

try:
    set_page_background(doc, 'EDF8F2')  # 浅绿背景
except Exception as e:
    print(f'背景色设置失败: {e}')

# ============================================================
# 封面页
# ============================================================
for _ in range(5):
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_after = Pt(0)

# 大赛名称
add_centered_text(doc, '第十一届（2026年）中国高校计算机大赛', 
                  size=14, color=DEEP_BLUE, space_after=4)
add_centered_text(doc, '—— 移动应用创新赛 ——', 
                  size=11, color=CALM_BLUE, space_after=30)

# Logo占位 / 品牌标识
add_centered_text(doc, '🌸', size=48, color=TEAL, space_after=16)

# 作品名称
add_centered_text(doc, '心安动识', size=38, color=DARK_GREEN, bold=True, space_after=4)
add_centered_text(doc, 'MindEase', size=18, color=TEAL, space_after=16)

# 副标题
add_centered_text(doc, '基于YOLO的多智能体协作情绪识别与心灵疗愈平台', 
                  size=13, color=DEEP_BLUE, space_after=40)

# 装饰线
add_colored_line(doc, TEAL)

# Slogan
add_centered_text(doc, '「 看见焦虑，遇见安宁 」', 
                  size=15, color=WARM_ORANGE, italic=True, space_after=40)

add_colored_line(doc, TEAL)

# 留白
for _ in range(6):
    add_centered_text(doc, '', size=8, color=BODY_TEXT, space_after=0)

# 日期
add_centered_text(doc, '2026年6月', size=11, color=MUTED_TEXT)

# ============================================================
# 分页 - 正文开始
# ============================================================
doc.add_page_break()

# ============================================================
# 正文
# ============================================================

# ---- 1. 问题背景与用户分析 ----
add_section_heading(doc, '一', '问题背景与用户分析')

content_1 = (
    '当前，焦虑已成为影响全民心理健康的核心议题。《中国国民心理健康发展报告（2023-2024）》'
    '显示，我国成年人焦虑障碍终生患病率约7.6%，青少年焦虑情绪检出率高达24.6%。焦虑往往不以'
    '言语表达，而是通过躯体化动作无意识呈现——咬指甲、拔头发、抓挠皮肤、抖腿、反复搓手等身体'
    '聚焦重复行为（BFRBs），这些行为既是焦虑的外在表征，又反向加重心理负担，形成恶性循环。'
)
add_body_text(doc, content_1)

content_1b = (
    '当前痛点集中：个人层面，超76%用户存在至少一种无意识焦虑动作，但不自知、羞于求助、缺乏'
    '科学工具；临床层面，医生依赖主观观察与患者自我报告，缺乏客观量化手段，患者面对医生时'
    '刻意克制导致数据失真；市场层面，国内尚无集"精准识别—智能分析—心理干预—持续疗愈"于一体'
    '的全流程产品。本项目目标用户覆盖12-25岁青少年、25-45岁职场人群、6-12岁儿童、60岁以上'
    '老年群体及学校、企业、临床机构等B端用户，市场需求真实且迫切。'
)
add_body_text(doc, content_1b)

# ---- 2. 相关竞品分析 ----
add_section_heading(doc, '二', '相关竞品分析')

content_2 = (
    '第一类，智能穿戴设备：以美国HabitAware Keen手环为代表，采用单一IMU传感器+振动提醒，'
    '虽为首创BFRB可穿戴产品，但仅能检测单一行为（如拔头发），无视觉识别、无疗愈功能，用户'
    '"被提醒但不知怎么办"。第二类，可穿戴贴片：如Spire Health Tag，仅监测呼吸心率，无法'
    '识别具体行为类别，产品已停更。第三类，AI心理应用：Woebot仅有NLP对话无行为识别；国内'
    '壹心理等以人工匹配咨询为主，缺乏AI驱动与硬件终端。第四类，通用可穿戴（Apple Watch等）：'
    '仅测心率步数，无法识别BFRBs等特定焦虑动作。'
)
add_body_text(doc, content_2)

content_2b = (
    '"心安动识"以多模态传感融合+YOLO视觉+深度学习+CBT数字疗法构建技术壁垒，实现≥90%精准'
    '识别18类行为、全场景覆盖、打通"检测→分析→干预→疗愈"闭环，是国内该蓝海赛道的定义者，'
    '与上述竞品存在代际差异。'
)
add_body_text(doc, content_2b)

# ---- 3. 可行性分析 ----
add_section_heading(doc, '三', '可行性分析')

content_3 = (
    '技术可行性：YOLOv8在20,000+标注样本上mAP≥92%，推理延迟<50ms/帧；Bi-LSTM+CNN异常检测'
    '算法实现精准行为分类；FastAPI+PyTorch+PostgreSQL+Redis后端已在本地完成Vue前端、Spring Boot后端、'
    'Flask YOLO推理服务三端联调；已接入DeepSeek大模型实现"安小宁"智能体流式对话，图片感知、视频感知、'
    '摄像头感知三大检测闭环完成技术验证。'
)
add_body_text(doc, content_3)

content_3b = (
    '市场可行性：中国数字心理健康市场预计2030年突破¥2,500亿（CAGR约28.8%），BFRBs智能检测细分'
    '赛道为蓝海市场。300+份调研显示：76.3%受访者有无意识焦虑动作，82.1%愿意尝试智能辅助，68.5%'
    '愿意付费（月均¥15-¥49），需求真实且付费意愿明确。'
)
add_body_text(doc, content_3b)

content_3c = (
    '团队与资源可行性：依托高校科研团队，已持有导师软著1项、发明专利1项、省级以上论文1篇，另有2项'
    '软著和1项实用新型专利在申请中。团队成员横跨计算机科学、AI、心理学、视觉设计等多学科，具备全栈'
    '开发能力，已与学校心理中心、附属医院建立合作关系。'
)
add_body_text(doc, content_3c)

content_3d = (
    '政策可行性：精准响应"健康中国2030"、《"十五五"卫生健康规划》、《数字中国建设整体布局规划》'
    '等国家战略，属于教育部大创计划重点支持领域（健康中国+人工智能），政策环境友好，准入条件持续优化。'
)
add_body_text(doc, content_3d)

# ---- 4. App创新点 ----
add_section_heading(doc, '四', 'App创新点')

content_4 = (
    '创新一：多模态温柔感知体系。突破传统单一传感器局限，融合YOLO计算机视觉（图片/视频/摄像头'
    '三通道）+IMU惯性传感+ToF距离传感，实现18类焦虑躯体化行为≥90%精准识别。独创"感知空间"页面——'
    '摄像头检测时虚拟伙伴"安小宁"悬浮陪伴，以温柔气泡文案替代传统警报："我注意到你在咬指甲，要一起'
    '做个深呼吸吗？"彻底消除"被监控"的不适感。所有YOLO推理在浏览器本地完成，原始视频不上传服务器，'
    '从技术底层保障隐私。'
)
add_body_text(doc, content_4)

content_4b = (
    '创新二：多智能体协作疗愈引擎。内置四大AI智能体协同——检测智能体负责行为识别与异常告警，分析智能体'
    '量化焦虑水平并分析触发场景，干预智能体基于CBT认知行为疗法以DeepSeek大模型驱动流式对话疏导并推荐'
    '正念训练与习惯逆转练习，陪伴智能体（全局桌宠"安小宁"）根据时段与情绪状态主动问候、展示成长变化，'
    '形成"检测→分析→干预→陪伴"的智能体闭环。用户可自主选择是否保存觉察记录与媒体路径，数据主权完全'
    '交还用户。'
)
add_body_text(doc, content_4b)

content_4c = (
    '创新三：情感隐喻可视化。将枯燥的行为数据转化为"情绪之花"（花瓣盛开数=情绪稳定度）、"心灵花园"'
    '（焦虑减少=花草绽放）、"星光币"（行为改善=星光积累）等温暖隐喻，配合自然白噪音与舒缓动画，让数据'
    '查看成为治愈体验。'
)
add_body_text(doc, content_4c)

content_4d = (
    '创新四：B2B2C全场景覆盖。不仅服务C端个人用户（基础版免费/专业版订阅），同时为学校、企业、临床'
    '机构提供管理后台与群体数据分析，构建"个人日常疗愈+机构群体管理"的双重价值闭环。'
)
add_body_text(doc, content_4d)

# ---- 5. 应用前景 ----
add_section_heading(doc, '五', '应用前景')

content_5 = (
    '"心安动识"瞄准需求真实、政策利好、技术成熟、竞品空白的高增长赛道，校园、职场、临床、养老四大'
    '应用场景市场空间明确。校园端：嵌入学校心理普测流程，为教师提供客观行为数据，实现学生焦虑问题'
    '早发现早干预，目标覆盖500+所学校。企业端：提供轻量化数字化EAP方案，降低因焦虑导致的隐性缺勤与'
    '生产力损失，目标合作200+企业。临床端：辅助精神科/心理科量化诊断与疗效追踪。养老板块：关注老年'
    '群体焦虑躯体化表现，服务100+养老机构。'
)
add_body_text(doc, content_5)

content_5b = (
    '商业层面，项目采用"硬件引流→软件留存→服务变现→数据反哺"的增长飞轮，以B2B2C模式实现规模化'
    '获客。C端订阅（专业版29.9元/月、家庭版49.9元/月）、B端SaaS（学校2.98万元/年）、智能硬件'
    '（199-399元）及品牌周边四大收入线构建多元盈利模型，保守预计第五年营收突破2亿元。项目致力于'
    '成为中国领先的情绪躯体化行为智能识别与数字心理健康解决方案提供商，以科技之力守护全民心理健康，'
    '助力"健康中国2030"战略落地。'
)
add_body_text(doc, content_5b)

# ============================================================
# 保存文档
# ============================================================
output_path = '心安动识_初赛作品说明文档.docx'
doc.save(output_path)

# ============================================================
# 字数统计
# ============================================================
print(f'✅ 文档已生成: {output_path}')
print()
print('📊 字数统计（中文字符）:')
print('─' * 40)

sections_data = [
    ('问题背景与用户分析', [content_1, content_1b]),
    ('相关竞品分析', [content_2, content_2b]),
    ('可行性分析', [content_3, content_3b, content_3c, content_3d]),
    ('App创新点', [content_4, content_4b, content_4c, content_4d]),
    ('应用前景', [content_5, content_5b]),
]

for i, (title, contents) in enumerate(sections_data, 1):
    full_text = ''.join(contents)
    cn_chars = sum(1 for c in full_text if '\u4e00' <= c <= '\u9fff')
    total_chars = len(full_text.replace('\n', '').replace(' ', ''))
    print(f'  {i}. {title}: 约{cn_chars}中文字, 总计{total_chars}字符')

print()
print('📋 模板建议字数:')
print('  1. 问题背景与用户分析 — 200字')
print('  2. 相关竞品分析 — 200字')
print('  3. 可行性分析 — 300字')
print('  4. App创新点 — 300字')
print('  5. 应用前景 — 200字')
