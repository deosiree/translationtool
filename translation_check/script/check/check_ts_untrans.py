import os
import xml.etree.ElementTree as ET

def check_untranslated_ts_files(directory):
    """检查目录下所有 .ts 文件中是否有未翻译的词条，并打印信息"""
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".ts"):
                ts_file_path = os.path.join(root, file)
                check_untranslated_entries(ts_file_path)

def check_untranslated_entries(ts_file):
    """检查单个 .ts 文件中未翻译的词条"""
    try:
        tree = ET.parse(ts_file)
        root = tree.getroot()
        untranslated_entries = []

        for message in root.findall(".//message"):
            source_text = message.find("source").text if message.find("source") is not None else "N/A"
            translation = message.find("translation")

            # 判断是否未翻译（type="unfinished"）
            if translation is not None and translation.attrib.get("type") == "unfinished" and translation.text is None:
                # 获取行号信息
                location = message.find("location")
                line_info = f"Line {location.attrib['line']}" if location is not None else "Unknown line"
                untranslated_entries.append((source_text, line_info))

        # 如果有未翻译的词条，打印信息
        if untranslated_entries:
            print(f"\n🔍 未翻译词条发现于文件: {ts_file}")
            for source, line in untranslated_entries:
                print(f"  {line}: \"{source}\" (未翻译)")
        else:
            print(f"✅ 文件 {ts_file} 所有词条均已翻译")

    except Exception as e:
        print(f"❌ 解析 {ts_file} 时出错: {e}")
        

if __name__ == "__main__":
    directory_path = input("请输入需要检查的目录路径: ").strip()
    if os.path.isdir(directory_path):
        check_untranslated_ts_files(directory_path)
    else:
        print("❌ 目录路径无效，请输入正确的路径！")
