export default{
    languageMap:{
        '英文':{language:"英文",code:"english",transIdName:"enTransId"},
        '俄文':{language:"俄文",code:"russian",transIdName:"ruTransId"},
        '西文':{language:"西文",code:"spanish",transIdName:"spaTransId"},
        '法文':{language:"法文",code:"french",transIdName:"fraTransId"},
    },
    checkboxList:[
        {label:'tag',value:'tag',index:3},
        {label: "Comment",value: "comment",index:4},
        {label: "英文释义",value: "englishInterpretation",index:5},
        {label: "中文释义",value: "chineseInterpretation",index:6},
        {label: "一级分类",value: "classfy1",index:8},
        {label: "二级分类",value: "classfy2",index:9},
        {label: "词条来源",value: "entrySource",index:10},
        {label: "回写辞典",value: "diFileName",index:11},
        {label: "abbr",value: "abbr",index:12},
    ],
    checkedColumn: ["abbr", "englishInterpretation","chineseInterpretation"],
    overlayStyle:{
        maxHeight:'300px',
        overflowY: 'scroll',
        backgroundColor: '#fff',
        backgroundClip: 'padding-box',
        borderRadius: '2px',
        // padding:'0',
        boxShadow: '0 3px 6px -4px rgb(0 0 0 / 12%), 0 6px 16px 0 rgb(0 0 0 / 8%), 0 9px 28px 8px rgb(0 0 0 / 5%)'
    },
    keys:['a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z']
}