import os
import sys
import io
import docx
from docx import Document
from PIL import Image
import easyocr
import numpy as np

def scan_word_and_ocr(file_path):
    if not os.path.exists(file_path):
        print(f"Error: File not found -> {file_path}", file=sys.stderr)
        return

    # 初始化 EasyOCR
    reader = easyocr.Reader(['ch_sim', 'en'])
    doc = Document(file_path)

    # 逐行扫描 Word 文档段落
    for p in doc.paragraphs:
        text = p.text.strip()
        if text:
            print(text)

        # 通过 XML 节点深度检测当前行内是否存在图片
        p_element = p._p
        blip_elements = p_element.xpath('.//a:blip')

        if blip_elements:
            for blip in blip_elements:
                try:
                    embed_id = blip.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}embed')
                    if not embed_id:
                        continue

                    # 提取 Word 底层图片二进制流并读取
                    image_part = doc.part.related_parts[embed_id]
                    image_bytes = image_part.blob
                    image = Image.open(io.BytesIO(image_bytes))

                    # 4060 显卡毫秒级本地计算
                    result = reader.readtext(np.array(image))

                    # 输出图片 OCR 结果
                    if result:
                        for line in result:
                            print(line[1])

                except Exception:
                    continue

if __name__ == "__main__":
    # 检查参数数量：必须是2个（第一个是脚本名本身，第二个是Java传过来的Word路径）
    if len(sys.argv) != 2:
        print("Usage: python ocr_processor.py <word_file_path>", file=sys.stderr)
        sys.exit(1)

    word_file = sys.argv[1]
    scan_word_and_ocr(word_file)