package com.example.backend.common.util;

/**
 * 雪花算法ID生成器
 * 用于生成分布式唯一ID
 * 
 * @author backend
 * @since 1.0.0
 */
public class SnowflakeIdGenerator {

    // 开始时间戳 (2024-01-01 00:00:00)
    private final static long START_TIMESTAMP = 1704067200000L;

    // 每一部分占用的位数
    private final static long SEQUENCE_BIT = 12; // 序列号占用的位数
    private final static long MACHINE_BIT = 5;   // 机器标识占用的位数
    private final static long DATA_CENTER_BIT = 5;// 数据中心占用的位数

    // 每一部分的最大值
    private final static long MAX_DATA_CENTER_NUM = ~(-1L << DATA_CENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    // 每一部分向左的位移
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;

    private long dataCenterId;  // 数据中心
    private long machineId;     // 机器标识
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上一次时间戳

    /**
     * 构造函数
     * @param dataCenterId 数据中心ID (0-31)
     * @param machineId 机器标识ID (0-31)
     */
    public SnowflakeIdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException("Data center ID can't be greater than " + MAX_DATA_CENTER_NUM + " or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("Machine ID can't be greater than " + MAX_MACHINE_NUM + " or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    /**
     * 生成下一个ID
     * @return 唯一的Long类型ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟被回拨，则抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = getNextTimestamp(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        // 上一次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT)
                | (dataCenterId << DATA_CENTER_LEFT)
                | (machineId << MACHINE_LEFT)
                | sequence;
    }

    /**
     * 获取当前时间戳
     * @return 当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取下一个时间戳
     * @param lastTimestamp 上一次时间戳
     * @return 下一个时间戳
     */
    private long getNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    // 单例模式
    private static volatile SnowflakeIdGenerator instance = null;

    /**
     * 获取单例实例
     * @return 单例实例
     */
    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator(0, 0);
                }
            }
        }
        return instance;
    }

    // 测试
    public static void main(String[] args) {
        SnowflakeIdGenerator generator = SnowflakeIdGenerator.getInstance();
        for (int i = 0; i < 10; i++) {
            System.out.println(generator.nextId());
        }
    }
}
