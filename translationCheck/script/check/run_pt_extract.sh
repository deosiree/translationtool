#!/bin/bash

# 脚本名称
SCRIPT_NAME="/home/shr/code/script/i18n_extract_entry/generate_ts_linux.py"

# 日志文件名称
LOG_FILE="/home/shr/i18n/extract/log.txt"
CHECK_FILE="/home/shr/i18n/extract/check.txt"
CHECK_PY="/home/shr/code/script/i18n_extract_entry/check_extract_log.py"
# 定义参数路径（根据实际路径修改）
SYS_PATH="/home/shr/code/system"       # --sp 参数值
ENV_PATH="/home/shr/code/system"       # --env 参数值
PLAT_PATH="/home/shr/code/system/src/plat/"  # --pp 参数值

# 检查脚本文件是否存在
if [ ! -f "$SCRIPT_NAME" ]; then
  echo "错误：脚本文件 $SCRIPT_NAME 不存在！"
  exit 1
fi

# 检查参数路径是否存在
if [ ! -d "$SYS_PATH" ] || [ ! -d "$ENV_PATH" ] || [ ! -d "$PLAT_PATH" ]; then
  echo "错误：指定的路径不存在，请检查参数配置！"
  exit 1
fi

# 检查日志文件是否存在，如果存在则清空
if [ -f "$LOG_FILE" ]; then
  echo "日志文件 $LOG_FILE 已存在，正在清空..."
  > "$LOG_FILE"
else
  echo "创建新的日志文件 $LOG_FILE..."
  touch "$LOG_FILE"
fi

# 执行 Python 脚本，并传递参数
echo "开始执行 $SCRIPT_NAME，日志将保存到 $LOG_FILE..."
echo "使用的参数："
echo "--sp $SYS_PATH"
echo "--env $ENV_PATH"
echo "--pp $PLAT_PATH"
python3 "$SCRIPT_NAME" \
  --sp "$SYS_PATH" \
  --env "$ENV_PATH" \
  --pp "$PLAT_PATH" 2>&1 | tee "$LOG_FILE"

echo "---- 执行检查日志脚本 ----"
python3 "$CHECK_PY"

