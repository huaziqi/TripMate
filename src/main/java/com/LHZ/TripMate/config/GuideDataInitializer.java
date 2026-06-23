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
        if (configRepo.findBySpotKey("swu").isPresent()) return;

        GuideSpotConfig config = new GuideSpotConfig();
        config.setSpotKey("swu");
        config.setPersonaName("小渝");
        config.setPersonaDesc("西南大学的专属校园导览助手，熟悉学校的历史沿革、标志性建筑、校园文化和日常生活，性格温暖幽默，说话亲切自然");
        config.setKnowledgeText("""
                西南大学创建于1906年，坐落于重庆市北碚区，是教育部直属的全国重点综合大学，\
                国家"211工程"和"985工程优势学科创新平台"建设高校。\
                校训为"含弘光大，继往开来"。\
                学校拥有北碚主校区和荣昌校区。\
                北碚主校区主要地标建筑包括：\
                含弘楼（学校行政办公中心，建筑宏伟）、\
                图书馆（馆藏丰富，建筑宏伟，是师生学习的重要场所）、\
                惟勤楼（主要教学楼群）、\
                博雅广场（校园中心广场，常举办各类活动）、\
                荷花池（校园标志性景观，四季皆美）、\
                大学生活动中心（学生文化艺术活动核心场所）。\
                学校拥有农学、教育学、心理学、蚕学等全国领先的优势学科，\
                是重庆市重要的科研和人才培养基地。\
                校园依山傍水，绿树成荫，被评为"最美大学校园"之一。\
                """);
        config.setActive(true);
        configRepo.save(config);
        log.info("西南大学导览配置初始化完成");
    }
}
