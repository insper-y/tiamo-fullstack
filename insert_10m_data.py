#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
高效批量注入1千万条数据到books表
"""
import pymysql
import time
import random
import string

# 数据库配置
DB_CONFIG = {
    'host': 'dbconn.sealosbja.site',
    'port': 48020,
    'user': 'root',
    'password': '7b4kzvdr',
    'database': 'mydb',
    'charset': 'utf8mb4',
    'autocommit': False,
}

TOTAL_RECORDS = 1_000  # 1千
BATCH_SIZE = 1_000     # 每批1千条
BATCHES = TOTAL_RECORDS // BATCH_SIZE  # 1批

# 预生成随机数据池
SOFTWARE_NAMES = ['好省', '粉象生活', '高佣联盟', '花生日记', '蜜源', '美逛', '淘小铺', '东小店', '芬香', '悦拜']
WECHAT_ACCOUNTS = [f'wx_{i:08d}' for i in range(1000)]
SOFTWARE_ACCOUNTS = [f'user_{i:010d}' for i in range(1000)]
WECHAT_REMARKS = [f'客户{i:05d}' for i in range(1000)]
PRODUCT_TITLES = [
    '夏季新款女装连衣裙', '男士休闲运动鞋', '儿童益智玩具积木', '家用智能扫地机器人',
    '无线蓝牙耳机降噪', '不锈钢保温杯大容量', '纯棉T恤男女同款', '笔记本电脑轻薄本',
    '手机壳防摔保护套', '充电宝20000毫安', '厨房用品刀具套装', '床上用品四件套',
    '护肤品套装补水保湿', '零食大礼包网红小吃', '水果新鲜当季整箱', '茶叶礼盒装高档',
    '咖啡速溶提神醒脑', '洗发水去屑控油', '沐浴露持久留香', '牙膏美白清新口气',
]

def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))

def generate_batch(start_id, batch_size):
    """生成一批数据"""
    batch = []
    for i in range(batch_size):
        idx = start_id + i
        batch.append((
            random.choice(SOFTWARE_NAMES),                          # name 分销软件
            random.choice(WECHAT_ACCOUNTS),                         # type 微信账号
            random.choice(SOFTWARE_ACCOUNTS),                       # description 软件账号
            random.choice(WECHAT_REMARKS),                          # aa 微信备注名
            f'PRD{idx:010d}',                                       # bd 商品ID
            f'https://item.taobao.com/item.htm?id={idx}',           # ac 商品链接
            f'https://img.alicdn.com/imgextra/i{idx % 10}/O1CN01{random_string(10)}.jpg',  # ab 商品主图
            random.choice(PRODUCT_TITLES) + f'_{idx}',             # ax 商品标题
            0,                                                        # deleted 软删除标记
        ))
    return batch

def main():
    print(f"开始注入 {TOTAL_RECORDS:,} 条数据到 books 表...")
    print(f"每批 {BATCH_SIZE:,} 条，共 {BATCHES} 批")
    print(f"数据库: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    print("-" * 60)

    start_time = time.time()

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 创建表（如果不存在）
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS books (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) DEFAULT NULL COMMENT '分销软件',
                type VARCHAR(100) DEFAULT NULL COMMENT '微信账号',
                description VARCHAR(255) DEFAULT NULL COMMENT '软件账号',
                aa VARCHAR(100) DEFAULT NULL COMMENT '微信备注名',
                bd VARCHAR(100) DEFAULT NULL COMMENT '商品ID',
                ac VARCHAR(500) DEFAULT NULL COMMENT '商品链接',
                ab VARCHAR(500) DEFAULT NULL COMMENT '商品主图',
                ax VARCHAR(255) DEFAULT NULL COMMENT '商品标题',
                deleted TINYINT DEFAULT 0 COMMENT '软删除标记',
                deleted_time DATETIME DEFAULT NULL COMMENT '删除时间',
                deleted_by VARCHAR(100) DEFAULT NULL COMMENT '删除操作人',
                INDEX idx_name (name),
                INDEX idx_type (type),
                INDEX idx_deleted (deleted)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分销商品数据'
        """)
        conn.commit()
        print("✓ books 表已就绪")

        # 临时优化插入性能
        cursor.execute("SET unique_checks=0")
        cursor.execute("SET foreign_key_checks=0")
        cursor.execute("SET sql_log_bin=0")
        print("✓ 已禁用唯一检查、外键检查、二进制日志")

        # 批量插入
        insert_sql = """
            INSERT INTO books (name, type, description, aa, bd, ac, ab, ax, deleted)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        total_inserted = 0
        for batch_num in range(BATCHES):
            batch_start = time.time()
            start_id = batch_num * BATCH_SIZE

            # 生成数据
            batch_data = generate_batch(start_id, BATCH_SIZE)

            # 批量插入
            cursor.executemany(insert_sql, batch_data)
            conn.commit()

            total_inserted += BATCH_SIZE
            batch_time = time.time() - batch_start
            elapsed = time.time() - start_time
            speed = BATCH_SIZE / batch_time if batch_time > 0 else 0
            avg_speed = total_inserted / elapsed if elapsed > 0 else 0
            eta = (TOTAL_RECORDS - total_inserted) / avg_speed if avg_speed > 0 else 0

            if (batch_num + 1) % 10 == 0 or batch_num == 0:
                print(f"[{batch_num+1:4d}/{BATCHES}] 已注入 {total_inserted:>12,} 条 | "
                      f"本批 {batch_time:.2f}s | 速度 {speed:,.0f}条/s | "
                      f"平均 {avg_speed:,.0f}条/s | 预计剩余 {eta/60:.1f}分钟")

        # 恢复设置
        cursor.execute("SET unique_checks=1")
        cursor.execute("SET foreign_key_checks=1")
        cursor.execute("SET sql_log_bin=1")

        # 验证数据量
        cursor.execute("SELECT COUNT(*) FROM books")
        count = cursor.fetchone()[0]

        total_time = time.time() - start_time
        print("-" * 60)
        print(f"✓ 数据注入完成！")
        print(f"  总记录数: {count:,}")
        print(f"  总耗时: {total_time:.2f}秒 ({total_time/60:.2f}分钟)")
        print(f"  平均速度: {count/total_time:,.0f}条/秒")

        cursor.close()
        conn.close()

    except Exception as e:
        print(f"✗ 错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()
