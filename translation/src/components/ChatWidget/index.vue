<template>
  <div class="chat-widget-root">
    <!-- 浮动触发按钮（Hidden 态展示） -->
    <div
      v-show="chatState === 'hidden'"
      ref="floatBtn"
      class="chat-float-btn"
      :class="{ dragging: isDragging }"
      :style="btnStyle"
      @pointerdown="startDragBtn"
      @click="onBtnClick"
    >
      <CustomerServiceOutlined />
    </div>

    <!-- 对话窗口（Open 态展示） -->
    <div
      v-if="chatState === 'open'"
      ref="chatPanel"
      class="chat-panel"
      :style="panelStyle"
    >
      <!-- Header：拖拽手柄 -->
      <div
        ref="panelHeader"
        class="chat-panel-header"
        @pointerdown="startDragPanel"
      >
        <span class="chat-panel-title">智能助手</span>
        <div class="chat-panel-actions">
          <a-button
            type="text"
            size="small"
            title="最小化"
            @click="minimize"
          >
            <template #icon><MinusOutlined /></template>
          </a-button>
          <a-button
            type="text"
            size="small"
            title="关闭"
            @click="closeChat"
          >
            <template #icon><CloseOutlined /></template>
          </a-button>
        </div>
      </div>

      <!-- Body：消息列表 -->
      <div ref="msgList" class="chat-panel-body">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="chat-msg"
          :class="msg.role === 'user' ? 'chat-msg--user' : 'chat-msg--assistant'"
        >
          <div class="chat-msg-bubble">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="chat-msg chat-msg--assistant">
          <div class="chat-msg-bubble chat-msg-loading">
            <span class="dot-pulse"></span>
          </div>
        </div>
        <div ref="msgBottom" />
      </div>

      <!-- Footer：输入区 -->
      <div class="chat-panel-footer">
        <a-textarea
          v-model:value="inputText"
          :rows="2"
          placeholder="输入您的问题…"
          :disabled="loading"
          @pressEnter="onEnter"
        />
        <a-button
          type="primary"
          :loading="loading"
          :disabled="!inputText.trim()"
          @click="send"
        >
          发送
        </a-button>
      </div>
    </div>

    <!-- 最小化边缘标签（Minimized 态展示） -->
    <div
      v-if="chatState === 'minimized'"
      ref="minimizedTab"
      class="chat-minimized-tab"
      :class="minimizedEdge === 'right' ? 'chat-minimized-tab--right' : 'chat-minimized-tab--left'"
      :style="minimizedTabStyle"
      @click="restoreFromMinimized"
    >
      <CustomerServiceOutlined />
      <span class="chat-minimized-label">助手</span>
    </div>
  </div>
</template>

<script>
import {
  CustomerServiceOutlined,
  MinusOutlined,
  CloseOutlined,
} from "@ant-design/icons-vue";
import { postChat } from "@/http/api/terminologyAgent";
import { normalizeFloatingPosition } from "@/utils";

const BTN_SIZE = 52;
const PANEL_WIDTH = 400;
const PANEL_HEIGHT = 520;
const STORAGE_KEY_BTN = "chatWidgetBtnPosition";
const STORAGE_KEY_PANEL = "chatWidgetPanelPosition";

