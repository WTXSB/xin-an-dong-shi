"""
生成「心安动识」初赛作品说明文档 — 最终版
- 复用原PDF美化背景（背景图片嵌入页眉）
- 遵循模板结构
- 专业内容，深度参考原PDF
"""
import docx
from docx import Document
from docx.shared import Pt, Cm, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import parse_xml, OxmlElement
import os

# ============================================================
# 品牌色常量
# ============================================================
DARK_GREEN = RGBColor(0x17, 0x3D, 0x36)
TEAL = RGBColor(0x2F, 0x9F, 0x91)
DEEP_BLUE = RGBColor(0x2C, 0x5F, 0x8A)
CALM_BLUE = RGBColor(0x4A, 0x90, 0xD9)
WARM_ORANGE = RGBColor(0xF5, 0xA6, 0x23)
BODY_TEXT = RGBColor(0x33, 0x33, 0x33)
MUTED_TEXT = RGBColor(0x99, 0x99, 0x99)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
BG_GREEN = 'EDF8F2'

# ============================================================
# 创建文档
# ============================================================
doc = Document()

# 页面设置
for section in doc.sections:
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.2)
    section.bottom_margin = Cm(2.0)
    section.left_margin = Cm(2.6)
    section.right_margin = Cm(2.6)

# ============================================================
# 添加页面背景（使用原PDF背景图 + 纯色fallback）
# ============================================================
def add_background_to_section(section, bg_image_path):
    """将背景图添加到页眉，模拟页面背景"""
    header = section.header
    header.is_linked_to_previous = False
    
    # 清空页眉默认段落
    for p in header.paragraphs:
        p.clear()
    
    paragraph = header.paragraphs[0]
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    # 设置段落零间距
    pPr = paragraph._element.get_or_add_pPr()
    spacing = OxmlElement('w:spacing')
    spacing.set(qn('w:before'), '0')
    spacing.set(qn('w:after'), '0')
    spacing.set(qn('w:line'), '240')
    pPr.insert(0, spacing)
    
    if os.path.exists(bg_image_path):
        run = paragraph.add_run()
        try:
            pic = run.add_picture(bg_image_path, width=Cm(21.0), height=Cm(29.7))
            # 尝试设置图片为衬于文字下方
            # 获取drawing元素
            drawing = run._element.find('.//' + qn('wp:inline'))
            if drawing is not None:
                # 计算图片位置使其覆盖整个页面
                extent = drawing.find('.//' + qn('wp:extent'))
                if extent is not None:
                    extent.set('cx', str(int(21.0 * 360000)))  # 21cm in EMU
                    extent.set('cy', str(int(29.7 * 360000)))  # 29.7cm in EMU
        except Exception as e:
            print(f'  [WARN] 背景图添加失败: {e}')

def set_page_bg_color(doc, hex_color='EDF8F2'):
    """设置页面背景颜色"""
    for section in doc.sections:
        sectPr = section._sectPr
        if sectPr is None:
            continue
        bg = OxmlElement('w:background')
        bg.set(qn('w:color'), hex_color)
        bg.set(qn('w:themeColor'), 'background2')
        bg.set(qn('w:shade'), '05')
        sectPr.insert(0, bg)

# 应用背景
bg_img = 'extracted_pdf_images/prod_page1_img0.jpeg'
print(f'背景图片路径: {bg_img}')
print(f'背景图片存在: {os.path.exists(bg_img)}')

if os.path.exists(bg_img):
    for section in doc.sections:
        add_background_to_section(section, bg_img)
    print('✅ 已添加PDF背景图片')
else:
    set_page_bg_color(doc, BG_GREEN)
    print('⚠️ 背景图不存在，使用纯色背景')

# 同时设置纯色背景作为兜底
try:
    set_page_bg_color(doc, BG_GREEN)
    print('✅ 已设置页面底色')
except Exception as e:
    print(f'[WARN] 底色设置失败: {e}')

# ============================================================
# 样式设置
# ============================================================
# Normal
style = doc.styles['Normal']
font = style.font
font.name = '微软雅黑'
font.size = Pt(10.5)
font.color.rgb = BODY_TEXT
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
style.paragraph_format.line_spacing = 1.65
style.paragraph_format.space_after = Pt(4)
style.paragraph_format.first_line_indent = Cm(0.74)

