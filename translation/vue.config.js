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
  pluginOptions:{
    electronBuilder:{
      nodeIntegration:true
    }
  },
  configureWebpack: {
    resolve: {
      fallback: { 
        path: require.resolve("path-browserify"),
        fs: false
     },
    },
  },
})
