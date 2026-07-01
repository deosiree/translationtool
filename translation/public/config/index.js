exports.app = {
    electron: false,   // 是否是electron    true:electron   false:web
    // 直连后端（非 Docker / 特殊调试时取消注释）:
    // serverURL: 'http://localhost:18001/'
    // serverURL: 'http://10.17.43.20:18001'   // 后端服务url（赵鸿鹏接口）(后跟/swagger-ui/#/工作台)
    // serverURL: 'http://10.17.77.30:18003/'   // 后端服务url（杨海潮开发接口）
    // serverURL: 'http://10.17.77.20:18002/'   // 后端服务url（杨海潮稳定接口）(用postman测接口)
    // serverURL: 'http://10.17.196.125:18001/'   // 后端服务url（旧非 Docker 内网直连）
    serverURL: ''   // Docker / 本地 dev：同源请求，由 nginx 或 webpack proxy 转发
    // serverURL: 'http://10.17.69.43:18002/'   // 后端服务url，对应18100端口
    // serverURL: 'http://10.17.43.20:18001'   // 客户端打包专用，给赵鸿鹏
    // serverURL: 'http://10.17.43.6:18001'   // 客户端打包专用，给赵鸿鹏
};