# Heading 1
h1 = doc.styles['Heading 1']
h1.font.name = '微软雅黑'
h1.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
h1.font.size = Pt(18)
h1.font.color.rgb = DARK_GREEN
h1.font.bold = True
h1.paragraph_format.space_before = Pt(28)
h1.paragraph_format.space_after = Pt(12)
h1.paragraph_format.first_line_indent = Cm(0)

# Heading 2
h2 = doc.styles['Heading 2']
h2.font.name = '微软雅黑'
h2.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
h2.font.size = Pt(13)
h2.font.color.rgb = DEEP_BLUE
h2.font.bold = True
h2.paragraph_format.space_before = Pt(18)
h2.paragraph_format.space_after = Pt(8)
h2.paragraph_format.first_line_indent = Cm(0)

# ============================================================
# 辅助函数
# ============================================================
def add_centered(doc, text, size=12, color=BODY_TEXT, bold=False, italic=False, sa=6):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_after = Pt(sa)
    p.paragraph_format.space_before = Pt(0)
    run = p.add_run(text)
    run.font.size = Pt(size)
    run.font.color.rgb = color
    run.font.bold = bold
    run.font.italic = italic
    run.font.name = '微软雅黑'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    return p

def add_section_title(doc, num, title):
    """章节标题带装饰"""
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_before = Pt(32)
    p.paragraph_format.space_after = Pt(4)
    
    run = p.add_run(f'{num}  {title}')
    run.font.size = Pt(17)
    run.font.color.rgb = DARK_GREEN
    run.font.bold = True
    run.font.name = '微软雅黑'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    
    # 装饰线
    p2 = doc.add_paragraph()
    p2.paragraph_format.first_line_indent = Cm(0)
    p2.paragraph_format.space_after = Pt(14)
    p2.paragraph_format.space_before = Pt(0)
    run2 = p2.add_run('━' * 45)
    run2.font.size = Pt(5)
    run2.font.color.rgb = TEAL
    
    return p

def add_body(doc, text):
    """添加正文"""
    p = doc.add_paragraph(text)
    return p

# ============================================================
# 封面页
# ============================================================
for _ in range(6):
    add_centered(doc, '', size=6, color=BODY_TEXT, sa=0)

add_centered(doc, '第十一届（2026年）中国高校计算机大赛', size=15, color=DEEP_BLUE, sa=3)
add_centered(doc, '—— 移动应用创新赛 ——', size=12, color=CALM_BLUE, sa=26)

# 标识
add_centered(doc, '🌸', size=52, color=TEAL, sa=14)

add_centered(doc, '心安动识', size=40, color=DARK_GREEN, bold=True, sa=2)
add_centered(doc, 'MindEase', size=19, color=TEAL, sa=14)

add_centered(doc, '基于YOLO的多智能体协作情绪识别与心灵疗愈平台', size=13, color=DEEP_BLUE, sa=36)

# 双装饰线
add_centered(doc, '━' * 30, size=6, color=TEAL, sa=8)
add_centered(doc, '「 看见焦虑，遇见安宁 」', size=16, color=WARM_ORANGE, italic=True, sa=10)
add_centered(doc, '━' * 30, size=6, color=TEAL, sa=36)

for _ in range(8):
    add_centered(doc, '', size=6, color=BODY_TEXT, sa=0)

add_centered(doc, '2026年6月', size=12, color=MUTED_TEXT)

# ============================================================
# 分页 → 正文
# ============================================================
doc.add_page_break()

# ============================================================
# 一、问题背景与用户分析
# ============================================================
add_section_title(doc, '一、', '问题背景与用户分析')

add_body(doc,
    '当前，焦虑已成为影响全民心理健康的核心议题。《中国国民心理健康发展报告（2023-2024）》'
    '显示，我国成年人焦虑障碍终生患病率约7.6%，青少年焦虑情绪检出率高达24.6%。焦虑往往不以'
    '言语表达，而是通过躯体化动作无意识呈现——咬指甲、拔头发、抓挠皮肤、抖腿、反复搓手等身体'
    '聚焦重复行为（BFRBs），这些行为既是焦虑的外在表征，又会反向加重心理负担，形成恶性循环。')

