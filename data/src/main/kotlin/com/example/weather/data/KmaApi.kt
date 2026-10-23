package com.example.weather.data

import com.example.weather.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Serializable
data class KmaItem(val category: String, val baseDate: String = "", val baseTime: String = "", val fcstDate: String = "", val fcstTime: String = "", val obsrValue: String? = null, val fcstValue: String? = null) {
    val value get() = fcstValue ?: obsrValue
    val validAt get() = KmaTime.instant(fcstDate.ifBlank { baseDate }, fcstTime.ifBlank { baseTime })
}
class KmaException(val failure: WeatherFailure) : Exception(failure.name)
interface KmaService {
    @GET("{operation}") suspend fun request(@Path("operation") operation: String, @QueryMap query: Map<String, String>): Response<ResponseBody>
}
class KmaApi(private val service: KmaService, private val key: String) {
    suspend fun fetch(product: Product, grid: Grid, release: LocalDateTime): List<KmaItem> {
        if (key.isBlank()) throw KmaException(WeatherFailure.CONFIGURATION)
        try {
            val response = service.request(product.endpoint, mapOf(
                "serviceKey" to key, "dataType" to "JSON", "numOfRows" to "2000", "pageNo" to "1",
                "base_date" to release.format(KmaTime.date), "base_time" to release.format(KmaTime.time),
                "nx" to grid.x.toString(), "ny" to grid.y.toString(),
            ))
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                throw KmaException(when (response.code()) { 401, 403 -> WeatherFailure.AUTHENTICATION; 429 -> WeatherFailure.QUOTA; else -> WeatherFailure.HTTP })
            }
            val body = withContext(Dispatchers.IO) { response.body()?.use { it.string() } }.orEmpty()
            return parse(body)
        } catch (e: CancellationException) { throw e
        } catch (e: SocketTimeoutException) { throw KmaException(WeatherFailure.TIMEOUT)
        } catch (e: IOException) { throw KmaException(WeatherFailure.NETWORK) }
    }
    companion object {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        fun create(key: String, baseUrl: String = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/"): KmaApi {
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
            return KmaApi(Retrofit.Builder().baseUrl(baseUrl).client(client).build().create(KmaService::class.java), key)
        }
        fun failure(code: String): WeatherFailure = when (code) {
            "03", "NODATA_ERROR" -> WeatherFailure.NO_DATA
            "20", "21", "30", "31", "32", "33", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR", "SERVICE_KEY_IS_NOT_REGISTERED", "SERVICE_ACCESS_DENIED_ERROR" -> WeatherFailure.AUTHENTICATION
            "22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR" -> WeatherFailure.QUOTA
            "05" -> WeatherFailure.TIMEOUT
            "01", "02", "04", "99" -> WeatherFailure.HTTP
            else -> WeatherFailure.MALFORMED
        }
        fun parse(body: String): List<KmaItem> {
            if (body.isBlank()) throw KmaException(WeatherFailure.NO_DATA)
            if (body.trimStart().startsWith("<")) {
                val code = Regex("<(?:returnReasonCode|resultCode)>([^<]+)</").find(body)?.groupValues?.get(1)
                throw KmaException(failure(code ?: Regex("<returnAuthMsg>([^<]+)</").find(body)?.groupValues?.get(1).orEmpty()))
            }
            try {
                val response = json.parseToJsonElement(body).jsonObject["response"]?.jsonObject ?: throw KmaException(WeatherFailure.MALFORMED)
                val code = response["header"]?.jsonObject?.get("resultCode")?.jsonPrimitive?.content ?: throw KmaException(WeatherFailure.MALFORMED)
                if (code != "00") throw KmaException(failure(code))
                val items = response["body"]?.jsonObject?.get("items") as? JsonObject
                val raw = items?.get("item") ?: throw KmaException(WeatherFailure.NO_DATA)
                val list = when (raw) { is JsonArray -> raw; is JsonObject -> JsonArray(listOf(raw)); else -> throw KmaException(WeatherFailure.NO_DATA) }
                if (list.isEmpty()) throw KmaException(WeatherFailure.NO_DATA)
                return json.decodeFromJsonElement<List<KmaItem>>(list)
            } catch (e: SerializationException) { throw KmaException(WeatherFailure.MALFORMED)
            } catch (e: IllegalArgumentException) { throw KmaException(WeatherFailure.MALFORMED) }
        }
    }
}
