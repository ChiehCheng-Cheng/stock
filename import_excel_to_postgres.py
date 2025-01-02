import pandas as pd 
import psycopg2

# 資料庫連線資訊
db_config = {
    "dbname": "your_database_name",  # 替換為您的資料庫名稱
    "user": "your_username",        # 替換為您的使用者名稱
    "password": "your_password",    # 替換為您的密碼
    "host": "localhost",
    "port": 5432
}

# 定義資料表結構生成函數
def generate_table_sql(sheet_name, data):
    table_name = sheet_name
    columns = data.columns
    dtypes = data.dtypes
    sql = f"CREATE TABLE {table_name} (\n"
    
    # 自動推測 PostgreSQL 資料型別
    for col, dtype in zip(columns, dtypes):
        if "int" in str(dtype):
            pg_type = "INTEGER"
        elif "float" in str(dtype):
            pg_type = "REAL"
        else:
            pg_type = "TEXT"
        sql += f"    {col.replace(' ', '_')} {pg_type},\n"
    
    sql = sql.rstrip(",\n") + "\n);"
    return sql

# 讀取 Excel 檔案
file_path = "檔案資料庫.xlsx"  # 替換為您的檔案路徑
excel_data = pd.ExcelFile(file_path)

# 連接資料庫
conn = psycopg2.connect(**db_config)
cursor = conn.cursor()

try:
    for sheet_name in excel_data.sheet_names:
        # 讀取每個工作表
        data = excel_data.parse(sheet_name)
        
        # 生成資料表 SQL
        table_sql = generate_table_sql(sheet_name, data)
        cursor.execute(f"DROP TABLE IF EXISTS {sheet_name};")
        cursor.execute(table_sql)
        
        # 插入資料
        for _, row in data.iterrows():
            columns = ", ".join(row.index)
            placeholders = ", ".join(["%s"] * len(row))
            insert_sql = f"INSERT INTO {sheet_name} ({columns}) VALUES ({placeholders})"
            cursor.execute(insert_sql, tuple(row))
        
        print(f"工作表 '{sheet_name}' 匯入成功！")

    conn.commit()
except Exception as e:
    print(f"發生錯誤: {e}")
    conn.rollback()
finally:
    cursor.close()
    conn.close()
