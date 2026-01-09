<template>
    <CustomModal :visible="visible" :okLoading="loading" modalTitle="导入回填" @handleClose="handleClose"
        @handleOK="handleOK">
        <div class="content">
            <a-form ref="backFillForm" :model="formModel">
                <a-form-item label="语种" name="language" :rules="[{ required: true, message: '请选择!' }]">
                    <a-select mode="multiple" v-model:value="formModel.language" placeholder="请选择语种"
                        :options='translateTypes' :fieldNames="{ label: 'name', value: 'name' }" allowClear>
                    </a-select>
                </a-form-item>
                <a-form-item label="文件" name="backFillFile"
                    :rules="[{ required: true, validator: validateBackFillFile }]">
                    <a-upload name="file" :beforeUpload="beforeUpload" :accept="backFillAccept" :max-count="1"
                        :fileList="formModel.backFillFileList" @change="handleBackFillUpload"
                        @remove="removeBackFillFile">
                        <a-button type="primary" size="small">
                            选择
                        </a-button>
                    </a-upload>
                </a-form-item>
                <a-form-item label="IP映射" name="ipMappingFile"
                    :rules="[{ required: true, validator: validateIpMappingFile }]">
                    <a-upload name="file" :beforeUpload="beforeUpload" :accept="ipMappingAccept" :max-count="1"
                        :fileList="formModel.ipMappingFileList" @change="handleIpMappingUpload"
                        @remove="removeIpMappingFile">
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
import { entryBatchImportExcel } from "@/utils/handleExcel";
export default {
    components: {
        CustomModal,
    },
    emits: ["handleClose", "handleOK"],
    props: {
        visible: {
            type: Boolean,
            default: false,
            required: true,
        },
        translateTypes: {
            type: Array,
            default: () => [],
        },
    },
    data() {
        return {
            formModel: {
                ipMappingFile: null,
                backFillFile: null,
                language: [],
            },
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
                this.formModel.ipMappingFile = null;
            } else {
                this.formModel.ipMappingFile = info.file;
            }
        },

        handleBackFillUpload(info) {
            this.backFillFileList = info.fileList;
            if (info.fileList.length === 0) {
                this.formModel.backFillFile = null;
            } else {
                this.formModel.backFillFile = info.file;
            }
        },

        beforeUpload() {
            return false;
        },

        removeIpMappingFile() {
            this.formModel.ipMappingFile = null;
            this.ipMappingFileList = [];
            return true;
        },

        removeBackFillFile() {
            this.formModel.backFillFile = null;
            this.backFillFileList = [];
            return true;
        },

        validateIpMappingFile() {
            if (!this.formModel.ipMappingFile) {
                return Promise.reject("请选择 ip映射.json 文件！");
            }
            if (!this.formModel.ipMappingFile.name.endsWith(".json")) {
                return Promise.reject("请选择 .json 格式的文件！");
            }
            return Promise.resolve();
        },

        validateBackFillFile() {
            if (!this.formModel.backFillFile) {
                return Promise.reject("请选择 回填.csv 文件！");
            }
            if (!this.formModel.backFillFile.name.endsWith(".csv")) {
                return Promise.reject("请选择 .csv 格式的文件！");
            }
            return Promise.resolve();
        },

        async handleOK() {
            if (!this.$refs.backFillForm) return;

            this.$refs.backFillForm
                .validate()
                .then(async () => {
                    if (!this.formModel.ipMappingFile || !this.formModel.backFillFile || !this.formModel.language.length) {
                        message.error("请选择语种、回填文件与映射文件！");
                        return;
                    }

                    this.$emit("handleOK");

                    const formData = new FormData();
                    formData.append("ipMappingFile", this.formModel.ipMappingFile);
                    formData.append("file", this.formModel.backFillFile);

                    this.loading = true;
                    try {
                        // const res = await entryBackFill({}, formData);
                        // if (res.type === "SUCCESS") {
                        //     // console.log("回填成功数据", res.data.list);
                        //     this.$emit("backFillSuccess", res.data.list);
                        //     this.$emit("handleClose");
                        // } else {
                        //     message.error("回填失败：" + res.message);
                        // }

                        let rls = await entryBatchImportExcel(this.formModel.language, formData)
                        console.log("rls", rls)
                        if (rls.success) {
                            this.$emit("handleClose");
                            console.log("回填成功：", rls.success);
                        } else if (rls.error) {
                            console.log("回填失败：", rls.error)
                        }

                    } catch (error) {
                        message.error("API调用失败！");
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
            this.resetForm();
            this.$emit("handleClose");
        },
        resetForm() {
            this.formModel = {
                ipMappingFile: null,
                backFillFile: null,
                language: [],
            };
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
