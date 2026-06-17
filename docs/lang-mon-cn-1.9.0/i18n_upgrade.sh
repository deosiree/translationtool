#!/bin/bash
# ===================== 请修改这两个路径 =====================
# 现场正在运行的配置
# $1
if [ ! -e ${ROOT_DIR} ];then
    echo "目录: ${ROOT_DIR}不存在"
fi
LIVE_FILE="${ROOT_DIR}/run/lang/i18n.json"
# 升级包里面的配置（会被修改）
UPGRADE_FILE="./i18n.json"    # 升级包的lang
# ===========================================================
# 检查文件是否存在
if [ ! -f "$LIVE_FILE" ]; then
    echo "错误：现场配置文件不存在 $LIVE_FILE"
    exit 1
fi
if [ ! -f "$UPGRADE_FILE" ]; then
    echo "无需修改: 升级包配置文件不存在 $UPGRADE_FILE"
    exit 0
fi
echo "正在读取现场语言配置..."
# 从现场文件提取 currentLanguage
CUR_LANG=$(sed -n 's/.*"currentLanguage": "\([^"]*\)".*/\1/p' "$LIVE_FILE")
# 从现场文件提取 translateLanguage
TRA_LANG=$(sed -n 's/.*"translateLanguage": "\([^"]*\)".*/\1/p' "$LIVE_FILE")
echo "现场 currentLanguage: $CUR_LANG"
echo "现场 translateLanguage: $TRA_LANG"
# 替换升级包中的 currentLanguage
sed -i "s/\"currentLanguage\": \".*\"/\"currentLanguage\": \"$CUR_LANG\"/" "$UPGRADE_FILE"
# 替换升级包中的 translateLanguage
sed -i "s/\"translateLanguage\": \".*\"/\"translateLanguage\": \"$TRA_LANG\"/" "$UPGRADE_FILE"

echo "开始添加操作系统俄语支持"
sudo sed -i 's/^# *ru_RU.UTF-8 UTF-8/ru_RU.UTF-8 UTF-8/' /etc/locale.gen
sudo locale-gen ru_RU.UTF-8
echo "添加操作系统俄语支持完成"

echo "同步完成！升级包语言已更新为：$CUR_LANG / $TRA_LANG"
exit 0