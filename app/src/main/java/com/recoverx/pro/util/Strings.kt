package com.recoverx.pro.util

import com.recoverx.pro.data.AppLanguage

data class UiStrings(
    val languageName: String,
    val appSubtitle: String,
    val home: String,
    val results: String,
    val settings: String,
    val scanNow: String,
    val scanFolder: String,
    val deepScan: String,
    val scanDescription: String,
    val recentFinds: String,
    val foundCount: (Int) -> String,
    val noResults: String,
    val permissionTitle: String,
    val permissionDescription: String,
    val grantAccess: String,
    val restore: String,
    val export: String,
    val selectAll: String,
    val clearSelection: String,
    val language: String,
    val english: String,
    val arabic: String,
    val privacy: String,
    val limitations: String,
    val appVersion: String,
    val realRecovery: String,
    val noFakeResults: String,
    val trashSource: String,
    val mediaSource: String,
    val folderSource: String,
    val carvedSource: String,
    val recoveryComplete: String,
    val exported: String,
    val chooseDestination: String,
    val chooseInputImage: String,
    val deepScanDescription: String,
    val cancel: String,
    val close: String,
    val permissionDenied: String,
    val scanError: String,
    val selectFolder: String,
    val selectStorageImage: String,
    val ready: String
)

fun uiStrings(language: AppLanguage) = if (language == AppLanguage.ARABIC) {
    UiStrings(
        "العربية", "استرجاع حقيقي، بدون نتائج وهمية", "الرئيسية", "النتائج", "الإعدادات", "ابدأ الفحص",
        "فحص مجلد", "الفحص العميق", "نفحص مصادر الوسائط التي يسمح بها Android فعلياً ونستعيد ما يمكن الوصول إليه.",
        "آخر النتائج", { n -> "$n صورة قابلة للاسترجاع" }, "لا توجد نتائج بعد. ابدأ فحصاً حقيقياً.",
        "الوصول إلى الصور", "امنح RecoverX صلاحية قراءة الوسائط حتى يفحص الصور وسلة المهملات التي يتيحها النظام.",
        "منح الوصول", "استرجاع", "تصدير", "تحديد الكل", "إلغاء التحديد", "اللغة", "English", "العربية",
        "الخصوصية", "القيود التقنية", "الإصدار 1.0.0", "استرجاع حقيقي", "لا توجد نتائج مزيفة", "سلة النظام",
        "مكتبة الوسائط", "المجلد المحدد", "صورة مُستخرجة", "اكتمل الاسترجاع", "تم التصدير", "اختر مكان الحفظ",
        "اختر ملف صورة/نسخة تخزين", "الفحص العميق يقرأ ملفاً يختاره المستخدم ويبحث داخله عن تواقيع صور حقيقية.",
        "إلغاء", "إغلاق", "تم رفض الصلاحية", "حدث خطأ أثناء الفحص", "اختيار مجلد", "اختيار ملف التخزين", "جاهز"
    )
} else {
    UiStrings(
        "English", "Real recovery, no fake results", "Home", "Results", "Settings", "Start Scan",
        "Scan Folder", "Deep Scan", "Scans media sources Android actually allows and restores what can be accessed.",
        "Recent Findings", { n -> "$n recoverable photos" }, "No results yet. Start a real scan.",
        "Photo Access", "Allow RecoverX to read media so it can scan photos and system trash exposed by Android.",
        "Grant Access", "Recover", "Export", "Select All", "Clear Selection", "Language", "English", "العربية",
        "Privacy", "Technical Limits", "Version 1.0.0", "Real Recovery", "No Fake Results", "System Trash",
        "Media Library", "Selected Folder", "Carved Image", "Recovery complete", "Exported", "Choose save location",
        "Choose image / storage dump", "Deep Scan reads a user-selected file and carves real image signatures from it.",
        "Cancel", "Close", "Permission denied", "Scan failed", "Choose Folder", "Choose Storage File", "Ready"
    )
}
