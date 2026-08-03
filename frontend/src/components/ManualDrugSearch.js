import React, { useState, useRef, useEffect } from 'react';
import { getToken } from '../utils/elderApi';

function ManualDrugSearch({ onSelectDrug }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [isLoading, setIsLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('');
  const [displayCount, setDisplayCount] = useState(3); // 当前显示的药品数量，默认3条
  const [isListening, setIsListening] = useState(false); // 语音输入监听状态
  const [voiceSupported] = useState(() => {
    if (typeof window === 'undefined') return false;
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  });
  const inputRef = useRef(null);
  const recognitionRef = useRef(null);

  const searchDrugs = async (query) => {
    if (!query || query.trim().length < 1) {
      setSearchResults([]);
      setShowResults(false);
      setDisplayCount(3); // 重置显示数量
      return;
    }

    setIsSearching(true);
    setDisplayCount(3); // 每次新搜索都重置为显示3条

    try {
      const response = await fetch(`/api/v1/drug/search?keyword=${encodeURIComponent(query)}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` },
      });
      const data = await response.json();

      if (data.code === 200 && data.data && data.data.length > 0) {
        // 按药品名称去重，相同名称只保留一个（忽略规格/厂家差异）
        const uniqueResults = [];
        const seenNames = new Set();
        data.data.forEach(drug => {
          const drugName = drug.drugName || drug.genericName;
          if (!seenNames.has(drugName)) {
            seenNames.add(drugName);
            uniqueResults.push(drug);
          }
        });
        setSearchResults(uniqueResults);
        setShowResults(true);
        setSelectedIndex(-1);
      } else {
        const aiResponse = await fetch(`/api/v1/drug/ai-search?keyword=${encodeURIComponent(query)}`, {
          headers: { 'Authorization': `Bearer ${getToken()}` },
        });
        const aiData = await aiResponse.json();
        
        if (aiData.code === 200 && aiData.data && aiData.data.length > 0) {
          // 按药品名称去重，相同名称只保留一个（忽略规格/厂家差异）
          const uniqueResults = [];
          const seenNames = new Set();
          aiData.data.forEach(drug => {
            const drugName = drug.drugName || drug.genericName;
            if (!seenNames.has(drugName)) {
              seenNames.add(drugName);
              uniqueResults.push(drug);
            }
          });
          setSearchResults(uniqueResults);
          setShowResults(true);
        } else {
          const localResults = generateLocalSuggestions(query);
          setSearchResults(localResults);
          setShowResults(localResults.length > 0);
        }
      }
    } catch (error) {
      console.error('搜索药品失败:', error);
      if (query.trim().length >= 2) {
        const localResults = generateLocalSuggestions(query);
        setSearchResults(localResults);
        setShowResults(localResults.length > 0);
      } else {
        setSearchResults([]);
        setShowResults(false);
      }
    } finally {
      setIsSearching(false);
    }
  };

  // 处理输入变化，隐藏之前的搜索结果
  const handleInputChange = (e) => {
    setSearchQuery(e.target.value);
    // 输入内容变化时，隐藏搜索结果
    setShowResults(false);
  };

  const generateLocalSuggestions = (query) => {
    const allDrugs = [
      { id: 'local-1', drugName: '对乙酰氨基酚', specification: '500mg', manufacturer: '泰诺', matchScore: 0.9 },
      { id: 'local-2', drugName: '布洛芬', specification: '400mg', manufacturer: '芬必得', matchScore: 0.85 },
      { id: 'local-3', drugName: '硝苯地平缓释片', specification: '20mg', manufacturer: '拜新同', matchScore: 0.8 },
      { id: 'local-4', drugName: '二甲双胍', specification: '500mg', manufacturer: '格华止', matchScore: 0.75 },
      { id: 'local-5', drugName: '阿莫西林', specification: '500mg', manufacturer: '阿莫仙', matchScore: 0.7 },
      { id: 'local-6', drugName: '奥美拉唑', specification: '20mg', manufacturer: '洛赛克', matchScore: 0.65 },
      { id: 'local-7', drugName: '氯雷他定', specification: '10mg', manufacturer: '开瑞坦', matchScore: 0.6 },
      { id: 'local-8', drugName: '蒙脱石散', specification: '3g', manufacturer: '思密达', matchScore: 0.55 }
    ];

    const categoryDrugs = {
      '感冒药': [
        { id: 'auto-1', drugName: '对乙酰氨基酚', specification: '500mg', manufacturer: '泰诺', category: '感冒药', matchScore: 0.9 },
        { id: 'auto-2', drugName: '布洛芬', specification: '400mg', manufacturer: '芬必得', category: '感冒药', matchScore: 0.85 },
        { id: 'auto-3', drugName: '复方氨酚烷胺', specification: '复方', manufacturer: '感康', category: '感冒药', matchScore: 0.8 },
        { id: 'auto-4a', drugName: '白加黑', specification: '复方', manufacturer: '拜耳', category: '感冒药', matchScore: 0.75 },
        { id: 'auto-4b', drugName: '感冒灵颗粒', specification: '10g', manufacturer: '三九制药', category: '感冒药', matchScore: 0.7 }
      ],
      '止痛药': [
        { id: 'auto-5', drugName: '布洛芬', specification: '400mg', manufacturer: '芬必得', category: '止痛药', matchScore: 0.9 },
        { id: 'auto-6', drugName: '对乙酰氨基酚', specification: '500mg', manufacturer: '泰诺', category: '止痛药', matchScore: 0.85 },
        { id: 'auto-7', drugName: '阿司匹林', specification: '300mg', manufacturer: '拜耳', category: '止痛药', matchScore: 0.8 },
        { id: 'auto-8', drugName: '双氯芬酸钠缓释片', specification: '75mg', manufacturer: '扶他林', category: '止痛药', matchScore: 0.75 }
      ],
      '消炎药': [
        { id: 'auto-9', drugName: '阿莫西林胶囊', specification: '500mg', manufacturer: '阿莫仙', category: '消炎药', matchScore: 0.9 },
        { id: 'auto-10', drugName: '头孢克肟分散片', specification: '100mg', manufacturer: '世福素', category: '消炎药', matchScore: 0.85 },
        { id: 'auto-11', drugName: '阿奇霉素片', specification: '250mg', manufacturer: '希舒美', category: '消炎药', matchScore: 0.8 },
        { id: 'auto-12', drugName: '罗红霉素分散片', specification: '150mg', manufacturer: '仁苏', category: '消炎药', matchScore: 0.75 },
        { id: 'auto-13', drugName: '左氧氟沙星片', specification: '500mg', manufacturer: '可乐必妥', category: '消炎药', matchScore: 0.7 }
      ],
      '胃药': [
        { id: 'auto-14', drugName: '奥美拉唑肠溶胶囊', specification: '20mg', manufacturer: '洛赛克', category: '胃药', matchScore: 0.9 },
        { id: 'auto-15', drugName: '蒙脱石散', specification: '3g', manufacturer: '思密达', category: '胃药', matchScore: 0.85 },
        { id: 'auto-16', drugName: '铝碳酸镁片', specification: '500mg', manufacturer: '达喜', category: '胃药', matchScore: 0.8 },
        { id: 'auto-17', drugName: '多潘立酮片', specification: '10mg', manufacturer: '吗丁啉', category: '胃药', matchScore: 0.75 },
        { id: 'auto-18', drugName: '泮托拉唑钠肠溶胶囊', specification: '40mg', manufacturer: '泮托拉唑', category: '胃药', matchScore: 0.7 }
      ],
      '降压药': [
        { id: 'auto-19', drugName: '硝苯地平缓释片', specification: '20mg', manufacturer: '拜新同', category: '降压药', matchScore: 0.9 },
        { id: 'auto-20', drugName: '苯磺酸氨氯地平片', specification: '5mg', manufacturer: '络活喜', category: '降压药', matchScore: 0.85 },
        { id: 'auto-21', drugName: '厄贝沙坦片', specification: '150mg', manufacturer: '安博维', category: '降压药', matchScore: 0.8 },
        { id: 'auto-22', drugName: '缬沙坦胶囊', specification: '80mg', manufacturer: '代文', category: '降压药', matchScore: 0.75 },
        { id: 'auto-23', drugName: '贝那普利片', specification: '10mg', manufacturer: '洛丁新', category: '降压药', matchScore: 0.7 }
      ],
      '降糖药': [
        { id: 'auto-24', drugName: '盐酸二甲双胍肠溶片', specification: '500mg', manufacturer: '格华止', category: '降糖药', matchScore: 0.9 },
        { id: 'auto-25', drugName: '格列齐特缓释片', specification: '80mg', manufacturer: '达美康', category: '降糖药', matchScore: 0.85 },
        { id: 'auto-26', drugName: '阿卡波糖片', specification: '50mg', manufacturer: '拜糖平', category: '降糖药', matchScore: 0.8 },
        { id: 'auto-27', drugName: '格列美脲片', specification: '2mg', manufacturer: '亚莫利', category: '降糖药', matchScore: 0.75 },
        { id: 'auto-28', drugName: '瑞格列奈片', specification: '1mg', manufacturer: '诺和龙', category: '降糖药', matchScore: 0.7 }
      ],
      '抗过敏药': [
        { id: 'auto-29', drugName: '氯雷他定片', specification: '10mg', manufacturer: '开瑞坦', category: '抗过敏药', matchScore: 0.9 },
        { id: 'auto-30', drugName: '盐酸西替利嗪片', specification: '10mg', manufacturer: '仙特明', category: '抗过敏药', matchScore: 0.85 },
        { id: 'auto-31', drugName: '地氯雷他定片', specification: '5mg', manufacturer: '恩理思', category: '抗过敏药', matchScore: 0.8 },
        { id: 'auto-32', drugName: '依巴斯汀片', specification: '10mg', manufacturer: '开思亭', category: '抗过敏药', matchScore: 0.75 }
      ],
      '退烧药': [
        { id: 'auto-33', drugName: '对乙酰氨基酚片', specification: '500mg', manufacturer: '泰诺林', category: '退烧药', matchScore: 0.9 },
        { id: 'auto-34', drugName: '布洛芬混悬液', specification: '100ml', manufacturer: '美林', category: '退烧药', matchScore: 0.85 },
        { id: 'auto-35', drugName: '复方锌布颗粒', specification: '复方', manufacturer: '臣功再欣', category: '退烧药', matchScore: 0.8 },
        { id: 'auto-36', drugName: '尼美舒利颗粒', specification: '50mg', manufacturer: '瑞芝清', category: '退烧药', matchScore: 0.75 }
      ],
      '甲状腺药': [
        { id: 'auto-37', drugName: '左甲状腺素钠片', specification: '50μg', manufacturer: '优甲乐', category: '甲状腺药', matchScore: 0.9 },
        { id: 'auto-38', drugName: '甲巯咪唑片', specification: '5mg', manufacturer: '他巴唑', category: '甲状腺药', matchScore: 0.85 },
        { id: 'auto-39', drugName: '丙硫氧嘧啶片', specification: '50mg', manufacturer: '丙赛优', category: '甲状腺药', matchScore: 0.8 },
        { id: 'auto-40', drugName: '甲状腺片', specification: '40mg', manufacturer: '上海长城药业', category: '甲状腺药', matchScore: 0.75 }
      ],
      '跌打损伤药': [
        { id: 'auto-41', drugName: '云南白药气雾剂', specification: '60g', manufacturer: '云南白药', category: '跌打损伤药', matchScore: 0.9 },
        { id: 'auto-42', drugName: '红花油', specification: '30ml', manufacturer: '广州白云山', category: '跌打损伤药', matchScore: 0.85 },
        { id: 'auto-43', drugName: '双氯芬酸二乙胺乳胶剂', specification: '20g', manufacturer: '扶他林', category: '跌打损伤药', matchScore: 0.8 },
        { id: 'auto-44', drugName: '正骨水', specification: '30ml', manufacturer: '玉林制药', category: '跌打损伤药', matchScore: 0.75 },
        { id: 'auto-45', drugName: '伤痛宁膏', specification: '7cm*10cm', manufacturer: '敬修堂', category: '跌打损伤药', matchScore: 0.7 }
      ],
      '清热解毒药': [
        { id: 'auto-46', drugName: '牛黄解毒片', specification: '0.3g', manufacturer: '北京同仁堂', category: '清热解毒药', matchScore: 0.9 },
        { id: 'auto-47', drugName: '板蓝根颗粒', specification: '10g', manufacturer: '广州白云山', category: '清热解毒药', matchScore: 0.85 },
        { id: 'auto-48', drugName: '金银花露', specification: '250ml', manufacturer: '华润三九', category: '清热解毒药', matchScore: 0.8 },
        { id: 'auto-49', drugName: '蒲地蓝消炎口服液', specification: '10ml', manufacturer: '济川药业', category: '清热解毒药', matchScore: 0.75 },
        { id: 'auto-50', drugName: '双黄连口服液', specification: '10ml', manufacturer: '河南太龙', category: '清热解毒药', matchScore: 0.7 },
        { id: 'auto-51', drugName: '清开灵颗粒', specification: '5g', manufacturer: '济安堂', category: '清热解毒药', matchScore: 0.65 }
      ],
      '抗生素': [
        { id: 'auto-52', drugName: '阿莫西林胶囊', specification: '500mg', manufacturer: '阿莫仙', category: '抗生素', matchScore: 0.9 },
        { id: 'auto-53', drugName: '头孢氨苄胶囊', specification: '250mg', manufacturer: '先锋霉素', category: '抗生素', matchScore: 0.85 },
        { id: 'auto-54', drugName: '阿奇霉素片', specification: '250mg', manufacturer: '希舒美', category: '抗生素', matchScore: 0.8 },
        { id: 'auto-55', drugName: '罗红霉素分散片', specification: '150mg', manufacturer: '仁苏', category: '抗生素', matchScore: 0.75 }
      ],
      '止咳化痰药': [
        { id: 'auto-56', drugName: '盐酸氨溴索口服溶液', specification: '100ml', manufacturer: '沐舒坦', category: '止咳化痰药', matchScore: 0.9 },
        { id: 'auto-57', drugName: '复方甘草口服溶液', specification: '100ml', manufacturer: '太极集团', category: '止咳化痰药', matchScore: 0.85 },
        { id: 'auto-58', drugName: '乙酰半胱氨酸泡腾片', specification: '600mg', manufacturer: '富露施', category: '止咳化痰药', matchScore: 0.8 },
        { id: 'auto-59', drugName: '川贝枇杷膏', specification: '300ml', manufacturer: '京都念慈菴', category: '止咳化痰药', matchScore: 0.75 },
        { id: 'auto-60', drugName: '右美沙芬愈创甘油醚糖浆', specification: '100ml', manufacturer: '惠菲宁', category: '止咳化痰药', matchScore: 0.7 }
      ],
      '维生素矿物质': [
        { id: 'auto-61', drugName: '复合维生素B片', specification: '复方', manufacturer: '汤臣倍健', category: '维生素矿物质', matchScore: 0.9 },
        { id: 'auto-62', drugName: '维生素C片', specification: '100mg', manufacturer: '力度伸', category: '维生素矿物质', matchScore: 0.85 },
        { id: 'auto-63', drugName: '钙尔奇D片', specification: '600mg', manufacturer: '惠氏', category: '维生素矿物质', matchScore: 0.8 },
        { id: 'auto-64', drugName: '善存多维元素片', specification: '复方', manufacturer: '惠氏', category: '维生素矿物质', matchScore: 0.75 },
        { id: 'auto-65', drugName: '葡萄糖酸钙锌口服溶液', specification: '10ml*24支', manufacturer: '澳诺', category: '维生素矿物质', matchScore: 0.7 }
      ],
      '安神助眠药': [
        { id: 'auto-66', drugName: '安神补脑液', specification: '10ml*10支', manufacturer: '敖东药业', category: '安神助眠药', matchScore: 0.9 },
        { id: 'auto-67', drugName: '褪黑素片', specification: '3mg', manufacturer: '汤臣倍健', category: '安神助眠药', matchScore: 0.85 },
        { id: 'auto-68', drugName: '养血安神片', specification: '0.4g', manufacturer: '同仁堂', category: '安神助眠药', matchScore: 0.8 },
        { id: 'auto-69', drugName: '柏子养心丸', specification: '9g*10丸', manufacturer: '同仁堂', category: '安神助眠药', matchScore: 0.75 }
      ],
      '心脑血管药': [
        { id: 'auto-70', drugName: '阿司匹林肠溶片', specification: '100mg', manufacturer: '拜耳', category: '心脑血管药', matchScore: 0.9 },
        { id: 'auto-71', drugName: '硫酸氢氯吡格雷片', specification: '75mg', manufacturer: '波立维', category: '心脑血管药', matchScore: 0.85 },
        { id: 'auto-72', drugName: '阿托伐他汀钙片', specification: '20mg', manufacturer: '立普妥', category: '心脑血管药', matchScore: 0.8 },
        { id: 'auto-73', drugName: '银杏叶片', specification: '9.6mg', manufacturer: '贵州益佰', category: '心脑血管药', matchScore: 0.75 },
        { id: 'auto-74', drugName: '复方丹参滴丸', specification: '27mg*180粒', manufacturer: '天士力', category: '心脑血管药', matchScore: 0.7 }
      ],
      '止泻药': [
        { id: 'auto-75', drugName: '蒙脱石散', specification: '3g', manufacturer: '思密达', category: '止泻药', matchScore: 0.9 },
        { id: 'auto-76', drugName: '口服补液盐III', specification: '5.125g', manufacturer: '博叶', category: '止泻药', matchScore: 0.85 },
        { id: 'auto-77', drugName: '肠炎宁片', specification: '0.42g', manufacturer: '康恩贝', category: '止泻药', matchScore: 0.8 },
        { id: 'auto-78', drugName: '枯草杆菌二联活菌肠溶胶囊', specification: '250mg', manufacturer: '妈咪爱', category: '止泻药', matchScore: 0.75 }
      ],
      '皮肤用药': [
        { id: 'auto-79', drugName: '曲安奈德益康唑乳膏', specification: '15g', manufacturer: '派瑞松', category: '皮肤用药', matchScore: 0.9 },
        { id: 'auto-80', drugName: '莫匹罗星软膏', specification: '5g', manufacturer: '百多邦', category: '皮肤用药', matchScore: 0.85 },
        { id: 'auto-81', drugName: '丹皮酚软膏', specification: '10g', manufacturer: '芙必叮', category: '皮肤用药', matchScore: 0.8 },
        { id: 'auto-82', drugName: '复方醋酸地塞米松乳膏', specification: '20g', manufacturer: '皮炎平', category: '皮肤用药', matchScore: 0.75 }
      ],
      '眼科用药': [
        { id: 'auto-83', drugName: '珍珠明目滴眼液', specification: '10ml', manufacturer: '天瑞制药', category: '眼科用药', matchScore: 0.9 },
        { id: 'auto-84', drugName: '左氧氟沙星滴眼液', specification: '5ml', manufacturer: '可乐必妥', category: '眼科用药', matchScore: 0.85 },
        { id: 'auto-85', drugName: '氯霉素滴眼液', specification: '8ml', manufacturer: '润洁', category: '眼科用药', matchScore: 0.8 },
        { id: 'auto-86', drugName: '玻璃酸钠滴眼液', specification: '10ml', manufacturer: '海露', category: '眼科用药', matchScore: 0.75 }
      ],
      '晕车药': [
        { id: 'auto-87', drugName: '茶苯海明片', specification: '25mg', manufacturer: '乘晕宁', category: '晕车药', matchScore: 0.9 },
        { id: 'auto-88', drugName: '盐酸地芬尼多片', specification: '25mg', manufacturer: '眩晕停', category: '晕车药', matchScore: 0.85 },
        { id: 'auto-89', drugName: '苯海拉明片', specification: '25mg', manufacturer: '可太敏', category: '晕车药', matchScore: 0.8 }
      ]
    };

    const lowerQuery = query.toLowerCase();
    const categoryKeywords = {
      '感冒药': ['感冒', '流感', '发烧', '咳嗽', '鼻塞', '流涕', '咽痛'],
      '止痛药': ['疼痛', '头痛', '牙痛', '关节痛', '腰痛', '止痛', '镇痛'],
      '退烧药': ['退烧', '发热', '体温'],
      '消炎药': ['消炎', '抗炎', '红肿', '发炎', '感染'],
      '胃药': ['胃', '胃痛', '胃酸', '胃胀', '消化', '反酸', '烧心', '恶心', '呕吐'],
      '降压药': ['血压', '降压', '高血压', '高压', '低压'],
      '降糖药': ['血糖', '降糖', '糖尿病', '高血糖'],
      '抗过敏药': ['过敏', '皮肤痒', '荨麻疹', '瘙痒', '皮疹', '湿疹'],
      '跌打损伤药': ['摔伤', '跌打', '损伤', '扭伤', '撞伤', '瘀伤', '挫伤', '外伤', '创伤', '骨伤', '撞伤', '拉伤'],
      '清热解毒药': ['清热解毒', '清热', '解毒', '上火', '炎症', '咽喉肿痛', '口舌生疮', '痤疮', '疖肿'],
      '甲状腺药': ['甲状腺', '甲亢', '甲减', '优甲乐', '甲亢治疗', '甲减治疗', '甲状腺功能'],
      '抗生素': ['抗生素', '抗菌', '杀菌', '消炎'],
      '止咳化痰药': ['咳嗽', '痰多', '咳痰', '痰液', '咽喉炎', '支气管炎', '肺炎'],
      '维生素矿物质': ['维生素', '补钙', '补铁', '贫血', '缺钙', '缺锌', '营养'],
      '安神助眠药': ['失眠', '睡眠', '安眠', '神经衰弱', '多梦', '易醒', '焦虑'],
      '心脑血管药': ['心脑血管', '心脏', '冠心病', '心绞痛', '心悸', '心慌', '血栓'],
      '止泻药': ['腹泻', '拉肚子', '止泻', '肠炎', '大便稀'],
      '皮肤用药': ['皮肤病', '皮肤', '湿疹', '皮炎', '癣', '瘙痒', '疱疹'],
      '眼科用药': ['眼睛', '眼', '结膜炎', '角膜炎', '眼干', '眼疲劳', '红眼'],
      '晕车药': ['晕车', '晕船', '晕机', '眩晕', '恶心', '呕吐']
    };

    for (const [category, keywords] of Object.entries(categoryKeywords)) {
      if (keywords.some(keyword => lowerQuery.includes(keyword))) {
        return categoryDrugs[category] || [];
      }
    }

    const drugAliases = {
      // 解热镇痛类
      '扑热息痛': '对乙酰氨基酚', '泰诺': '对乙酰氨基酚', '泰诺林': '对乙酰氨基酚',
      '芬必得': '布洛芬', '美林': '布洛芬', '臣功再欣': '复方锌布颗粒',
      '瑞芝清': '尼美舒利颗粒',
      
      // 消炎抗生素类
      '阿莫仙': '阿莫西林胶囊', '世福素': '头孢克肟分散片',
      '希舒美': '阿奇霉素片', '仁苏': '罗红霉素分散片',
      '可乐必妥': '左氧氟沙星片', '先锋霉素': '头孢氨苄胶囊',
      
      // 胃肠道类
      '洛赛克': '奥美拉唑肠溶胶囊', '思密达': '蒙脱石散',
      '达喜': '铝碳酸镁片', '吗丁啉': '多潘立酮片',
      '肠炎宁': '肠炎宁片', '妈咪爱': '枯草杆菌二联活菌肠溶胶囊',
      '博叶': '口服补液盐III',
      
      // 心血管类
      '拜新同': '硝苯地平缓释片', '络活喜': '苯磺酸氨氯地平片',
      '安博维': '厄贝沙坦片', '代文': '缬沙坦胶囊',
      '洛丁新': '贝那普利片', '波立维': '硫酸氢氯吡格雷片',
      '立普妥': '阿托伐他汀钙片',
      
      // 降糖类
      '格华止': '盐酸二甲双胍肠溶片', '达美康': '格列齐特缓释片',
      '拜糖平': '阿卡波糖片', '亚莫利': '格列美脲片',
      '诺和龙': '瑞格列奈片',
      
      // 抗过敏类
      '开瑞坦': '氯雷他定片', '仙特明': '盐酸西替利嗪片',
      '恩理思': '地氯雷他定片', '开思亭': '依巴斯汀片',
      
      // 中药类
      '云南白药': '云南白药气雾剂', '正骨水': '正骨水',
      '牛黄解毒': '牛黄解毒片', '板蓝根': '板蓝根颗粒',
      '双黄连': '双黄连口服液', '蒲地蓝': '蒲地蓝消炎口服液',
      '金银花': '金银花露', '清开灵': '清开灵颗粒',
      '川贝枇杷膏': '川贝枇杷膏', '安神补脑': '安神补脑液',
      '柏子养心': '柏子养心丸', '养血安神': '养血安神片',
      '银杏叶片': '银杏叶片', '复方丹参': '复方丹参滴丸',
      
      // 外用类
      '扶他林': '双氯芬酸二乙胺乳胶剂', '伤痛宁': '伤痛宁膏',
      '派瑞松': '曲安奈德益康唑乳膏', '百多邦': '莫匹罗星软膏',
      '皮炎平': '复方醋酸地塞米松乳膏', '芙必叮': '丹皮酚软膏',
      '润洁': '氯霉素滴眼液', '海露': '玻璃酸钠滴眼液',
      '沐舒坦': '盐酸氨溴索口服溶液', '富露施': '乙酰半胱氨酸泡腾片',
      '惠菲宁': '右美沙芬愈创甘油醚糖浆',
      
      // 甲状腺类
      '优甲乐': '左甲状腺素钠片', '他巴唑': '甲巯咪唑片',
      '丙赛优': '丙硫氧嘧啶片', '甲状腺片': '甲状腺片',
      
      // 晕车类
      '乘晕宁': '茶苯海明片', '眩晕停': '盐酸地芬尼多片',
      '可太敏': '苯海拉明片',
      
      // 其他常用
      '感冒灵': '感冒灵颗粒', '白加黑': '白加黑',
      '感康': '复方氨酚烷胺',
      '钙尔奇': '钙尔奇D片', '善存': '善存多维元素片',
      '澳诺': '葡萄糖酸钙锌口服溶液', '力度伸': '维生素C片',
      '褪黑素': '褪黑素片'
    };

    const canonicalName = drugAliases[lowerQuery] || drugAliases[query] || query;

    // 修复：使用统一的模糊匹配逻辑，显示所有符合条件的药品
    return allDrugs.filter(drug => {
      const drugNameLower = drug.drugName.toLowerCase();
      const manufacturerLower = drug.manufacturer.toLowerCase();
      
      // 只要药品名称或生产厂家包含搜索关键词，就显示
      return drugNameLower.includes(lowerQuery) || 
             manufacturerLower.includes(lowerQuery) ||
             drugNameLower.includes(canonicalName.toLowerCase());
    }).slice(0, 20); // 增加返回数量到20条
  };

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (!showResults || searchResults.length === 0) return;

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex(prev => prev < searchResults.length - 1 ? prev + 1 : prev);
          break;
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
          break;
        case 'Enter':
          e.preventDefault();
          if (selectedIndex >= 0 && selectedIndex < searchResults.length) {
            handleSelectDrug(searchResults[selectedIndex]);
          }
          break;
        case 'Escape':
          setShowResults(false);
          break;
        default:
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [showResults, searchResults, selectedIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handleClickOutside = (e) => {
      const searchContainer = document.querySelector('.manual-search-container');
      const resultsContainer = document.querySelector('.manual-search-results');
      // 点击搜索容器内部或搜索结果列表内部不关闭
      if (searchContainer && !searchContainer.contains(e.target) && 
          (!resultsContainer || !resultsContainer.contains(e.target))) {
        setShowResults(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelectDrug = (drug) => {
    // 关闭搜索结果列表
    setShowResults(false);
    setSelectedIndex(-1);
    
    // 直接确认选择，不显示确认弹窗
    confirmAndSelect(drug);
  };

  const confirmAndSelect = (drug) => {
    setShowResults(false);
    setSearchQuery('');
    setIsLoading(true);
    setLoadingMessage(`正在加载 ${drug.drugName || drug.genericName || drug.name} 的详细信息...`);
    
    const normalizedDrug = {
      id: drug.id || Date.now(),
      name: drug.drugName || drug.genericName || drug.name,
      spec: drug.specification || drug.spec || '',
      manufacturer: drug.manufacturer || '',
      matchScore: drug.matchScore || 0,
      genericName: drug.drugName || drug.genericName || drug.name,
      tradeName: drug.tradeName || '',
      category: drug.category || ''
    };

    // 调用父组件的回调，传入加载完成回调函数
    onSelectDrug(normalizedDrug, {
      onComplete: () => {
        // 当药品详情获取完成并跳转到说明页面后，关闭加载状态
        setTimeout(() => {
          setIsLoading(false);
          setLoadingMessage('');
        }, 300); // 延迟一点关闭，让页面跳转更平滑
      },
      onProgress: (message) => {
        // 进度更新回调
        if (message) {
          setLoadingMessage(message);
        }
      }
    });
  };

  const handleSmartSearch = () => {
    if (searchQuery.trim()) {
      searchDrugs(searchQuery);
    }
  };

  // 切换语音输入
  const toggleVoiceInput = () => {
    if (isListening) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      return;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    const recognition = new SpeechRecognition();
    recognition.lang = 'zh-CN';
    recognition.interimResults = true;
    recognition.continuous = false;

    recognition.onstart = () => setIsListening(true);

    recognition.onresult = (event) => {
      let transcript = '';
      for (let i = 0; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }
      setSearchQuery(transcript);
    };

    recognition.onerror = () => setIsListening(false);
    recognition.onend = () => setIsListening(false);

    recognitionRef.current = recognition;
    recognition.start();
  };

  // 加载更多药品
  const handleLoadMore = () => {
    setDisplayCount(prev => prev + 3); // 每次再加载3条
  };

  // 触摸滑动处理
  const touchStartRef = useRef(null);
  const touchEndRef = useRef(null);

  const handleTouchStart = (e) => {
    touchStartRef.current = e.touches[0].clientY;
  };

  const handleTouchMove = (e) => {
    touchEndRef.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e) => {
    if (!touchStartRef.current || !touchEndRef.current) return;
    
    const distance = touchStartRef.current - touchEndRef.current;
    const isSwipeUp = distance > 50; // 向上滑动超过50px

    // 向上滑动到底部时，加载更多
    if (isSwipeUp && displayCount < searchResults.length) {
      const container = e.target.closest('.manual-search-drawer-content');
      if (container) {
        const { scrollTop, scrollHeight, clientHeight } = container;
        // 如果接近底部（距离底部小于100px），加载更多
        if (scrollHeight - scrollTop - clientHeight < 100) {
          handleLoadMore();
        }
      }
    }

    // 重置触摸位置
    touchStartRef.current = null;
    touchEndRef.current = null;
  };

  return (
    <div className="manual-search-container" style={{ position: 'relative', width: '100%' }}>
      {/* 加载状态提示 */}
      {isLoading && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'radial-gradient(ellipse at center, rgba(74, 144, 226, 0.15) 0%, rgba(0, 0, 0, 0.35) 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          backdropFilter: 'blur(8px)',
          WebkitBackdropFilter: 'blur(8px)'
        }}>
          <div style={{
            background: 'linear-gradient(145deg, #FFFFFF 0%, #F8F9FA 100%)',
            borderRadius: '28px',
            padding: '44px 68px',
            textAlign: 'center',
            boxShadow: '0 24px 72px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.9) inset',
            maxWidth: '420px',
            animation: 'fadeInUp 0.3s ease-out',
            WebkitAnimation: 'fadeInUp 0.3s ease-out'
          }}>
            {/* 加载动画 */}
            <div style={{
              width: '80px',
              height: '80px',
              margin: '0 auto 24px',
              position: 'relative'
            }}>
              <div style={{
                width: '80px',
                height: '80px',
                border: '6px solid #E3F2FD',
                borderTop: '6px solid #4A90E2',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite'
              }} />
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                fontSize: '32px'
              }}>
                💊
              </div>
            </div>
            
            {/* 加载文字 */}
            <h3 style={{
              fontSize: '22px',
              fontWeight: '700',
              color: '#3D3D3D',
              margin: '0 0 12px 0'
            }}>
              正在处理您的选择
            </h3>
            <p style={{
              fontSize: '16px',
              color: '#6B6B6B',
              margin: 0,
              lineHeight: '1.5'
            }}>
              {loadingMessage}
            </p>
            
            {/* 进度指示 */}
            <div style={{
              marginTop: '24px',
              width: '100%',
              height: '6px',
              background: '#E3F2FD',
              borderRadius: '3px',
              overflow: 'hidden'
            }}>
              <div style={{
                width: '70%',
                height: '100%',
                background: 'linear-gradient(90deg, #4A90E2 0%, #66BB6A 100%)',
                borderRadius: '3px',
                animation: 'loading 1.5s ease-in-out infinite'
              }} />
            </div>
          </div>
        </div>
      )}
      
      {/* CSS 动画样式 */}
      <style>{`
        .manual-search-drawer-override {
          position: absolute !important;
          top: 100% !important;
          left: 0 !important;
          right: 0 !important;
          width: 100% !important;
          background: white !important;
          border-radius: 16px !important;
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15) !important;
          z-index: 1000 !important;
          margin-top: 8px !important;
          border: 2px solid #E0E0E0 !important;
        }
        .manual-search-drawer-content {
          width: 100% !important;
          max-height: 500px !important;
          background: white !important;
          border-radius: 16px !important;
          box-shadow: none !important;
          overflow-y: auto !important; /* 允许垂直滚动 */
          overflow-x: hidden !important;
          display: flex !important;
          flex-direction: column !important;
          animation: slideDown 0.2s ease !important;
          -webkit-overflow-scrolling: touch !important; /* iOS平滑滚动 */
        }
        @keyframes slideDown {
          0% {
            opacity: 0;
            transform: translateY(-10px);
          }
          100% {
            opacity: 1;
            transform: translateY(0);
          }
        }
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        @keyframes loading {
          0% { width: 0%; }
          50% { width: 70%; }
          100% { width: 100%; }
        }
        @keyframes fadeInUp {
          0% {
            opacity: 0;
            transform: translateY(20px) scale(0.95);
          }
          100% {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
        }
        @-webkit-keyframes fadeInUp {
          0% {
            opacity: 0;
            -webkit-transform: translateY(20px) scale(0.95);
          }
          100% {
            opacity: 1;
            -webkit-transform: translateY(0) scale(1);
          }
        }
      `}</style>

      <div style={{ position: 'relative', display: 'flex', gap: '12px', alignItems: 'center' }}>
        <input
          ref={inputRef}
          type="text"
          value={searchQuery}
          onChange={handleInputChange}
          onKeyPress={(e) => {
            if (e.key === 'Enter' && searchQuery.trim() && !isLoading) {
              handleSmartSearch();
            }
          }}
          disabled={isLoading}
          placeholder={isListening ? '正在聆听，请说出药品名称...' : '请输入药品名称、症状或类别，如：感冒药、甲状腺、摔伤...'}
          style={{
            flex: 1,
            padding: '20px 24px 20px 64px',
            fontSize: '20px',
            border: isListening ? '3px solid var(--danger-red, #E74C3C)' : '3px solid #F0EBE3',
            borderRadius: '20px',
            outline: 'none',
            transition: 'all 0.3s ease',
            background: isLoading ? '#F5F5F5' : '#FAF7F2',
            fontFamily: 'inherit',
            boxSizing: 'border-box',
            opacity: isLoading ? 0.6 : 1,
            cursor: isLoading ? 'not-allowed' : 'text'
          }}
          onFocus={(e) => {
            if (!isLoading) {
              if (isListening) {
                e.target.style.borderColor = 'var(--danger-red, #E74C3C)';
                e.target.style.boxShadow = '0 0 0 6px rgba(231, 76, 60, 0.15)';
              } else {
                e.target.style.borderColor = '#4A90E2';
                e.target.style.boxShadow = '0 0 0 6px rgba(74, 144, 226, 0.12)';
              }
              e.target.style.background = 'white';
            }
          }}
          onBlur={(e) => {
            if (!isLoading) {
              setTimeout(() => {
                e.target.style.borderColor = '#F0EBE3';
                e.target.style.boxShadow = 'none';
                e.target.style.background = '#FAF7F2';
              }, 200);
            }
          }}
        />
        <span style={{
          position: 'absolute',
          left: '20px',
          top: '50%',
          transform: 'translateY(-50%)',
          fontSize: '24px',
          opacity: isLoading ? 0.5 : 1
        }}>
          🔍
        </span>
        {voiceSupported && (
          <button
            onClick={toggleVoiceInput}
            disabled={isLoading}
            title={isListening ? '停止语音输入' : '语音输入'}
            style={{
              padding: '16px',
              fontSize: '18px',
              fontWeight: '600',
              border: 'none',
              borderRadius: '16px',
              background: isListening 
                ? 'linear-gradient(135deg, #F87171 0%, #C0392B 100%)' 
                : 'linear-gradient(135deg, #5B9EF0 0%, var(--tech-blue-dark, #357ABD) 100%)',
              color: 'white',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              transition: 'all 0.3s ease',
              boxShadow: isListening 
                ? '0 4px 16px rgba(231, 76, 60, 0.4)' 
                : '0 4px 16px rgba(74, 144, 226, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              opacity: isLoading ? 0.6 : 1
            }}
            onMouseEnter={(e) => {
              if (!isLoading) {
                e.target.style.transform = 'translateY(-2px)';
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.transform = 'translateY(0)';
            }}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
              <line x1="12" y1="19" x2="12" y2="22" />
            </svg>
          </button>
        )}
        <button
          onClick={handleSmartSearch}
          disabled={!searchQuery.trim() || isSearching || isLoading}
          style={{
            padding: '16px 24px',
            fontSize: '18px',
            fontWeight: '600',
            border: 'none',
            borderRadius: '16px',
            background: searchQuery.trim() && !isSearching && !isLoading
              ? 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)' 
              : '#E0E0E0',
            color: searchQuery.trim() && !isSearching && !isLoading ? 'white' : '#9E9E9E',
            cursor: searchQuery.trim() && !isSearching && !isLoading ? 'pointer' : 'not-allowed',
            transition: 'all 0.3s ease',
            boxShadow: searchQuery.trim() && !isSearching && !isLoading 
              ? '0 4px 16px rgba(74, 144, 226, 0.3)' 
              : 'none',
            whiteSpace: 'nowrap',
            opacity: isLoading ? 0.6 : 1
          }}
          onMouseEnter={(e) => {
            if (searchQuery.trim() && !isSearching && !isLoading) {
              e.target.style.transform = 'translateY(-2px)';
              e.target.style.boxShadow = '0 6px 20px rgba(74, 144, 226, 0.4)';
            }
          }}
          onMouseLeave={(e) => {
            e.target.style.transform = 'translateY(0)';
            e.target.style.boxShadow = searchQuery.trim() && !isSearching && !isLoading 
              ? '0 4px 16px rgba(74, 144, 226, 0.3)' 
              : 'none';
          }}
        >
          {isSearching ? '🔄 识别中...' : '✨ 搜索识别'}
        </button>
      </div>
      
      {searchQuery.trim() && !showResults && !isSearching && !isLoading && (
        <div style={{
          marginTop: '12px',
          padding: '12px 16px',
          background: 'linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%)',
          borderRadius: '12px',
          border: '2px solid #FF9800'
        }}>
          <p style={{
            fontSize: '14px',
            color: '#E65100',
            margin: 0,
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            � 提示：请点击"搜索识别"按钮或按 Enter 键搜索"【{searchQuery}】"
          </p>
        </div>
      )}

      {showResults && searchResults.length > 0 && (
        <div 
          className="manual-search-drawer-override" 
          onClick={() => setShowResults(false)}
          style={{
            position: 'absolute', // 相对于父容器定位
            top: '100%', // 在输入框下方
            left: 0,
            right: 0,
            marginTop: '8px',
            zIndex: 1000
          }}
        >
          <div 
            className="manual-search-drawer-content"
            onClick={(e) => e.stopPropagation()}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            style={{
              width: '100%',
              maxHeight: '500px', // 固定最大高度
              overflowY: 'auto', // 允许垂直滚动
              scrollBehavior: 'smooth', // 平滑滚动
              background: 'white',
              borderRadius: '16px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
              border: '2px solid #E0E0E0'
            }}
          >
            {/* 拖拽指示器 */}
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              padding: '16px 0',
              background: '#F5F5F5'
            }}>
              <div style={{
                width: '48px',
                height: '5px',
                background: '#D0D0D0',
                borderRadius: '3px'
              }} />
            </div>
            
            <div style={{
              padding: '16px 20px',
              background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
              borderBottom: '2px solid #4A90E2'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <p style={{
                    fontSize: '16px',
                    color: '#4A90E2',
                    fontWeight: '600',
                    margin: 0
                  }}>
                    ✅ 智能搜索成功
                  </p>
                  <p style={{
                    fontSize: '14px',
                    color: '#1976D2',
                    margin: '4px 0 0 0'
                  }}>
                    为您找到 {searchResults.length} 个"【{searchQuery}】"相关药品
                    {searchResults[0]?.category && <span style={{ marginLeft: '8px', fontWeight: '600' }}>
                      • 类别：{searchResults[0].category}
                    </span>}
                  </p>
                </div>
                <button
                  onClick={() => {
                    setShowResults(false);
                    setSearchQuery('');
                  }}
                  style={{
                    padding: '8px 16px',
                    fontSize: '14px',
                    border: '2px solid #F0EBE3',
                    borderRadius: '12px',
                    background: 'white',
                    color: '#6B6B6B',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.borderColor = '#E53935';
                    e.target.style.color = '#E53935';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.borderColor = '#F0EBE3';
                    e.target.style.color = '#6B6B6B';
                  }}
                >
                  关闭
                </button>
              </div>
            </div>

            {searchResults.slice(0, displayCount).map((drug, index) => (
              <div
                key={drug.id}
                onClick={() => handleSelectDrug(drug)}
                style={{
                  padding: '16px 20px',
                  cursor: 'pointer',
                  borderBottom: index < searchResults.length - 1 ? '1px solid #F0F0F0' : 'none',
                  backgroundColor: selectedIndex === index ? '#E3F2FD' : 'white',
                  transition: 'background-color 0.2s ease'
                }}
                onMouseEnter={() => setSelectedIndex(index)}
                onMouseLeave={() => setSelectedIndex(-1)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <p style={{
                      fontSize: '18px',
                      fontWeight: '600',
                      color: '#3D3D3D',
                      margin: '0 0 4px 0'
                    }}>
                      {drug.drugName || drug.genericName}
                    </p>
                    <p style={{
                      fontSize: '14px',
                      color: '#6B6B6B',
                      margin: 0
                    }}>
                      {drug.specification || drug.spec} - {drug.manufacturer}
                      {drug.category && <span style={{ marginLeft: '8px', color: '#4A90E2' }}>• {drug.category}</span>}
                    </p>
                  </div>
                  {drug.matchScore && drug.matchScore > 0 && (
                    <div style={{
                      padding: '6px 12px',
                      background: drug.matchScore >= 0.8 ? '#E8F5E9' : drug.matchScore >= 0.5 ? '#FFF3E0' : '#FFEBEE',
                      borderRadius: '20px',
                      fontSize: '14px',
                      fontWeight: '600',
                      color: drug.matchScore >= 0.8 ? '#2E7D32' : drug.matchScore >= 0.5 ? '#E65100' : '#C62828'
                    }}>
                      {Math.round(drug.matchScore * 100)}%
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* 加载更多按钮 */}
            {displayCount < searchResults.length && (
              <div style={{
                padding: '16px 20px',
                textAlign: 'center',
                borderTop: '1px solid #F0F0F0'
              }}>
                <button
                  onClick={handleLoadMore}
                  style={{
                    padding: '12px 32px',
                    fontSize: '16px',
                    fontWeight: '600',
                    border: '2px solid #4A90E2',
                    borderRadius: '12px',
                    background: 'white',
                    color: '#4A90E2',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    boxShadow: '0 2px 8px rgba(74, 144, 226, 0.2)'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.background = '#4A90E2';
                    e.target.style.color = 'white';
                    e.target.style.boxShadow = '0 4px 12px rgba(74, 144, 226, 0.3)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.background = 'white';
                    e.target.style.color = '#4A90E2';
                    e.target.style.boxShadow = '0 2px 8px rgba(74, 144, 226, 0.2)';
                  }}
                >
                  📦 再多加载三条（还剩 {searchResults.length - displayCount} 条）
                </button>
              </div>
            )}

            <div style={{
              padding: '12px 20px',
              background: '#FAFAFA',
              textAlign: 'center'
            }}>
              <p style={{
                fontSize: '14px',
                color: '#9E9E9E',
                margin: 0
              }}>
                点击药品查看详情
              </p>
            </div>
          </div>
        </div>
      )}

      {showResults && searchResults.length === 0 && !isSearching && (
        <div style={{
          position: 'absolute',
          top: 'calc(100% + 8px)',
          left: 0,
          right: 0,
          background: 'white',
          borderRadius: '16px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
          padding: '32px',
          textAlign: 'center',
          border: '2px solid #E0E0E0'
        }}>
          <div style={{
            width: '80px',
            height: '80px',
            background: 'linear-gradient(135deg, #F5F5F5 0%, #E0E0E0 100%)',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
            fontSize: '40px'
          }}>
            �
          </div>
          <h3 style={{
            fontSize: '20px',
            fontWeight: '700',
            color: '#616161',
            margin: '0 0 12px 0'
          }}>
            暂无相关信息
          </h3>
          <p style={{
            fontSize: '16px',
            color: '#6B6B6B',
            margin: 0
          }}>
            系统未能在数据库中找到与"【{searchQuery}】"相关的药品
          </p>
          <div style={{
            marginTop: '20px',
            padding: '16px',
            background: '#FFF8E1',
            borderRadius: '12px'
          }}>
            <p style={{
              fontSize: '14px',
              color: '#F57C00',
              margin: '0 0 8px 0',
              fontWeight: '600'
            }}>
              � 建议上传药品图片进行识别
            </p>
            <p style={{
              fontSize: '14px',
              color: '#6B6B6B',
              margin: 0,
              lineHeight: '1.5'
            }}>
              如果您有药品包装盒照片，可以尝试使用页面上方的图片识别功能，通过拍照或上传图片来识别药品信息。
            </p>
          </div>
          <div style={{
            marginTop: '16px',
            padding: '12px',
            background: '#E3F2FD',
            borderRadius: '12px'
          }}>
            <p style={{
              fontSize: '14px',
              color: '#1976D2',
              margin: '0 0 8px 0',
              fontWeight: '600'
            }}>
              💡 也可以尝试：
            </p>
            <ul style={{
              fontSize: '13px',
              color: '#424242',
              margin: 0,
              paddingLeft: '20px',
              textAlign: 'left',
              lineHeight: '1.6'
            }}>
              <li>使用更通用的类别名称（如：感冒药、止痛药）</li>
              <li>尝试输入具体药品名称（如：布洛芬、阿莫西林）</li>
              <li>使用症状描述（如：发烧、头痛、胃痛）</li>
            </ul>
          </div>
          <button
            onClick={() => {
              setShowResults(false);
              setSearchQuery('');
            }}
            style={{
              marginTop: '20px',
              padding: '12px 24px',
              fontSize: '16px',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              background: 'white',
              color: '#616161',
              cursor: 'pointer',
              transition: 'all 0.3s ease'
            }}
            onMouseEnter={(e) => {
              e.target.style.borderColor = '#4A90E2';
              e.target.style.color = '#4A90E2';
            }}
            onMouseLeave={(e) => {
              e.target.style.borderColor = '#E0E0E0';
              e.target.style.color = '#616161';
            }}
          >
            返回重新搜索
          </button>
        </div>
      )}
    </div>
  );
}

export default ManualDrugSearch;
