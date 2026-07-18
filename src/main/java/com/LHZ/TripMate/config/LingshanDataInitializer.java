package com.LHZ.TripMate.config;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.repository.ScenicSpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 灵山胜境景点种子数据。
 * 首次启动时清除旧景点数据（原西南大学校园数据），写入无锡灵山胜境 + 拈花湾禅意小镇景点。
 * 数据来源：示范景区公开资料包《灵山胜境 景点结构化数据集》。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LingshanDataInitializer implements CommandLineRunner {

    private static final String LINGSHAN_ADDRESS = "江苏省无锡市滨湖区马山灵山路1号 灵山胜境景区内";
    private static final String NIANHUAWAN_ADDRESS = "江苏省无锡市滨湖区马山环山西路 拈花湾禅意小镇内";
    private static final String REGION = "无锡";

    private final ScenicSpotRepository spotRepository;

    @Override
    public void run(String... args) {
        if (!spotRepository.findByNameContainingIgnoreCase("灵山大佛").isEmpty()) {
            return;
        }

        long oldCount = spotRepository.count();
        if (oldCount > 0) {
            spotRepository.deleteAllInBatch();
            log.info("已清除旧景点数据 {} 条", oldCount);
        }

        List<ScenicSpot> spots = List.of(
                lingshan("灵山大照壁", "门户地标", 31.0805, 120.0958,
                        "景区入口处长39.8米、高7米的青石照壁，被誉为“华夏第一壁”。正面是赵朴初亲题的鎏金“灵山胜境”四字，北立面刻其诗作《小灵山》，将无锡小灵山与印度灵鹫山相媲美，奠定整个景区的佛教文化基调。照壁与太湖碧波相映，是入园第一处打卡点。"),
                lingshan("五明桥", "佛教建筑", 31.0815, 120.0955,
                        "五座汉白玉石拱桥并列横跨香水海，桥栏雕刻莲花、飞天与神兽。五桥对应佛教五种智慧——声明、因明、内明、医方明、工巧明，漫步过桥寓意开启智慧、走向觉悟，是进入核心景区的必经之路。"),
                lingshan("佛足坛", "祈福圣迹", 31.0824, 120.0952,
                        "整块青铜铸造的巨型佛足印一对，每只长1.2米，足心刻有千辐轮相、宝瓶鱼纹等32种吉祥图案。相传佛祖涅槃前留双足印嘱托弟子“佛足所至，即为佛地”，被称为“两足尊”，是朝圣祈福的核心点位。"),
                lingshan("五智门", "佛教建筑", 31.0832, 120.0950,
                        "高16.8米、宽35米的五门六柱汉白玉石牌坊。五门象征五方五佛，六柱代表布施、持戒、忍辱、精进、禅定、般若“六度”，与灵山大佛同处一条中轴线。穿过此门即从凡俗之境踏入禅意圣地。"),
                lingshan("菩提大道", "自然景观", 31.0843, 120.0948,
                        "长约250米的朝圣步道，两侧对称种植近百棵从印度引进的菩提树，枝叶交错形成天然禅意拱廊。春季菩提花开、秋季落叶铺路，微风过处叶响如佛音，是全景区最具禅意的步道。"),
                lingshan("九龙灌浴", "演艺景观", 31.0855, 120.0946,
                        "总高27.5米的大型音乐动态群雕，再现释迦牟尼诞生时“九龙吐水”的祥瑞场景。音乐响起时莲花缓缓绽放，7.2米高的鎏金太子佛自莲中升起自转一周，九龙同时喷水沐浴。表演每日10:00、11:30、13:30、15:00，结束后可接取祈福“圣水”。"),
                lingshan("降魔浮雕", "佛教艺术", 31.0862, 120.0944,
                        "长26米、高4.6米的巨型花岗岩浮雕，生动再现佛陀在菩提树下战胜魔王波旬诱惑与威胁、觉悟成佛的历程。高浮雕与浅浮雕结合，人物发丝衣纹清晰可辨，是佛教石刻艺术珍品。"),
                lingshan("阿育王柱", "文化地标", 31.0868, 120.0943,
                        "通高16.9米、重180吨，由整块花岗岩一次雕成。柱头四头狮子朝向四方，象征佛法向世界传播。阿育王是古印度弘扬佛法最著名的君主，此柱是佛教东传历史的重要象征。"),
                lingshan("百子戏弥勒", "祈福体验", 31.0873, 120.0942,
                        "高3米、宽7.8米的青铜群雕：弥勒佛袒胸卧姿笑容可掬，身上百名孩童嬉戏攀爬、形态各异。寓意多子多福、家庭和睦，“摸弥勒肚皮，享一生福气”，是最受亲子家庭欢迎的景观。"),
                lingshan("祥符禅寺", "历史古迹", 31.0880, 120.0940,
                        "始建于唐贞观年间的千年古刹，玄奘弟子窥基在此开坛讲经，北宋赐额“祥符禅寺”。寺内千年银杏、被茶圣陆羽品鉴过的六角古井见证千年兴衰；钟楼悬挂12.8吨“江南第一钟”，撞钟祈福寓意烦恼尽除、福慧增长。"),
                lingshan("灵山大佛", "文化地标", 31.0899, 120.0937,
                        "通高88米（含台基101.5米）的世界最高露天青铜释迦牟尼立像，用铜725吨。右手施无畏印除众生痛苦，左手施与愿印赐众生欢乐；216级登云道暗合108烦恼与108愿望。登顶可抱佛脚祈福，俯瞰太湖三万顷碧波，夕阳时分“佛光普照”最为壮观。"),
                lingshan("佛教文化博览馆", "文化场馆", 31.0897, 120.0936,
                        "设于灵山大佛座基内的三层展馆，总面积一万平方米：一层展示五方五佛与四大名山文化，二层梳理世界佛教发展史并设“佛法东传”互动区，三层万佛殿内9999尊小佛像与室外大佛构成“万佛朝宗”。免费参观，馆内提供定时讲解。"),
                lingshan("灵山梵宫", "佛教艺术", 31.0868, 120.0912,
                        "建筑面积7.2万平方米，被誉为“东方卢浮宫”，世界佛教论坛永久会址，荣获鲁班奖。内藏100公斤纯金绘制的星空穹顶天象图、160块琉璃熔铸的《华藏世界》、金丝楠木东阳木雕群等艺术瑰宝。《吉祥颂》演出每日10:35、11:30、14:00、16:00。"),
                lingshan("五印坛城", "佛教建筑", 31.0857, 120.0905,
                        "香水海中的藏式坛城，总高约30米，白墙红边金顶，有“小布达拉宫”之称。内藏1500平方米手绘曼荼罗壁画与尼泊尔工匠鎏金雕刻的五方五佛，转经廊设108个转经筒，“转经一圈，福慧双增”。"),
                lingshan("曼飞龙塔", "佛教建筑", 31.0864, 120.0898,
                        "复刻云南西双版纳曼飞龙白塔，主塔高16.9米，八座小塔环绕如“众星拱月”。塔身浅浮雕刻佛成道图与傣族纹样，与梵宫（汉传）、坛城（藏传）共同构成佛教三大语系建筑群落，夜间亮灯后尤为出片。"),
                lingshan("无尽意斋", "人文纪念", 31.0888, 120.0925,
                        "复刻赵朴初先生北京故居的四合院纪念馆，“无尽意”取自《无尽意菩萨经》。馆内陈列其生平事迹、与灵山的往来书信及数十幅书法真迹；东西厢房设禅意茶室，可免费品鉴灵山禅茶，是山林间闹中取静的人文角落。"),
                lingshan("佛手广场", "祈福体验", 31.0838, 120.0946,
                        "广场上的“天下第一掌”为灵山大佛右手1:1复制，高11.7米、宽5.5米。右手施无畏印意为除却众生痛苦，“摸佛掌，沾福气”，与“抱佛脚”并称灵山两大祈福体验。"),

                nianhuawan("拈花广场", "禅意街区", 31.0705, 120.0778,
                        "拈花湾禅意小镇的门户，中央矗立12米高的“拈花微笑”鎏金雕塑，源自“迦叶拈花、佛陀微笑”的禅宗公案。每日9:30有开园仪式，是进入小镇的第一站。"),
                nianhuawan("梵天花海", "自然景观", 31.0700, 120.0765,
                        "占地约3万平方米的四季花海：春季格桑花、波斯菊竞放，夏季硫华菊艳丽，秋季波斯菊配金黄芦苇。1500米木质步道贯穿花田，中央凉亭可俯瞰整片花海，寓意“一花一世界，一叶一菩提”。"),
                nianhuawan("香月花街", "禅意街区", 31.0697, 120.0782,
                        "贯穿小镇南北的800米禅意街巷，白墙黛瓦、飞檐灯笼，融合中式禅意与日式町屋风格。街边有禅意文创、非遗手作、素面禅茶等特色商铺，无叫卖喧嚣。傍晚灯笼点亮后氛围最佳。"),
                nianhuawan("拈花堂", "禅意体验", 31.0694, 120.0785,
                        "藏在绿植中的中式禅堂，设禅坐区、抄经区与禅茶区，静坐、抄经、禅茶品鉴均免费。每日10:30、15:30有禅意讲座，是小镇里最适合静心的地方，入内需保持安静。"),
                nianhuawan("五灯湖", "自然景观", 31.0688, 120.0787,
                        "小镇最大水景，湖面约5000平方米，“五灯”呼应灵山五智文化。白天可漫步木栈道赏荷观柳，夜间19:00、20:00两场《禅行》灯光秀：灯光、水雾与经文投影在湖面交融，是小镇夜间人气最高的点位。"),
                nianhuawan("鹿鸣谷", "自然景观", 31.0706, 120.0760,
                        "小镇西侧山林间最静谧的自然区域，植被覆盖率90%以上，香樟松柏成荫。木质步道穿行林间，空气清新，适合晨间漫步与森林冥想，远离主客流。")
        );

        spotRepository.saveAll(spots);
        log.info("灵山胜境景点数据初始化完成，共 {} 条", spots.size());
    }

    private ScenicSpot lingshan(String name, String category, double lat, double lng, String description) {
        return spot(name, category, lat, lng, description, LINGSHAN_ADDRESS);
    }

    private ScenicSpot nianhuawan(String name, String category, double lat, double lng, String description) {
        return spot(name, category, lat, lng, description, NIANHUAWAN_ADDRESS);
    }

    private ScenicSpot spot(String name, String category, double lat, double lng,
                            String description, String address) {
        ScenicSpot s = new ScenicSpot();
        s.setName(name);
        s.setAddress(address);
        s.setDescription(description);
        s.setLatitude(lat);
        s.setLongitude(lng);
        s.setRegion(REGION);
        s.setCategory(category);
        return s;
    }
}
