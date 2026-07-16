const { defineConfig } = require("@vue/cli-service");
const path = require("path");

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  configureWebpack: (config) => {
    // 避免开发态默认 eval-source-map 在懒加载 SFC 时触发
    // `ReferenceError: __webpack_require__ is not defined`（登录后进入 /translate）
    if (process.env.NODE_ENV === "development") {
      config.devtool = "cheap-module-source-map";
    }
    config.resolve = config.resolve || {};
    config.resolve.alias = {
      ...(config.resolve.alias || {}),
      "@": path.resolve(__dirname, "src"),
      "@prototype": path.resolve(__dirname, "prototype"),
    };
    config.resolve.extensions = [".js", ".vue", ".json"];
    config.resolve.fallback = {
      ...(config.resolve.fallback || {}),
      path: require.resolve("path-browserify"),
      fs: false,
    };
  },
  devServer: {
    port: 18000,
    client: { overlay: { runtimeErrors: false } },
    proxy: {
      // 主后端
      "^/(userLogin|userManage|entry|backendInfo|checkManage|product|configManage|Syk|taskManage|translate|workbench|version|secondClassify|entryInfo|I18Sever|userPartiality|test)":
        {
          target: "http://localhost:18001",
          changeOrigin: true,
        },
      // 术语 Agent
      "/agent": {
        target: "http://localhost:18002",
        changeOrigin: true,
      },
    },
  },
  pluginOptions: {
    electronBuilder: {
      nodeIntegration: true,
      builderOptions: {
        extraResources: [
          {
            from: "env", // 源文件夹（相对于项目根目录）
            to: "env", // 目标文件夹（相对于打包后的应用的资源目录）
          },
        ],
      },
    },
  },
});
