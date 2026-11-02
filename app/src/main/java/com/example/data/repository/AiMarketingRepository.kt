package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

class AiMarketingRepository {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateMultiPlatformAds(
        title: String,
        price: String,
        description: String,
        city: String,
        dialect: String // "دارجة" or "فصحى"
    ): Map<String, String> {
        val dialectInstruction = if (dialect == "دارجة") {
            "استخدم لهجة سعودية وخليجية بيضاء شائعة وجذابة في البيع والإعلانات التجارية."
        } else {
            "استخدم لغة عربية فصحى تسويقية راقية واضحة ومقنعة."
        }

        val prompt = """
            أنت خبير تسويق رقمي وكتابة إعلانات في السوق العربي والخليجي.
            المطلوب كتابة 4 نصوص إعلانية مخصصة لنفس المنتج/الخدمة التالية بحسب كل منصة وأسلوبها:
            
            بيانات المنتج:
            - اسم المنتج/الخدمة: $title
            - السعر: $price
            - المدينة/الموقع: $city
            - التفاصيل والمواصفات: $description
            
            تعليمات الأسلوب:
            $dialectInstruction
            
            يجب أن يكون الناتج مقسماً بدقة باستخدام هذه العناوين كفواصل واضحة:
            
            === منصة حراج ومرجان ===
            (عنوان إعلان مبوب جذاب، السعر، المواصفات بشكل نقاط واضحة، تفاصيل التواصل، كلمات دلالية)
            
            === منصة تيك توك ===
            (نص فيديو قصير مع هوك جاذب، فكرة تصوير مقترحة للفيديو، وهاشتاقات نشطة)
            
            === منصة فيسبوك ===
            (منشور تسويقي تفاعلي يعرض الفائدة والمميزات مع دعوة واضحة لاتخاذ إجراء CTA)
            
            === منصة واتساب ===
            (رسالة برودكاست قصيرة ومباشرة مع رابط محادثة وموجز سريع ومحفز للتواصل)
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text ?: ""
            parsePlatformSections(text)
        } catch (e: TimeoutCancellationException) {
            Log.e("AiMarketing", "AI generation timed out", e)
            throw IllegalStateException("استغرقت عملية التوليد وقتاً أطول من المعتاد، يرجى المحاولة مرة أخرى.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiMarketing", "Failed to generate multi-platform ads", e)
            throw e
        }
    }

    suspend fun generatePersonalizedMessage(
        clientName: String,
        status: String,
        purpose: String,
        notes: String,
        dialect: String
    ): String {
        val dialectInstruction = if (dialect == "دارجة") {
            "اكتب بلهجة خليجية بيضاء دافئة وودية."
        } else {
            "اكتب بلغة عربية فصحى مهنية ولبقة."
        }

        val prompt = """
            أنت مندوب مبيعات وخدمة عملاء محترف.
            المطلوب كتابة رسالة تواصل شخصية للعميل التالي:
            - اسم العميل: $clientName
            - حالة العميل في مسار البيع: $status
            - الغرض من الرسالة: $purpose
            - ملاحظات سابقة: $notes
            
            الأسلوب المطلوب:
            $dialectInstruction
            
            اكتب فقط نص الرسالة الجاهزة للإرسال مباشرة دون مقدمات أو شروحات إضافية، واذكر اسم العميل في البداية بترحيب مهذب.
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            response.text?.trim() ?: "مرحباً $clientName، نسعد بتواصلك معنا!"
        } catch (e: TimeoutCancellationException) {
            Log.e("AiMarketing", "Message generation timed out", e)
            throw IllegalStateException("استغرقت عملية صياغة الرسالة وقتاً أطول من المعتاد.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiMarketing", "Failed to generate personalized message", e)
            throw e
        }
    }

    private fun parsePlatformSections(fullText: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val sections = fullText.split("===")
        for (i in 1 until sections.size step 2) {
            val header = sections[i].trim()
            val content = if (i + 1 < sections.size) sections[i + 1].trim() else ""
            when {
                header.contains("حراج") || header.contains("مرجان") -> {
                    result["حراج"] = content
                    result["مرجان"] = content
                }
                header.contains("تيك توك") -> result["تيك توك"] = content
                header.contains("فيسبوك") -> result["فيسبوك"] = content
                header.contains("واتساب") -> result["واتساب"] = content
            }
        }

        // Fallback if formatting was slightly different
        if (result.isEmpty()) {
            result["حراج"] = fullText
            result["مرجان"] = fullText
            result["تيك توك"] = fullText
            result["فيسبوك"] = fullText
            result["واتساب"] = fullText
        }
        return result
    }
}
