# This is a sample Python script.
import os
import fnmatch
import re
import argparse
import time
import sys
import datetime
import xlwt
import xml.etree.ElementTree as ET
import shutil
import re
import json
from pathlib import Path
import sys
import io
import subprocess

# !!! 平台和监控的脚本 替换 jk/pt 和 env.sh和linux_env.sh !!!


extracted_variables = {}
schema_type = {"comdb","scadamdl","alarmmdl","eventinfo","agvcmdl","appcfg","calcmdl","capmdl"}
plat_schema_path = set()
app_schema_path = set()
# L_UPDATE = "E:\\QT\\5.12.4\\winrt_x86_msvc2017\\bin\\lupdate.exe"
lupdatePath = ''
L_UPDATE = lupdatePath
DEST_LIST = [('zh', 'zh_CN'), ('en', 'en_US')]
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')


rec = re.compile(
    r'(i18n_tr|i18n_getTranslateStr)\s*\(\s*"(?P<target>.+?)"\s*(?:,\s*"(?P<tag>[^"]*)")?\s*(?:,\s*"(?P<third>[^"]*)")?\s*\)',
    re.IGNORECASE
)

extracted_variables = {}
extracted_variables["QT_VERSION_MAJOR"] = "5"
extracted_variables["QT_VERSION_MINOR"] = "12"
extracted_variables["QT_VERSION_PATCH"] = "4"
def print_success(name):
    # Use a breakpoint in the code line below to debug your script.
    print(f'success ! output file path : , {name}')  # Press Ctrl+F8 to toggle the breakpoint.


def extract_header_paths(cmake_file):
    file_path1 =""
    # str(Path(file_path).parent) 存储到set中
    file_paths_set = set()
    file_paths_set.add(systemPath + "/src/3rd")
    with open(cmake_file, 'r', encoding='utf-8') as file:
        content = file.read()


        # Match set(HEADER_LIST ...) block
        match = re.search(r'set\s*\(\s*HEADER_LIST\s*(.*?)\)', content, re.DOTALL)
        if match:
            # Extract file paths within the block
            file_paths = match.group(1).split()

            for file_path in file_paths:
                # 如果行首以#开头则跳过
                if file_path.startswith('#'):
                    continue

                file_path = file_path.strip()
                file_path = replace_variables(file_path, systemPath, extracted_variables)


                # 如果file_path 以.h 或者.hpp 结尾 则获取上层路径
                if file_path.endswith(('.h', '.hpp', '.cpp')):
                    #如果只是文件名，则获取路径
                    if "." == str(Path(file_path).parent):
                        file_paths_set.add(str(Path(cmake_file).parent))
                        continue;
                    else:
                        file_paths_set.add(str(Path(file_path).parent))
                        continue;
                if file_path and not file_path.startswith('#'):
                    file_paths_set.add(str(Path(file_path).parent))
    file_path1 = " ".join(f"-I {path}" for path in file_paths_set)
    return file_path1

#匹配camke list set(var value) 形式
def parse_cmake(file_path):
    variables = {}
    h_file_paths = set()
    with open(file_path, 'r') as f:
        content = f.read()
        # Match set(SOURCE_LIST ...) block
        match = re.search(r'set\s*\(\s*SOURCE_LIST\s*(.*?)\)', content, re.DOTALL)
        if match:
            # Extract file paths within the block
            file_paths = match.group(1).split()
            for file_path in file_paths:
                # Normalize and resolve paths
                file_path = file_path.strip()
                file_path = replace_variables(file_path, systemPath, extracted_variables)
                if file_path.endswith(('.h', '.hpp')):
                    h_file_paths.add(str(Path(file_path).parent))
    return list(h_file_paths)

def get_boost_include_dirs(cmake_file):
    # Run cmake to get the cache variables
    result = subprocess.run(['cmake', '-LAH', cmake_file], capture_output=True, text=True)

    # Check if the command was successful
    if result.returncode != 0:
        print(f"Error running cmake: {result.stderr}")
        return None

    # Search for Boost_INCLUDE_DIRS in the output
    match = re.search(r'Boost_INCLUDE_DIRS:PATH=(.*)', result.stdout)
    if match:
        return match.group(1)
    else:
        print("Boost_INCLUDE_DIRS not found in CMake output")
        return None

