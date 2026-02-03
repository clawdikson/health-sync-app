package com.clawd.healthsync

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Renpho API Client
 * Fetches body composition data directly from Renpho
 */
class RenphoApi(
    private val email: String,
    private val password: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    private var sessionKey: String? = null
    private var userId: String? = null

    companion object {
        private const val API_BASE = "https://renpho.qnclouds.com"
        private const val AUTH_URL = "$API_BASE/api/v3/users/sign_in.json?app_id=Renpho"
        private const val MEASUREMENTS_URL = "$API_BASE/api/v2/measurements/list.json"
        
        // Renpho's public key for password encryption
        private const val PUBLIC_KEY = """
            MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+25I2upukpfQ7rIaaTZtVE744
            u2zV+HaagrUhDOTq2fMraicFq0tnWyBa4DnqOqRkMJKbcMZs2DkEQ8hQl95FOwdn
            BjCkLH17m0n3RCnHRIg2wQk4RFKasdzynx2eON7cBuCUhWexShlBMtjRYdNXRlDP
            CsHqpKaDBOvbdUMf1QIDAQAB
        """
    }

    data class AuthRequest(
        @SerializedName("secure_flag") val secureFlag: String,
        val email: String,
        val password: String
    )

    data class AuthResponse(
        @SerializedName("status_code") val statusCode: String?,
        @SerializedName("status_message") val statusMessage: String?,
        @SerializedName("terminal_user_session_key") val sessionKey: String?,
        val id: String?
    )

    data class Measurement(
        val id: Long?,
        @SerializedName("time_stamp") val timeStamp: Long?,
        val weight: Double?,
        val bmi: Double?,
        val bodyfat: Double?,
        val muscle: Double?,
        val water: Double?,
        val bone: Double?,
        val visfat: Int?,
        val bmr: Int?,
        val protein: Double?,
        val bodyage: Int?
    )

    data class MeasurementsResponse(
        @SerializedName("status_code") val statusCode: String?,
        @SerializedName("last_ary") val measurements: List<Measurement>?
    )

    private fun encryptPassword(password: String): String {
        try {
            val publicKeyPEM = PUBLIC_KEY
                .replace("\n", "")
                .replace("\r", "")
                .trim()
            
            val keyBytes = Base64.decode(publicKeyPEM, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)
            
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            // If encryption fails, return plain password
            return password
        }
    }

    suspend fun authenticate(): Boolean = suspendCoroutine { continuation ->
        // Try with plain password first (secure_flag=0)
        val authData = AuthRequest(
            secureFlag = "0",
            email = email,
            password = password
        )

        val json = gson.toJson(authData)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(AUTH_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Renpho/4.9.0 (Android)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val authResponse = gson.fromJson(responseBody, AuthResponse::class.java)
                    
                    if (authResponse.statusCode == "20000" && authResponse.sessionKey != null) {
                        sessionKey = authResponse.sessionKey
                        userId = authResponse.id
                        continuation.resume(true)
                    } else {
                        // Try with encrypted password
                        tryEncryptedAuth(continuation)
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    private fun tryEncryptedAuth(continuation: kotlin.coroutines.Continuation<Boolean>) {
        val encryptedPwd = encryptPassword(password)
        val authData = AuthRequest(
            secureFlag = "1",
            email = email,
            password = encryptedPwd
        )

        val json = gson.toJson(authData)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(AUTH_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "Renpho/4.9.0 (Android)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val authResponse = gson.fromJson(responseBody, AuthResponse::class.java)
                    
                    if (authResponse.statusCode == "20000" && authResponse.sessionKey != null) {
                        sessionKey = authResponse.sessionKey
                        userId = authResponse.id
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    suspend fun getMeasurements(): List<Measurement> = suspendCoroutine { continuation ->
        if (sessionKey == null || userId == null) {
            continuation.resume(emptyList())
            return@suspendCoroutine
        }

        val url = "$MEASUREMENTS_URL?user_id=$userId&last_at=883612800&locale=en&app_id=Renpho&terminal_user_session_key=$sessionKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "Renpho/4.9.0 (Android)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val measurementsResponse = gson.fromJson(responseBody, MeasurementsResponse::class.java)
                    
                    if (measurementsResponse.statusCode == "20000") {
                        continuation.resume(measurementsResponse.measurements ?: emptyList())
                    } else {
                        continuation.resume(emptyList())
                    }
                } catch (e: Exception) {
                    continuation.resume(emptyList())
                }
            }
        })
    }

    fun toSyncData(measurements: List<Measurement>): List<Map<String, Any?>> {
        return measurements.map { m ->
            mapOf(
                "timestamp" to m.timeStamp,
                "date" to m.timeStamp?.let { 
                    java.time.Instant.ofEpochSecond(it).toString().substring(0, 10) 
                },
                "weight" to m.weight,
                "bmi" to m.bmi,
                "bodyFat" to m.bodyfat,
                "muscle" to m.muscle,
                "water" to m.water,
                "bone" to m.bone,
                "visceralFat" to m.visfat,
                "bmr" to m.bmr,
                "protein" to m.protein,
                "bodyAge" to m.bodyage
            )
        }
    }
}
