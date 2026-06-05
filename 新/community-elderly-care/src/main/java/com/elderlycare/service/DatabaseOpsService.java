package com.elderlycare.service;

import com.elderlycare.config.DatabaseInitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DatabaseOpsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseOpsService.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final DatabaseInitProperties properties;
    private final ReentrantLock initLock = new ReentrantLock();

    public DatabaseOpsService(DataSource dataSource,
                              JdbcTemplate jdbcTemplate,
                              ResourceLoader resourceLoader,
                              DatabaseInitProperties properties) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public Map<String, Object> checkConnectivity() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime start = LocalDateTime.now();
        result.put("checkTime", Timestamp.valueOf(start));
        result.put("healthQuery", properties.getHealthQuery());

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Integer pingValue = jdbcTemplate.queryForObject(properties.getHealthQuery(), Integer.class);

            result.put("connected", true);
            result.put("databaseProduct", metaData.getDatabaseProductName());
            result.put("databaseVersion", metaData.getDatabaseProductVersion());
            result.put("url", metaData.getURL());
            result.put("username", metaData.getUserName());
            result.put("pingResult", pingValue);
            result.put("message", "数据库连接正常");
        } catch (Exception e) {
            log.error("数据库连通性校验失败", e);
            result.put("connected", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> initializeSchema() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("initEnabled", properties.getInit().isEnabled());
        result.put("scriptLocation", properties.getInit().getScriptLocation());
        result.put("executedAt", Timestamp.valueOf(LocalDateTime.now()));

        if (!properties.getInit().isEnabled()) {
            result.put("success", false);
            result.put("message", "数据库初始化已禁用，请检查 app.database.init.enabled 配置");
            return result;
        }

        if (!initLock.tryLock()) {
            result.put("success", false);
            result.put("message", "已有初始化流程正在执行，请稍后再试");
            return result;
        }

        Connection connection = null;
        try {
            Resource script = resourceLoader.getResource(properties.getInit().getScriptLocation());
            if (!script.exists()) {
                throw new IllegalStateException("初始化脚本不存在: " + properties.getInit().getScriptLocation());
            }

            connection = DataSourceUtils.getConnection(dataSource);
            ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
            Map<String, Object> repairSummary = repairLegacyDemoData(connection);
            result.put("success", true);
            result.put("message", "数据库初始化完成");
            result.put("repairSummary", repairSummary);
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            result.put("success", false);
            result.put("message", "数据库初始化失败: " + e.getMessage());
            if (properties.getInit().isFailOnError()) {
                throw new IllegalStateException("数据库初始化失败", e);
            }
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
            initLock.unlock();
        }
        return result;
    }

    private Map<String, Object> repairLegacyDemoData(Connection connection) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        String databaseProduct = metaData.getDatabaseProductName();
        result.put("databaseProduct", databaseProduct);

        if (databaseProduct == null || !databaseProduct.toLowerCase().contains("mysql")) {
            result.put("skipped", true);
            result.put("message", "当前仅对 MySQL 执行示例数据修复");
            return result;
        }

        int normalizedCategories = normalizeSeedCategories();
        int normalizedItems = normalizeSeedItems();
        int remappedOrders = remapLegacyOrdersToCanonicalItems();
        int removedDuplicateItems = removeDuplicateSeedItems();
        int removedDuplicateCategories = removeDuplicateSeedCategories();
        ensureUniqueIndex(
                "service_category",
                "uk_service_category_name",
                "CREATE UNIQUE INDEX uk_service_category_name ON service_category (name)"
        );
        ensureUniqueIndex(
                "service_item",
                "uk_service_item_category_name",
                "CREATE UNIQUE INDEX uk_service_item_category_name ON service_item (category_id, name)"
        );

        int repairedUsers = repairLegacyUsers();
        int repairedBindings = repairLegacyBindings();
        int repairedElderlyInfo = repairLegacyElderlyInfo();
        int repairedOrders = repairLegacyOrderText();
        int repairedMessages = repairLegacyMessages();
        int seededInformation = seedInformationSamples();

        result.put("normalizedSeedCategories", normalizedCategories);
        result.put("normalizedSeedItems", normalizedItems);
        result.put("remappedLegacyOrders", remappedOrders);
        result.put("removedDuplicateSeedItems", removedDuplicateItems);
        result.put("removedDuplicateSeedCategories", removedDuplicateCategories);
        result.put("repairedLegacyUsers", repairedUsers);
        result.put("repairedLegacyBindings", repairedBindings);
        result.put("repairedLegacyElderlyInfo", repairedElderlyInfo);
        result.put("repairedLegacyOrders", repairedOrders);
        result.put("repairedLegacyMessages", repairedMessages);
        result.put("seededInformationSamples", seededInformation);
        result.put("message", "数据库示例数据修复完成");
        return result;
    }

    private int normalizeSeedCategories() {
        int updated = 0;
        updated += jdbcTemplate.update(
                "UPDATE service_category SET name = ?, description = ?, icon = ?, sort_order = ?, status = 1 WHERE id = ?",
                "生活照料", "提供日常生活照料服务", "life", 1, 1
        );
        updated += jdbcTemplate.update(
                "UPDATE service_category SET name = ?, description = ?, icon = ?, sort_order = ?, status = 1 WHERE id = ?",
                "健康护理", "提供健康护理和康复服务", "health", 2, 2
        );
        updated += jdbcTemplate.update(
                "UPDATE service_category SET name = ?, description = ?, icon = ?, sort_order = ?, status = 1 WHERE id = ?",
                "精神慰藉", "提供精神陪伴和心理疏导服务", "mind", 3, 3
        );
        updated += jdbcTemplate.update(
                "UPDATE service_category SET name = ?, description = ?, icon = ?, sort_order = ?, status = 1 WHERE id = ?",
                "法律咨询", "提供法律咨询和援助服务", "law", 4, 4
        );
        return updated;
    }

    private int normalizeSeedItems() {
        int updated = 0;
        updated += updateServiceItem(1, 1, "助餐服务", "为老人提供送餐、做饭服务", 30.00, 60);
        updated += updateServiceItem(2, 1, "助浴服务", "为老人提供洗浴辅助服务", 50.00, 45);
        updated += updateServiceItem(3, 1, "助洁服务", "为老人提供居家清洁服务", 40.00, 90);
        updated += updateServiceItem(4, 1, "陪诊服务", "陪同老人就医取药", 60.00, 120);
        updated += updateServiceItem(5, 2, "血压监测", "定期为老人测量血压", 20.00, 20);
        updated += updateServiceItem(6, 2, "血糖监测", "定期为老人测量血糖", 20.00, 20);
        updated += updateServiceItem(7, 2, "康复护理", "专业康复护理服务", 80.00, 60);
        updated += updateServiceItem(8, 2, "用药指导", "专业用药指导服务", 30.00, 30);
        updated += updateServiceItem(9, 3, "陪伴聊天", "与老人进行情感交流", 40.00, 60);
        updated += updateServiceItem(10, 3, "心理疏导", "专业心理咨询服务", 100.00, 60);
        updated += updateServiceItem(11, 3, "文化娱乐", "组织文化娱乐活动", 0.00, 120);
        updated += updateServiceItem(12, 4, "法律咨询", "提供法律咨询服务", 50.00, 60);
        updated += updateServiceItem(13, 4, "纠纷调解", "帮助调解家庭纠纷", 80.00, 90);
        return updated;
    }

    private int updateServiceItem(long id, long categoryId, String name, String description, double price, int duration) {
        return jdbcTemplate.update(
                "UPDATE service_item SET category_id = ?, name = ?, description = ?, price = ?, duration = ?, status = 1 WHERE id = ?",
                categoryId, name, description, price, duration, id
        );
    }

    private int remapLegacyOrdersToCanonicalItems() {
        int updated = 0;
        updated += remapOrderItem(List.of("助餐服务", "鍔╅?鏈嶅姟"), 1L);
        updated += remapOrderItem(List.of("助浴服务", "鍔╂荡鏈嶅姟"), 2L);
        updated += remapOrderItem(List.of("助洁服务", "鍔╂磥鏈嶅姟"), 3L);
        updated += remapOrderItem(List.of("陪诊服务", "闄?瘖鏈嶅姟"), 4L);
        updated += remapOrderItem(List.of("血压监测", "琛?帇鐩戞祴"), 5L);
        updated += remapOrderItem(List.of("血糖监测", "琛?硸鐩戞祴"), 6L);
        updated += remapOrderItem(List.of("康复护理", "搴峰?鎶ょ悊"), 7L);
        updated += remapOrderItem(List.of("用药指导", "鐢ㄨ嵂鎸囧?"), 8L);
        updated += remapOrderItem(List.of("陪伴聊天", "闄?即鑱婂ぉ"), 9L);
        updated += remapOrderItem(List.of("心理疏导", "蹇冪悊鐤忓?"), 10L);
        updated += remapOrderItem(List.of("文化娱乐", "鏂囧寲濞变箰"), 11L);
        updated += remapOrderItem(List.of("法律咨询", "娉曞緥鍜ㄨ?"), 12L);
        updated += remapOrderItem(List.of("纠纷调解", "绾犵悍璋冭В"), 13L);
        return updated;
    }

    private int remapOrderItem(List<String> itemNames, Long canonicalItemId) {
        String placeholders = String.join(",", java.util.Collections.nCopies(itemNames.size(), "?"));
        Object[] args = new Object[itemNames.size() + 1];
        args[0] = canonicalItemId;
        for (int i = 0; i < itemNames.size(); i++) {
            args[i + 1] = itemNames.get(i);
        }
        return jdbcTemplate.update(
                "UPDATE service_order o " +
                        "JOIN service_item si ON o.service_item_id = si.id " +
                        "SET o.service_item_id = ? " +
                        "WHERE si.name IN (" + placeholders + ") AND o.service_item_id <> ?",
                buildArgsWithTail(args, canonicalItemId)
        );
    }

    private Object[] buildArgsWithTail(Object[] headArgs, Object tailArg) {
        Object[] args = java.util.Arrays.copyOf(headArgs, headArgs.length + 1);
        args[args.length - 1] = tailArg;
        return args;
    }

    private int removeDuplicateSeedItems() {
        return jdbcTemplate.update(
                "DELETE si FROM service_item si " +
                        "JOIN service_category sc ON si.category_id = sc.id " +
                        "WHERE sc.id > 4 AND sc.name IN ('生活照料', '健康护理', '精神慰藉', '法律咨询')"
        );
    }

    private int removeDuplicateSeedCategories() {
        return jdbcTemplate.update(
                "DELETE FROM service_category " +
                        "WHERE id > 4 AND name IN ('生活照料', '健康护理', '精神慰藉', '法律咨询')"
        );
    }

    private void ensureUniqueIndex(String tableName, String indexName, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(createSql);
        }
    }

    private int repairLegacyUsers() {
        int updated = 0;
        updated += jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE username = ?",
                "刘春梅", "上海市杨浦区安和里6号", "刘先生", "13800002222", "family202604132359"
        );
        updated += jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE username = ?",
                "王秀兰", "上海市黄浦区宜养路18号", "陈海燕", "13910001003", "elder202604140021"
        );
        updated += jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE username = ?",
                "陈海燕", "上海市黄浦区宜养路20号", "王秀兰", "13910001001", "family202604140021"
        );
        updated += jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE username = ?",
                "李桂芳", "上海市静安区康养路88号1室", "李强", "13920001003", "elder202604140041"
        );
        updated += jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE username = ?",
                "李强", "上海市静安区康养路88号2室", "李桂芳", "13920001001", "family202604140041"
        );
        return updated;
    }

    private int repairLegacyBindings() {
        int updated = 0;
        updated += jdbcTemplate.update(
                "UPDATE family_binding SET relation = '女儿' WHERE id = 1 OR (elderly_id = 9 AND family_id = 10)"
        );
        updated += jdbcTemplate.update(
                "UPDATE family_binding SET relation = '儿子' WHERE id = 2 OR (elderly_id = 12 AND family_id = 13)"
        );
        return updated;
    }

    private int repairLegacyElderlyInfo() {
        return jdbcTemplate.update(
                "UPDATE elderly_info SET health_status = ?, medical_history = ? WHERE user_id = ?",
                "行动稍缓，需日常照看",
                "高血压病史 5 年，长期规律服药，近期状态稳定",
                9L
        );
    }

    private int repairLegacyOrderText() {
        int updated = 0;
        updated += jdbcTemplate.update(
                "UPDATE service_order SET service_address = ?, remark = ? WHERE id = ?",
                "上海市静安区康养路88号A座", "首次上门助餐服务", 1L
        );
        updated += jdbcTemplate.update(
                "UPDATE service_order SET service_address = ?, remark = ? WHERE id = ?",
                "上海市静安区康养路88号B座", "家属临时调整时间，已取消预约", 2L
        );
        return updated;
    }

    private int repairLegacyMessages() {
        int updated = 0;
        updated += jdbcTemplate.update(
                "UPDATE message SET content = ? WHERE id IN (9, 10)",
                "紧急求助提醒：王秀兰发起了摔倒求助，位置：幸福小区2栋1单元。"
        );
        updated += jdbcTemplate.update(
                "UPDATE message SET content = ? WHERE id IN (11, 12)",
                "应急进展提醒：王秀兰的摔倒求助已由服务链服务人员响应。"
        );
        updated += jdbcTemplate.update(
                "UPDATE message SET content = ? WHERE id IN (13, 14)",
                "应急进展提醒：王秀兰的摔倒求助已处理完成。"
        );
        updated += jdbcTemplate.update(
                "UPDATE message SET content = ? WHERE id IN (20, 21)",
                "应急进展提醒：服务链老人25222040的摔倒求助已由联调服务人员响应。"
        );
        return updated;
    }

    private int seedInformationSamples() {
        int inserted = 0;
        inserted += jdbcTemplate.update(
                "INSERT INTO information (title, content, info_type, publisher_id, status, view_count, create_time, publish_time) " +
                        "SELECT ?, ?, ?, 1, 1, 0, NOW(), NOW() FROM DUAL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM information WHERE title = ?)",
                "居家安全检查提醒",
                "近期天气转热，请家属协助老人检查家中防滑垫、夜灯和燃气阀门，重点关注厨房、卫生间和卧室动线安全。",
                3,
                "居家安全检查提醒"
        );
        inserted += jdbcTemplate.update(
                "INSERT INTO information (title, content, info_type, publisher_id, status, view_count, create_time, publish_time) " +
                        "SELECT ?, ?, ?, 1, 1, 0, NOW(), NOW() FROM DUAL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM information WHERE title = ?)",
                "社区义诊活动预告",
                "本周六上午九点至十一点，社区卫生服务中心将在邻里活动室开展慢病咨询和血压血糖义诊，欢迎老人及家属参加。",
                2,
                "社区义诊活动预告"
        );
        inserted += jdbcTemplate.update(
                "INSERT INTO information (title, content, info_type, publisher_id, status, view_count, create_time, publish_time) " +
                        "SELECT ?, ?, ?, 1, 1, 0, NOW(), NOW() FROM DUAL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM information WHERE title = ?)",
                "长期护理险申请提示",
                "符合失能评估条件的老人可通过街道便民窗口预约长期护理险评估，建议提前准备身份证、医保卡和近期诊疗材料。",
                1,
                "长期护理险申请提示"
        );
        return inserted;
    }
}