def make_cmakelist(src_root, fileType):
    m = []
    current_address = src_root
    for parent, dirnames, filenames in os.walk(current_address):
        if "netsec" in parent:
            continue
        for filename in filenames:
            if fnmatch.fnmatch(filename, fileType):
                k = (parent, filename)
                m.append(k)
    return m

def replace_variables(entry, systemPath, extracted_variables):
    replacements = {
        "$ENV{SRC_ROOT_DIR}": systemPath + "/src",
        "${SRC_ROOT_DIR}": systemPath + "/src",
        "$ENV{ROOT_DIR}": systemPath,
        "${ROOT_DIR}": systemPath,
        "${CMAKE_CURRENT_SOURCE_DIR}": systemPath + "/src",
        "$ENV{CMAKE_CURRENT_SOURCE_DIR}": systemPath + "/src",
        "${THIRD_PART_DIR}": systemPath + "/src/3rd",
        "$ENV{THIRD_PART_DIR}": systemPath + "/src/3rd",
        "${APP_ROOT_DIR}": systemPath + "/src/app",
        "$ENV{APP_ROOT_DIR}": systemPath + "/src/app",
        "${PLAT_ROOT_DIR}": systemPath + "/src/plat",
        "$ENV{PLAT_ROOT_DIR}": systemPath + "/src/plat",
        "${RUN_ROOT_DIR}": systemPath + "/run",
        "$ENV{RUN_ROOT_DIR}": systemPath + "/run",
        "$ENV{QTDIR}": extracted_variables.get("QTDIR"),
        "${QTDIR}": extracted_variables.get("QTDIR"),
        "$ENV{QT_VERSION_MAJOR}": extracted_variables.get("QT_VERSION_MAJOR", ""),
        "${QT_VERSION_MAJOR}": extracted_variables.get("QT_VERSION_MAJOR", ""),
        "$ENV{QT_VERSION_MINOR}": extracted_variables.get("QT_VERSION_MINOR", ""),
        "${QT_VERSION_MINOR}": extracted_variables.get("QT_VERSION_MINOR", ""),
        "$ENV{QT_VERSION_PATCH}": extracted_variables.get("QT_VERSION_PATCH", ""),
        "${QT_VERSION_PATCH}": extracted_variables.get("QT_VERSION_PATCH", ""),
        "${Boost_INCLUDE_DIRS}": os.path.dirname(os.path.abspath(systemPath))  + "/boost_1_78_0/include/boost-1_78/boost",
        "${BOOST_ROOT}": os.path.dirname(os.path.abspath(systemPath))  + "/boost_1_78_0/boost",
        "$ENV{BOOST_ROOT}": os.path.dirname(os.path.abspath(systemPath))  + "/boost_1_78_0/boost",
        "$ENV{Boost_INCLUDE_DIRS}": os.path.dirname(os.path.abspath(systemPath))  + "/boost_1_78_0/include/boost-1_78/boost",
        "${target} PRIVATE": "",
        "${target} private": "",
        "${target} public": ""


    }

    for key, value in replacements.items():
        if key in entry:
            entry = entry.replace(key, value)

    return entry

