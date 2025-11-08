import csv
import random
import sys
from datetime import datetime, timedelta

path = [
    (37.525932, 127.119039),
    (37.518135, 127.113442),
    (37.512643, 127.127675),
    (37.517242, 127.131229),
    (37.518116, 127.129434),
    (37.520068, 127.128979),
    (37.522391, 127.127773),
    (37.523017, 127.127398),
    (37.523724, 127.127722),
    (37.525861, 127.122699),
    (37.525932, 127.119039),
]

# 설정
points_per_segment = 80  # 각 구간당 보간 포인트 수
interval = 1  # 초 단위
start_time = datetime(2025, 11, 8, 14, 0, 0)

# 파일명 설정
if len(sys.argv) > 1:
    filename = sys.argv[1]
    if not filename.endswith('.csv'):
        filename += '.csv'
else:
    # 랜덤 파일명 생성 (타임스탬프 기반)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"dummy_data_{timestamp}.csv"

rows = []
t = start_time

# 경로를 따라 보간
for i in range(len(path) - 1):
    lat1, lon1 = path[i]
    lat2, lon2 = path[i + 1]

    for j in range(points_per_segment):
        ratio = j / points_per_segment
        lat = lat1 + (lat2 - lat1) * ratio
        lon = lon1 + (lon2 - lon1) * ratio
        rsrp = random.randint(-110, -80)
        rsrq = random.randint(-14, -6)
        rows.append([t.strftime("%Y-%m-%d %H:%M:%S"), round(lat, 6), round(lon, 6), rsrp, rsrq])
        t += timedelta(seconds=interval)

# CSV 저장
with open(filename, "w", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["timestamp", "latitude", "longitude", "rsrp", "rsrq"])
    writer.writerows(rows)

print(f"✅ CSV 파일 생성 완료: {filename}")
