package com.spin.secure.net
import androidx.collection.SimpleArrayMap
import com.spin.secure.utils.KLog
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NewHttpClient {
    private val client = OkHttpClient()
    //存储请求，用于取消
    private val callMap = SimpleArrayMap<Any, Call>()
    suspend fun get(url: String): HttpResponse {
        val request = createRequest()
            .url(url)
            .build()

        return execute(request)
    }

    suspend fun getParams(url: String,params: Map<String, Any>): HttpResponse {
        val urlBuilder = url.toHttpUrl().newBuilder()
        params.forEach { entry ->
            urlBuilder.addEncodedQueryParameter(
                entry.key,
                URLEncoder.encode(entry.value.toString(), StandardCharsets.UTF_8.toString())
            )
        }
        KLog.e("TBA","Cloak接入--params=${JsonUtil.toJson(params)}")
        val request = createRequest()
            .get()
            .tag(params)
            .url(urlBuilder.build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
         val mClient = OkHttpClient.Builder().build()

        val newCall = mClient.newCall(request)
        //存储请求，用于取消
        callMap.put(request.tag(), newCall)
        return execute(request)
    }

    suspend fun post(url: String, body: Any): HttpResponse {
        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), body.toString())
        val request = createRequest()
            .post(requestBody)
            .url(url)
            .tag(body)
            .build()
        return execute(request)
    }
    private fun createRequest(): Request.Builder {
        val builder = Request.Builder()
        val packageName = AppUtils.getAppPackageName()
        builder.addHeader("QIN", packageName)
        builder.addHeader("AML", "ZZ")
        return builder
    }
    private suspend fun execute(request: Request): HttpResponse = withContext(Dispatchers.IO) {
            val call: Call = client.newCall(request)
            val response: Response = call.execute()
            val responseBody = response.body?.string()
            val statusCode = response.code
            HttpResponse(responseBody, statusCode)
    }
}
data class HttpResponse(val body: String?, val statusCode: Int)