#获取头文件路径，返回头文件路径集合（-I path）
def find_include_directories(cmake_file):


    include_paths = set()
    h_file_paths = set()

    # Match lines like include_directories(<path>) or target_include_directories(<target> <path>)
    # This regex will capture content inside parentheses, quotes, or after a space.
    include_pattern = re.compile(r"(?:include_directories|target_include_directories|INCLUDE_DIRECTORIES)\s*\(([^)]+)\)")
    quote_pattern = re.compile(r"[\"']([^\"']+\.(?:h|hpp))[\"']")

    #Boost_INCLUDE_DIRS 为systemPath 的上上层目录



    with open(cmake_file, 'r', encoding='utf-8') as cmake:
        for line in cmake:
            # Check for include-related commands
            #如果开头是# 则跳过
            if line.find("schema.cmake") != -1:
                print(" -- schema.cmake -- ")
                # 如果include_paths 为空则 include_paths = plat_schema_path.union(app_schema_path)
                if len(include_paths) == 0:
                    include_paths = plat_schema_path.union(app_schema_path)
                include_paths.union(app_schema_path)
                include_paths.union(plat_schema_path)

                continue
            #打印include_paths内容


            if line.startswith("#"):
                continue
            match = include_pattern.search(line)
            if match:
                # Split potential paths within the cfommand
                entries = match.group(1).split()

                for entry in entries:
                    # If it looks like a .h file
                    # 获取cmake 路径
                    if "${target}" in entry or "PRIVATE" in entry or "private" in entry:
                        continue

                    #F:\project\翻译工具\词条抽取\shr\system
                    entry = replace_variables(entry, systemPath, extracted_variables)
                    if entry.find("$ENV{CMAKE_CURRENT_BINARY_DIR}") != -1:
                        entry = entry.replace("$ENV{CMAKE_CURRENT_BINARY_DIR}", os.path.dirname(cmake.name))
                    if entry.find("${CMAKE_CURRENT_BINARY_DIR}") != -1:
                        entry = entry.replace("${CMAKE_CURRENT_BINARY_DIR}", os.path.dirname(cmake.name))
                    if entry.find("${CMAKE_BINARY_DIR}") != -1:
                        entry = entry.replace("${CMAKE_BINARY_DIR}", os.path.dirname(cmake.name))
                    if entry.find("$ENV{CMAKE_BINARY_DIR}") != -1:
                        entry = entry.replace("$ENV{CMAKE_BINARY_DIR}",os.path.dirname(cmake.name))
                    if entry.find("$ENV{PROJECT_SOURCE_DIR}") != -1:
                        entry = entry.replace("$ENV{PROJECT_SOURCE_DIR}", os.path.dirname(cmake.name))
                    if entry.find("${PROJECT_SOURCE_DIR}") != -1:
                        entry = entry.replace("${PROJECT_SOURCE_DIR}", os.path.dirname(cmake.name))

                    #entry 里面包含了几个../ 就取几层目录
                    if entry.find("../") != -1:
                        #遍历entry中的../ 替换为cmake_file的上层目录
                        for i in range(entry.count("../")):
                            entry = entry.replace("../", os.path.dirname(os.path.dirname(cmake_file)) + '/')
                    if "./" in entry:
                        # 替换entry中..为cmake_file的上层目录
                        entry = entry.replace("./", os.path.dirname(cmake_file) + '/')
                    if entry ==".":
                          entry = os.path.dirname(cmake_file)
                    if entry.endswith((".h", ".hpp")):
                        h_file_paths.add(entry)
                    else:
                        # Otherwise consider it a directory or variable
                        include_paths.add(entry)
            # Check for directly quoted .h files
            quote_match = quote_pattern.search(line)

            if quote_match:
                h_file_paths.add(quote_match.group(1))

    # Resolve directories for .h files
    parent_paths = {str(Path(file).parent) for file in h_file_paths}


    for include_path in include_paths:
        for root, _, files in os.walk(include_path):
            for file in files:
                file_path = os.path.join(root, file)
                parent_paths.add(os.path.dirname(file_path))
    p = ''
    s = set()
    if len(parent_paths) == 0:
        return p
    #打印includepath
    for path in sorted(include_paths):

        if path.find("/ACE_wrappers") != -1:
            path = path.replace("/ACE_wrappers", "/ACE_wrappers/*")
        s.add(path)

        p = " ".join(f" -I {path}" for path in s)

    return p


