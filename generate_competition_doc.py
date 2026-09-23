"""
生成「心安动识」初赛作品说明文档
基于原PDF内容和模板结构，复用原PDF美化背景
"""
import docx
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml
import os

# ============================================================
# 创建文档
# ============================================================
doc = Document()

# 页面设置 - A4
for section in doc.sections:
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.18)
    section.right_margin = Cm(3.18)

# ============================================================
# 定义样式
# ============================================================
style = doc.styles['Normal']
font = style.font
font.name = '微软雅黑'
font.size = Pt(11)
font.color.rgb = RGBColor(0x33, 0x33, 0x33)
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
style.paragraph_format.line_spacing = 1.5
style.paragraph_format.space_after = Pt(6)

# 标题样式
for i in range(1, 4):
    heading_style = doc.styles[f'Heading {i}']
    heading_font = heading_style.font
    heading_font.name = '微软雅黑'
    heading_style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    if i == 1:
        heading_font.size = Pt(22)
        heading_font.color.rgb = RGBColor(0x17, 0x3D, 0x36)  # 深绿
        heading_font.bold = True
    elif i == 2:
        heading_font.size = Pt(16)
        heading_font.color.rgb = RGBColor(0x2C, 0x5F, 0x8A)  # 深蓝
        heading_font.bold = True
    elif i == 3:
        heading_font.size = Pt(13)
        heading_font.color.rgb = RGBColor(0x4A, 0x90, 0xD9)  # 宁静蓝
        heading_font.bold = True

# ============================================================
# 尝试添加页面背景（通过header添加背景图）
# ============================================================
def add_page_background(section, image_path):
    """在页眉中添加背景图片"""
    header = section.header
    header.is_linked_to_previous = False
    
    # 创建一个包含背景图的段落
    paragraph = header.paragraphs[0]
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    # 添加图片
    if os.path.exists(image_path):
        run = paragraph.add_run()
        run.add_picture(image_path, width=Cm(21.0), height=Cm(29.7))
        
        # 设置图片为衬于文字下方
        drawing = run._element.find('.//' + qn('wp:inline'))
        if drawing is None:
            drawing = run._element.find('.//' + qn('wp:anchor'))
        if drawing is not None:
            # 设置 behind text
            pass

# 尝试添加背景
bg_image = 'extracted_pdf_images/prod_page1_img0.jpeg'
if os.path.exists(bg_image):
    for section in doc.sections:
        try:
            add_page_background(section, bg_image)
        except Exception as e:
            print(f'背景添加失败（将继续无背景生成）: {e}')

# ============================================================
# 封面
# ============================================================
# 添加空行调整间距
for _ in range(6):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.space_before = Pt(0)

# 大赛名称
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('第十一届（2026年）中国高校计算机大赛')
run.font.size = Pt(15)
run.font.color.rgb = RGBColor(0x2C, 0x5F, 0x8A)
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('— 移动应用创新赛 —')
run.font.size = Pt(13)
run.font.color.rgb = RGBColor(0x4A, 0x90, 0xD9)
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

doc.add_paragraph()  # 空行

# 作品名称
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('心安动识')
run.font.size = Pt(36)
run.font.color.rgb = RGBColor(0x17, 0x3D, 0x36)
run.font.bold = True
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('MindEase')
run.font.size = Pt(18)
run.font.color.rgb = RGBColor(0x2F, 0x9F, 0x91)
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

doc.add_paragraph()

# 副标题
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('基于YOLO的多智能体协作情绪识别与心灵疗愈平台')
run.font.size = Pt(14)
run.font.color.rgb = RGBColor(0x4A, 0x90, 0xD9)
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

for _ in range(4):
    doc.add_paragraph()

# Slogan
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('「 看见焦虑，遇见安宁 」')
run.font.size = Pt(16)
run.font.color.rgb = RGBColor(0xF5, 0xA6, 0x23)
run.font.italic = True
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

for _ in range(4):
    doc.add_paragraph()

# 团队信息
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('2026年6月')
run.font.size = Pt(12)
run.font.color.rgb = RGBColor(0x99, 0x99, 0x99)
run.font.name = '微软雅黑'
run._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

# ============================================================
# 分页 - 正文开始
# ============================================================
doc.add_page_break()

# ============================================================
# 1. 问题背景与用户分析（200字）
# ============================================================
doc.add_heading('一、问题背景与用户分析', level=1)

