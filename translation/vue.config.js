const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    client: {
      overlay: {
        //当出现编译错误或告警时  是否在浏览器中全屏覆盖
        runtimeErrors: false,
      },
    },
  },
  pluginOptions: {
    electronBuilder: {
      nodeIntegration: true,
      builderOptions: {
        extraResources: [
          {
            from: 'env',                // 源文件夹（相对于项目根目录）
            to: 'env',        // 目标文件夹（相对于打包后的应用的资源目录）
          }
        ],
      }
    }
  },
  configureWebpack: {
    resolve: {
      fallback: {
        path: require.resolve("path-browserify"),
        fs: false,
        // timers: require.resolve("timers-browserify"),
        // stream: require.resolve("stream-browserify"),
      },
    },
  },
})
