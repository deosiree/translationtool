def filter_lines(input_file, output_file):
    # 关键字列表
    keywords = [
        "Qualifying with unknown namespace/class",
        "tr() cannot be called without context",
        "lacks Q_OBJECT macro"
    ]

    # 用于存储已写入的行，避免重复
    seen_lines = set()

    try:
        # 打开输入文件和输出文件
        with open(input_file, 'r', encoding='utf-8') as infile, open(output_file, 'w', encoding='utf-8') as outfile:
            for line in infile:
                # 如果行包含任意一个关键字，并且未写入过
                if any(keyword in line for keyword in keywords):
                    stripped_line = line.strip()
                    if stripped_line not in seen_lines:
                        outfile.write(line)
                        seen_lines.add(stripped_line)

        print(f"筛选完成，结果已保存到 {output_file}")

    except FileNotFoundError:
        print(f"错误：找不到文件 {input_file}")
    except Exception as e:
        print(f"发生错误：{e}")


if __name__ == "__main__":
    # 示例用法
    input_path = "log.txt"   # 输入文件路径
    output_path = "/home/shr/i18n/extract/result.txt"  # 输出文件路径
    filter_lines(input_path, output_path)