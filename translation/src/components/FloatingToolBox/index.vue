<template>
  <div class="floating-tool-box">
    <!-- 悬浮按钮 -->
    <div ref="floatingButtonRef" class="floating-button" :class="{ dragging: isDragging }" :style="buttonStyle"
      @click="handleClick" @pointerdown="startDrag">
      <ToolOutlined />
    </div>

    <!-- 工具面板 -->
    <div v-if="panelVisible" v-show="panelShow" ref="toolPanelRef" class="tool-panel" :style="panelStyle"
      @mouseleave="handlePanelMouseLeave" @click.capture="handleInnerClick">
      <div style="margin-bottom: 8px;" class="tool-panel-button">
        <GitCommitButton size="small" buttonTitle="git推送" buttonClass="yellowBtn" />
      </div>
      <div style="margin-bottom: 8px;">
        <BackFillModal mode="button" size="small" buttonTitle="更新翻译" modalTitle="更新翻译" :translateTypes="translateTypes"
          :showFileTypeSelect="true" :defaultAccept="'.csv'" />
      </div>
      <div style="margin-bottom: 8px;">
        <BackFillModal_v1_5 mode="button" size="small" buttonTitle="去重回填V1.5" modalTitle="去重回填V1.5"
          :needRelationFile="true" :defaultAccept="'.csv'" />
      </div>
      <div style="margin-bottom: 8px;">
        <BackFillModal mode="button" size="small" buttonTitle="去重回填" modalTitle="去重回填" :translateTypes="translateTypes"
          :needRelationFile="true" :defaultAccept="'.csv'" />
      </div>
      <a-button block @click="closePanel">关闭</a-button>
    </div>
  </div>
</template>

<script>
import { ToolOutlined } from "@ant-design/icons-vue";
import GitCommitButton from "@/components/Button/gitCommitButton.vue";
import BackFillModal from "@/components/Button/fileManage/backFill/modal.vue";
import BackFillModal_v1_5 from "@/components/Button/fileManage/backFill/modal_v1.5.vue";
import { closeAllNotifications } from "@/utils/notificationUtils";
import { getLanguage } from "@/http/api/translate";
import "@/assets/style/common.less";

