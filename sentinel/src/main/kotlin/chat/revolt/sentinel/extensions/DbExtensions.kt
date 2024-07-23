package chat.revolt.sentinel.extensions

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun ResultRow.toMap(): Map<String, Any> {
    val mutableMap = mutableMapOf<String, Any>()
    val dataList = this::class.memberProperties.find { it.name == "data" }?.apply {
        isAccessible = true
    }?.call(this) as Array<*>
    fieldIndex.entries.forEach { entry ->
        val column = entry.key as Column<*>
        mutableMap[column.name] = dataList[entry.value] as Any
    }
    return mutableMap
}