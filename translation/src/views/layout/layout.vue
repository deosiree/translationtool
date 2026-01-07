<template>
    <div class="layout">
        <a-row type="flex">
            <a-col :flex="menuWidth">
                <div class="menu">
                    <template v-for='(item, index) in menu' :key="index">
                        <a-tooltip placement="right" :visible="showToolTip">
                            <template #title>
                                <span>{{ item.menuName }}</span>
                            </template>
                            <div :class="isActive === item.url ? 'menuItem active' : 'menuItem'"
                                @click="clickMenu(item.url, index)">
                                <img :src="isActive === item.url ? menuIcon[item.activeIcon] : menuIcon[item.icon]" />
                                <span v-show="showName">{{ item.menuName }}</span>
                            </div>
                        </a-tooltip>

                    </template>
                    <div class="closeMenu" @click="changeMenuWidth">
                        <LeftOutlined v-if="showName" />
                        <RightOutlined v-else />
                    </div>
                </div>
            </a-col>
            <a-col flex="auto">
                <div class="content">
                    <div class="contentView">
                        <router-view />
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>
</template>
<script>
import Work from '../../assets/title/work.png'
import WorkActive from '../../assets/title/work_active.png'
import Entry from '../../assets/title/entry.png'
import EntryActive from '../../assets/title/entry_active.png'
import Config from '../../assets/title/config.png'
import ConfigActive from '../../assets/title/config_active.png'
import FileManage from '../../assets/title/config.png'
import FileManageActive from '../../assets/title/config_active.png'
import {
    LeftOutlined,
    RightOutlined
} from '@ant-design/icons-vue';
export default ({
    name: 'layout',
    components: {
        LeftOutlined,
        RightOutlined
    },
    data() {
        return {
            menuIcon: { 
                'Work': Work, 'WorkActive': WorkActive, 
                'Entry': Entry, 'EntryActive': EntryActive, 
                'Config': Config, 'ConfigActive': ConfigActive,
                'FileManage': FileManage, 'FileManageActive': FileManageActive,
             },
            menu: [],
            isActive: "",
            menuWidth: '90px',
            showName: true,
            showToolTip: false
        };
    },
    mounted() {
        this.$nextTick(() => {
            // 页面加载完成后执行的代码
            this.menu = this.$store.state.menu
            this.isActive = this.$route.path === '/translate' ? this.menu[0].url : this.$route.path
            this.$router.push(this.isActive)
            //   console.log(this.$route.path)
            //   if(this.menu.length > 0){
            //     this.$router.push(this.menu[0].url)
            //   }
        })
    },
    methods: {
        clickMenu(url, index) {
            this.$router.push(url)
            this.isActive = url;
            this.$store.commit("removeTabActive")
        },
        changeMenuWidth() {
            if (this.showName) {
                this.menuWidth = '35px'
                this.showName = false
                this.showToolTip = undefined
            } else {
                this.menuWidth = '90px'
                this.showName = true
                this.showToolTip = false
            }
        },
    },
})
</script>

<style lang="less" scoped>
.layout {
    width: 100%;
    height: calc(100% - 40px);
    // padding: 10px 10px 10px 0;
}

.ant-row {
    height: 100%;
}

.menu {
    width: 100%;
    // height: calc(100% - 20px);
    height: 100%;
    background-color: rgb(243, 243, 243);
    // padding: 10px 0px;
    text-align: center;
    position: relative;
    overflow: hidden;
}

.closeMenu {
    width: 100%;
    height: 20px;
    background-color: #e6e8e7;
    position: absolute;
    bottom: 0;
    color: #a8afac;
}

.content {
    width: 100%;
    height: 100%;
    background-color: rgb(243, 243, 243);
    // padding: 10px 10px 10px 0;
    position: relative;
    // overflow: hidden;

    .contentView {
        position: absolute;
        // width:calc(100% - 10px);
        // height:calc(100% - 20px);
        width: 100%;
        height: 100%;
        background-color: white;
    }
}

.menu .menuItem {
    width: 100%;
    height: 90px;
    border-bottom: 1px solid #E7E7E7;
    position: relative;
}

.menuItem img {
    width: 32px;
    height: 32px;
    position: absolute;
    left: 0;
    right: 0;
    top: 20px;
    margin: 0 auto;
}

.menuItem span {
    position: absolute;
    left: 0;
    right: 0;
    top: 50px;
    margin: 0 auto;
    text-align: center;
    font-size: 12px;
    color: #000000;
}

.menuItem:hover span {
    color: rgb(87, 159, 249);
    cursor: default
}

.active {
    background-color: white;
}

.active span {
    color: #369FFF;
}
</style>