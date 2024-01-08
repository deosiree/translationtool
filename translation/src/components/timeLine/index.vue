<template>
    <div class="timeaxis" >
        <template v-for="(item,index) in taskList" :key="index">
            <div class="box" v-if="index === 0" style="margin-left:50px">
                <div class="circular circular_green">
                    <div class="bottom_info">
                        <span class="user">{{item.userType}}:{{item.user}}</span>
                        <span class="info" v-if="item.operateTime != '' && item.operateTime != null">任务下发时间：</span>
                        <span class="info">{{item.operateTime}}</span>
                    </div>
                </div>
            </div>
            <div class="box" v-else>
                <div :class="task.state >= item.state ? 'line line_green' : 'line line_grey'"></div>
                <div :class="task.state >= item.state ? 'circular circular_green' : 'circular circular_grey'">
                    <div class="top_info">
                        <div v-if="showButton" class="buttonBox">
                            <a-button type="primary" ghost size="small" v-if="item.state === '1'" :disabled="task.state === item.state ? false : true" @click="importEntry">导入</a-button>
                            <a-button type="primary" ghost size="small" v-if="item.state === '2'" :disabled="task.state === item.state ? false : true" @click="examineEntry">词条审核</a-button>
                            <a-button type="primary" ghost size="small" v-if="item.state === '3'" :disabled="task.state === item.state ? false : true" @click="translateEntry">翻译</a-button>
                            <a-button type="primary" ghost size="small" v-if="item.state === '4'" :disabled="task.state === item.state ? false : true" @click="examineTranslate">翻译审核</a-button>
                            <a-button type="primary" ghost size="small" v-if="item.state != '5'" :disabled="task.state === item.state ? false : true" @click="submitTask">递交</a-button>
                            <a-button type="primary" ghost size="small" v-if="item.state === '5'" :disabled="task.state === item.state ? false : true">导出</a-button>
                        </div>
                        <span v-if="!showButton && item.state === task.state">
                            <span v-if="task.state === '6'">已完成</span>
                            <span v-else>当前流程</span>
                        </span>
                    </div>
                    <div class="bottom_info">
                        <span class="user">{{item.userType}}:{{item.user}}</span>
                        <span class="info" v-if="item.operateTime != '' && item.operateTime != null">操作时间：</span>
                        <span class="info" v-if="item.operateTime != '' && item.operateTime != null">{{item.operateTime}}</span>
                    </div>
                </div>
            </div>
        </template>

    </div>
