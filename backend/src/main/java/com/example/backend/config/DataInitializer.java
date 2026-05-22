package com.example.backend.config;

import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.model.entity.DrugBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化组件
 * 在应用启动时自动插入药品测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final DrugBaseMapper drugBaseMapper;

    public DataInitializer(DrugBaseMapper drugBaseMapper) {
        this.drugBaseMapper = drugBaseMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查药品表是否已有数据
        List<DrugBase> existingDrugs = drugBaseMapper.selectList(null);
        
        if (existingDrugs.isEmpty()) {
            logger.info("药品表为空，开始插入测试数据...");
            
            // 插入测试药品数据
            insertTestDrugs();
            
            logger.info("药品测试数据插入完成");
        } else {
            logger.info("药品表已有 {} 条数据，跳过初始化", existingDrugs.size());
        }
    }

    private void insertTestDrugs() {
        // 感冒灵颗粒
        DrugBase drug1 = new DrugBase();
        drug1.setApprovalNumber("国药准字H11021904");
        drug1.setGenericName("感冒灵颗粒");
        drug1.setTradeName("999");
        drug1.setCommonName("三九感冒灵");
        drug1.setSpecification("10g*9袋");
        drug1.setManufacturer("华润三九医药股份有限公司");
        drug1.setCategory("非处方药");
        drug1.setDescription("成分：三叉苦、金盏银盘、野菊花、岗梅、咖啡因、对乙酰氨基酚、马来酸氯苯那敏、薄荷油。适应症：解热镇痛。用于感冒引起的头痛，发热，鼻塞，流涕，咽痛。用法用量：开水冲服，一次1袋，一日3次。注意事项：1.忌烟、酒及辛辣、生冷、油腻食物。2.不宜在服药期间同时服用滋补性中成药。3.脾胃虚寒者慎用。4.糖尿病患者及有高血压、心脏病、肝病、肾病等慢性病严重者应在医师指导下服用。5.儿童、年老体弱者应在医师指导下服用。不良反应：偶见皮疹、荨麻疹、药热及粒细胞减少；长期大量用药会导致肝肾功能异常。");
        drugBaseMapper.insert(drug1);

        // 硝苯地平缓释片
        DrugBase drug2 = new DrugBase();
        drug2.setApprovalNumber("国药准字H37021369");
        drug2.setGenericName("硝苯地平缓释片");
        drug2.setTradeName("欣然");
        drug2.setSpecification("20mg*30片");
        drug2.setManufacturer("山东新时代药业有限公司");
        drug2.setCategory("处方药");
        drug2.setDescription("成分：硝苯地平。适应症：用于治疗高血压、心绞痛。用法用量：口服，一次1片，一日1-2次。注意事项：1.低血压患者慎用。2.肝肾功能不全者慎用。3.孕妇及哺乳期妇女慎用。4.长期给药不宜骤停。不良反应：常见不良反应有头痛、面部潮红、下肢水肿、心悸、头晕等，一般较轻，多可耐受。");
        drugBaseMapper.insert(drug2);

        // 阿司匹林肠溶片
        DrugBase drug3 = new DrugBase();
        drug3.setApprovalNumber("国药准字H10950010");
        drug3.setGenericName("阿司匹林肠溶片");
        drug3.setTradeName("拜阿司匹灵");
        drug3.setSpecification("100mg*30片");
        drug3.setManufacturer("拜耳医药保健有限公司");
        drug3.setCategory("处方药");
        drug3.setDescription("成分：阿司匹林。适应症：用于抑制血小板聚集，预防心肌梗死、脑梗死、短暂性脑缺血发作。用法用量：口服，一次1片，一日1次。注意事项：1.对阿司匹林过敏者禁用。2.有出血倾向者慎用。3.孕妇及哺乳期妇女慎用。不良反应：常见胃肠道反应，如恶心、呕吐、上腹部不适等。");
        drugBaseMapper.insert(drug3);

        // 阿莫西林胶囊
        DrugBase drug4 = new DrugBase();
        drug4.setApprovalNumber("国药准字H20033593");
        drug4.setGenericName("阿莫西林胶囊");
        drug4.setTradeName("阿莫仙");
        drug4.setSpecification("0.5g*24粒");
        drug4.setManufacturer("香港联邦制药厂有限公司");
        drug4.setCategory("处方药");
        drug4.setDescription("成分：阿莫西林。适应症：用于敏感菌所致的呼吸道感染、泌尿道感染、消化道感染、皮肤和软组织感染等。用法用量：口服，一次0.5g，每6～8小时1次。注意事项：1.对青霉素过敏者禁用。2.传染性单核细胞增多症患者应用本品易发生皮疹，应避免使用。3.疗程较长患者应检查肝、肾功能和血常规。不良反应：恶心、呕吐、腹泻及假膜性肠炎等胃肠道反应较为常见。");
        drugBaseMapper.insert(drug4);

        // 板蓝根颗粒
        DrugBase drug5 = new DrugBase();
        drug5.setApprovalNumber("国药准字Z44022460");
        drug5.setGenericName("板蓝根颗粒");
        drug5.setSpecification("10g*20袋");
        drug5.setManufacturer("广州白云山和记黄埔中药有限公司");
        drug5.setCategory("非处方药");
        drug5.setDescription("成分：板蓝根。适应症：清热解毒，凉血，利咽。用于肺胃热盛，咽喉肿痛，口咽干燥；急性扁桃体炎见上述证候者。用法用量：开水冲服，一次5-10克，一日3-4次。注意事项：1.忌烟、酒及辛辣、生冷、油腻食物。2.不宜在服药期间同时服用滋补性中药。3.糖尿病患者及有高血压、心脏病、肝病、肾病等慢性病严重者应在医师指导下服用。不良反应：尚不明确。");
        drugBaseMapper.insert(drug5);

        // 氯雷他定片
        DrugBase drug6 = new DrugBase();
        drug6.setApprovalNumber("国药准字H19993038");
        drug6.setGenericName("氯雷他定片");
        drug6.setTradeName("开瑞坦");
        drug6.setSpecification("10mg*6片");
        drug6.setManufacturer("上海先灵葆雅制药有限公司");
        drug6.setCategory("非处方药");
        drug6.setDescription("成分：氯雷他定。适应症：用于缓解过敏性鼻炎有关的症状，如喷嚏、流涕、鼻痒、鼻塞以及眼部痒及烧灼感。用法用量：口服，成人及12岁以上儿童，一日1次，一次1片。注意事项：1.对氯雷他定过敏者禁用。2.孕妇及哺乳期妇女慎用。3.肝功能不全者应在医师指导下使用。不良反应：常见乏力、疲倦、口干和头痛等。");
        drugBaseMapper.insert(drug6);

        // 藿香正气水
        DrugBase drug7 = new DrugBase();
        drug7.setApprovalNumber("国药准字Z20027142");
        drug7.setGenericName("藿香正气水");
        drug7.setSpecification("10ml*10支");
        drug7.setManufacturer("太极集团重庆涪陵制药厂有限公司");
        drug7.setCategory("非处方药");
        drug7.setDescription("成分：苍术、陈皮、厚朴(姜制)、白芷、茯苓、大腹皮、生半夏、甘草浸膏、广藿香油、紫苏叶油。适应症：解表化湿，理气和中。用于外感风寒、内伤湿滞或夏伤暑湿所致的感冒，头痛昏重、胸膈痞闷、脘腹胀痛、呕吐泄泻；胃肠型感冒见上述证候者。用法用量：口服，一次5-10毫升，一日2次，用时摇匀。注意事项：1.忌烟、酒及辛辣、生冷、油腻食物，饮食宜清淡。2.不宜在服药期间同时服用滋补性中药。3.服药后不得驾驶机、车、船，从事高空作业、机械作业及操作精密仪器。不良反应：个别患者服药后出现皮疹、瘙痒、头晕、潮红、心悸等。");
        drugBaseMapper.insert(drug7);

        // 葡萄糖酸钙片
        DrugBase drug8 = new DrugBase();
        drug8.setApprovalNumber("国药准字H10880016");
        drug8.setGenericName("葡萄糖酸钙片");
        drug8.setSpecification("0.5g*100片");
        drug8.setManufacturer("哈药集团制药六厂");
        drug8.setCategory("非处方药");
        drug8.setDescription("成分：葡萄糖酸钙。适应症：用于预防和治疗钙缺乏症，如骨质疏松、手足抽搐症、骨发育不全、佝偻病以及儿童、妊娠和哺乳期妇女、绝经期妇女、老年人钙的补充。用法用量：口服，一次1-4片，一日3次。注意事项：1.心肾功能不全者慎用。2.对本品过敏者禁用，过敏体质者慎用。不良反应：偶见便秘。");
        drugBaseMapper.insert(drug8);
    }
}