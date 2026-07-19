package com.LHZ.TripMate.config;

import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuideDataInitializer implements CommandLineRunner {

    private final GuideSpotConfigRepository configRepo;

    @Override
    public void run(String... args) {
        // 清除旧的西南大学导览配置
        configRepo.findBySpotKey("swu").ifPresent(config -> {
            configRepo.delete(config);
            log.info("已删除旧的西南大学导览配置");
        });

        if (configRepo.findBySpotKey("lingshan").isPresent()) return;

        GuideSpotConfig config = new GuideSpotConfig();
        config.setSpotKey("lingshan");
        config.setPersonaName("小灵");
        config.setPersonaDesc("无锡灵山胜境的专属导览数字人，熟悉景区千年佛教历史、核心景点、演出时间与个性化游览路线，性格温暖亲切，讲解生动自然");
        config.setKnowledgeText("""
                灵山胜境坐落于江苏省无锡市太湖之滨的马山半岛，占地约30万平方米，\
                是国家5A级旅游景区、世界佛教论坛永久会址，被誉为"东方佛国"。\
                唐贞观年间玄奘法师见此地山形酷似印度灵鹫山，命名"小灵山"，\
                并嘱大弟子窥基在此开坛讲经；北宋赐额"祥符禅寺"，千年香火延续至今。\
                核心景点沿中轴线分布：灵山大照壁（华夏第一壁）、五明桥、佛足坛、五智门、\
                菩提大道、九龙灌浴（每日10:00、11:30、13:30、15:00动态表演，可接祈福圣水）、\
                降魔浮雕、阿育王柱、百子戏弥勒、祥符禅寺（千年银杏、江南第一钟）、\
                灵山大佛（88米世界最高露天青铜释迦牟尼立像，216级登云道，可抱佛脚）。\
                西侧香水海畔有灵山梵宫（东方卢浮宫，《吉祥颂》演出每日10:35、11:30、14:00、16:00）、\
                五印坛城（藏式建筑，108个转经筒）、曼飞龙塔（南传白塔），构成佛教三大语系建筑群。\
                邻近的拈花湾禅意小镇有拈花广场、香月花街、拈花堂（免费抄经禅茶）、\
                五灯湖（夜间19:00、20:00《禅行》灯光秀）、梵天花海等，适合慢生活体验。\
                门票：成人票210元，6-18周岁及学生半价105元，70岁以上老人免票；观光车40元。\
                景区可根据游客兴趣（历史文化、自然风光、亲子家庭、佛教艺术、祈福体验、禅意慢生活）\
                推荐个性化游览路线。\
                """);
        config.setActive(true);
        configRepo.save(config);
        log.info("灵山胜境导览配置初始化完成");
    }
}