</template>
<script>
import { message ,Modal} from 'ant-design-vue'
import { defineComponent, ref, createVNode } from 'vue';
import {
  ExclamationCircleOutlined
} from '@ant-design/icons-vue';
import {
    updateTaskInfo
} from '@/http/api/task'
import {
    getEntryTempByTaskID
} from '@/http/api/workbench'
export default {
    props:{
        showButton:{
            type: Boolean,
            default: true
        },
        currentTask:{}
    },
    emits:['importEntry','examineEntry','translateEntry','examineTranslate','refresh'],
    data(){
        return {
            task:{},
            taskList:[],
            currentUser:{}
        }
    },
    mounted(){
        //保证初次传的值给到
        this.task = this.currentTask
        this.init()
    },
    
    watch:{
        currentTask(newval,oldval){
            this.task = newval
            // console.log(newval)
            this.init()
        },
    },
    methods:{
        init(){
            // 获取登录用户名
            this.currentUser = this.$store.state.user
            this.taskList = []
            if(this.task.creator != ''){
                let item = {
                    userType:'管理员',
                    user:this.task.creator,
                    operateTime:this.task.deliveryTime,
                    state:'0'
                }
                this.taskList.push(item)
            }
            if(this.task.developer != ''){
                let item = {
                    userType:'开发员',
                    user:this.task.developer,
                    operateTime:this.task.importTime,
                    state:'1'
                }
                this.taskList.push(item)
            }
            if(this.task.entryAuditor != ''){
                let item = {
                    userType:'词条审核员',
                    user:this.task.entryAuditor,
                    operateTime:this.task.entryAutiorStartTime,
                    state:'2'
                }
                this.taskList.push(item)
            }
            if(this.task.translator != ''){
                let item = {
                    userType:'翻译员',
                    user:this.task.translator,
                    operateTime:this.task.translateStartTime,
                    state:'3'
                }
                this.taskList.push(item)
            }
            if(this.task.translationAuditor != ''){
                let item = {
                    userType:'翻译审核员',
                    user:this.task.translationAuditor,
                    operateTime:this.task.translationAuditorStartTime,
                    state:'4'
                }
                this.taskList.push(item)
            }
            if(this.task.developer != ''){
                let item = {
                    userType:'导出',
                    user:this.task.developer,
                    operateTime:this.task.endTime,
                    state:'5'
                }
                this.taskList.push(item)
            }
            
        },
        // 词条导入
        importEntry(){
            this.$emit('importEntry')
        },
        // 词条审核
        examineEntry(){
            this.$emit('examineEntry')
        },
        // 词条翻译
        translateEntry(){
            this.$emit('translateEntry')
        },
        // 翻译审核
        examineTranslate(){
            this.$emit('examineTranslate')
        },
        submitTask(){
            Modal.confirm({
                title: '是否确定递交?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    let index = this.taskList.findIndex(item => item.state === this.task.state)
                    // console.log(index)
                    let operateTime = new Date().toLocaleString().replaceAll('/','-')
                    if(this.task.state === '1'){
                        this.task.importTime = operateTime
                    }else if(this.task.state === '2'){
                        this.task.entryAutiorStartTime = operateTime
                    }else if(this.task.state === '3'){
                        this.task.translateStartTime = operateTime
                    }else if(this.task.state === '4'){
                        this.task.translationAuditorStartTime = operateTime
                    }else if(this.task.state === '5'){
                        this.task.endTime = operateTime
                    }
                    if(this.task.state === '2' || this.task.state === '4'){
                        // 词条审核   翻译审核
                        let params = {
                            taskID: this.task.id,
                            pageIndex: -1,
                            pageSize: -1
                        }
                        let flag = true
                        getEntryTempByTaskID(params).then((res) => {
                            res.data.list.forEach(element => {
                                if(element.auditState === 0){
                                    flag = false
                                }
                            });
                            if(flag){
                                // 下一步
                                this.task.state = this.taskList[index + 1].state
                                this.updataTask("任务已递交！")
                            }else{
                                // 上一步
                                this.task.state = this.taskList[index - 1].state
                                this.updataTask("任务已驳回！")
                            }
                            
                        })
                        
                    }else{
                        // 下一步
                        this.task.state = this.taskList[index + 1].state
                        this.updataTask("任务已递交！")
                    }
                }
            });
            
        },
        updataTask(msg){
            updateTaskInfo(this.task).then((res) => {
                message.success(msg)
                this.$emit('refresh')
            })
        }
    }
}
</script>
<style scoped lang='less'>
.timeaxis{
    width: 100%;
    // height: 130px;
    color: black;
    display: flex;
    align-items: center;
    // border: 1px solid red;
    margin-top: 30px;
}
.box{
    // height: 100%;
    display: flex;
    align-items: center;
    // border: 1px solid red;
    

    .line{
        width: 260px;
        height: 0.1px;
    }

    .line_green{
        border-top: 2px solid #36BF7D;
    }

    .line_grey{
        border-top: 2px solid #DCDCDC;
    }

    .circular{
        width: 8px;
        height: 8px;
        border-radius: 8px;
        position: relative;
    }

    .circular_green{
        border: 1px solid #36BF7D;
        background-color: #36BF7D;
    }
    .circular_grey{
        border: 1px solid #DCDCDC;
        background-color: #DCDCDC;
    }

    .top_info{
        width: 130px;
        height: 30px;
        // border: 1px solid red;
        position: absolute;
        bottom: 8px;
        left: 50%;
        transform: translateX(-50%);
        display: flex;
        align-items: center;
        justify-content: center;

        span{
            color: var(--positive-green-normal, #36BF7D);
            /* 五级文字/常规 */
            font-family: Microsoft YaHei;
            font-size: 12px;
            font-style: normal;
            font-weight: 400;
            line-height: 20px;
        }
    }
    .bottom_info{
        width: 130px;
        height: 100px;
        // border: 1px solid red;
        position: absolute;
        top: 8px;
        left: 50%;
        transform: translateX(-50%);
        display: flex;
        align-items: center;
        flex-direction:column;
    }

    .user{
        color: #000;
        /* 五级文字/加粗 */
        font-family: Microsoft YaHei;
        font-size: 12px;
        font-style: normal;
        font-weight: 700;
        line-height: 20px; 
    }
    .info{
        color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40));
        /* 五级文字/常规 */
        font-family: Microsoft YaHei;
        font-size: 12px;
        font-style: normal;
        font-weight: 400;
        line-height: 20px;
    }
    
}

.buttonBox{
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
}

</style>
