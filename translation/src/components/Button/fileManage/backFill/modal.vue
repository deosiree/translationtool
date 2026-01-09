<template>
    <CustomModal :visible="visible" :okLoading="loading" modalTitle="导入回填" @handleClose="handleClose"
        @handleOK="handleOK" @afterClose="afterClose">
        <div class="content">
            <a-form ref="backFillForm" :model="formModel">
                <a-form-item label="ip映射.json" name="ipMappingFile" :rules="[{ validator: validateIpMappingFile }]">
                    <a-upload name="file" :beforeUpload="beforeUpload" :accept="ipMappingAccept" :max-count="1"
                        :fileList="ipMappingFileList" @change="handleIpMappingUpload" @remove="removeIpMappingFile">
                        <a-button type="primary" size="small">
                            选择
                        </a-button>
                    </a-upload>
                </a-form-item>

                <a-form-item label="回填.csv" name="backFillFile" :rules="[{ validator: validateBackFillFile }]">
                    <a-upload name="file" :beforeUpload="beforeUpload" :accept="backFillAccept" :max-count="1"
                        :fileList="backFillFileList" @change="handleBackFillUpload" @remove="removeBackFillFile">
                        <a-button type="primary" size="small">
                            选择
                        </a-button>
                    </a-upload>
                </a-form-item>
            </a-form>
        </div>
    </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { entryBackFill } from "@/http/api/entryManage";

export default {
    components: {
        CustomModal,
    },
    emits: ["handleClose", "handleOK", "afterClose", "backFillSuccess"],
    props: {
        visible: {
            type: Boolean,
            default: false,
            required: true,
        },
    },
    data() {
        return {
            formModel: {},
            ipMappingFile: null,
            backFillFile: null,
            ipMappingFileList: [],
            backFillFileList: [],
            loading: false,
            ipMappingAccept: ".json",
            backFillAccept: ".csv",
        };
    },
    watch: {
        visible(newVal) {
            if (newVal) {
                this.resetForm();
            }
        },
    },
    methods: {
        handleIpMappingUpload(info) {
            this.ipMappingFileList = info.fileList;
            if (info.fileList.length === 0) {
                this.ipMappingFile = null;
            } else {
                this.ipMappingFile = info.file;
            }
        },

        handleBackFillUpload(info) {
            this.backFillFileList = info.fileList;
            if (info.fileList.length === 0) {
                this.backFillFile = null;
            } else {
                this.backFillFile = info.file;
            }
        },

        beforeUpload() {
            return false;
        },

        removeIpMappingFile() {
            this.ipMappingFile = null;
            this.ipMappingFileList = [];
            return true;
        },

        removeBackFillFile() {
            this.backFillFile = null;
            this.backFillFileList = [];
            return true;
        },

        validateIpMappingFile() {
            if (!this.ipMappingFile) {
                return Promise.reject("请选择 ip映射.json 文件！");
            }
            if (!this.ipMappingFile.name.endsWith(".json")) {
                return Promise.reject("请选择 .json 格式的文件！");
            }
            return Promise.resolve();
        },

        validateBackFillFile() {
            if (!this.backFillFile) {
                return Promise.reject("请选择 回填.csv 文件！");
            }
            if (!this.backFillFile.name.endsWith(".csv")) {
                return Promise.reject("请选择 .csv 格式的文件！");
            }
            return Promise.resolve();
        },

        async handleOK() {
            if (!this.$refs.backFillForm) return;

            this.$refs.backFillForm
                .validate()
                .then(async () => {
                    if (!this.ipMappingFile || !this.backFillFile) {
                        message.error("请选择两个文件！");
                        return;
                    }

                    this.$emit("handleOK");

                    const formData = new FormData();
                    formData.append("ipMappingFile", this.ipMappingFile);
                    formData.append("backFillFile", this.backFillFile);

                    this.loading = true;
                    try {
                        const res = await entryBackFill({}, formData);
                        if (res.type === "SUCCESS") {
                            // console.log("回填成功数据", res.data.list);
                            this.$emit("backFillSuccess", res.data.list);
                            this.$emit("handleClose");
                        } else {
                            message.error("回填失败：" + res.message);
                        }
                    } catch (error) {
                        message.error("网络错误，请重试！");
                        console.error("回填失败：", error);
                    } finally {
                        this.loading = false;
                    }
                })
                .catch((err) => {
                    console.log("表单校验失败", err);
                });
        },

        handleClose() {
            this.$emit("handleClose");
        },

        afterClose() {
            this.resetForm();
            this.$emit("afterClose");
        },

        resetForm() {
            this.ipMappingFile = null;
            this.backFillFile = null;
            this.ipMappingFileList = [];
            this.backFillFileList = [];
            this.loading = false;
            if (this.$refs.backFillForm) {
                this.$refs.backFillForm.clearValidate();
            }
        },
    },
};
</script>

<style scoped lang="less">
.content {
    padding: 20px;
}
</style>
