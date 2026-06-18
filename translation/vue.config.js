const { defineConfig } = require("@vue/cli-service");
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
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
  configureWebpack: {
    resolve: {
      alias: {
        "@": require("path").resolve(__dirname, "src"),
        "@prototype": require("path").resolve(__dirname, "prototype"),
      },
      extensions: [".js", ".vue", ".json"],
      fallback: {
        path: require.resolve("path-browserify"),
        fs: false,
        // timers: require.resolve("timers-browserify"),
        // stream: require.resolve("stream-browserify"),
      },
    },
  },
});
