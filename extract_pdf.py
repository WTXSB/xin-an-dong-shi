"""从原始PDF提取图片和内容"""
import fitz
import os

os.makedirs('extracted_pdf_images', exist_ok=True)

# 提取产品说明书PDF
doc = fitz.open('项目文档/心安动识产品说明书.pdf')
print(f'产品说明书PDF: {doc.page_count} 页')

for i, page in enumerate(doc):
    rect = page.rect
    print(f'  Page {i}: {rect.width:.0f}x{rect.height:.0f}')
    images = page.get_images()
    print(f'    Images: {len(images)}')
    for j, img in enumerate(images):
        xref = img[0]
        base_image = doc.extract_image(xref)
        ext = base_image['ext']
        fname = f'extracted_pdf_images/prod_page{i}_img{j}.{ext}'
        with open(fname, 'wb') as f:
            f.write(base_image['image'])
        print(f'    Saved: {fname} ({len(base_image["image"])} bytes)')

doc.close()

# 提取PPT PDF
doc2 = fitz.open('项目文档/心安动识ppt .pdf')
print(f'\nPPT PDF: {doc2.page_count} 页')

for i, page in enumerate(doc2):
    rect = page.rect
    print(f'  Page {i}: {rect.width:.0f}x{rect.height:.0f}')
    images = page.get_images()
    print(f'    Images: {len(images)}')
    for j, img in enumerate(images):
        xref = img[0]
        base_image = doc2.extract_image(xref)
        ext = base_image['ext']
        fname = f'extracted_pdf_images/ppt_page{i}_img{j}.{ext}'
        with open(fname, 'wb') as f:
            f.write(base_image['image'])
        print(f'    Saved: {fname} ({len(base_image["image"])} bytes)')

doc2.close()
print('\nDone!')
