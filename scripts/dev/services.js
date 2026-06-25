/** 四层服务定义：compose 名、容器名、端口、本地启动命令 */

const BROWSER_URL = "http://localhost:18000";

/** Java / Agent 本地与 Docker 同端口；prepare 互斥，不会双占 */
const SERVICES = {
  ui: {
    layer: "ui",
    compose: "translation-ui",
    container: "translation-ui",
    port: 18000,
    localDir: "translation",
    localScript: null,
  },
  java: {
    layer: "java",
    compose: "translationtoolservice",
    container: "translationtoolservice",
    port: 18001,
    localDir: "translationtoolservice",
    localScript: "dev",
  },
  agent: {
    layer: "agent",
    compose: "terminology-agent",
    container: "terminology-agent",
    port: 18002,
    localDir: "terminology-agent",
    localScript: "dev",
    healthPath: "/agent/health",
  },
  mysql: {
    layer: "infra",
    compose: "mysql",
    container: "translation-mysql",
    hasHealth: true,
  },
  redis: {
    layer: "infra",
    compose: "redis",
    container: "translation-redis",
    hasHealth: true,
  },
};

const INFRA_LAYERS = ["mysql", "redis"];

module.exports = {
  BROWSER_URL,
  SERVICES,
  INFRA_LAYERS,
};
