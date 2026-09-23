"""
将项目文档 Markdown 文件转换为温馨色调的 HTML 页面
"""
import re
import os
import html as html_mod

# ============================================================
# 文档列表
# ============================================================
DOCS = [
    {
        "id": "proposal",
        "file": "01_项目策划书.md",
        "title": "项目策划书",
        "subtitle": "「心安动识」—— 针对不同群体的情绪焦虑躯体化动作识别与疗愈平台",
        "type": "Project Proposal",
        "icon": "📋",
    },
    {
        "id": "ppt",
        "file": "02_路演PPT大纲.md",
        "title": "路演PPT大纲",
        "subtitle": "针对不同群体的情绪焦虑躯体化动作识别与疗愈平台",
        "type": "Pitch Deck Outline",
        "icon": "📊",
    },
    {
        "id": "logo",
        "file": "03_Logo与品牌视觉方案.md",
        "title": "Logo与品牌视觉方案",
        "subtitle": "「心安动识」品牌视觉识别系统（VIS）方案",
        "type": "Brand VIS",
        "icon": "🎨",
    },
    {
        "id": "backend",
        "file": "04_Web后端与YOLO嵌入技术方案.md",
        "title": "Web后端与YOLO嵌入技术方案",
        "subtitle": "Backend Architecture & YOLO Model Integration Plan",
        "type": "Technical Design",
        "icon": "⚙️",
    },
    {
        "id": "web",
        "file": "05_Web全栈开发方案.md",
        "title": "Web全栈开发方案",
        "subtitle": "MindEase Full-Stack Web Development Plan",
        "type": "Full-Stack Plan",
        "icon": "💻",
    },
]

# ============================================================
# HTML 模板
# ============================================================
HTML_TEMPLATE = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{title} | 心安动识 MindEase</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<link rel="stylesheet" href="shared.css">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><circle cx='16' cy='16' r='15' fill='%234A90D9'/><circle cx='12' cy='13' r='3' fill='white'/><circle cx='20' cy='13' r='3' fill='white'/><path d='M12 20 Q16 25 20 20' stroke='white' stroke-width='2' fill='none' stroke-linecap='round'/></svg>">
</head>
<body>

<!-- 顶部导航栏 -->
<nav class="top-nav">
  <div class="top-nav-inner">
    <a href="01_项目策划书.html" class="nav-brand">
      <span class="logo-icon">🌸</span>
      <span>心安动识</span>
    </a>
    <div class="nav-links">
      {nav_links}
    </div>
  </div>
</nav>

<!-- 主内容区 -->
<main class="page-wrapper">
  <header class="doc-header">
    <div class="doc-type">{icon} {doc_type}</div>
    <h1>{title}</h1>
    <p class="subtitle">{subtitle}</p>
    <div class="doc-meta">
      <span>📅 版本 V2.0</span>
      <span>📝 编制日期 2026年6月</span>
      <span>🧸 心安动识 · MindEase</span>
    </div>
  </header>

  {content}

  <footer class="doc-footer">
    <div class="footer-brand">🌸 心安动识 · MindEase</div>
    <p>看见焦虑，遇见安宁</p>
    <p style="margin-top:8px;">© 2026 心安动识项目组 · 文档版本 V2.0</p>
  </footer>
</main>

<!-- 安小宁伙伴悬浮球 -->
<div class="companion-corner" title="安小宁在这里陪着你">🧸</div>
<div class="companion-tooltip">安小宁说：慢慢看，不着急～ 🌿</div>

<!-- 回到顶部 -->
<button class="back-to-top" id="backToTop" title="回到顶部">↑</button>

<script>
// 回到顶部按钮
const backToTop = document.getElementById('backToTop');
window.addEventListener('scroll', () => {{
  backToTop.classList.toggle('visible', window.scrollY > 400);
}});
backToTop.addEventListener('click', () => {{
  window.scrollTo({{ top: 0, behavior: 'smooth' }});
}});

// 目录链接平滑滚动
document.querySelectorAll('a[href^="#"]').forEach(link => {{
  link.addEventListener('click', function(e) {{
    const target = document.querySelector(this.getAttribute('href'));
    if (target) {{
      e.preventDefault();
      target.scrollIntoView({{ behavior: 'smooth', block: 'start' }});
      // 更新URL hash
      history.pushState(null, null, this.getAttribute('href'));
    }}
  }});
}});

// 高亮当前页面的导航链接
const currentPage = window.location.pathname.split('/').pop();
document.querySelectorAll('.nav-links a').forEach(link => {{
  if (link.getAttribute('href') === currentPage) {{
    link.classList.add('active');
  }}
}});
</script>