export default {
  name: "FloatingToolBox",
  components: {
    ToolOutlined,
    GitCommitButton,
    BackFillModal,
    BackFillModal_v1_5,
  },
  data() {
    return {
      // 按钮位置
      buttonPosition: {
        x: null, // null表示使用默认位置（左上角）
        y: null,
      },
      // 拖拽相关
      isDragging: false,
      dragStartPos: { x: 0, y: 0 },
      dragStartMousePos: { x: 0, y: 0 },
      // 仅用于拖拽过程（避免频繁触发 Vue 响应式更新）
      dragPointerId: null,
      dragTargetPos: { x: 0, y: 0 },
      dragRafId: null,
      dragRafPending: false,
      dragCurrentPos: { x: 0, y: 0 },
      dragHasMoved: false,
      lastDragEndTime: 0,
      // 面板显示
      panelVisible: false,
      // 用于强制每次打开面板时重新计算 panelStyle
      panelReflowKey: 0,
      // 单击/双击区分
      clickTimer: null,
      // 语种列表
      translateTypes: [],
      panelShow: false,
    };
  },
  computed: {
    buttonStyle() {
      const style = {
        position: "fixed",
        zIndex: 9999,
        cursor: this.isDragging ? "grabbing" : "grab",
      };

      if (this.buttonPosition.x !== null) {
        style.left = `${this.buttonPosition.x}px`;
      } else {
        // 默认位置：左上角
        style.left = "20px";
      }

      if (this.buttonPosition.y !== null) {
        style.top = `${this.buttonPosition.y}px`;
      } else {
        // 默认位置：左上角
        style.top = "20px";
      }

      return style;
    },
    panelStyle() {
      if (!this.$refs.floatingButtonRef) {
        return {};
      }

      // 建立显式依赖：每次打开面板时强制重新计算
      // eslint-disable-next-line no-unused-vars
      const _reflow = this.panelReflowKey;

      const buttonRect = this.$refs.floatingButtonRef.getBoundingClientRect();
      const panelWidth = 150; // 面板宽度
      const panelHeight = 200; // 估算面板高度
      const spacing = 10; // 按钮和面板之间的间距

      let left = buttonRect.left;
      let top = buttonRect.top - panelHeight - spacing;

      // 考虑屏幕边界
      if (left + panelWidth > window.innerWidth) {
        left = window.innerWidth - panelWidth - 10;
      }
      if (left < 10) {
        left = 10;
      }
      if (top < 10) {
        // 如果上方空间不足，显示在下方
        top = buttonRect.bottom + spacing;
      }

      return {
        position: "fixed",
        left: `${left}px`,
        top: `${top}px`,
        zIndex: 9998,
      };
    },
  },
  mounted() {
    // 从localStorage恢复按钮位置
    this.loadButtonPosition();
    // 获取语种列表
    this.getLanguage();
  },
  beforeUnmount() {
    // 清理定时器
    if (this.clickTimer) {
      clearTimeout(this.clickTimer);
    }
    // 移除全局事件监听
    document.removeEventListener("pointermove", this.handleDrag);
    document.removeEventListener("pointerup", this.stopDrag);
    document.removeEventListener("pointercancel", this.stopDrag);
    if (this.dragRafId) {
      cancelAnimationFrame(this.dragRafId);
      this.dragRafId = null;
    }
  },
  methods: {
    // 加载按钮位置
    loadButtonPosition() {
      const saved = localStorage.getItem("floatingToolBoxPosition");
      if (saved) {
        try {
          const pos = JSON.parse(saved);
          this.buttonPosition = { x: pos.x, y: pos.y };
        } catch (e) {
          console.error("Failed to load button position:", e);
        }
      }
    },
    // 保存按钮位置
    saveButtonPosition() {
      if (this.buttonPosition.x !== null || this.buttonPosition.y !== null) {
        localStorage.setItem(
          "floatingToolBoxPosition",
          JSON.stringify(this.buttonPosition)
        );
      }
    },
    // 单击事件（包含自定义双击检测）
    handleClick() {
      // 拖拽结束后会触发一次 click，这里用时间窗口屏蔽，避免"拖完自动打开面板"
      if (this.lastDragEndTime && Date.now() - this.lastDragEndTime < 250) {
        return;
      }

      // 若此前存在未过期的单击定时器 => 认为是连续点击（双击）
      if (this.clickTimer) {
        clearTimeout(this.clickTimer);
        this.clickTimer = null;

        // 仅在非拖拽场景下识别为双击（确保是连续点击，而不是单击+拖拽）
        if (!this.isDragging && !this.dragHasMoved) {
          this.handleDoubleClick();
        }
        return;
      }

      // 设置单击定时器：若到期则判定为单击
      this.clickTimer = setTimeout(() => {
        if (!this.isDragging) {
          // 每次打开前强制触发一次 panelStyle 的重新计算
          this.panelReflowKey += 1;
          // 关闭所有通知
          closeAllNotifications();
        }
        this.clickTimer = null;
      }, 200);
    },
    // 双击事件：切换显示/隐藏面板，打开时强制重新计算样式并关闭通知
    handleDoubleClick() {
      // 清除单击定时器
      if (this.clickTimer) {
        clearTimeout(this.clickTimer);
        this.clickTimer = null;
      }

      const willShow = !this.panelVisible;
      if (willShow) {
        // 每次打开前强制触发一次 panelStyle 的重新计算
        this.panelReflowKey += 1;
        // 关闭所有通知
        closeAllNotifications();
        this.panelShow = true;
        this.panelVisible = true;
      } else {
        // 隐藏时同时同步 panelShow，以触发 watcher 或其他依赖
        this.panelVisible = false;
        this.panelShow = false;
      }
    },
    // 开始拖拽
    startDrag(e) {
      e.preventDefault();
      e.stopPropagation();

      // 只响应主键（鼠标左键）
      if (typeof e.button === "number" && e.button !== 0) return;

      // 如果使用默认位置，转换为固定坐标（只转换 null 的坐标，保留已设置的坐标）
      const rect = this.$refs.floatingButtonRef.getBoundingClientRect();
      if (this.buttonPosition.x === null) {
        this.buttonPosition.x = rect.left;
      }
      if (this.buttonPosition.y === null) {
        this.buttonPosition.y = rect.top;
      }

      this.isDragging = true;
      this.dragStartPos = { ...this.buttonPosition };
      this.dragStartMousePos = { x: e.clientX, y: e.clientY };
      this.dragCurrentPos = { ...this.buttonPosition };
      this.dragTargetPos = { ...this.buttonPosition };
      this.dragPointerId = e.pointerId ?? null;
      this.dragHasMoved = false;

      // Pointer capture：避免移出元素后丢事件
      const el = e.currentTarget;
      if (el && typeof el.setPointerCapture === "function" && this.dragPointerId !== null) {
        try {
          el.setPointerCapture(this.dragPointerId);
        } catch (err) {
          // 某些情况下可能捕获失败（忽略即可）
        }
      }

      // 添加全局事件监听
      document.addEventListener("pointermove", this.handleDrag, { passive: true });
      document.addEventListener("pointerup", this.stopDrag, { passive: true });
      document.addEventListener("pointercancel", this.stopDrag, { passive: true });
    },
    // 拖拽中
    handleDrag(e) {
      if (!this.isDragging) return;
      if (this.dragPointerId !== null && e.pointerId !== this.dragPointerId) return;

      const deltaX = e.clientX - this.dragStartMousePos.x;
      const deltaY = e.clientY - this.dragStartMousePos.y;
      // 超过阈值才视为真正拖拽，用于屏蔽拖拽后的 click
      if (!this.dragHasMoved && Math.abs(deltaX) + Math.abs(deltaY) > 3) {
        this.dragHasMoved = true;
      }

      let newX = this.dragStartPos.x + deltaX;
      let newY = this.dragStartPos.y + deltaY;

      // 限制在可视区域内
      const buttonWidth = 50; // 按钮宽度
      const buttonHeight = 50; // 按钮高度
      newX = Math.max(0, Math.min(newX, window.innerWidth - buttonWidth));
      newY = Math.max(0, Math.min(newY, window.innerHeight - buttonHeight));

      // 仅更新目标位置，合帧写入 DOM transform（避免每次 move 触发 Vue 更新）
      this.dragTargetPos = { x: newX, y: newY };
      if (!this.dragRafPending) {
        this.dragRafPending = true;
        this.dragRafId = requestAnimationFrame(() => {
          this.dragRafPending = false;
          const btn = this.$refs.floatingButtonRef;
          if (!btn) return;

          const x = this.dragTargetPos.x;
          const y = this.dragTargetPos.y;
          this.dragCurrentPos = { x, y };

          // 拖拽过程中使用 transform，减少 layout（注意：transform 使用相对起点的偏移）
          const dx = x - this.dragStartPos.x;
          const dy = y - this.dragStartPos.y;
          btn.style.transform = `translate3d(${dx}px, ${dy}px, 0)`;
        });
      }
    },
    // 停止拖拽
    stopDrag(e) {
      if (this.isDragging) {
        this.isDragging = false;
        if (this.dragHasMoved) {
          this.lastDragEndTime = Date.now();
        }

        // 结束时取消 rAF，并将最终位置一次性写回响应式状态用于持久化
        if (this.dragRafId) {
          cancelAnimationFrame(this.dragRafId);
          this.dragRafId = null;
        }
        this.dragRafPending = false;

        const finalPos =
          this.dragCurrentPos && typeof this.dragCurrentPos.x === "number"
            ? this.dragCurrentPos
            : this.dragTargetPos;

        this.buttonPosition = { x: finalPos.x, y: finalPos.y };
        this.saveButtonPosition();

        // 清除 transform，让非拖拽态继续使用 left/top（保持现有计算属性逻辑）
        const btn = this.$refs.floatingButtonRef;
        if (btn) {
          btn.style.transform = "";
        }

        // 释放 pointer capture
        if (e && this.dragPointerId !== null && btn && typeof btn.releasePointerCapture === "function") {
          try {
            btn.releasePointerCapture(this.dragPointerId);
          } catch (err) {
            // ignore
          }
        }
        this.dragPointerId = null;

        // 移除全局事件监听
        document.removeEventListener("pointermove", this.handleDrag);
        document.removeEventListener("pointerup", this.stopDrag);
        document.removeEventListener("pointercancel", this.stopDrag);
      }
    },
    // 关闭面板
    closePanel() {
      this.panelVisible = false;
    },
    // 面板内部点击（捕获阶段）——当面板打开且内部有点击时，关闭面板
    handleInnerClick() {
      if (this.panelVisible) {
        // this.panelVisible = false;// 容器不显示了，则弹窗也会不显示
        this.panelShow = false;
      }
    },
    // 子组件通过事件主动通知（例如 BackFillModal_v1_5），统一在这里关闭面板
    onChildHandleButtonClick(value) {
      // 如果子组件传递了明确布尔值，优先使用；否则直接关闭
      if (typeof value === "boolean") {
        this.panelShow = value;
      } else {
        this.panelShow = false;
      }
      this.panelVisible = false;
    },
    // 面板鼠标离开
    handlePanelMouseLeave() {
      // 暂时不自动隐藏，等待后续优化
      // this.panelVisible = false;
    },
    // 获取语种列表
    getLanguage() {
      let data = {};
      getLanguage(data)
        .then((res) => {
          if (res && res.data && Array.isArray(res.data.list)) {
            this.translateTypes = res.data.list;
          }
        })
        .catch((err) => {
          // 避免未捕获的 Promise 拒绝导致测试/运行时报错
          console.error("获取语种列表失败:", err);
        });
    },
  },
};
</script>

<style scoped>
.floating-tool-box {
  position: fixed;
  z-index: 9999;
}

.floating-button {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  transition: box-shadow 0.2s ease, background 0.2s ease;
  user-select: none;
  touch-action: none;
  will-change: transform;
}

.floating-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.floating-button:active {
  transform: scale(0.95);
}

.floating-button.dragging {
  cursor: grabbing;
  transition: none;
}

.floating-button.dragging:hover,
.floating-button.dragging:active {
  /* 避免 hover/active 的 transform 与拖拽 transform 冲突 */
  transform: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.tool-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 12px;
  min-width: 150px;
  animation: slideDown 0.3s ease;
}

.tool-panel>div {
  width: 100%;
}

.tool-panel>div :deep(.ant-btn) {
  width: 100%;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