content_1 = """在快节奏的现代社会，焦虑已成为影响全民心理健康的核心问题。《中国国民心理健康发展报告（2023-2024）》显示，我国成年人焦虑障碍终生患病率约为7.6%，青少年焦虑情绪检出率高达24.6%。焦虑往往以躯体化动作无意识呈现——咬指甲、拔头发、抓挠皮肤、抖腿等身体聚焦重复行为（BFRBs），这些行为既是焦虑的外在表征，又会反向加重心理负担，形成恶性循环。

当前痛点集中体现在三个方面：个人层面，超过76%的用户存在至少一种无意识焦虑动作，但大多数人并不自知，且羞于启齿、缺乏科学便捷的改善工具；临床层面，医生依赖主观观察与患者自我报告，缺乏客观量化工具，患者面对医生时往往刻意克制行为导致数据失真；市场层面，针对BFRBs的智能检测产品在国内几乎空白，现有方案功能单一、无疗愈能力，无法形成"检测-分析-干预-康复"的全流程服务闭环。

本项目的目标用户覆盖四大群体：12-25岁青少年及大学生（学业压力、社交焦虑）、25-45岁职场人群（工作压力、职业倦怠）、6-12岁儿童（分离焦虑、早期干预需求）以及60岁以上老年群体（老年焦虑、躯体化表现），同时服务学校、企业、临床机构等B端用户，市场空间广阔。"""

p = doc.add_paragraph(content_1)

# ============================================================
# 2. 相关竞品分析（200字）
# ============================================================
doc.add_heading('二、相关竞品分析', level=1)

content_2 = """当前市场上与"心安动识"相关的产品可分为四类，均存在明显短板：

（1）智能穿戴类：以美国HabitAware Keen手环为代表，采用单一IMU传感器+振动提醒，虽为首创BFRB可穿戴设备，但仅能检测单一行为（如拔头发），缺乏视觉识别能力，更无疗愈功能，无法告知用户"检测到行为后该怎么办"。（2）可穿戴贴片类：如Spire Health Tag，仅监测呼吸和心率，无法识别具体行为类别，且产品已停更。（3）AI心理健康应用类：以Woebot（AI聊天机器人）和国内壹心理等为代表，前者仅有NLP对话功能无行为识别，后者以人工匹配咨询为主，缺乏AI驱动能力和硬件终端。（4）传统通用可穿戴：如Apple Watch、小米手环等，仅能监测基础心率、步数，无法识别BFRBs等特定焦虑动作。

相较之下，"心安动识"创新性地构建了"多模态传感融合+YOLO视觉识别+深度学习异常检测+CBT数字疗法"四位一体技术体系，实现≥90%精准识别18类行为、覆盖全场景、打通"检测→分析→干预→疗愈"完整闭环，是国内该蓝海赛道的先行者。"""

p = doc.add_paragraph(content_2)

# ============================================================
# 3. 可行性分析（300字）
# ============================================================
doc.add_heading('三、可行性分析', level=1)

content_3 = """（一）技术可行性：项目核心技术栈成熟可靠。YOLOv8目标检测模型在18类行为数据集（20,000+标注样本）上mAP≥92%，边缘端推理延迟<50ms/帧；Bi-LSTM+CNN联合检测算法实现异常行为精准识别；FastAPI+PyTorch+PostgreSQL+Redis构建的高性能后端已在本地完成三端联调（Vue前端、Spring Boot后端、Flask YOLO推理服务）。同时，项目已接入DeepSeek大模型实现"安小宁"智能体流式对话，完成图片感知、视频感知、摄像头感知三大核心检测闭环的技术验证。

（二）市场可行性：数字心理健康市场处于高速增长期，中国市场规模预计从2025年约¥680亿增长至2030年约¥2,500亿（CAGR约28.8%）。BFRBs专项智能检测细分赛道尚属蓝海，国内无直接竞品。300+份用户调研显示：76.3%受访者承认有无意识焦虑动作，82.1%愿意尝试智能设备辅助改善，68.5%愿意付费（月均¥15-¥49），需求真实且付费意愿明确。

（三）团队与资源可行性：项目依托高校科研团队，已持有导师软件著作权1项、发明专利1项、省级以上期刊论文1篇，另有2项软著和1项实用新型专利在申请中。团队成员横跨计算机科学、人工智能、心理学、视觉设计等多学科领域，具备全栈开发与产品设计能力，并与学校心理中心、附属医院建立合作关系，可获取临床验证场景与专业指导。

（四）政策可行性：项目精准响应"健康中国2030"、《"十五五"卫生健康规划》、《数字中国建设整体布局规划》等国家战略，属于教育部大学生创新创业训练计划重点支持领域（健康中国+人工智能），政策环境友好，市场准入条件持续优化。"""