</body>
</html>'''


# ============================================================
# Markdown → HTML 转换器
# ============================================================
def convert_md_to_html(md_text):
    """将Markdown文本转换为HTML"""
    lines = md_text.split('\n')
    html_lines = []
    i = 0
    in_code_block = False
    code_lang = ''
    code_content = []
    in_table = False
    table_lines = []
    in_ascii = False
    ascii_lines = []
    list_stack = []  # (type, indent_level)

    def flush_paragraph():
        nonlocal para_lines
        if para_lines:
            text = ' '.join(para_lines).strip()
            if text:
                html_lines.append(f'<p>{process_inline(text)}</p>')
            para_lines = []

    def flush_list():
        nonlocal list_items
        if list_items:
            tag = list_stack[-1][0]
            html_lines.append(f'<{tag}l>')
            for item in list_items:
                html_lines.append(f'<li>{process_inline(item)}</li>')
            html_lines.append(f'</{tag}l>')
            list_items = []

    def flush_table():
        nonlocal table_lines, in_table
        if table_lines and len(table_lines) >= 2:
            html_lines.append('<div class="table-wrapper"><table>')
            # Header
            header_cells = [c.strip() for c in table_lines[0].split('|')[1:-1]]
            html_lines.append('<thead><tr>')
            for cell in header_cells:
                html_lines.append(f'<th>{process_inline(cell)}</th>')
            html_lines.append('</tr></thead>')
            # Body (skip separator line)
            html_lines.append('<tbody>')
            for row_line in table_lines[2:]:
                cells = [c.strip() for c in row_line.split('|')[1:-1]]
                html_lines.append('<tr>')
                for cell in cells:
                    html_lines.append(f'<td>{process_inline(cell)}</td>')
                html_lines.append('</tr>')
            html_lines.append('</tbody></table></div>')
        table_lines = []
        in_table = False

    def process_inline(text):
        """处理行内元素"""
        # Bold
        text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
        # Italic
        text = re.sub(r'(?<!\*)\*([^*\n]+?)\*(?!\*)', r'<em>\1</em>', text)
        # Inline code
        text = re.sub(r'`([^`\n]+?)`', r'<code>\1</code>', text)
        # Links
        text = re.sub(r'\[([^\]]+?)\]\(([^)]+?)\)', r'<a href="\2">\1</a>', text)
        # Strikethrough
        text = re.sub(r'~~(.+?)~~', r'<del>\1</del>', text)
        # HTML escape
        # (already partially handled)
        return text

    def close_all_blocks():
        nonlocal in_code_block, in_ascii
        if in_code_block:
            code = '\n'.join(code_content)
            lang_attr = f' class="language-{code_lang}"' if code_lang else ''
            html_lines.append(f'<pre><code{lang_attr}>{html_mod.escape(code)}</code></pre>')
            code_content.clear()
            in_code_block = False
        if in_ascii:
            art = '\n'.join(ascii_lines)
            html_lines.append(f'<div class="ascii-art">{html_mod.escape(art)}</div>')
            ascii_lines.clear()
            in_ascii = False

    para_lines = []
    list_items = []

    for line in lines:
        # Code block detection
        if line.strip().startswith('```'):
            if not in_code_block:
                close_all_blocks()
                flush_paragraph()
                flush_list()
                in_code_block = True
                code_lang = line.strip()[3:].strip()
                code_content = []
            else:
                code = '\n'.join(code_content)
                lang_attr = f' class="language-{code_lang}"' if code_lang else ''
                html_lines.append(f'<pre><code{lang_attr}>{html_mod.escape(code)}</code></pre>')
                code_content = []
                in_code_block = False
                code_lang = ''
            continue

        if in_code_block:
            code_content.append(line)
            continue

        # ASCII art detection (lines starting with specific patterns, not markdown)
        # Skip - handled in regular flow

        # Table detection
        if '|' in line and line.strip().startswith('|') and not in_table:
            close_all_blocks()
            flush_paragraph()
            flush_list()
            in_table = True
            table_lines = [line]
            continue
        if in_table:
            if '|' in line and line.strip().startswith('|'):
                table_lines.append(line)
                continue
            else:
                flush_table()
                # Fall through to process this line normally

        # Headers
        if line.startswith('#### '):
            close_all_blocks()
            flush_paragraph()
            flush_list()
            html_lines.append(f'<h4>{process_inline(line[5:].strip())}</h4>')
            continue
        if line.startswith('### '):
            close_all_blocks()
            flush_paragraph()
            flush_list()
            html_lines.append(f'<h3>{process_inline(line[4:].strip())}</h3>')
            continue
        if line.startswith('## '):
            close_all_blocks()
            flush_paragraph()
            flush_list()
            html_lines.append(f'<h2>{process_inline(line[3:].strip())}</h2>')
            continue
        if line.startswith('# '):
            close_all_blocks()
            flush_paragraph()
            flush_list()
            html_lines.append(f'<h1>{process_inline(line[2:].strip())}</h1>')
            continue

        # Horizontal rules
        if line.strip() == '---' or line.strip() == '***':
            close_all_blocks()
            flush_paragraph()
            flush_list()
            html_lines.append('<hr class="gradient">')
            continue

        # Blockquotes
        if line.startswith('> '):
            close_all_blocks()
            flush_paragraph()
            flush_list()
            # Collect blockquote lines
            bq_lines = [line[2:]]
            j = i + 1
            while j < len(lines) and lines[j].startswith('> '):
                bq_lines.append(lines[j][2:])
                j += 1
            # Advance outer loop
            # We'll handle by setting a flag
            html_lines.append(f'<blockquote><p>{process_inline(" ".join(bq_lines))}</p></blockquote>')
            # Skip these lines
            for _ in range(len(bq_lines) - 1):
                next(iter([]), None)  # Can't easily skip in for loop
            continue

        # Unordered lists
        ul_match = re.match(r'^(\s*)[-*]\s+(.+)', line)
        if ul_match:
            if list_stack and list_stack[-1][0] != 'u':
                flush_list()
                list_stack.pop()
            close_all_blocks()
            flush_paragraph()
            if not list_stack:
                list_stack.append(('u', 0))
            list_items.append(ul_match.group(2))
            continue

        # Ordered lists
        ol_match = re.match(r'^(\s*)\d+\.\s+(.+)', line)
        if ol_match:
            if list_stack and list_stack[-1][0] != 'o':
                flush_list()
                list_stack.pop()
            close_all_blocks()
            flush_paragraph()
            if not list_stack:
                list_stack.append(('o', 0))
            list_items.append(ol_match.group(2))
            continue

        # Continuation of list item (indented)
        if list_items and line.startswith('    ') or line.startswith('\t'):
            # Append to last list item
            list_items[-1] += ' ' + line.strip()
            continue

        # Empty line
        if line.strip() == '':
            close_all_blocks()
            flush_paragraph()
            flush_list()
            list_stack.clear()
            list_items = []
            continue

        # Regular paragraph text
        flush_list()
        list_stack.clear()
        list_items = []
        para_lines.append(line)

    # Flush remaining
    close_all_blocks()
    flush_paragraph()
    flush_list()
    flush_table()

    return '\n'.join(html_lines)


def extract_toc(md_text):
    """提取目录链接"""
    toc_items = []
    for line in md_text.split('\n'):
        if line.startswith('## '):
            title = line[3:].strip()
            anchor = title.lower().replace(' ', '-').replace('（', '').replace('）', '')
            toc_items.append((title, anchor))
    return toc_items


def wrap_sections(html_content):
    """将HTML内容包裹在 section div 中"""
    # Split by h2 headers and wrap
    parts = re.split(r'(<h2>.+?</h2>)', html_content)
    result = []
    i = 0
    while i < len(parts):
        part = parts[i]
        if re.match(r'<h2>', part):
            result.append('<section class="content-section">')
            result.append(part)
            if i + 1 < len(parts):
                result.append(parts[i + 1])
                i += 1
            result.append('</section>')
        else:
            result.append(part)
        i += 1

    # Also wrap content before first h2
    final = '\n'.join(result)
    # Find first <h2>
    first_h2 = final.find('<h2>')
    if first_h2 > 0:
        before = final[:first_h2]
        after = final[first_h2:]
        # Check if there's already a <section> tag
        if '<section' not in before[:200]:
            final = before + after  # Keep as is
    return final


def build_nav_links(current_id):
    """构建导航链接"""
    links = []
    for doc in DOCS:
        href = f"{doc['file'].replace('.md', '.html')}"
        active_class = ' class="active"' if doc['id'] == current_id else ''
        links.append(f'<a href="{href}"{active_class}>{doc["icon"]} {doc["title"]}</a>')
    return '\n      '.join(links)


def generate_html(doc_info):
    """生成单个HTML文件"""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    # md文件在项目文档目录下（与脚本同目录）
    md_path = os.path.join(base_dir, doc_info['file'])

    if not os.path.exists(md_path):
        print(f"  ⚠️ 文件不存在: {md_path}")
        return

    with open(md_path, 'r', encoding='utf-8') as f:
        md_text = f.read()

    # Remove YAML frontmatter if present
    md_text = re.sub(r'^---\n.*?\n---\n', '', md_text, flags=re.DOTALL)

    # Convert markdown
    html_body = convert_md_to_html(md_text)

    # Wrap sections
    html_body = wrap_sections(html_body)

    # Build HTML
    nav_links = build_nav_links(doc_info['id'])
    html_output = HTML_TEMPLATE.format(
        title=doc_info['title'],
        subtitle=doc_info['subtitle'],
        doc_type=doc_info['type'],
        icon=doc_info['icon'],
        nav_links=nav_links,
        content=html_body,
    )

    # Write file
    output_dir = os.path.join(base_dir, 'html')
    output_path = os.path.join(output_dir, doc_info['file'].replace('.md', '.html'))
    os.makedirs(output_dir, exist_ok=True)

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html_output)

    print(f"  ✅ {doc_info['file'].replace('.md', '.html')}")


# ============================================================
# Main
# ============================================================
if __name__ == '__main__':
    print("\n🌸 心安动识 — 文档HTML转换器\n")
    print("正在生成温馨治愈风格的HTML文档...\n")

    for doc in DOCS:
        generate_html(doc)

    print(f"\n🧸 转换完成！共生成 {len(DOCS)} 个HTML文件")
    print(f"📁 输出目录: {os.path.join(os.path.dirname(os.path.abspath(__file__)), 'html')}")
    print("💚 安小宁祝您使用愉快～\n")
