export default {
    // 获取字节数  中文2个字节 其他1个字节
    byteLength(str) {
        if(str === null || str === undefined){
            return 0
        }
        // 去除首尾空格
        str = (""+str).trim()
        var strlen = 0;
        for(var i = 0;i < str.length; i++){
            if(str.charCodeAt(i) >= 0x4E00 && str.charCodeAt(i) <= 0x9FA5){ 
                //如果是汉字，则字符串长度加2
                strlen += 2;
            }
            else{
                strlen++;
            }
        }
        return strlen
    },
    // 获取当前时间
    getCurrentFormattedTime() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
}