p = doc.add_paragraph(content_3)

# ============================================================
# 4. App创新点（300字）
# ============================================================
doc.add_heading('四、App创新点', level=1)

content_4 = """"心安动识"App围绕"让科技隐形，让陪伴显现"的设计理念，打造多项核心创新：

（一）多模态温柔感知体系。突破传统单一传感器局限，创新融合YOLO计算机视觉（图片/视频/摄像头三通道）、IMU惯性传感、ToF距离传感等多模态数据，实现18类焦虑躯体化行为的≥90%精准识别。独创"感知空间"页面设计，摄像头检测时虚拟伙伴"安小宁"悬浮陪伴，以气泡文案、动态表情替代传统警报，消除"被监控"的不适感——识别到焦虑行为时，安小宁会说"我注意到你在咬指甲，要一起做个深呼吸吗？"而非冷冰冰的告警。

（二）多智能体协作疗愈引擎。App内置四大AI智能体协同工作——"检测智能体"负责行为识别与异常告警，"分析智能体"量化焦虑水平、分析触发场景、生成情绪健康画像，"干预智能体"基于CBT认知行为疗法开展流式对话疏导、推荐正念训练与习惯逆转练习，"陪伴智能体"即全局桌宠"安小宁"，根据时段与情绪状态主动问候、展示成长变化，形成"检测→分析→干预→陪伴"的智能体协作闭环。

（三）隐私优先的温柔设计哲学。App在交互全流程贯彻"不强求、不评判、不泄露"原则：所有YOLO推理在浏览器本地完成，原始视频不上传服务器；用户可自主选择是否保存觉察记录、是否保留图片/视频路径；对话模块仅发送用户主动输入的文字，摄像头画面、识别记录不会自动进入AI对话；提供游客体验入口，零门槛体验核心功能。

（四）数据可视化中的情感隐喻。将枯燥的行为数据转化为"情绪之花"（花瓣盛开数=情绪稳定度）、"心灵花园"（焦虑减少=花草绽放）、"星光币"（行为改善=星光积累）等可视化隐喻，配合自然白噪音与舒缓动画，让每一次数据查看都成为治愈体验。

（五）B2B2C全场景覆盖。不仅服务C端个人用户，同时为学校、企业、临床机构提供管理后台与群体数据分析，实现"个人日常疗愈+机构群体管理"的双重价值闭环。"""

p = doc.add_paragraph(content_4)

# ============================================================
# 5. 应用前景（200字）
# ============================================================
doc.add_heading('五、应用前景', level=1)

content_5 = """"心安动识"瞄准的是一个需求真实、政策利好、技术成熟、竞品空白的高增长赛道，应用前景广阔：

校园心理健康筛查：可嵌入学校年度心理普测流程，为心理教师提供客观行为数据支撑，实现学生焦虑问题的早发现、早干预，目标覆盖500+所学校。企业员工关怀（EAP）：为企业提供轻量化数字化心理健康方案，降低员工因焦虑导致的隐性缺勤与生产力损失，目标合作200+企业。临床辅助诊疗：为精神科/心理科提供客观量化工具，辅助诊断与疗效评估，减少对主观量表的依赖。养老照护场景：关注老年群体焦虑躯体化表现，辅助老年心理健康评估与照护，服务100+养老机构。

项目采用"硬件引流→软件留存→服务变现→数据反哺"的商业飞轮，以B2B2C模式实现规模化用户获取，通过C端订阅（基础版免费/专业版29.9元/月）、B端SaaS（学校2.98万元/年）、智能硬件（199-399元）及品牌周边四大收入线构建多元盈利模型。保守预计Year3收入可达3,500万元，Year5突破2亿元。项目致力于成为中国领先的情绪躯体化行为智能识别与数字心理健康解决方案提供商，以科技之力守护全民心理健康，助力"健康中国2030"战略落地。"""

p = doc.add_paragraph(content_5)

# ============================================================
# 保存文档
# ============================================================
output_path = '心安动识_初赛作品说明文档.docx'
doc.save(output_path)
print(f'文档已生成: {output_path}')
print(f'字数统计:')
for i, (title, content) in enumerate([
    ('问题背景与用户分析', content_1),
    ('相关竞品分析', content_2),
    ('可行性分析', content_3),
    ('App创新点', content_4),
    ('应用前景', content_5),
], 1):
    # 统计中文字符数
    cn_chars = sum(1 for c in content if '\u4e00' <= c <= '\u9fff' or '\u3000' <= c <= '\u303f')
    print(f'  {i}. {title}: 约{cn_chars}字（含标点约{len(content)}字）')
