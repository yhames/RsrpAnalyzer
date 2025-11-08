package com.example.rsrpanalyzer.util

import com.example.rsrpanalyzer.data.db.SignalRecordEntity
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvHelper {
    private const val CSV_HEADER = "timestamp,latitude,longitude,rsrp,rsrq"
    
    private fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    /**
     * 세션 기록을 CSV 형식으로 내보내기
     */
    fun exportToCsv(
        records: List<SignalRecordEntity>,
        outputStream: OutputStream
    ) {
        val dateFormat = getDateFormat()
        outputStream.bufferedWriter().use { writer ->
            // 헤더 작성
            writer.write(CSV_HEADER)
            writer.newLine()

            // 데이터 작성
            records.forEach { record ->
                val line = buildString {
                    append(dateFormat.format(Date(record.timestampMillis)))
                    append(",")
                    append(record.latitude)
                    append(",")
                    append(record.longitude)
                    append(",")
                    append(record.rsrp)
                    append(",")
                    append(record.rsrq)
                }
                writer.write(line)
                writer.newLine()
            }
        }
    }

    /**
     * CSV 파일에서 기록 불러오기
     * @return Pair<성공 여부, 기록 리스트 또는 에러 메시지>
     */
    fun importFromCsv(inputStream: InputStream): CsvImportResult {
        val dateFormat = getDateFormat()
        try {
            val records = mutableListOf<SignalRecordEntity>()

            inputStream.bufferedReader().use { reader ->
                // 헤더 읽기 및 검증
                val header =
                    reader.readLine()?.trim() ?: return CsvImportResult.Error("파일이 비어있습니다.")

                if (!isValidHeader(header)) {
                    return CsvImportResult.Error("올바른 CSV 형식이 아닙니다.\n필수 컬럼: timestamp, latitude, longitude, rsrp, rsrq")
                }

                // 데이터 읽기
                var lineNumber = 1
                var line = reader.readLine()
                while (line != null) {
                    lineNumber++
                    if (line.isNotBlank()) {
                        val record = parseCsvLine(line, dateFormat)
                            ?: return CsvImportResult.Error("${lineNumber}번째 줄 파싱 실패: $line")

                        records.add(record)
                    }
                    line = reader.readLine()
                }
            }

            return if (records.isEmpty()) {
                CsvImportResult.Error("불러올 데이터가 없습니다.")
            } else {
                CsvImportResult.Success(records)
            }
        } catch (e: Exception) {
            return CsvImportResult.Error("파일 읽기 실패: ${e.message}")
        }
    }

    private fun isValidHeader(header: String): Boolean {
        val normalized = header.replace(" ", "").lowercase()
        val required = CSV_HEADER.replace(" ", "").lowercase()
        return normalized == required
    }

    private fun parseCsvLine(line: String, dateFormat: SimpleDateFormat): SignalRecordEntity? {
        return try {
            val parts = line.split(",").map { it.trim() }
            if (parts.size != 5) return null

            val timestamp = dateFormat.parse(parts[0])?.time ?: return null
            val latitude = parts[1].toDoubleOrNull() ?: return null
            val longitude = parts[2].toDoubleOrNull() ?: return null
            val rsrp = parts[3].toIntOrNull() ?: return null
            val rsrq = parts[4].toIntOrNull() ?: return null

            // 데이터 유효성 검증
            if (latitude !in -90.0..90.0) return null
            if (longitude !in -180.0..180.0) return null
            if (rsrp !in -140..-44) return null // RSRP 일반적인 범위
            if (rsrq !in -20..0) return null // RSRQ 일반적인 범위

            SignalRecordEntity(
                sessionId = 0, // 나중에 설정됨
                timestampMillis = timestamp,
                latitude = latitude,
                longitude = longitude,
                rsrp = rsrp,
                rsrq = rsrq
            )
        } catch (_: Exception) {
            null
        }
    }

    sealed class CsvImportResult {
        data class Success(val records: List<SignalRecordEntity>) : CsvImportResult()
        data class Error(val message: String) : CsvImportResult()
    }
}
