#!/bin/bash

process_arguments(){

prev_command=
    for val in $@;do
        # echo ${prev_command}
        if [ ${val} = "-p" ]; then
            prev_command=${val}
        elif [ ${val} = "--container-name" ];then
            prev_command=${val}


        elif [ ${prev_command} = "-p" ]; then
            PASSWD=${val}
        elif [ ${prev_command} = "--container-name" ]; then
            CONTAINER_NAME=${val}
        fi
    done
}

echo_result(){
    if [ $1 -eq 0 ]
    then
        echo $2
    else
        echo $3
        exit 1
    fi
}


BACKUP_FILE_PREFIX="backup" 
PASSWD="123456"
DATE=`date +"%Y-%m-%d-%H-%M-%S"`
FILE_NAME="${BACKUP_FILE_PREFIX}-${DATE}.sql"
CONTAINER_NAME=
if [ $# -ne 0 ]; then
    process_arguments $@
    if [ $? -ne 0 ];then
        echo "解析传参失败,传参为: $*"
        exit 1
    fi
fi

if [ -n "${CONTAINER_NAME}" ]
then
    HOME_PATH="/home/backup"
    CONTAINER_BACKUP_FILE_PATH="${HOME_PATH}/${FILE_NAME}"
    # 容器侧备份
    echo "当前选择备份数据库的容器名称为${CONTAINER_NAME}"
    echo "当前进行mysql数据库备份操作,容器侧\"${CONTAINER_NAME}\"备份的地址为:${CONTAINER_BACKUP_FILE_PATH}"
    docker exec ${CONTAINER_NAME} sh -c "mysqldump -p${PASSWD} --all-databases --single-transaction > ${CONTAINER_BACKUP_FILE_PATH}"
    echo_result $? "容器\"${CONTAINER_NAME}\"内部备份成功" "数据库在容器\"${CONTAINER_NAME}\"内部备份失败, 退出运行"
    # 服务器侧备份
    SERVER_FILE_DIR="/home/shr/translationtoolservice/mysqlBackup"
    SERVER_FILE_PATH="${SERVER_FILE_DIR}/${FILE_NAME}"
    echo "将备份文件传递到宿主机,宿主机侧保存的地址为:${SERVER_FILE_PATH}"
    docker cp ${CONTAINER_NAME}:${CONTAINER_BACKUP_FILE_PATH} ${SERVER_FILE_PATH}
    echo_result $? "宿主机侧备份完成" "宿主机侧数据库备份失败, 退出运行"
else
    SERVER_FILE_DIR="/home/shr/translationtoolservice/mysqlBackup"
    SERVER_FILE_PATH="${SERVER_FILE_DIR}/${FILE_NAME}"
    echo "当前进行mysql数据库备份操作, 备份的地址为:${SERVER_FILE_PATH}"
    mysqldump -p${PASSWD} --all-databases --single-transaction > ${SERVER_FILE_PATH}
    echo_result $? "备份完成" "服务器侧数据库备份失败, 退出运行"
fi



