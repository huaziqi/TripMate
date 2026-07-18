package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.route.*;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.HistoryRecord;
import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.entity.SpotFavorite;
import com.LHZ.TripMate.repository.*;
import com.LHZ.TripMate.service.RouteRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 灵山胜境个性化路线推荐。
 *
 * 游客画像 = 显式问卷 + 收藏信号 + 浏览/搜索历史 + 数字人对话关键词 + 季节上下文，
 * 合成 8 维兴趣向量后与各路线的兴趣权重做匹配打分，
 * 并为路线中每个景点挑选贴合游客兴趣的讲解重点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRecommendServiceImpl implements RouteRecommendService {

    private final ScenicSpotRepository scenicSpotRepository;
    private final SpotFavoriteRepository spotFavoriteRepository;
    private final HistoryRecordRepository historyRecordRepository;
    private final GuideSessionRepository guideSessionRepository;
    private final GuideMessageRepository guideMessageRepository;

    // ---------------- 兴趣维度 ----------------

    private static final String HISTORY = "history";
    private static final String BUDDHIST_ART = "buddhist_art";
    private static final String NATURE = "nature";
    private static final String ARCHITECTURE = "architecture";
    private static final String BLESSING = "blessing";
    private static final String FAMILY = "family";
    private static final String PHOTOGRAPHY = "photography";
    private static final String ZEN = "zen";

    private static final Map<String, String> DIM_LABELS = new LinkedHashMap<>() {{
        put(HISTORY, "历史文化");
        put(BUDDHIST_ART, "佛教艺术");
        put(NATURE, "自然风光");
        put(ARCHITECTURE, "建筑美学");
        put(BLESSING, "祈福体验");
        put(FAMILY, "亲子同游");
        put(PHOTOGRAPHY, "摄影打卡");
        put(ZEN, "禅修静心");
    }};

    /** 对话/搜索文本 -> 兴趣维度 的关键词词典 */
    private static final Map<String, List<String>> DIM_KEYWORDS = Map.of(
            HISTORY, List.of("历史", "典故", "由来", "古刹", "朝代", "玄奘", "唐代", "宋代", "渊源", "文物", "赵朴初", "千年"),
            BUDDHIST_ART, List.of("佛像", "壁画", "琉璃", "木雕", "艺术", "唐卡", "雕刻", "浮雕", "造像", "飞天", "梵宫"),
            NATURE, List.of("风景", "风光", "太湖", "花海", "银杏", "樱花", "湖", "山", "自然", "景色", "菩提树", "日落"),
            ARCHITECTURE, List.of("建筑", "牌坊", "宝塔", "宫殿", "风格", "藏式", "飞檐", "斗拱", "鲁班奖"),
            BLESSING, List.of("祈福", "许愿", "保佑", "圣水", "撞钟", "上香", "拜佛", "平安", "抱佛脚", "转经", "开光"),
            FAMILY, List.of("孩子", "小孩", "亲子", "儿童", "宝宝", "带娃", "一家", "老人", "全家"),
            PHOTOGRAPHY, List.of("拍照", "打卡", "拍摄", "摄影", "出片", "合影", "夜景", "取景", "机位"),
            ZEN, List.of("禅", "冥想", "静心", "安静", "抄经", "素斋", "禅茶", "慢生活", "放松", "拈花湾")
    );

    /** 景点名称片段 -> 兴趣维度权重，用于把收藏/浏览行为映射为画像信号 */
    private static final List<SpotDimHint> SPOT_DIM_HINTS = List.of(
            new SpotDimHint("照壁", Map.of(HISTORY, 0.5, PHOTOGRAPHY, 0.5)),
            new SpotDimHint("五明桥", Map.of(ARCHITECTURE, 0.4, HISTORY, 0.3)),
            new SpotDimHint("佛足坛", Map.of(BLESSING, 0.6, HISTORY, 0.3)),
            new SpotDimHint("五智门", Map.of(ARCHITECTURE, 0.6, HISTORY, 0.3)),
            new SpotDimHint("菩提大道", Map.of(NATURE, 0.7, ZEN, 0.4)),
            new SpotDimHint("九龙灌浴", Map.of(BLESSING, 0.5, FAMILY, 0.4, PHOTOGRAPHY, 0.3)),
            new SpotDimHint("降魔浮雕", Map.of(BUDDHIST_ART, 0.6, HISTORY, 0.4)),
            new SpotDimHint("阿育王柱", Map.of(HISTORY, 0.5, ARCHITECTURE, 0.4)),
            new SpotDimHint("百子戏弥勒", Map.of(FAMILY, 0.7, BLESSING, 0.4)),
            new SpotDimHint("祥符禅寺", Map.of(HISTORY, 0.7, BLESSING, 0.4, ZEN, 0.3)),
            new SpotDimHint("灵山大佛", Map.of(BLESSING, 0.5, PHOTOGRAPHY, 0.4, NATURE, 0.3)),
            new SpotDimHint("博览馆", Map.of(BUDDHIST_ART, 0.6, HISTORY, 0.4)),
            new SpotDimHint("梵宫", Map.of(BUDDHIST_ART, 0.7, ARCHITECTURE, 0.6)),
            new SpotDimHint("五印坛城", Map.of(BUDDHIST_ART, 0.6, ARCHITECTURE, 0.5, BLESSING, 0.3)),
            new SpotDimHint("曼飞龙塔", Map.of(ARCHITECTURE, 0.6, PHOTOGRAPHY, 0.4)),
            new SpotDimHint("无尽意斋", Map.of(ZEN, 0.5, HISTORY, 0.4)),
            new SpotDimHint("佛手广场", Map.of(BLESSING, 0.7, FAMILY, 0.3)),
            new SpotDimHint("拈花", Map.of(ZEN, 0.6, PHOTOGRAPHY, 0.4)),
            new SpotDimHint("花海", Map.of(NATURE, 0.8, PHOTOGRAPHY, 0.5)),
            new SpotDimHint("香月花街", Map.of(ZEN, 0.5, PHOTOGRAPHY, 0.4)),
            new SpotDimHint("五灯湖", Map.of(NATURE, 0.5, PHOTOGRAPHY, 0.5, ZEN, 0.4)),
            new SpotDimHint("鹿鸣谷", Map.of(NATURE, 0.8, ZEN, 0.5))
    );

    private record SpotDimHint(String nameFragment, Map<String, Double> dims) {}

    // ---------------- 路线模板 ----------------

    private enum StaminaLevel { LOW, MEDIUM, HIGH }

    private record SpotTemplate(String displayName, List<String> keywords,
                                String defaultFocus, Map<String, String> focusByInterest) {}

    private record RouteTemplate(String id, String name, String theme, String description,
                                 String estimatedTime, double durationHours, StaminaLevel stamina,
                                 String guideText, String suitableFor,
                                 Map<String, Double> weights, List<SpotTemplate> spots) {}

    private static final List<RouteTemplate> ROUTES = buildRoutes();

    private static List<RouteTemplate> buildRoutes() {
        List<RouteTemplate> routes = new ArrayList<>();

        routes.add(new RouteTemplate(
                "history-deep", "历史文化深度线", "历史文化",
                "从华夏第一壁出发，沿景区中轴线穿越千年：唐代古刹祥符禅寺、88米灵山大佛、东方卢浮宫灵山梵宫，一线读懂小灵山1300年佛教文脉。",
                "约6小时", 6, StaminaLevel.HIGH,
                "历史文化深度线以玄奘命名小灵山的典故开篇，经大照壁、五智门进入核心区，在祥符禅寺追溯千年兴衰，登216级登云道瞻仰灵山大佛，最后深入灵山梵宫与五印坛城，感受佛教艺术与历史的交融。",
                "适合对历史典故、佛教文化感兴趣、时间充裕的深度游客",
                Map.of(HISTORY, 1.0, BUDDHIST_ART, 0.7, ARCHITECTURE, 0.5, BLESSING, 0.3, ZEN, 0.2),
                List.of(
                        new SpotTemplate("灵山大照壁", List.of("灵山大照壁", "大照壁", "照壁"),
                                "长39.8米的“华夏第一壁”，进入灵山的第一帧画面。",
                                Map.of(
                                        HISTORY, "细读赵朴初亲题的鎏金“灵山胜境”与背面《小灵山》诗刻：诗中将无锡小灵山与印度灵鹫山相媲美，奠定了整个景区的佛教文化基调。",
                                        PHOTOGRAPHY, "照壁与太湖碧波同框构成“湖光壁影”，是入园第一处打卡点，如愿火车站装置是小众取景框。",
                                        BUDDHIST_ART, "青石深浮雕拼块贴面，中央是以“灵山胜境”为主题的大型浮雕，雕刻细节值得驻足细品。")),
                        new SpotTemplate("五明桥", List.of("五明桥"),
                                "五座汉白玉石拱桥横跨香水海，走过即寓意开启智慧。",
                                Map.of(
                                        HISTORY, "五桥对应佛教五明——声明、因明、内明、医方明、工巧明，佛教认为人类文明的核心尽在其中，过桥即是开启智慧之路。",
                                        ARCHITECTURE, "汉白玉桥栏雕刻莲花、飞天、神兽，五桥倒映香水海如五条玉带，是水与建筑禅意融合的范本。")),
                        new SpotTemplate("五智门", List.of("五智门"),
                                "高16.8米的五门六柱汉白玉牌坊，凡俗与圣境的分界。",
                                Map.of(
                                        HISTORY, "五门象征五方五佛，六柱代表布施、持戒、忍辱、精进、禅定、般若“六度”，门柱经文与后方灵山大佛同处一条中轴线。",
                                        ARCHITECTURE, "五门六柱石牌坊气势恢宏，门楣飞天神兽雕刻栩栩如生，是中轴线景观序列的重要一环。")),
                        new SpotTemplate("佛手广场", List.of("佛手广场", "天下第一掌"),
                                "“天下第一掌”复制自灵山大佛右手，高11.7米。",
                                Map.of(
                                        HISTORY, "佛掌为大佛右手1:1复制，右手施无畏印意为除却众生痛苦，可近距离理解大佛手印的佛教含义。",
                                        BLESSING, "“摸佛掌，沾福气”，与“抱佛脚”并称灵山两大祈福体验。")),
                        new SpotTemplate("祥符禅寺", List.of("祥符禅寺"),
                                "始建于唐贞观年间的千年古刹，小灵山佛教文化的发源地。",
                                Map.of(
                                        HISTORY, "玄奘弟子窥基在此开坛讲经，北宋大中祥符年间宋真宗赐额“祥符禅寺”。千年银杏、被陆羽品鉴过的六角古井，都是千年兴衰的见证。",
                                        BLESSING, "参与撞钟祈福，聆听12.8吨“江南第一钟”，钟声寓意烦恼尽除、福慧增长。",
                                        ZEN, "红墙黛瓦的仿唐建筑群里香火绵延，秋季银杏铺金，是整条中轴线上最静谧庄严的一段。")),
                        new SpotTemplate("灵山大佛", List.of("灵山大佛"),
                                "88米世界最高露天青铜释迦牟尼立像，灵山胜境的核心地标。",
                                Map.of(
                                        HISTORY, "大佛源自赵朴初“五方五佛”之论——与天坛大佛、乐山大佛、云冈大佛、龙门大佛共成五方格局；725吨青铜、35公里焊缝，是传统造像与现代工程的结合。",
                                        BLESSING, "登216级登云道，前108级寓意“烦恼尽除”，后108级寓意“愿望圆满”，登顶可抱佛脚祈福。",
                                        PHOTOGRAPHY, "夕阳西下时金色阳光洒在大佛身上，“佛光普照”与太湖波光交相辉映，是全景区最经典的机位。",
                                        NATURE, "大佛背靠小灵山、面朝太湖三万顷，登顶平台可俯瞰马山半岛全景。")),
                        new SpotTemplate("佛教文化博览馆", List.of("佛教文化博览馆", "博览馆"),
                                "大佛座基内的三层文化展馆，万佛殿藏9999尊小佛像。",
                                Map.of(
                                        HISTORY, "二层以时间为轴梳理世界佛教发展史，“佛法东传”互动区可触屏了解佛教入华的关键节点。",
                                        BUDDHIST_ART, "三层万佛殿9999尊1:100复刻小佛像与室外大佛构成“万佛朝宗”，鎏金装饰与暖光交映，视觉震撼。")),
                        new SpotTemplate("灵山梵宫", List.of("灵山梵宫", "梵宫"),
                                "被誉为“东方卢浮宫”的佛教艺术殿堂，鲁班奖建筑。",
                                Map.of(
                                        HISTORY, "第二、四届世界佛教论坛永久会址，是当代佛教文化交流的最高舞台。",
                                        BUDDHIST_ART, "100公斤纯金绘制的星空穹顶天象图、160块琉璃熔铸的《华藏世界》、金丝楠木东阳木雕群——每一件都是非遗级艺术瑰宝。",
                                        ARCHITECTURE, "7.2万平方米“莲花环抱”之势，五座莲花圣塔象征五方五佛，融合华藏塔风格与石窟艺术，荣获鲁班奖。")),
                        new SpotTemplate("五印坛城", List.of("五印坛城", "坛城"),
                                "香水海中的藏式坛城，有“小布达拉宫”之称。",
                                Map.of(
                                        HISTORY, "“五印”即五方五佛的五种手印，坛城与梵宫、曼飞龙塔构成汉传、藏传、南传三大语系建筑群落，见证佛教文化交融。",
                                        BUDDHIST_ART, "1500平方米纯手工绘制的三重曼荼罗壁画、尼泊尔工匠鎏金雕刻的五方五佛造像，是藏传佛教艺术精髓。",
                                        BLESSING, "顺时针转动转经廊里的108个转经筒，寓意“转经一圈，福慧双增”。"))
                )));

        routes.add(new RouteTemplate(
                "nature-panorama", "自然风光全景线", "自然风光",
                "菩提大道的林荫、香水海的碧波、登顶大佛俯瞰的太湖三万顷——把灵山“三山环抱、面朝太湖”的湖光山色一次看全。",
                "约5小时", 5, StaminaLevel.MEDIUM,
                "自然风光全景线从佛足坛出发，看一场九龙灌浴音乐喷泉，漫步菩提大道林荫拱廊，登灵山大佛俯瞰太湖与马山半岛，再沿香水海赏曼飞龙塔倒影，最后在无尽意斋庭院小憩，感受“前有照、后有靠、左右有抱”的山水格局。",
                "适合喜欢自然风光、湖景山色与户外漫步的游客",
                Map.of(NATURE, 1.0, PHOTOGRAPHY, 0.7, ZEN, 0.45, BLESSING, 0.25),
                List.of(
                        new SpotTemplate("佛足坛", List.of("佛足坛"),
                                "青铜铸造的佛祖双足印，菩提大道的起点。",
                                Map.of(
                                        NATURE, "佛足坛地处中轴线起点，从这里北望，五智门、菩提大道与灵山大佛层层递进，是理解灵山山水格局的第一站。",
                                        BLESSING, "触摸足心32种吉祥纹相祈福，相传“佛足所至，即为佛地”。")),
                        new SpotTemplate("九龙灌浴", List.of("九龙灌浴"),
                                "总高27.5米的大型音乐动态群雕，“花开见佛”的祥瑞现场。",
                                Map.of(
                                        NATURE, "九龙喷涌的水柱高达数十米，阳光好时水幕会折射出七彩“佛光”，是水景与阳光的最佳合作。",
                                        PHOTOGRAPHY, "占据顺光位拍摄莲花绽放、太子佛升起的瞬间；表演每日10:00、11:30、13:30、15:00。",
                                        BLESSING, "表演结束后在广场两侧接取龙头流出的“圣水”，寓意吉祥安康。")),
                        new SpotTemplate("菩提大道", List.of("菩提大道"),
                                "250米林荫步道，两侧是从印度引进的正宗菩提树。",
                                Map.of(
                                        NATURE, "近百棵菩提树交错成天然拱廊，风过叶响如佛音萦绕；春季菩提花开，秋季落叶铺路，四季景致各异。",
                                        ZEN, "放慢脚步听菩提叶沙沙作响，捡一片落叶做书签，为登大佛前静一静心。")),
                        new SpotTemplate("灵山大佛", List.of("灵山大佛"),
                                "88米青铜大佛，登顶俯瞰太湖全景的最佳位置。",
                                Map.of(
                                        NATURE, "大佛选址暗合“前有照（太湖）、后有靠（灵山）、左右有抱（青龙白虎二山）”的风水格局，登顶平台是俯瞰太湖三万顷的绝佳视角。",
                                        PHOTOGRAPHY, "推荐日落时分登顶：金光洒佛身、太湖泛波光，是灵山最出片的黄金一小时。",
                                        BLESSING, "登云道216级台阶走完，抱一抱佛脚，寓意平安如意。")),
                        new SpotTemplate("曼飞龙塔", List.of("曼飞龙塔"),
                                "复刻西双版纳曼飞龙白塔的九塔组合，异域风情地标。",
                                Map.of(
                                        NATURE, "白塔金刹与香水海的绿水、岸边绿树相映成趣，园林式布局让建筑完全融进山水。",
                                        PHOTOGRAPHY, "以香水海与五印坛城为背景拍九塔全景；夜间亮灯后轮廓分明，夜景同样出片。",
                                        ARCHITECTURE, "主塔16.9米、八座小塔环绕如“众星拱月”，是南传佛教干栏式建筑美学的代表。")),
                        new SpotTemplate("无尽意斋", List.of("无尽意斋"),
                                "复刻赵朴初北京故居的四合院，山林间的静谧庭院。",
                                Map.of(
                                        NATURE, "天井里海棠、石榴、翠竹婆娑，坐在石凳上听风吹竹叶，是全景区最安静的角落。",
                                        ZEN, "在禅意茶室免费品一盏灵山禅茶，为全天行程收一个安静的尾。",
                                        HISTORY, "“无尽意”取自《无尽意菩萨经》，馆内书信、手稿讲述赵朴初与灵山的渊源。"))
                )));

        routes.add(new RouteTemplate(
                "family-fun", "亲子家庭欢乐线", "亲子同游",
                "看九龙灌浴喷泉、摸天下第一掌、找百子戏弥勒里最调皮的孩童——4小时轻松路线，让孩子在互动里爱上传统文化。",
                "约4小时", 4, StaminaLevel.LOW,
                "亲子家庭欢乐线全程平缓无爬坡：先看九龙灌浴动态表演听佛陀诞生的故事，再到佛手广场摸“天下第一掌”，在百子戏弥勒找一百个形态各异的小孩，最后进梵宫看星空穹顶与七彩琉璃，用孩子听得懂的方式讲传统文化。",
                "适合带小朋友出行的家庭，全程轻松无台阶压力",
                Map.of(FAMILY, 1.0, BLESSING, 0.5, PHOTOGRAPHY, 0.35, BUDDHIST_ART, 0.25),
                List.of(
                        new SpotTemplate("九龙灌浴", List.of("九龙灌浴"),
                                "九条飞龙同时喷水为太子佛沐浴的大型动态表演。",
                                Map.of(
                                        FAMILY, "用故事讲给孩子听：小王子一出生，九条龙飞来喷水给他洗澡——“花开见佛”的机关式表演孩子看得目不转睛。",
                                        BLESSING, "带孩子接一杯“圣水”，许一个健康成长的小心愿。",
                                        PHOTOGRAPHY, "提前10分钟占据前排，拍下莲花绽放、佛像升起的连续瞬间。")),
                        new SpotTemplate("佛手广场", List.of("佛手广场", "天下第一掌"),
                                "11.7米高的“天下第一掌”，可以近距离触摸。",
                                Map.of(
                                        FAMILY, "让孩子把小手贴在巨大的佛掌上比大小，直观感受88米大佛“一只手”的尺度。",
                                        BLESSING, "“摸佛掌，沾福气”，全家依次摸掌祈平安。")),
                        new SpotTemplate("百子戏弥勒", List.of("百子戏弥勒"),
                                "弥勒佛身上爬满一百个嬉戏孩童的青铜群雕。",
                                Map.of(
                                        FAMILY, "和孩子玩“找不同”：一百个小孩有的挠痒痒、有的爬肩膀，找出最调皮的那一个；这是全景区最受小朋友欢迎的雕塑。",
                                        BLESSING, "摸摸弥勒大肚皮，寓意“大肚能容、笑口常开”，寄托家庭和睦的心愿。",
                                        PHOTOGRAPHY, "让孩子模仿雕塑里孩童的姿势合影，天然的亲子大片。")),
                        new SpotTemplate("佛教文化博览馆", List.of("佛教文化博览馆", "博览馆"),
                                "大佛座基内的展馆，有互动屏和沉浸式投影。",
                                Map.of(
                                        FAMILY, "二层“佛法东传”触屏问答和沉浸式投影是孩子的最爱，边玩边了解四大名山与佛教故事。",
                                        BUDDHIST_ART, "三层万佛殿9999尊小金佛整齐排布，孩子第一眼就会“哇”出声。")),
                        new SpotTemplate("灵山梵宫", List.of("灵山梵宫", "梵宫"),
                                "有星空穹顶和七彩琉璃的“艺术宫殿”。",
                                Map.of(
                                        FAMILY, "指给孩子看28米高的星空穹顶如何模拟日月星辰流转，《吉祥颂》演出的全息投影和水雾特效孩子看得入迷。",
                                        BUDDHIST_ART, "用色彩讲艺术：让孩子数一数《华藏世界》琉璃壁画里有多少种颜色。",
                                        PHOTOGRAPHY, "廊厅油画与星空穹顶下随手一拍都是大片。"))
                )));

        routes.add(new RouteTemplate(
                "art-palace", "佛教艺术殿堂线", "佛教艺术",
                "一条线看全汉传、藏传、南传三大语系建筑：梵宫的琉璃与木雕、坛城的曼荼罗壁画、白塔的傣族雕刻，佛教艺术爱好者的朝圣之路。",
                "约4.5小时", 4.5, StaminaLevel.MEDIUM,
                "佛教艺术殿堂线聚焦灵山的艺术瑰宝：从降魔浮雕的花岗岩群像、阿育王柱的四狮柱头开始，重头戏是“东方卢浮宫”灵山梵宫的穹顶天象图与《华藏世界》琉璃巨制，再对比五印坛城的藏式曼荼罗与曼飞龙塔的南传雕刻，最后在博览馆万佛殿收尾。",
                "适合佛教艺术、建筑与雕刻爱好者慢慢品味",
                Map.of(BUDDHIST_ART, 1.0, ARCHITECTURE, 0.85, HISTORY, 0.5, PHOTOGRAPHY, 0.4),
                List.of(
                        new SpotTemplate("降魔浮雕", List.of("降魔浮雕"),
                                "长26米的巨型花岗岩浮雕，再现佛陀降魔成道。",
                                Map.of(
                                        BUDDHIST_ART, "高浮雕与浅浮雕结合的分层刻画：佛陀目光如炬，魔女妖娆、魔兵狰狞，连发丝衣纹都清晰可辨，是佛教石刻中的珍品。",
                                        HISTORY, "画面讲述佛陀在菩提树下战胜魔王波旬的“八相成道”关键一幕，读懂它就读懂了佛传故事的高潮。")),
                        new SpotTemplate("阿育王柱", List.of("阿育王柱"),
                                "16.9米整石雕成的石柱，柱头四狮朝向四方。",
                                Map.of(
                                        BUDDHIST_ART, "四头狮子形态各异、威而不怒，梵文经文历经风雨依然清晰，是整石雕刻的工艺极限。",
                                        HISTORY, "阿育王放下屠刀弘扬佛法，将佛教传向世界——这根柱子是佛法东传的历史象征。",
                                        ARCHITECTURE, "180吨整块花岗岩一次雕成，与大佛、五智门构成中轴线三大石作。")),
                        new SpotTemplate("灵山梵宫", List.of("灵山梵宫", "梵宫"),
                                "汇集东阳木雕、琉璃、油画、景泰蓝的艺术殿堂。",
                                Map.of(
                                        BUDDHIST_ART, "必看三件：100公斤纯金绘制、148尊飞天的穹顶天象图；2000吨琉璃熔铸的《华藏世界》（翡翠菩提叶脉络在特定角度显现般若经文）；金丝楠木东阳木雕群。",
                                        ARCHITECTURE, "外观融合华藏塔风格与石窟元素，圣坛藏全球唯一大型旋转舞台，建筑本身就是展品。",
                                        PHOTOGRAPHY, "星空穹顶下的广角仰拍、廊厅12幅巨型油画的对称构图，都是经典机位（禁闪光灯）。")),
                        new SpotTemplate("五印坛城", List.of("五印坛城", "坛城"),
                                "藏式碉楼建筑，内藏1500平方米手绘曼荼罗壁画。",
                                Map.of(
                                        BUDDHIST_ART, "中央、金刚界、胎藏界三重曼荼罗壁画全部以天然矿物颜料手工绘制；主殿五方五佛由尼泊尔工匠鎏金雕刻。",
                                        ARCHITECTURE, "以布达拉宫雪村大门为原型的山门、白墙红边金顶的碉楼形制，与江南水景形成强烈反差。",
                                        PHOTOGRAPHY, "登五层观景台俯拍香水海、梵宫与大佛同框的全景。")),
                        new SpotTemplate("曼飞龙塔", List.of("曼飞龙塔"),
                                "复刻西双版纳白塔的九塔组合，南传佛教代表作。",
                                Map.of(
                                        BUDDHIST_ART, "塔身浅浮雕刻有佛成道图、阿罗汉像与傣族卷草纹，是傣族雕刻艺术在江南的唯一样本。",
                                        ARCHITECTURE, "主塔如葫芦收窄、八小塔众星拱月——对比刚看过的汉传梵宫与藏传坛城，三大语系建筑语言的差异一目了然。")),
                        new SpotTemplate("佛教文化博览馆", List.of("佛教文化博览馆", "博览馆"),
                                "三层展馆：五方五佛、佛教发展史与万佛殿。",
                                Map.of(
                                        BUDDHIST_ART, "压轴的万佛殿：9999尊1:100复刻小佛像布满四周与穹顶，与室外大佛“万佛朝宗”，为艺术之旅收官。",
                                        HISTORY, "一层的佛像复刻品与历代法器，把前面看过的艺术品放回历史脉络里。"))
                )));

        routes.add(new RouteTemplate(
                "blessing-walk", "祈福纳福体验线", "祈福体验",
                "接圣水、摸佛掌、摸弥勒肚皮、撞禅钟、抱佛脚——把灵山最灵验的五大祈福仪式一次做全，为家人求一份平安。",
                "约3.5小时", 3.5, StaminaLevel.MEDIUM,
                "祈福纳福体验线串起灵山所有祈福节点：佛足坛触摸吉祥纹相开场，九龙灌浴接“圣水”，佛手广场摸“天下第一掌”，百子戏弥勒摸肚皮求阖家欢喜，祥符禅寺撞响江南第一钟，最后登云道登顶抱佛脚，愿望圆满收官。",
                "适合想体验祈福文化、为家人祈平安的游客",
                Map.of(BLESSING, 1.0, ZEN, 0.4, FAMILY, 0.35, HISTORY, 0.25),
                List.of(
                        new SpotTemplate("佛足坛", List.of("佛足坛"),
                                "佛祖真身足印复刻，“两足尊”祈福首站。",
                                Map.of(
                                        BLESSING, "亲手触摸足心的千辐轮相与宝瓶鱼纹等32种吉祥图案，相传“佛足所至，佛光普照”。",
                                        HISTORY, "佛祖涅槃前留双足印嘱托弟子“佛足所至，即为佛地”，这是佛教千百年供奉的圣迹。")),
                        new SpotTemplate("九龙灌浴", List.of("九龙灌浴"),
                                "“花开见佛”动态表演，散场后可接祈福圣水。",
                                Map.of(
                                        BLESSING, "表演结束后到广场两侧龙头下接一杯“圣水”，寓意沾取佛诞祥瑞、吉祥安康——记得自备小瓶。",
                                        FAMILY, "带着家人一起看太子佛在音乐中升起，一起接圣水，仪式感满满。")),
                        new SpotTemplate("佛手广场", List.of("佛手广场", "天下第一掌"),
                                "摸“天下第一掌”，沾福气保平安。",
                                Map.of(
                                        BLESSING, "施无畏印的右手意为“除却众生痛苦”，摸掌祈福是灵山流传最广的仪式之一。")),
                        new SpotTemplate("百子戏弥勒", List.of("百子戏弥勒"),
                                "摸弥勒肚皮，求阖家欢喜、多子多福。",
                                Map.of(
                                        BLESSING, "“摸弥勒肚皮，享一生福气”——弥勒是未来佛，百子环绕寓意家庭和睦、子孙满堂。",
                                        FAMILY, "全家轮流摸肚皮合影，是最有烟火气的祈福瞬间。")),
                        new SpotTemplate("祥符禅寺", List.of("祥符禅寺"),
                                "千年古刹撞钟祈福，钟声响彻太湖之滨。",
                                Map.of(
                                        BLESSING, "撞响12.8吨的“江南第一钟”，一声烦恼尽除、二声福慧增长；六角井传说为“八功德水”，可讨一口甘甜。",
                                        ZEN, "在千年银杏下静立片刻，听钟声余韵在山谷回荡。",
                                        HISTORY, "在千年祖庭里祈福，唐贞观年间延续至今的香火本身就是最大的“灵验”。")),
                        new SpotTemplate("灵山大佛", List.of("灵山大佛"),
                                "登216级登云道，抱佛脚愿望圆满。",
                                Map.of(
                                        BLESSING, "前108级“烦恼尽除”、后108级“愿望圆满”，登顶抱佛脚是整条祈福线的高潮；体力不便可乘景区观光车。",
                                        PHOTOGRAPHY, "抱佛脚平台仰拍大佛全身，广角镜头才能装下88米的庄严。"))
                )));

        routes.add(new RouteTemplate(
                "zen-slow", "拈花湾禅意慢生活线", "禅修静心",
                "到拈花湾把节奏调慢：香月花街喝一盏禅茶、拈花堂抄一页经、五灯湖看一场《禅行》灯光秀，在花海与灯影里治愈自己。",
                "约3小时", 3, StaminaLevel.LOW,
                "拈花湾禅意慢生活线远离主景区的人流：从拈花广场的“拈花微笑”雕塑开始，逛香月花街的禅意商铺与非遗手作，在拈花堂静坐抄经、免费品禅茶，傍晚到五灯湖看灯光倒映湖面，最后漫步梵天花海，四季花开各有其美。",
                "适合想放慢脚步、静心减压的游客，傍晚至夜间体验最佳",
                Map.of(ZEN, 1.0, PHOTOGRAPHY, 0.65, NATURE, 0.6, FAMILY, 0.2),
                List.of(
                        new SpotTemplate("拈花广场", List.of("拈花广场"),
                                "拈花湾门户，中央是12米高“拈花微笑”鎏金雕塑。",
                                Map.of(
                                        ZEN, "“迦叶拈花、佛陀微笑”——不立文字、以心传心的禅宗第一公案，就是小镇名字的由来。",
                                        PHOTOGRAPHY, "鎏金雕塑在阳光下熠熠生辉，是小镇第一张标准照。")),
                        new SpotTemplate("香月花街", List.of("香月花街"),
                                "800米禅意街巷，白墙黛瓦配灯笼，商铺无叫卖。",
                                Map.of(
                                        ZEN, "在非遗手作铺体验陶艺木刻，在禅茶铺喝一盏茶——这条街的规矩是“慢”，没有叫卖声打扰。",
                                        PHOTOGRAPHY, "傍晚6点灯笼点亮后，中式町屋与灯影是最出片的时段。",
                                        FAMILY, "带孩子做一次剪纸或陶艺，比买纪念品更有记忆点。")),
                        new SpotTemplate("拈花堂", List.of("拈花堂"),
                                "藏在绿植后的禅堂：禅坐、抄经、禅茶全部免费。",
                                Map.of(
                                        ZEN, "选一把禅椅静坐听禅乐，或抄一页经文带走；每日10:30、15:30有禅意讲座，是整条线的静心内核。")),
                        new SpotTemplate("五灯湖", List.of("五灯湖"),
                                "小镇最大水景，夜间《禅行》灯光秀的舞台。",
                                Map.of(
                                        ZEN, "“五灯”呼应灵山五智，湖水象征清净本心；坐在湖心亭看水面，心自然就静了。",
                                        PHOTOGRAPHY, "19:00、20:00两场《禅行》灯光秀：灯光、水雾与经文投影在湖面交融，提前30分钟占机位。",
                                        NATURE, "夏季荷花亭亭、岸边垂柳拂水，白天的五灯湖是江南水乡的样子。")),
                        new SpotTemplate("梵天花海", List.of("梵天花海", "花海"),
                                "3万平方米四季花海，木质步道贯穿其间。",
                                Map.of(
                                        NATURE, "春天格桑花与波斯菊铺满大地，秋天波斯菊配金黄芦苇——“一花一世界”在这里具象化。",
                                        PHOTOGRAPHY, "花海与远处禅意建筑同框、凉亭俯拍花田纹理，都是氛围感大片。",
                                        ZEN, "沿1500米木栈道慢慢走完，什么都不想，就是最好的冥想。"))
                )));

        return routes;
    }

    // ---------------- 对外接口 ----------------

    @Override
    public List<RecommendRouteDTO> defaultRoutes() {
        List<RecommendRouteDTO> result = new ArrayList<>();
        for (RouteTemplate route : ROUTES) {
            List<RouteSpotDTO> spots = route.spots().stream()
                    .map(this::matchSpotBasic)
                    .toList();
            result.add(new RecommendRouteDTO(route.id(), route.name(), route.theme(),
                    route.description(), route.estimatedTime(), route.guideText(), spots));
        }
        return result;
    }

    @Override
    public List<InterestOptionDTO> interestOptions() {
        return List.of(
                new InterestOptionDTO(HISTORY, "历史文化", "千年古刹、玄奘典故、佛教东传的历史脉络", "📜"),
                new InterestOptionDTO(BUDDHIST_ART, "佛教艺术", "琉璃壁画、东阳木雕、曼荼罗与石刻造像", "🎨"),
                new InterestOptionDTO(NATURE, "自然风光", "太湖碧波、菩提林荫、四季花海", "🏞️"),
                new InterestOptionDTO(ARCHITECTURE, "建筑美学", "梵宫、藏式坛城、南传白塔三大语系建筑", "🏛️"),
                new InterestOptionDTO(BLESSING, "祈福体验", "接圣水、撞钟、摸佛掌、抱佛脚", "🙏"),
                new InterestOptionDTO(FAMILY, "亲子同游", "动态表演、互动展馆，孩子也能玩得开心", "👨‍👩‍👧"),
                new InterestOptionDTO(PHOTOGRAPHY, "摄影打卡", "佛光夕照、星空穹顶、灯光秀机位", "📷"),
                new InterestOptionDTO(ZEN, "禅修静心", "抄经、禅茶、慢生活，给心放个假", "🧘")
        );
    }

    @Override
    public PersonalizedRecommendationDTO personalize(PersonalizeRequestDTO request, Long userId) {
        Map<String, Double> profile = new HashMap<>();
        List<String> summary = new ArrayList<>();

        collectExplicitSignals(request, profile, summary);
        collectBehaviorSignals(userId, profile, summary);

        boolean hasSignal = profile.values().stream().anyMatch(v -> v > 0);
        if (!hasSignal) {
            summary.add("暂未获取到你的兴趣信息，以下为通用推荐排序；填写兴趣问卷后推荐会更准。");
            DIM_LABELS.keySet().forEach(dim -> profile.put(dim, 1.0));
        }

        normalize(profile);

        String seasonNote = seasonNote();
        if (seasonNote != null) {
            summary.add(seasonNote);
        }

        List<PersonalizedRouteDTO> routes = new ArrayList<>();
        for (RouteTemplate route : ROUTES) {
            routes.add(scoreRoute(route, profile, request, hasSignal, seasonNote != null));
        }
        routes.sort(Comparator.comparingInt(PersonalizedRouteDTO::getMatchScore).reversed());

        PersonalizedRecommendationDTO dto = new PersonalizedRecommendationDTO();
        dto.setProfileSummary(summary);
        dto.setInterestWeights(toPercentWeights(profile));
        dto.setRoutes(routes);
        return dto;
    }

    // ---------------- 画像信号采集 ----------------

    /** 信号一 & 二：显式问卷选择 + 自由描述关键词 */
    private void collectExplicitSignals(PersonalizeRequestDTO request,
                                        Map<String, Double> profile, List<String> summary) {
        if (request == null) return;

        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (String key : request.getInterests()) {
                if (DIM_LABELS.containsKey(key)) {
                    profile.merge(key, 1.0, Double::sum);
                    labels.add(DIM_LABELS.get(key));
                }
            }
            if (!labels.isEmpty()) {
                summary.add("问卷选择：" + String.join("、", labels));
            }
        }

        if (request.getFreeText() != null && !request.getFreeText().isBlank()) {
            Set<String> hit = matchDims(request.getFreeText());
            if (!hit.isEmpty()) {
                hit.forEach(dim -> profile.merge(dim, 0.6, Double::sum));
                summary.add("从你的描述中识别到：" +
                        hit.stream().map(DIM_LABELS::get).reduce((a, b) -> a + "、" + b).orElse(""));
            }
        }
    }

    /** 信号三~五：收藏、浏览/搜索历史、数字人对话，全部容错处理 */
    private void collectBehaviorSignals(Long userId, Map<String, Double> profile, List<String> summary) {
        if (userId == null) return;

        try {
            collectFavoriteSignals(userId, profile, summary);
        } catch (Exception e) {
            log.warn("读取收藏画像失败 userId={}", userId, e);
        }
        try {
            collectHistorySignals(userId, profile, summary);
        } catch (Exception e) {
            log.warn("读取浏览历史画像失败 userId={}", userId, e);
        }
        try {
            collectChatSignals(userId, profile, summary);
        } catch (Exception e) {
            log.warn("读取对话画像失败 userId={}", userId, e);
        }
    }

    private void collectFavoriteSignals(Long userId, Map<String, Double> profile, List<String> summary) {
        List<SpotFavorite> favorites = spotFavoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (favorites.isEmpty()) return;

        Map<String, Double> gained = new HashMap<>();
        List<String> names = new ArrayList<>();
        for (SpotFavorite favorite : favorites) {
            Optional<ScenicSpot> spot = scenicSpotRepository.findById(favorite.getSpotId());
            if (spot.isEmpty()) continue;
            String name = spot.get().getName();
            Map<String, Double> dims = spotDims(name);
            if (dims.isEmpty()) continue;
            if (names.size() < 3) names.add(name);
            dims.forEach((dim, w) -> gained.merge(dim, w * 0.5, Double::sum));
        }
        if (gained.isEmpty()) return;

        // 收藏信号封顶，避免刷收藏淹没问卷
        gained.replaceAll((dim, v) -> Math.min(v, 1.2));
        gained.forEach((dim, v) -> profile.merge(dim, v, Double::sum));

        String top = topDimLabels(gained, 2);
        summary.add("收藏偏好：你收藏过「" + String.join("」「", names) + "」等景点，加强了 " + top + " 权重");
    }

    private void collectHistorySignals(Long userId, Map<String, Double> profile, List<String> summary) {
        List<HistoryRecord> records = historyRecordRepository.findTop50ByUserIdOrderByCreateTimeDesc(userId);
        if (records.isEmpty()) return;

        Map<String, Double> gained = new HashMap<>();
        int spotViews = 0;
        for (HistoryRecord record : records) {
            String type = record.getType() == null ? "" : record.getType();
            if (("VIEW_SPOT".equals(type) || "PLAY_AUDIO".equals(type)) && record.getTargetId() != null) {
                Optional<ScenicSpot> spot = scenicSpotRepository.findById(record.getTargetId());
                if (spot.isPresent()) {
                    spotViews++;
                    spotDims(spot.get().getName())
                            .forEach((dim, w) -> gained.merge(dim, w * 0.25, Double::sum));
                }
            } else if (record.getContent() != null &&
                    ("SEARCH_SPOT".equals(type) || "AI_CHAT".equals(type))) {
                matchDims(record.getContent())
                        .forEach(dim -> gained.merge(dim, 0.15, Double::sum));
            }
        }
        if (gained.isEmpty()) return;

        gained.replaceAll((dim, v) -> Math.min(v, 0.9));
        gained.forEach((dim, v) -> profile.merge(dim, v, Double::sum));
        summary.add("浏览足迹：最近浏览过 " + spotViews + " 次景点详情，偏向 " + topDimLabels(gained, 2));
    }

    private void collectChatSignals(Long userId, Map<String, Double> profile, List<String> summary) {
        List<GuideSession> sessions = guideSessionRepository.findByUserId(userId);
        if (sessions.isEmpty()) return;

        Map<String, Double> gained = new HashMap<>();
        Map<String, Integer> hitCount = new HashMap<>();
        for (GuideSession session : sessions) {
            List<GuideMessage> messages =
                    guideMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(session.getId());
            for (GuideMessage message : messages) {
                if (!"USER".equalsIgnoreCase(message.getRole())) continue;
                for (String dim : matchDims(message.getContent())) {
                    gained.merge(dim, 0.15, Double::sum);
                    hitCount.merge(dim, 1, Integer::sum);
                }
            }
        }
        if (gained.isEmpty()) return;

        gained.replaceAll((dim, v) -> Math.min(v, 0.6));
        gained.forEach((dim, v) -> profile.merge(dim, v, Double::sum));
        summary.add("对话洞察：你和数字人聊天时常问到 " + topDimLabels(gained, 2) +
                " 相关话题（共 " + hitCount.values().stream().mapToInt(Integer::intValue).sum() + " 次）");
    }

    // ---------------- 打分 ----------------

    private PersonalizedRouteDTO scoreRoute(RouteTemplate route, Map<String, Double> profile,
                                            PersonalizeRequestDTO request,
                                            boolean hasSignal, boolean seasonBoostActive) {
        double raw = 0;
        for (Map.Entry<String, Double> entry : route.weights().entrySet()) {
            raw += profile.getOrDefault(entry.getKey(), 0.0) * entry.getValue();
        }

        List<String> reasons = new ArrayList<>();
        if (hasSignal) {
            // 找出对得分贡献最大的两个维度作为解释
            route.weights().entrySet().stream()
                    .sorted((a, b) -> Double.compare(
                            profile.getOrDefault(b.getKey(), 0.0) * b.getValue(),
                            profile.getOrDefault(a.getKey(), 0.0) * a.getValue()))
                    .limit(2)
                    .filter(e -> profile.getOrDefault(e.getKey(), 0.0) * e.getValue() > 0.04)
                    .forEach(e -> reasons.add("你的「" + DIM_LABELS.get(e.getKey()) + "」偏好与本线契合"));
        }

        double multiplier = 1.0;

        String duration = request == null ? null : request.getDuration();
        double availableHours = switch (duration == null ? "" : duration) {
            case "half" -> 3;
            case "most" -> 5;
            case "full" -> 8;
            default -> -1;
        };
        if (availableHours > 0 && route.durationHours() > availableHours + 0.5) {
            multiplier *= 0.72;
            reasons.add("本线" + route.estimatedTime() + "，可能超出你的时间安排");
        }

        String companions = request == null ? null : request.getCompanions();
        if ("kids".equals(companions)) {
            if (route.weights().getOrDefault(FAMILY, 0.0) >= 0.5) {
                multiplier *= 1.18;
                reasons.add("互动多、无爬坡，适合带小朋友");
            } else if (route.stamina() == StaminaLevel.HIGH) {
                multiplier *= 0.88;
            }
        } else if ("elder".equals(companions)) {
            if (route.stamina() == StaminaLevel.LOW) {
                multiplier *= 1.15;
                reasons.add("全程平缓，适合带老人同游");
            } else if (route.stamina() == StaminaLevel.HIGH) {
                multiplier *= 0.72;
                reasons.add("含216级登云道，带老人建议乘观光车");
            }
        }

        String stamina = request == null ? null : request.getStamina();
        if ("low".equals(stamina)) {
            if (route.stamina() == StaminaLevel.HIGH) {
                multiplier *= 0.7;
                reasons.add("路程较长，体力要求偏高");
            } else if (route.stamina() == StaminaLevel.MEDIUM) {
                multiplier *= 0.92;
            }
        }

        if (seasonBoostActive && route.weights().getOrDefault(NATURE, 0.0) >= 0.6) {
            multiplier *= 1.08;
            reasons.add("当季花期/秋色正好，自然景观加分");
        }

        double finalScore = Math.clamp(raw * multiplier, 0.05, 0.99);

        PersonalizedRouteDTO dto = new PersonalizedRouteDTO();
        dto.setId(route.id());
        dto.setName(route.name());
        dto.setTheme(route.theme());
        dto.setDescription(route.description());
        dto.setEstimatedTime(route.estimatedTime());
        dto.setGuideText(route.guideText());
        dto.setSuitableFor(route.suitableFor());
        dto.setMatchScore((int) Math.round(finalScore * 100));
        dto.setMatchReasons(reasons);
        dto.setTags(route.weights().entrySet().stream()
                .filter(e -> e.getValue() >= 0.5)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> DIM_LABELS.get(e.getKey()))
                .toList());
        dto.setSpots(route.spots().stream()
                .map(spot -> matchSpotPersonalized(spot, profile))
                .toList());
        return dto;
    }

    // ---------------- 景点匹配与讲解重点 ----------------

    private RouteSpotDTO matchSpotBasic(SpotTemplate template) {
        ScenicSpot spot = findSpot(template);
        if (spot != null) {
            return new RouteSpotDTO(template.displayName(), spot.getId(), spot.getName(),
                    spot.getAddress(), spot.getLatitude(), spot.getLongitude(), true);
        }
        return new RouteSpotDTO(template.displayName(), null, template.displayName(),
                "数据库中暂未匹配到该景点", null, null, false);
    }

    private PersonalizedRouteSpotDTO matchSpotPersonalized(SpotTemplate template, Map<String, Double> profile) {
        ScenicSpot spot = findSpot(template);

        // 在该景点提供的讲解维度里，选游客画像权重最高的一个
        String bestDim = null;
        double bestWeight = 0;
        for (String dim : template.focusByInterest().keySet()) {
            double w = profile.getOrDefault(dim, 0.0);
            if (w > bestWeight) {
                bestWeight = w;
                bestDim = dim;
            }
        }

        String focusText = bestDim != null && bestWeight > 0.02
                ? template.focusByInterest().get(bestDim)
                : template.defaultFocus();
        String focusLabel = bestDim != null && bestWeight > 0.02 ? DIM_LABELS.get(bestDim) : null;

        if (spot != null) {
            return new PersonalizedRouteSpotDTO(template.displayName(), spot.getId(), spot.getName(),
                    spot.getAddress(), spot.getLatitude(), spot.getLongitude(), true, focusText, focusLabel);
        }
        return new PersonalizedRouteSpotDTO(template.displayName(), null, template.displayName(),
                "数据库中暂未匹配到该景点", null, null, false, focusText, focusLabel);
    }

    private ScenicSpot findSpot(SpotTemplate template) {
        for (String keyword : template.keywords()) {
            List<ScenicSpot> spots = scenicSpotRepository.findByNameContainingIgnoreCase(keyword);
            if (!spots.isEmpty()) {
                return spots.get(0);
            }
        }
        return null;
    }

    // ---------------- 工具方法 ----------------

    private Set<String> matchDims(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isBlank()) return result;
        DIM_KEYWORDS.forEach((dim, keywords) -> {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    result.add(dim);
                    return;
                }
            }
        });
        return result;
    }

    private Map<String, Double> spotDims(String spotName) {
        if (spotName == null) return Map.of();
        for (SpotDimHint hint : SPOT_DIM_HINTS) {
            if (spotName.contains(hint.nameFragment())) {
                return hint.dims();
            }
        }
        return Map.of();
    }

    private String topDimLabels(Map<String, Double> gained, int limit) {
        return gained.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> DIM_LABELS.get(e.getKey()))
                .reduce((a, b) -> a + " / " + b)
                .orElse("");
    }

    private void normalize(Map<String, Double> profile) {
        double sum = profile.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return;
        profile.replaceAll((dim, v) -> v / sum);
    }

    private Map<String, Integer> toPercentWeights(Map<String, Double> profile) {
        Map<String, Integer> result = new LinkedHashMap<>();
        DIM_LABELS.forEach((dim, label) -> {
            int percent = (int) Math.round(profile.getOrDefault(dim, 0.0) * 100);
            if (percent >= 1) {
                result.put(label, percent);
            }
        });
        return result;
    }

    /** 季节上下文：春(3-5月)花期、秋(9-11月)银杏，为自然风光路线加权 */
    private String seasonNote() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 3 && month <= 5) {
            return "季节提示：正值春季，樱花桃花与梵天花海花期正好，自然风光路线加分";
        }
        if (month >= 9 && month <= 11) {
            return "季节提示：正值秋季，祥符禅寺千年银杏与秋日芦苇正美，自然风光路线加分";
        }
        return null;
    }
}
