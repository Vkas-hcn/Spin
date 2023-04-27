package com.spin.secure.net

import com.blankj.utilcode.util.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Buffer

class HeaderInterceptor : Interceptor {

    companion object {
        private val gson = GsonBuilder()
            .enableComplexMapKeySerialization()
            .create()
        private val mapType = object : TypeToken<Map<String, Any>>() {}.type
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originRequest = chain.request()

        val attachHeaders = mutableListOf<Pair<String, String>>(
            "version" to AppUtils.getAppVersionName(),
            "AML" to "ZZ",
            "QIN" to AppUtils.getAppPackageName()
        )
        val signHeaders = mutableListOf<Pair<String, String>>()
        signHeaders.addAll(attachHeaders)
        //get的请求，参数
        if (originRequest.method == "GET") {
            originRequest.url.queryParameterNames.forEach { key ->
                signHeaders.add(key to (originRequest.url.queryParameter(key) ?: ""))
            }
        }
        //post的请求 formBody形式，或json形式的，都需要将内部的字段，遍历出来，参与sign的计算
        val requestBody = originRequest.body
        if (originRequest.method == "POST") {
            //formBody
            if (requestBody is FormBody) {
                for (i in 0 until requestBody.size) {
                    signHeaders.add(requestBody.name(i) to requestBody.value(i))
                }
            }
            // 检查请求内容类型
            if (requestBody?.contentType() == "application/json".toMediaTypeOrNull()) {
                val buffer = Buffer()
                requestBody?.writeTo(buffer)
                val json = buffer.use { it.readUtf8() }
                val map = gson.fromJson<Map<String, Any>>(json, mapType)
                // 将Map中的键值对添加到headers中
                map.forEach { (key, value) ->
                    signHeaders.add(key to value.toString())
                }
            }

        }

        //todo 算法：都必须是非空参数！！！  sign = MD5（ascii排序后的 headers及params的key=value拼接&后，最后拼接appkey和value）//32位的大写,
        val signValue = signHeaders
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }
            .plus("&appkey=$")

        val newBuilder = originRequest.newBuilder()
            .cacheControl(CacheControl.FORCE_NETWORK)
        attachHeaders.forEach { newBuilder.header(it.first, it.second) }
        newBuilder.header("sign", EncryptUtils.encryptMD5ToString(signValue))

        if (originRequest.method == "POST" && requestBody != null) {
            newBuilder.post(requestBody)
        } else if (originRequest.method == "GET") {
            newBuilder.get()
        }
        return chain.proceed(newBuilder.build())
    }

}