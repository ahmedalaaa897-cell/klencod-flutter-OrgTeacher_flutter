package com.orgteacher

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Teacher {
    private const val SYS =
        "إنت مدرّس أورج شرقي (زي ORG وKorg Pa) بتكلّم طالب مبتدئ بالمصري، بنبرة صبورة ومشجعة. " +
        "رد في 3 جمل بالكتير، وبشكل عملي: قوله يضغط إيه وبأنهي إيد وليه. من غير ماركداون."

    /** بيرجّع null لو مفيش مفتاح أو حصل خطأ، فالمتصل يستخدم الشرح المحلي. */
    fun ask(apiKey: String, context: String, question: String, cb: (String?) -> Unit) {
        if (apiKey.isBlank()) { cb(null); return }
        Thread {
            try {
                val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.setRequestProperty("content-type", "application/json")
                c.setRequestProperty("x-api-key", apiKey)
                c.setRequestProperty("anthropic-version", "2023-06-01")
                c.connectTimeout = 10000
                c.readTimeout = 30000
                c.doOutput = true
                val body = JSONObject()
                    .put("model", "claude-sonnet-4-6")
                    .put("max_tokens", 400)
                    .put("system", SYS)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "$context\n$question")))
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val txt = c.inputStream.bufferedReader(Charsets.UTF_8).readText()
                cb(JSONObject(txt).getJSONArray("content").getJSONObject(0).getString("text"))
            } catch (e: Exception) {
                cb(null)
            }
        }.start()
    }
}
