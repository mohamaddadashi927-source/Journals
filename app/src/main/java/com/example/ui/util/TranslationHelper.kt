package com.example.ui.util

object Loc {
    // Current selected language stored in SharedPreferences or ViewModel
    // Can be: "fa" (Persian), "en" (English), "ar" (Arabic)
    
    fun tr(key: String, lang: String): String {
        val map = dictionary[key] ?: return key
        return map[lang] ?: map["fa"] ?: key
    }

    private val dictionary = mapOf(
        "app_name" to mapOf(
            "fa" to "ژورنال معاملاتی",
            "en" to "Trading Journal",
            "ar" to "سجل التداول"
        ),
        "dashboard_title" to mapOf(
            "fa" to "ژورنال معاملاتی",
            "en" to "Trading Journal",
            "ar" to "سجل التداول"
        ),
        "settings_title" to mapOf(
            "fa" to "تنظیمات کاربری",
            "en" to "User Settings",
            "ar" to "إعدادات المستخدم"
        ),
        "app_lang" to mapOf(
            "fa" to "زبان برنامه",
            "en" to "App Language",
            "ar" to "لغة التطبيق"
        ),
        "currency" to mapOf(
            "fa" to "واحد مالی",
            "en" to "Currency",
            "ar" to "العملة"
        ),
        "theme" to mapOf(
            "fa" to "تم برنامه",
            "en" to "App Theme",
            "ar" to "مظهر التطبيق"
        ),
        "theme_system" to mapOf(
            "fa" to "سیستم",
            "en" to "System",
            "ar" to "النظام"
        ),
        "theme_light" to mapOf(
            "fa" to "روشن",
            "en" to "Light",
            "ar" to "فاتح"
        ),
        "theme_dark" to mapOf(
            "fa" to "تاریک",
            "en" to "Dark",
            "ar" to "داكن"
        ),
        "back" to mapOf(
            "fa" to "بازگشت",
            "en" to "Back",
            "ar" to "رجوع"
        ),
        "save" to mapOf(
            "fa" to "ذخیره",
            "en" to "Save",
            "ar" to "حفظ"
        ),
        "cancel" to mapOf(
            "fa" to "انصراف",
            "en" to "Cancel",
            "ar" to "إلغاء"
        ),
        "add_trade" to mapOf(
            "fa" to "ثبت معامله جدید",
            "en" to "Add New Trade",
            "ar" to "إضافة صفقة جديدة"
        ),
        "edit_trade" to mapOf(
            "fa" to "ویرایش معامله",
            "en" to "Edit Trade",
            "ar" to "تعديل الصفقة"
        ),
        "trade_details" to mapOf(
            "fa" to "جزئیات معامله",
            "en" to "Trade Details",
            "ar" to "تفاصيل الصفقة"
        ),
        "trade_list" to mapOf(
            "fa" to "لیست معاملات",
            "en" to "Trade List",
            "ar" to "قائمة الصفقات"
        ),
        "initial_balance_title" to mapOf(
            "fa" to "پیکربندی اولیه حساب",
            "en" to "Account Configuration",
            "ar" to "إعداد الحساب الأولي"
        ),
        "initial_balance_desc" to mapOf(
            "fa" to "لطفاً بالانس اولیه و نام حساب خود را وارد کنید تا محاسبات سود/زیان کل و موجودی حساب بر اساس آن انجام شود.",
            "en" to "Please enter your initial balance and account name to calculate your total profit/loss and current balance.",
            "ar" to "يرجى إدخال الرصيد الأولي واسم حسابك لحساب إجمالي الربح/الخسارة والرصيد الحالي."
        ),
        "initial_balance" to mapOf(
            "fa" to "بالانس اولیه حساب",
            "en" to "Initial Balance",
            "ar" to "الرصيد الأولي"
        ),
        "account_name" to mapOf(
            "fa" to "نام حساب",
            "en" to "Account Name",
            "ar" to "اسم الحساب"
        ),
        "start" to mapOf(
            "fa" to "شروع و پیکربندی",
            "en" to "Start Configuration",
            "ar" to "ابدأ الإعداد"
        ),
        "current_balance" to mapOf(
            "fa" to "بالانس فعلی حساب",
            "en" to "Account Balance",
            "ar" to "رصيد الحساب الحالي"
        ),
        "total_pnl" to mapOf(
            "fa" to "سود و زیان کل حساب",
            "en" to "Account P&L",
            "ar" to "ربح وخسارة الحساب"
        ),
        "winrate" to mapOf(
            "fa" to "وین ریت (نسبت برد)",
            "en" to "Win Rate",
            "ar" to "نسبة الفوز"
        ),
        "total_trades" to mapOf(
            "fa" to "کل معاملات",
            "en" to "Total Trades",
            "ar" to "إجمالي الصفقات"
        ),
        "open_trades" to mapOf(
            "fa" to "معاملات باز",
            "en" to "Open Trades",
            "ar" to "الصفقات المفتوحة"
        ),
        "closed_trades" to mapOf(
            "fa" to "معاملات بسته",
            "en" to "Closed Trades",
            "ar" to "الصفقات المغلقة"
        ),
        "wins" to mapOf(
            "fa" to "معاملات سودده",
            "en" to "Wins",
            "ar" to "الصفقات الرابحة"
        ),
        "losses" to mapOf(
            "fa" to "معاملات زیان‌ده",
            "en" to "Losses",
            "ar" to "الصفقات الخاسرة"
        ),
        "pnl_manual_label" to mapOf(
            "fa" to "ثبت دستی سود یا زیان معامله",
            "en" to "Manual Profit/Loss Entry",
            "ar" to "إدخال يدوي للربح أو الخسارة"
        ),
        "pnl_amount" to mapOf(
            "fa" to "مبلغ سود یا زیان (دستی)",
            "en" to "Profit or Loss Amount",
            "ar" to "مبلغ الربح أو الخسارة"
        ),
        "side" to mapOf(
            "fa" to "جهت معامله",
            "en" to "Side",
            "ar" to "اتجاه الصفقة"
        ),
        "buy" to mapOf(
            "fa" to "خرید (BUY)",
            "en" to "BUY",
            "ar" to "شراء (BUY)"
        ),
        "sell" to mapOf(
            "fa" to "فروش (SELL)",
            "en" to "SELL",
            "ar" to "بيع (SELL)"
        ),
        "profit_btn" to mapOf(
            "fa" to "🟢 معامله سودده",
            "en" to "🟢 Profit (Win)",
            "ar" to "🟢 صفقة رابحة"
        ),
        "loss_btn" to mapOf(
            "fa" to "🔴 معامله زیان‌ده",
            "en" to "🔴 Loss (Loss)",
            "ar" to "🔴 صفقة خاسرة"
        ),
        "fees" to mapOf(
            "fa" to "کارمزد",
            "en" to "Fees",
            "ar" to "الرسوم/العمولة"
        ),
        "volume" to mapOf(
            "fa" to "حجم معامله",
            "en" to "Volume",
            "ar" to "حجم الصفقة"
        ),
        "entry_price" to mapOf(
            "fa" to "قیمت ورود",
            "en" to "Entry Price",
            "ar" to "سعر الدخول"
        ),
        "exit_price" to mapOf(
            "fa" to "قیمت خروج (اختیاری)",
            "en" to "Exit Price (Optional)",
            "ar" to "سعر الخروج (اختياري)"
        ),
        "date_time" to mapOf(
            "fa" to "تاریخ و ساعت",
            "en" to "Date & Time",
            "ar" to "التاريخ والوقت"
        ),
        "rich_notes_label" to mapOf(
            "fa" to "دلایل ورود و یادداشت تفصیلی (قالب‌بندی غنی)",
            "en" to "Detailed Entry Reasons & Notes (Rich formatting)",
            "ar" to "سبب الدخول وملاحظات تفصيلية (تنسيق غني)"
        ),
        "emotional_label" to mapOf(
            "fa" to "حالت روحی شما حین ورود به معامله",
            "en" to "Your Emotional State during entry",
            "ar" to "حالتك العاطفية عند الدخول"
        ),
        "live_preview" to mapOf(
            "fa" to "پیش‌نمایش زنده نوشته غنی شده",
            "en" to "Live Rich Formatting Preview",
            "ar" to "معاينة حية للملاحظات المنسقة"
        ),
        "chart_title" to mapOf(
            "fa" to "نمودار رشد سرمایه و عملکرد حساب",
            "en" to "Equity Growth & Account Balance Chart",
            "ar" to "مخطط نمو رأس المال ورصيد الحساب"
        ),
        "no_chart_data" to mapOf(
            "fa" to "داده‌ای برای رسم نمودار رشد سرمایه وجود ندارد. (معاملات بسته شده نیاز است)",
            "en" to "No closed trades available to draw the equity growth chart.",
            "ar" to "لا توجد صفقات مغلقة لرسم مخطط نمو رأس المال."
        ),
        "tags" to mapOf(
            "fa" to "برچسب‌ها (تگ‌ها)",
            "en" to "Tags",
            "ar" to "الوسوم (التاغات)"
        ),
        "delete_trade" to mapOf(
            "fa" to "حذف معامله",
            "en" to "Delete Trade",
            "ar" to "حذف الصفقة"
        ),
        "delete_confirm" to mapOf(
            "fa" to "آیا از حذف این معامله مطمئن هستید؟",
            "en" to "Are you sure you want to delete this trade?",
            "ar" to "هل أنت متأكد من حذف هذه الصفقة؟"
        ),
        "confirm" to mapOf(
            "fa" to "تایید",
            "en" to "Confirm",
            "ar" to "تأكيد"
        ),
        "market" to mapOf(
            "fa" to "نماد معاملاتی (جفت‌ارز/سهام)",
            "en" to "Trading Asset / Market",
            "ar" to "الرمز المتداول / السوق"
        ),
        "search_placeholder" to mapOf(
            "fa" to "جستجوی جفت‌ارز، تگ‌ها یا یادداشت‌ها...",
            "en" to "Search assets, tags, notes...",
            "ar" to "البحث عن جفت‌ارز، وسوم، ملاحظات..."
        ),
        "sort_by" to mapOf(
            "fa" to "مرتب‌سازی بر اساس",
            "en" to "Sort By",
            "ar" to "ترتيب حسب"
        ),
        "sort_date_desc" to mapOf(
            "fa" to "تاریخ (جدیدترین)",
            "en" to "Date (Newest)",
            "ar" to "التاريخ (الأحدث)"
        ),
        "sort_date_asc" to mapOf(
            "fa" to "تاریخ (قدیمی‌ترین)",
            "en" to "Date (Oldest)",
            "ar" to "التاريخ (الأقدم)"
        ),
        "sort_pnl_desc" to mapOf(
            "fa" to "بیشترین سود",
            "en" to "Highest Profit",
            "ar" to "الأعلى ربحاً"
        ),
        "sort_pnl_asc" to mapOf(
            "fa" to "بیشترین زیان",
            "en" to "Highest Loss",
            "ar" to "الأعلى خسارة"
        ),
        "import_export" to mapOf(
            "fa" to "خروجی و بک‌آپ داده‌ها",
            "en" to "Export & Backup Data",
            "ar" to "تصدير ونسخ البيانات احتياطياً"
        ),
        "export_backup" to mapOf(
            "fa" to "پشتیبان‌گیری (دانلود فایل JSON)",
            "en" to "Backup (Download JSON)",
            "ar" to "نسخ احتياطي (تحميل ملف JSON)"
        ),
        "restore_backup" to mapOf(
            "fa" to "بازیابی پشتیبان (انتخاب فایل JSON)",
            "en" to "Restore Backup (Select JSON)",
            "ar" to "استعادة النسخة الاحتياطية (اختر JSON)"
        ),
        "export_csv" to mapOf(
            "fa" to "خروجی اکسل (دانلود فایل CSV)",
            "en" to "Excel Export (Download CSV)",
            "ar" to "تصدير إلى إكسل (تحميل CSV)"
        ),
        "dangerous_zone" to mapOf(
            "fa" to "بخش خطرناک (ریست کل برنامه)",
            "en" to "Danger Zone (Reset Application)",
            "ar" to "منطقة الخطر (إعادة تعيين التطبيق)"
        ),
        "reset_data" to mapOf(
            "fa" to "پاک کردن کامل تمامی اطلاعات",
            "en" to "Clear All Data",
            "ar" to "مسح كافة البيانات"
        ),
        "reset_confirm_title" to mapOf(
            "fa" to "حذف دائمی کل اطلاعات؟",
            "en" to "Permanently Delete All Data?",
            "ar" to "حذف كافة البيانات نهائياً؟"
        ),
        "reset_confirm_desc" to mapOf(
            "fa" to "آیا از حذف دائمی کل معاملات، مارکت‌ها و تنظیمات حساب خود مطمئن هستید؟ این عمل غیرقابل بازگشت است.",
            "en" to "Are you sure you want to permanently delete all trades, markets, and account configurations? This action is irreversible.",
            "ar" to "هل أنت متأكد من حذف كافة الصفقات والأسواق وإعدادات الحساب نهائياً؟ هذا الإجراء لا يمكن التراجع عنه."
        ),
        "pnl_header" to mapOf(
            "fa" to "سود/زیان معامله",
            "en" to "Trade P&L",
            "ar" to "ربح/خسارة الصفقة"
        )
    )
}