add_body(doc,
    '当前痛点集中在三个层面：个人层面，超过76%的用户至少有一种无意识焦虑动作，但大多不自知、'
    '羞于求助，且缺乏科学便捷的改善工具；临床层面，医生依赖主观观察与患者自我报告，患者面诊时'
    '往往刻意克制行为导致数据失真，缺乏客观量化手段；市场层面，国内尚无集"精准识别—智能分析—'
    '心理干预—持续疗愈"于一体的全流程解决方案。本项目聚焦12-25岁青少年及大学生、25-45岁职场'
    '人群、6-12岁儿童及60岁以上老年群体四大C端用户，同时服务学校、企业、临床机构等B端用户，'
    '市场需求真实、迫切且体量庞大。')

# ============================================================
# 二、相关竞品分析
# ============================================================
add_section_title(doc, '二、', '相关竞品分析')

add_body(doc,
    '第一类，智能穿戴设备：以美国HabitAware Keen手环为代表，采用单一IMU传感器加振动提醒，'
    '虽为首款BFRB可穿戴产品，但仅能检测单一行为（如拔头发），无视觉识别、无疗愈功能，用户'
    '"被提醒了但不知道该怎么办"。第二类，可穿戴贴片：如Spire Health Tag，仅监测呼吸与心率'
    '变异性，无法识别具体行为类别，且产品已停止更新。第三类，AI心理健康应用：Woebot仅有NLP'
    '对话功能而无行为识别能力；国内壹心理等以人工匹配咨询为主，缺乏AI驱动与硬件终端联动。'
    '第四类，通用可穿戴设备（如Apple Watch、小米手环）：仅监测基础心率与步数，完全无法识别'
    'BFRBs等特定焦虑躯体化动作，与用户真实需求存在巨大鸿沟。')

add_body(doc,
    '"心安动识"以多模态传感融合+YOLO计算机视觉+深度学习异常检测+CBT数字疗法构建系统性技术'
    '壁垒，实现≥90%精准识别18类行为、覆盖居家/办公/校园/临床/养老五大场景、打通"检测→分析'
    '→干预→疗愈"全流程闭环。相较于上述竞品，"心安动识"在技术维度、场景覆盖、疗愈能力和商业'
    '模式四个层面均存在代际领先优势，是国内BFRBs智能检测蓝海赛道的先行定义者。')

# ============================================================
# 三、可行性分析
# ============================================================
add_section_title(doc, '三、', '可行性分析')

add_body(doc,
    '技术可行性：YOLOv8目标检测模型在20,000+标注样本上mAP≥92%，边缘端推理延迟<50ms/帧；'
    'Bi-LSTM+CNN联合检测算法实现对异常行为模式的精准识别；后端采用FastAPI+PyTorch+PostgreSQL'
    '+Redis高性能架构，已在本地完成Vue前端、Spring Boot后端、Flask YOLO推理服务三端联调；'
    '已成功接入DeepSeek大模型实现"安小宁"智能体流式对话，图片感知、视频感知、摄像头感知三大核心'
    '检测闭环均已通过技术验证。')

add_body(doc,
    '市场可行性：中国数字心理健康市场处于高速增长期，预计2030年市场规模将突破¥2,500亿元'
    '（CAGR约28.8%），BFRBs专项智能检测细分赛道为蓝海市场，国内尚无直接竞品。300+份有效问卷'
    '调研显示：76.3%受访者承认有无意识焦虑动作，82.1%愿意尝试智能辅助改善，68.5%愿意为有效'
    '方案付费（月均付费意愿¥15-¥49元），需求真实且付费意愿明确。')

add_body(doc,
    '资源可行性：依托高校科研团队，已持有导师软件著作权1项、发明专利1项、省级以上期刊论文1篇，'
    '另有2项软著与1项实用新型专利在申请中。团队成员横跨计算机科学、人工智能、心理学、视觉设计'
    '等多学科领域，具备全栈开发与产品设计能力，并与学校心理中心及附属医院建立合作关系，可获取'
    '临床验证场景与专业指导。政策层面，项目精准响应"健康中国2030"、《"十五五"卫生健康规划》、'
    '《数字中国建设整体布局规划》等国家战略，属于教育部大创计划重点支持领域，政策红利持续释放。')

# ============================================================
# 四、App创新点
# ============================================================
add_section_title(doc, '四、', 'App创新点')

