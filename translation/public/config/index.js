exports.app = {
    electron: false,   // 是否是electron    true:electron   false:web
    // serverURL: 'http://10.17.43.20:18001'   // 后端服务url（赵鸿鹏接口）(后跟/swagger-ui/#/工作台)
    // serverURL: 'http://10.17.77.18:18003/'   // 后端服务url（杨海潮开发接口）
    // serverURL: 'http://10.17.77.18:18002/'   // 后端服务url（杨海潮稳定接口）(用postman测接口)
    serverURL: 'http://10.17.14.125:18001/'   // 后端服务url，对应18000端口
    // serverURL: 'http://10.17.69.27:18002/'   // 后端服务url，对应18100端口
    // serverURL: 'http://10.17.43.20:18001'   // 客户端打包专用，给赵鸿鹏
    // serverURL: 'http://10.17.43.6:18001'   // 客户端打包专用，给赵鸿鹏
};