def run_command(cmd):
    try:
        result = subprocess.run(cmd, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        print("Command output:", result.stdout)
    except subprocess.CalledProcessError as e:
        print("Command failed with error:", e.stderr)

def make_ts(flist, ts_root):
    # 寻找target的正则

   # rec = re.compile(r'SET\(target(?P<target>.+?)\)', re.IGNORECASE | re.MULTILINE)
    rec = re.compile(r'SET\(target\b\s+(?P<target>\S+)\)', re.IGNORECASE | re.MULTILINE)
    # 按照语言预定义创建目录
    for (dir, name) in DEST_LIST:
        path = os.path.join(ts_root, name, 'ts')    # /home/shr/code/system/run/lang/(zh_CN,en_US)/ts
        #os.makedirs(path, exist_ok=True)
        #if os.path.exists(path):
        #    shutil.rmtree(path)
        #os.mkdir(path)
        #dicpath = os.path.join(ts_root, name, 'rd')
        #os.makedirs(dicpath, exist_ok=True)
        #utility.delete_dir(path)
        #utility.confirm_dir_exist(path)

    for (path, name) in flist:
        filePath = path + '/' + name

        #path = parse_cmake_file(filePath)
        cpplist = make_cmakelist(path, "*")
        print('read file', filePath)
        if (os.path.exists(filePath) == False):
            print(filePath, 'not exists')
        openok = False
        try:

            fcmake = open(filePath, "r", encoding="utf-8")
            strcontext = fcmake.read()
            openok = True
        except Exception as e:
            print(e)
        if not openok:
            try:
                fcmake = open(filePath, "r", encoding="gbk")
                strcontext = fcmake.read()
                openok = True
            except Exception as e:
                print(e)
                continue
        # 当前cmake生成模块，查找cmake的目标，如果是dir或者是测试target直接跳过
        m = strcontext.find("add_executable")
        n = strcontext.find("add_library")
        s = strcontext.find("set")
        s = strcontext.find("file")
        regMatch = rec.search(strcontext)
        if (regMatch == None):
            print('no target existed', 'path\n')
            continue
        # 取target名字
        targetName = regMatch.groupdict().get('target').strip()
        targetName = targetName.replace(" ","")
        #if strcontext.find("add_executable") != -1 or strcontext.find("add_library") != -1:
        if "/test/" in filePath:
            print(' ---- test target continue ----', 'path\n')
            continue
        print(' -----====---- path', path)
        if (os.path.basename(path).find('test') != -1):
            print(' ---- test target continue ----', 'path\n')
            continue
        xlsx_target = os.path.join(ts_root, 'dic',"pt", targetName + '.dic')
        get_excel(cpplist, xlsx_target, targetName)

        include = find_include_directories(filePath)
        se = extract_header_paths(filePath)
        include = include + " " + se
        #parse_cmake(filePath)
        for (dir, name) in DEST_LIST:
            # 抽取i18n_tr词条
            # xlsx_target = os.path.join(ts_root, name, 'code_dic', targetName + "_" + name + '_common.xlsx')

            target = os.path.join(ts_root, name, 'ts', targetName + "_" + name + '.ts')

            # 生成ts文件
            cmd_parts = [
                lupdatePath,
                include,
                path,
                "-ts",
                target
            ]
            #cmd = " ".join(cmd_parts)
            cmd = lupdatePath + ' -no-obsolete' +  include + " " + path + " -ts " + target
            print(cmd)
            status = os.system(cmd)
            run_command(cmd)
            if status ==1:
                return
            # 检查文件是否有词条
            print(check_ts_content(target))

def parse_cmake_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as file:
        lines = file.readlines()

    cpp_file_paths = []
    base_dir = os.path.dirname(os.path.abspath(filepath))  # 获取文件的上层路径
    inside_file_glob = False
    for line in lines:
        stripped_line = line.strip()

        # 检测是否是 file( 开始的行
        if stripped_line.startswith("file("):
            inside_file_glob = True
            continue

        # 如果进入 file(GLOB 块中，处理多行的 .cpp
        if inside_file_glob:
            if stripped_line.endswith(")"):
                inside_file_glob = False

            # 使用正则提取所有的 .cpp 文件路径
            matches = re.findall(r'\$.*[\/\w\*]+\.cpp', stripped_line, re.IGNORECASE)
            for match in matches:
                # 替换 ${CMAKE_CURRENT_SOURCE_DIR} 为实际路径
                resolved_path = match.replace("${CMAKE_CURRENT_SOURCE_DIR}", base_dir).replace("${PLAT_ROOT_DIR}", base_dir)
                full_path = os.path.normpath(resolved_path)
                cpp_file_paths.append(full_path)
    # 获取最上层路径
    if cpp_file_paths:
        top_most_path = os.path.commonpath(cpp_file_paths)
        return top_most_path
    return os.path.dirname(filepath)

def check_ts_content(file_path):
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()

        # 查找 <TS> 标签
        aa =  root.__len__()
        if  root.tag == "TS" and root.__len__() >0:
            return " 检查 【" + file_path + "】 文件成功 √"
        else:
            os.remove(file_path)
            return " 检查 【" + file_path + "】 文件中不存在词条 ，已删除文件 ！"


    except ET.ParseError:
        return "The file is not a valid XML file."


def get_words(flist, fileName):
   # rec = re.compile(r'(i18n_tr|i18n_getTranslateStr)\("(?P<target>.+?)"\)', re.IGNORECASE)
    fR = open(fileName, "w+", encoding="utf-8")
    for (path, name) in flist:
        filePath = path + '/' + name
        with open(filePath, "r", encoding="utf-8") as temp_f:  # 目前只处理utf-8文件
            try:
                datafile = temp_f.readlines()
            except Exception as e:
                print(filePath, e)
                continue
            for line in datafile:
                regMatch = rec.search(line)
                if (regMatch == None):
                    continue
                result = extract_i18n_tr(line)
                for item in result:
                    print(f"词条: {item[0]}, tag: {item[1]}")
                linebits = regMatch.groupdict()
                for k, v in linebits.items():
                    str = v + ',' + filePath + '\n'
                    fR.write(str)

    fR.close()


def extract_i18n_tr(content):
    matches = rec.findall(content)
    results = []

    for match in matches:
        target = match[1]  # 获取词条
        tag = match[2] if match[2] else ""  # 获取tag，如果没有tag则返回空字符串
        third = match[3] if match[3] else ""  # 获取第三个参数，如果不存在则返回空字符串
        # 假设第三个参数为英语翻译（en_US）
        translation_en_us = third if third else ""
        results.append({
            "comments": translation_en_us,
            "source": target,
            "tag": tag,
            "translation": {
                "en_US": ""
            }
        })

    return results



def load_existing_dic(file_path):
    print(file_path)
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as dic_file:
            return json.load(dic_file)
    return []

def save_dic(data, file_path):
    # 创建文件夹（如果不存在）
    folder = os.path.dirname(file_path)
    if not os.path.exists(folder):
        os.makedirs(folder)

    # 如果文件不存在，创建文件
    if not os.path.exists(file_path):
        with open(file_path, 'w', encoding='utf-8') as dic_file:
            json.dump([], dic_file, indent=4, ensure_ascii=False)

    # 写入数据
    with open(file_path, 'w', encoding='utf-8') as dic_file:
        json.dump(data, dic_file, indent=4, ensure_ascii=False)



def get_excel(flist, xlsPath,targetName):
    #rec = re.compile('(i18n_tr|i18n_getTranslateStr)\("(?P<target>.+?)"\)', re.IGNORECASE)
    work_book = xlwt.Workbook(encoding='utf-8')
    sheet_data = work_book.add_sheet('other_entry')
    sheet_data.write(1, 0, 'id')
    sheet_data.write(1, 1, 'abbr')
    sheet_data.write(1, 2, '词条')
    sheet_data.write(1, 3, '英文翻译')
    sheet_data.write(1, 4, 'source')
    sheet_data.write(1, 5, '回写词典')
    sheet_data.write(1, 6, 'entry_label')
    sheet_data.write(1, 7, 'tag')
    sheet_data.write(1, 8, 'common')

    sheet_data.write_merge(0, 0, 0, 5, 'other_entry')
    line_num = 2
    is_write = -1
    for (path, name) in flist:
        filePath = path + '/' + name
        with open(filePath, "r", encoding="utf-8") as temp_f:  # 目前只处理utf-8文件
            try:
                datafile = temp_f.readlines()
            except Exception as e:
                print(filePath, e)
                continue
            entry_num = 0
            for line in datafile:
                regMatch = rec.search(line)
                if (regMatch == None):
                    continue
                result = extract_i18n_tr(line)
                # 加载现有的 .dic 文件内容
                existing_data = load_existing_dic(xlsPath)

                # 合并现有数据和新数据
                existing_data.extend(result)

                # 保存更新后的数据
                save_dic(existing_data, xlsPath)
                entry_num = entry_num +1;
            print(f' Found [{entry_num}]  source dic , update  [{xlsPath}]  success  ! √')



def extract_environment_variables(file_path, variables_to_extract):
    """
    Extracts specified environment variables from a .bat file.

    Args:
        file_path (str): Path to the .bat file.
        variables_to_extract (list): List of variable names to extract.

    Returns:
        dict: A dictionary with variable names as keys and their values as values.
    """
    global extracted_variables
    envPath = file_path + "/linux_env.sh"
    print(envPath)
    try:
        with open(envPath, 'r', encoding='utf-8') as file:
            file_content = file.readlines()

        for line in file_content:
            #取:前面的字符串
            if ":$" in line:
                line = line.split(":")[0]
            if "%cd%" in line:
                line = line.replace("%cd%", file_path)
            if "$QTDIR" in line:
                line = line.replace("$QTDIR", extracted_variables.get("QTDIR"))
            if "$ROOT_DIR" in line:
                line = line.replace("$ROOT_DIR", extracted_variables.get("ROOT_DIR"))
            if "$THIRD_PART_DIR" in line:
                line = line.replace("$THIRD_PART_DIR", extracted_variables.get("THIRD_PART_DIR"))
            line = line.strip()
            if any(var in line for var in variables_to_extract):
                for var in variables_to_extract:
                    if line.startswith(f"export {var}="):
                        value = line.split(f"export {var}=")[-1].strip()
                        extracted_variables[var] = value

    except FileNotFoundError:
            print(f"Error: The file {envPath} was not found.")
    except Exception as e:
        print(f"An error occurred: {e}")

    return extracted_variables


def parse_cmake_variable(line, variables):
    """解析 CMake 变量并更新字典"""
    match = re.match(r"set\s*\(\s*(\w+)\s+(.+?)\s*\)", line)
    #解析set(comdb_schema_version 107) 取 comdb 放到variables

    if match:
        var_name, var_value = match.groups()
        variables[var_name] = var_value

def parse_schema_cmake(file_path):
    """解析 schema.cmake 并解析 include 路径"""
    variables = {}
    include_path = None

    with open(file_path, "r", encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if line.startswith("set("):  # 解析 set 变量
                parse_cmake_variable(line, variables)

            # 查找包含路径
            match = re.search(r"\$ENV\{SRC_ROOT_DIR\}/plat/schema/\$\{schema\}/v\$\{schema_version\}/include", line)
            if match:
                include_path = line

    if include_path:
        # 替换变量
        #遍历schema_type
        for type in schema_type:
            if type.find(type) != -1:
                src_root_dir = os.getenv("SRC_ROOT_DIR", "/default/path")
                schema = type
                schema_version = variables.get(schema + "_schema_version")
                plat_schema_path.add(f"{src_root_dir}/plat/schema/{schema}/v{schema_version}/include")
                app_schema_path.add(f"{src_root_dir}/app/schema/{schema}/v{schema_version}/include")


# Press the green button in the gutter to run the script.
# sp : system path
# lp : lupdate.exe path\
# pp : 扫描代码的根目录
# python main.py --sp \code\system\src\plat --lp \QT\5.12.4\winrt_x86_msvc2017\bin\lupdate.exe
if __name__ == '__main__':


    # 创建解析器对象
    parser = argparse.ArgumentParser(description="生成 ts 文件")
    # 添加参数
    parser.add_argument('-s', '--sp', help='cmake 文件扫描的根目录')
    parser.add_argument('-e', '--env', help='环境变量文件')
    parser.add_argument('-p', '--pp', help='ts 文件生成的根目录')
    # 解析参数
    args = parser.parse_args()

    # sys.argv = ['main.py','--pp','E:\code\system\src\plat','--tp','.\\run\\etc\\language\\','--lp','E:\\QT\\5.12.4\\winrt_x86_msvc2017\\bin\\lupdate.exe']
    # system路径
    systemPath = ''
    platPath = '/src/plat'
    tsPath = '/run/lang'

    # platPath = 'E:\code\system\src\plat'
    # tsPath = '.\\run\\etc\\language\\'
    # lupdatePath = 'E:\\QT\\5.12.4\\winrt_x86_msvc2017\\bin\\lupdate.exe'

    if args.sp is None or args.sp == '':
        print('无 -sp 参数 (systemPath)')
        sys.exit()

    if args.pp is None or args.pp == '':
        print('无 -pp 参数 (platPath)')
        sys.exit()
    if args.env is None or args.env == '':
        print('无 -pp 参数 (platPath)')
        sys.exit()
    if not os.path.exists(args.env):
        print(f'envpath [{args.env}] not exist')
        sys.exit()
    systemPath = args.sp        #/home/shr/code/system
    #lupdatePath = args.lp
    platPath = args.pp  # cmake 文件扫描的根目录,/home/shr/code/system/src/plat
    PLAT_ROOT_DIR = systemPath + "/src"
    tsPath = systemPath + tsPath  # ts 文件生成的根目录,/home/shr/code/system/run/lang


    envPath = args.env

    #读取环境变量
    variables_to_extract = [
        "THIRD_PART_DIR",
        "QMAKEPATH",
        "QTDIR",
        "QT_PLUGIN_PATH",
        "ACE_ROOT",
        "BOOST_ROOT",
        "PROTOBUF_ROOT",
        "ROOT_DIR"
    ]

    extracted_variables = extract_environment_variables(envPath, variables_to_extract)
    print(" ==== QTDIR ==== " + extracted_variables.get("QTDIR"))
    lupdatePath = extracted_variables.get("QTDIR") + "/bin/lupdate"

    #utility.confirm_dir_exist(tsPath)
    #清空产物文件夹



    #os.mkdir(tsPath)
    if not os.path.exists(platPath):
        print(f'platPath [{platPath}] not exist')
        sys.exit()
    if not os.path.exists(tsPath):
        print(f'tsPath [{tsPath}] not exist')
        sys.exit()
    #
    schema_cmake = systemPath + "/src/app/schema/schema.cmake"
    print( " **** " + schema_cmake)
    # 如果路径schema.cmake存在
    if os.path.exists(schema_cmake):
        parse_schema_cmake(schema_cmake)
    else:
        print(f"Error: The file {schema_cmake} was not found.")
    flist = make_cmakelist(platPath, "CMakeLists.txt")  #/home/shr/code/system/src/plat
    #readENV(envPath)

    #生成TS文件
    make_ts(flist, tsPath)


    #for cmakePath,cmakeList in flist:
    #    cpplist = make_cmakelist(cmakePath, "*.cpp")
    #    get_excel(cpplist, tsPath,cmakePath);
    #cpplist = make_cmakelist(platPath, "*.cpp")
    # 抽取被i18n_tr和getTranslate 包裹的词条
    #get_excel(cpplist,  tsPath);
    #


# flist = make_cmakelist("E:\code\system\src\plat", "CMakeLists.txt")
# make_ts(flist, ".\\run\\etc\\language\\")


#    flist = make_cmakelist("D:\\i18n\\system\\src\\plat\\sysmgr\\tool\\gui_sysconfig", "CMakeLists.txt")


# cpplist = make_cmakelist("D:\\shr\\system\\src\\plat","*.cpp")
# get_words(cpplist,'D:\\ts\\words.csv')
# See PyCharm help at https://www.jetbrains.com/help/pycharm/