add_body(doc,
    '创新一：多模态温柔感知体系。突破传统单一传感器局限，融合YOLO计算机视觉（支持图片、视频、'
    '摄像头三通道输入）与IMU惯性传感、ToF距离传感等多模态数据，实现18类焦虑躯体化行为≥90%'
    '精准识别。独创"感知空间"页面——摄像头检测时虚拟伙伴"安小宁"以悬浮桌宠形式实时陪伴，检测到'
    '焦虑行为时以温柔气泡文案替代传统警报（如"我注意到你在咬指甲，要一起做个深呼吸吗？"），从'
    '交互层面彻底消除"被监控"的不适感。所有YOLO推理运算在浏览器本地完成，原始视频与图像数据'
    '不上传服务器，从技术底层保障用户隐私。')

add_body(doc,
    '创新二：多智能体协作疗愈引擎。App内置四大AI智能体协同工作——"检测智能体"负责行为识别与'
    '异常告警，"分析智能体"量化焦虑水平并分析触发场景，生成个性化情绪健康画像，"干预智能体"'
    '基于CBT认知行为疗法以DeepSeek大模型驱动流式对话疏导并推荐正念训练与习惯逆转练习，"陪伴'
    '智能体"即全局悬浮桌宠"安小宁"，根据时段与情绪状态主动问候、展示成长变化。四大智能体形成'
    '"检测→分析→干预→陪伴"的完整闭环，且用户可自主选择是否保存觉察记录与媒体路径，将数据主权'
    '完全交还用户。')

add_body(doc,
    '创新三：情感隐喻可视化与疗愈化交互。将枯燥的行为数据转化为"情绪之花"（花瓣盛开数量=情绪'
    '稳定程度）、"心灵花园"（焦虑行为减少=花草绽放）、"星光币"（行为改善=星光积累）等温暖直观的'
    '视觉隐喻，配合自然白噪音、轻柔提示音与舒缓过渡动画，让每一次数据查看与功能使用都成为微型'
    '疗愈体验，贯彻"让科技隐形，让陪伴显现"的设计哲学。')

add_body(doc,
    '创新四：B2B2C全场景覆盖。不仅服务C端个人用户（基础版免费、专业版29.9元/月、家庭版49.9'
    '元/月），同时为学校、企业、临床机构提供专属管理后台与群体数据分析服务，构建"个人日常疗愈'
    '+机构群体健康管理"的双重价值闭环，实现从单一个体服务到组织级心理健康基础设施的跨越。')

# ============================================================
# 五、应用前景
# ============================================================
add_section_title(doc, '五、', '应用前景')

add_body(doc,
    '"心安动识"锚定需求真实、政策利好、技术成熟、竞品空白的高增长赛道，在校园、职场、临床、'
    '养老四大场景具备明确的应用前景与落地路径。校园端：嵌入学校年度心理普测流程，为心理教师提供'
    '客观行为数据支撑，实现学生焦虑问题的早发现、早干预，目标覆盖500所以上院校；企业端：提供'
    '轻量化数字化员工援助方案（EAP），降低因焦虑情绪导致的隐性缺勤与生产力损失，目标合作200家'
    '以上企业；临床端：为精神科/心理科提供客观量化诊断参考与疗效追踪工具，减少对主观量表的依赖；'
    '养老板块：关注老年群体焦虑躯体化表现，辅助老年心理健康评估与照护，服务100家以上养老机构。')

add_body(doc,
    '商业模式层面，项目构建"硬件引流→软件留存→服务变现→数据反哺"的增长飞轮，以B2B2C模式'
    '实现规模化用户获取。通过C端订阅（基础版免费/专业版29.9元/月）、B端SaaS服务（学校2.98万元'
    '/年/校）、智能硬件销售（199-399元/件）及品牌周边产品四大收入线构建多元盈利模型，保守预计'
    '第三年营收达3,500万元、第五年突破2亿元。项目致力于成为中国领先的情绪躯体化行为智能识别与'
    '数字心理健康解决方案提供商，以科技之力守护全民心理健康，助力"健康中国2030"战略全面落地。')

# ============================================================
# 保存
# ============================================================
output_path = '心安动识_初赛作品说明文档.docx'
doc.save(output_path)

# ============================================================
# 统计
# ============================================================
print(f'\n✅ 文档已保存: {output_path}')
print(f'📁 完整路径: {os.path.abspath(output_path)}')
print()

