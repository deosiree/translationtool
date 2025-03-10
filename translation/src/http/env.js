const config = require('../../public/config')
export default {
    // 开发环境
    dev: {
        // baseUrl: "http://localhost:18001"
        // baseUrl: "http://10.17.43.20:18001"
        // baseUrl: "http://10.17.70.29:18001"
        baseUrl: config.app.serverURL
    },
};