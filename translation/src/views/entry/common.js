export default {
    // 获取字节数  中文2个字节 其他1个字节
    byteLength(str) {
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
}