sections_data = [
    ('问题背景与用户分析', [
        '当前，焦虑已成为影响全民心理健康的核心议题。《中国国民心理健康发展报告（2023-2024）》显示，我国成年人焦虑障碍终生患病率约7.6%，青少年焦虑情绪检出率高达24.6%。焦虑往往不以言语表达，而是通过躯体化动作无意识呈现——咬指甲、拔头发、抓挠皮肤、抖腿、反复搓手等身体聚焦重复行为（BFRBs），这些行为既是焦虑的外在表征，又会反向加重心理负担，形成恶性循环。',
        '当前痛点集中在三个层面：个人层面，超过76%的用户至少有一种无意识焦虑动作，但大多不自知、羞于求助，且缺乏科学便捷的改善工具；临床层面，医生依赖主观观察与患者自我报告，患者面诊时往往刻意克制行为导致数据失真，缺乏客观量化手段；市场层面，国内尚无集"精准识别—智能分析—心理干预—持续疗愈"于一体的全流程解决方案。本项目聚焦12-25岁青少年及大学生、25-45岁职场人群、6-12岁儿童及60岁以上老年群体四大C端用户，同时服务学校、企业、临床机构等B端用户，市场需求真实、迫切且体量庞大。',
    ]),
    ('相关竞品分析', [
        '第一类，智能穿戴设备：以美国HabitAware Keen手环为代表，采用单一IMU传感器加振动提醒，虽为首款BFRB可穿戴产品，但仅能检测单一行为（如拔头发），无视觉识别、无疗愈功能，用户"被提醒了但不知道该怎么办"。第二类，可穿戴贴片：如Spire Health Tag，仅监测呼吸与心率变异性，无法识别具体行为类别，且产品已停止更新。第三类，AI心理健康应用：Woebot仅有NLP对话功能而无行为识别能力；国内壹心理等以人工匹配咨询为主，缺乏AI驱动与硬件终端联动。第四类，通用可穿戴设备（如Apple Watch、小米手环）：仅监测基础心率与步数，完全无法识别BFRBs等特定焦虑躯体化动作，与用户真实需求存在巨大鸿沟。',
        '"心安动识"以多模态传感融合+YOLO计算机视觉+深度学习异常检测+CBT数字疗法构建系统性技术壁垒，实现≥90%精准识别18类行为、覆盖居家/办公/校园/临床/养老五大场景、打通"检测→分析→干预→疗愈"全流程闭环。相较于上述竞品，"心安动识"在技术维度、场景覆盖、疗愈能力和商业模式四个层面均存在代际领先优势，是国内BFRBs智能检测蓝海赛道的先行定义者。',
    ]),
    ('可行性分析', [
        '技术可行性：YOLOv8目标检测模型在20,000+标注样本上mAP≥92%，边缘端推理延迟<50ms/帧；Bi-LSTM+CNN联合检测算法实现对异常行为模式的精准识别；后端采用FastAPI+PyTorch+PostgreSQL+Redis高性能架构，已在本地完成Vue前端、Spring Boot后端、Flask YOLO推理服务三端联调；已成功接入DeepSeek大模型实现"安小宁"智能体流式对话，图片感知、视频感知、摄像头感知三大核心检测闭环均已通过技术验证。',
        '市场可行性：中国数字心理健康市场处于高速增长期，预计2030年市场规模将突破¥2,500亿元（CAGR约28.8%），BFRBs专项智能检测细分赛道为蓝海市场，国内尚无直接竞品。300+份有效问卷调研显示：76.3%受访者承认有无意识焦虑动作，82.1%愿意尝试智能辅助改善，68.5%愿意为有效方案付费（月均付费意愿¥15-¥49元），需求真实且付费意愿明确。',
        '资源可行性：依托高校科研团队，已持有导师软件著作权1项、发明专利1项、省级以上期刊论文1篇，另有2项软著与1项实用新型专利在申请中。团队成员横跨计算机科学、人工智能、心理学、视觉设计等多学科领域，具备全栈开发与产品设计能力，并与学校心理中心及附属医院建立合作关系，可获取临床验证场景与专业指导。政策层面，项目精准响应"健康中国2030"、《"十五五"卫生健康规划》、《数字中国建设整体布局规划》等国家战略，属于教育部大创计划重点支持领域，政策红利持续释放。',
    ]),
    ('App创新点', [
        '创新一：多模态温柔感知体系。突破传统单一传感器局限，融合YOLO计算机视觉（支持图片、视频、摄像头三通道输入）与IMU惯性传感、ToF距离传感等多模态数据，实现18类焦虑躯体化行为≥90%精准识别。独创"感知空间"页面——摄像头检测时虚拟伙伴"安小宁"以悬浮桌宠形式实时陪伴，检测到焦虑行为时以温柔气泡文案替代传统警报（如"我注意到你在咬指甲，要一起做个深呼吸吗？"），从交互层面彻底消除"被监控"的不适感。所有YOLO推理运算在浏览器本地完成，原始视频与图像数据不上传服务器，从技术底层保障用户隐私。',
        '创新二：多智能体协作疗愈引擎。App内置四大AI智能体协同工作——"检测智能体"负责行为识别与异常告警，"分析智能体"量化焦虑水平并分析触发场景，生成个性化情绪健康画像，"干预智能体"基于CBT认知行为疗法以DeepSeek大模型驱动流式对话疏导并推荐正念训练与习惯逆转练习，"陪伴智能体"即全局悬浮桌宠"安小宁"，根据时段与情绪状态主动问候、展示成长变化。四大智能体形成"检测→分析→干预→陪伴"的完整闭环，且用户可自主选择是否保存觉察记录与媒体路径，将数据主权完全交还用户。',
        '创新三：情感隐喻可视化与疗愈化交互。将枯燥的行为数据转化为"情绪之花"（花瓣盛开数量=情绪稳定程度）、"心灵花园"（焦虑行为减少=花草绽放）、"星光币"（行为改善=星光积累）等温暖直观的视觉隐喻，配合自然白噪音、轻柔提示音与舒缓过渡动画，让每一次数据查看与功能使用都成为微型疗愈体验，贯彻"让科技隐形，让陪伴显现"的设计哲学。',
        '创新四：B2B2C全场景覆盖。不仅服务C端个人用户（基础版免费、专业版29.9元/月、家庭版49.9元/月），同时为学校、企业、临床机构提供专属管理后台与群体数据分析服务，构建"个人日常疗愈+机构群体健康管理"的双重价值闭环，实现从单一个体服务到组织级心理健康基础设施的跨越。',
    ]),
    ('应用前景', [
        '"心安动识"锚定需求真实、政策利好、技术成熟、竞品空白的高增长赛道，在校园、职场、临床、养老四大场景具备明确的应用前景与落地路径。校园端：嵌入学校年度心理普测流程，为心理教师提供客观行为数据支撑，实现学生焦虑问题的早发现、早干预，目标覆盖500所以上院校；企业端：提供轻量化数字化员工援助方案（EAP），降低因焦虑情绪导致的隐性缺勤与生产力损失，目标合作200家以上企业；临床端：为精神科/心理科提供客观量化诊断参考与疗效追踪工具，减少对主观量表的依赖；养老板块：关注老年群体焦虑躯体化表现，辅助老年心理健康评估与照护，服务100家以上养老机构。',
        '商业模式层面，项目构建"硬件引流→软件留存→服务变现→数据反哺"的增长飞轮，以B2B2C模式实现规模化用户获取。通过C端订阅（基础版免费/专业版29.9元/月）、B端SaaS服务（学校2.98万元/年/校）、智能硬件销售（199-399元/件）及品牌周边产品四大收入线构建多元盈利模型，保守预计第三年营收达3,500万元、第五年突破2亿元。项目致力于成为中国领先的情绪躯体化行为智能识别与数字心理健康解决方案提供商，以科技之力守护全民心理健康，助力"健康中国2030"战略全面落地。',
    ]),
]

print('📊 字数统计:')
print('─' * 50)
for i, (title, contents) in enumerate(sections_data, 1):
    full_text = ''.join(contents)
    cn_chars = sum(1 for c in full_text if '\u4e00' <= c <= '\u9fff')
    total = len(full_text.replace('\n', '').replace(' ', ''))
    print(f'  {i}. {title}: {cn_chars}中文字 / {total}总字符')

print()
print('✨ 文档生成完成！可在Word中打开查看效果。')
print('💡 提示：若背景图未显示，可在Word中手动设置页面颜色为 #EDF8F2。')