export default {
  name: "ChatWidget",
  components: { CustomerServiceOutlined, MinusOutlined, CloseOutlined },
  data() {
    return {
      // 状态：hidden | open | minimized
      chatState: "hidden",

      // 按钮位置
      btnPos: { x: null, y: null },

      // 按钮拖拽
      isDragging: false,
      dragStartMouse: { x: 0, y: 0 },
      dragStartPos: { x: 0, y: 0 },
      dragPointerId: null,
      dragTargetPos: { x: 0, y: 0 },
      dragRafId: null,
      dragRafPending: false,
      dragCurrentPos: { x: 0, y: 0 },
      dragHasMoved: false,
      lastDragEndTime: 0,

      // 面板拖拽
      isDraggingPanel: false,
      panelDragStartMouse: { x: 0, y: 0 },
      panelPos: { x: null, y: null },
      panelDragTargetPos: { x: 0, y: 0 },
      panelDragRafId: null,
      panelDragRafPending: false,
      panelDragCurrentPos: { x: 0, y: 0 },
      panelDragPointerId: null,

      // 最小化
      minimizedEdge: "right",
      minimizedTop: 200,

      // 对话
      messages: [],
      inputText: "",
      loading: false,
      sessionId: null,
    };
  },
  computed: {
    btnStyle() {
      const s = { position: "fixed", zIndex: 9999, cursor: this.isDragging ? "grabbing" : "grab" };
      if (this.btnPos.x !== null) s.left = `${this.btnPos.x}px`;
      else s.right = "24px";
      if (this.btnPos.y !== null) s.top = `${this.btnPos.y}px`;
      else s.bottom = "120px";
      return s;
    },
    panelStyle() {
      const x = this.panelPos.x !== null ? this.panelPos.x : window.innerWidth - PANEL_WIDTH - 24;
      const y = this.panelPos.y !== null ? this.panelPos.y : Math.max(60, window.innerHeight - PANEL_HEIGHT - 80);
      return {
        position: "fixed",
        zIndex: 10000,
        left: `${Math.max(0, Math.min(x, window.innerWidth - PANEL_WIDTH))}px`,
        top: `${Math.max(0, Math.min(y, window.innerHeight - PANEL_HEIGHT))}px`,
        width: `${PANEL_WIDTH}px`,
        height: `${PANEL_HEIGHT}px`,
      };
    },
    minimizedTabStyle() {
      return {
        position: "fixed",
        zIndex: 9999,
        top: `${this.minimizedTop}px`,
        [this.minimizedEdge]: "0px",
        transition: "transform 0.25s ease",
      };
    },
  },
  mounted() {
    this.loadPositions();
  },
  beforeUnmount() {
    this.cleanupBtnDrag();
    this.cleanupPanelDrag();
  },
  methods: {
    // ── 位置持久化 ──
    loadPositions() {
      const bp = normalizeFloatingPosition(STORAGE_KEY_BTN, { x: null, y: null }, { width: BTN_SIZE, height: BTN_SIZE });
      this.btnPos = bp;
      const pp = normalizeFloatingPosition(STORAGE_KEY_PANEL, { x: null, y: null }, { width: PANEL_WIDTH, height: PANEL_HEIGHT });
      this.panelPos = pp;
    },
    saveBtnPos() {
      localStorage.setItem(STORAGE_KEY_BTN, JSON.stringify(this.btnPos));
    },
    savePanelPos() {
      localStorage.setItem(STORAGE_KEY_PANEL, JSON.stringify(this.panelPos));
    },

    // ── 按钮交互 ──
    onBtnClick() {
      if (this.lastDragEndTime && Date.now() - this.lastDragEndTime < 250) return;
      if (this.dragHasMoved) return;
      this.openChat();
    },
    openChat() {
      this.chatState = "open";
      if (this.messages.length === 0) {
        this.messages.push({
          role: "assistant",
          content: "您好！我是翻译工具的智能助手，有什么可以帮您的吗？",
        });
      }
    },
    closeChat() {
      this.chatState = "hidden";
    },
    minimize() {
      // 计算最小化吸附边缘
      const px = this.panelPos.x !== null ? this.panelPos.x : window.innerWidth - PANEL_WIDTH - 24;
      const cx = px + PANEL_WIDTH / 2;
      this.minimizedEdge = cx < window.innerWidth / 2 ? "left" : "right";
      // 保存面板 Y 位置用于恢复
      const py = this.panelPos.y !== null ? this.panelPos.y : Math.max(60, window.innerHeight - PANEL_HEIGHT - 80);
      this.minimizedTop = Math.max(40, Math.min(py, window.innerHeight - 200));
      this.chatState = "minimized";
    },
    restoreFromMinimized() {
      // 从边缘恢复面板，偏移一些距离避免完全贴边
      if (this.panelPos.x === null || this.panelPos.x <= 2 || this.panelPos.x >= window.innerWidth - PANEL_WIDTH - 2) {
        if (this.minimizedEdge === "right") {
          this.panelPos = { x: window.innerWidth - PANEL_WIDTH - 24, y: this.minimizedTop };
        } else {
          this.panelPos = { x: 24, y: this.minimizedTop };
        }
      }
      this.chatState = "open";
    },

    // ── 按钮拖拽 ──
    startDragBtn(e) {
      if (e.button !== 0) return;
      e.preventDefault();
      e.stopPropagation();
      const rect = this.$refs.floatBtn.getBoundingClientRect();
      if (this.btnPos.x === null) this.btnPos = { x: rect.left, y: rect.top };
      this.isDragging = true;
      this.dragStartMouse = { x: e.clientX, y: e.clientY };
      this.dragStartPos = { ...this.btnPos };
      this.dragCurrentPos = { ...this.btnPos };
      this.dragTargetPos = { ...this.btnPos };
      this.dragPointerId = e.pointerId ?? null;
      this.dragHasMoved = false;
      const el = e.currentTarget;
      if (el && el.setPointerCapture && this.dragPointerId !== null) {
        try { el.setPointerCapture(this.dragPointerId); } catch (_) {}
      }
      document.addEventListener("pointermove", this.handleDragBtn, { passive: true });
      document.addEventListener("pointerup", this.stopDragBtn, { passive: true });
      document.addEventListener("pointercancel", this.stopDragBtn, { passive: true });
    },
    handleDragBtn(e) {
      if (!this.isDragging) return;
      if (this.dragPointerId !== null && e.pointerId !== this.dragPointerId) return;
      const dx = e.clientX - this.dragStartMouse.x;
      const dy = e.clientY - this.dragStartMouse.y;
      if (!this.dragHasMoved && Math.abs(dx) + Math.abs(dy) > 3) this.dragHasMoved = true;
      let nx = Math.max(0, Math.min(this.dragStartPos.x + dx, window.innerWidth - BTN_SIZE));
      let ny = Math.max(0, Math.min(this.dragStartPos.y + dy, window.innerHeight - BTN_SIZE));
      this.dragTargetPos = { x: nx, y: ny };
      if (!this.dragRafPending) {
        this.dragRafPending = true;
        this.dragRafId = requestAnimationFrame(() => {
          this.dragRafPending = false;
          const btn = this.$refs.floatBtn;
          if (!btn) return;
          this.dragCurrentPos = { ...this.dragTargetPos };
          const tdx = this.dragTargetPos.x - this.dragStartPos.x;
          const tdy = this.dragTargetPos.y - this.dragStartPos.y;
          btn.style.transform = `translate3d(${tdx}px, ${tdy}px, 0)`;
        });
      }
    },
    stopDragBtn(e) {
      if (!this.isDragging) return;
      this.isDragging = false;
      if (this.dragHasMoved) this.lastDragEndTime = Date.now();
      this.cleanupBtnDrag();
      const final = this.dragCurrentPos.x !== undefined ? this.dragCurrentPos : this.dragTargetPos;
      this.btnPos = { x: final.x, y: final.y };
      this.saveBtnPos();
      const btn = this.$refs.floatBtn;
      if (btn) btn.style.transform = "";
      if (e && this.dragPointerId !== null && btn && btn.releasePointerCapture) {
        try { btn.releasePointerCapture(this.dragPointerId); } catch (_) {}
      }
      this.dragPointerId = null;
    },
    cleanupBtnDrag() {
      document.removeEventListener("pointermove", this.handleDragBtn);
      document.removeEventListener("pointerup", this.stopDragBtn);
      document.removeEventListener("pointercancel", this.stopDragBtn);
      if (this.dragRafId) { cancelAnimationFrame(this.dragRafId); this.dragRafId = null; }
      this.dragRafPending = false;
    },

    // ── 面板拖拽（通过 header） ──
    startDragPanel(e) {
      if (e.button !== 0) return;
      // 忽略 header 上按钮的点击
      if (e.target.closest(".chat-panel-actions")) return;
      e.preventDefault();
      const rect = this.$refs.chatPanel.getBoundingClientRect();
      const px = rect.left;
      const py = rect.top;
      if (this.panelPos.x === null) this.panelPos = { x: px, y: py };
      this.isDraggingPanel = true;
      this.panelDragStartMouse = { x: e.clientX, y: e.clientY };
      this.panelDragCurrentPos = { x: px, y: py };
      this.panelDragTargetPos = { x: px, y: py };
      this.panelDragPointerId = e.pointerId ?? null;
      const el = e.currentTarget;
      if (el && el.setPointerCapture && this.panelDragPointerId !== null) {
        try { el.setPointerCapture(this.panelDragPointerId); } catch (_) {}
      }
      document.addEventListener("pointermove", this.handleDragPanel, { passive: true });
      document.addEventListener("pointerup", this.stopDragPanel, { passive: true });
      document.addEventListener("pointercancel", this.stopDragPanel, { passive: true });
    },
    handleDragPanel(e) {
      if (!this.isDraggingPanel) return;
      if (this.panelDragPointerId !== null && e.pointerId !== this.panelDragPointerId) return;
      const dx = e.clientX - this.panelDragStartMouse.x;
      const dy = e.clientY - this.panelDragStartMouse.y;
      let nx = Math.max(0, Math.min(this.panelPos.x + dx, window.innerWidth - PANEL_WIDTH));
      let ny = Math.max(0, Math.min(this.panelPos.y + dy, window.innerHeight - PANEL_HEIGHT));
      this.panelDragTargetPos = { x: nx, y: ny };
      if (!this.panelDragRafPending) {
        this.panelDragRafPending = true;
        this.panelDragRafId = requestAnimationFrame(() => {
          this.panelDragRafPending = false;
          const panel = this.$refs.chatPanel;
          if (!panel) return;
          this.panelDragCurrentPos = { ...this.panelDragTargetPos };
          panel.style.transition = "none";
          panel.style.left = `${this.panelDragTargetPos.x}px`;
          panel.style.top = `${this.panelDragTargetPos.y}px`;
        });
      }
    },
    stopDragPanel(e) {
      if (!this.isDraggingPanel) return;
      this.isDraggingPanel = false;
      this.cleanupPanelDrag();
      const final = this.panelDragCurrentPos;
      this.panelPos = { x: final.x, y: final.y };
      this.savePanelPos();
      const panel = this.$refs.chatPanel;
      if (panel) {
        panel.style.transition = "";
        panel.style.left = "";
        panel.style.top = "";
      }
      if (e && this.panelDragPointerId !== null) {
        const el = this.$refs.panelHeader;
        if (el && el.releasePointerCapture) {
          try { el.releasePointerCapture(this.panelDragPointerId); } catch (_) {}
        }
      }
      this.panelDragPointerId = null;
    },
    cleanupPanelDrag() {
      document.removeEventListener("pointermove", this.handleDragPanel);
      document.removeEventListener("pointerup", this.stopDragPanel);
      document.removeEventListener("pointercancel", this.stopDragPanel);
      if (this.panelDragRafId) { cancelAnimationFrame(this.panelDragRafId); this.panelDragRafId = null; }
      this.panelDragRafPending = false;
    },

    // ── 对话逻辑 ──
    onEnter(e) {
      if (e.shiftKey) return; // Shift+Enter 换行
      e.preventDefault();
      this.send();
    },
    async send() {
      const text = this.inputText.trim();
      if (!text || this.loading) return;
      this.messages.push({ role: "user", content: text });
      this.inputText = "";
      this.loading = true;
      this.$nextTick(() => this.scrollToBottom());
      try {
        const payload = { messages: this.messages.slice(-20), session_id: this.sessionId };
        const res = await postChat(payload);
        if (res && res.code === 200 && res.data) {
          this.sessionId = res.data.session_id;
          this.messages.push({ role: "assistant", content: res.data.reply });
        } else {
          const errMsg = (res && res.message) || "请求失败";
          this.messages.push({ role: "assistant", content: `抱歉，${errMsg}，请稍后重试。` });
        }
      } catch (err) {
        console.error("ChatWidget send error:", err);
        this.messages.push({ role: "assistant", content: "抱歉，网络异常，请检查服务是否启动后重试。" });
      } finally {
        this.loading = false;
        this.$nextTick(() => this.scrollToBottom());
      }
    },
    scrollToBottom() {
      const el = this.$refs.msgBottom;
      if (el) el.scrollIntoView({ behavior: "smooth" });
    },
  },
};
</script>

<style scoped>
/* ── 浮动按钮 ── */
.chat-float-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  user-select: none;
  touch-action: none;
  will-change: transform;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: pointer;
}
.chat-float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.45);
}
.chat-float-btn.dragging,
.chat-float-btn.dragging:hover {
  cursor: grabbing;
  transform: none;
  transition: none;
}

/* ── 对话窗口 ── */
.chat-panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15), 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: chatFadeIn 0.25s ease;
}
@keyframes chatFadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}

.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.chat-panel-header:active {
  cursor: grabbing;
}
.chat-panel-title {
  font-size: 15px;
  font-weight: 600;
}
.chat-panel-actions {
  display: flex;
  gap: 4px;
}
.chat-panel-actions :deep(.ant-btn) {
  color: rgba(255, 255, 255, 0.85);
}
.chat-panel-actions :deep(.ant-btn:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
}

/* ── 消息列表 ── */
.chat-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}
.chat-msg {
  display: flex;
  margin-bottom: 12px;
}
.chat-msg--user {
  justify-content: flex-end;
}
.chat-msg--assistant {
  justify-content: flex-start;
}
.chat-msg-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.chat-msg--user .chat-msg-bubble {
  background: #1890ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-msg--assistant .chat-msg-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* ── Loading 动画 ── */
.chat-msg-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 4px 0;
}
.dot-pulse {
  position: relative;
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bbb;
  animation: dotPulse 1.2s infinite ease-in-out both;
}
.dot-pulse::before,
.dot-pulse::after {
  content: "";
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bbb;
  position: absolute;
  top: 0;
  animation: dotPulse 1.2s infinite ease-in-out both;
}
.dot-pulse::before {
  left: -16px;
  animation-delay: -0.32s;
}
.dot-pulse::after {
  left: 16px;
  animation-delay: 0.32s;
}
@keyframes dotPulse {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* ── 输入区 ── */
.chat-panel-footer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: #fff;
}
.chat-panel-footer :deep(.ant-input) {
  resize: none;
}

/* ── 最小化边缘标签 ── */
.chat-minimized-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 36px;
  height: 100px;
  border-radius: 8px 0 0 8px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(24, 144, 255, 0.3);
  user-select: none;
  font-size: 16px;
  transition: width 0.25s ease, box-shadow 0.25s ease;
}
.chat-minimized-tab:hover {
  width: 44px;
  box-shadow: 0 4px 20px rgba(24, 144, 255, 0.45);
}
.chat-minimized-tab--right {
  border-radius: 8px 0 0 8px;
}
.chat-minimized-tab--left {
  border-radius: 0 8px 8px 0;
}
.chat-minimized-label {
  font-size: 11px;
  writing-mode: vertical-rl;
  letter-spacing: 2px;
}
</style>
