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
        ),
        "journal_book" to mapOf(
            "fa" to "دفترچه معاملات",
            "en" to "Trading Journal",
            "ar" to "دفتر الصفقات"
        ),
        "advanced_filter" to mapOf(
            "fa" to "فیلتر پیشرفته",
            "en" to "Advanced Filter",
            "ar" to "تصفية متقدمة"
        ),
        "search_placeholder_adv" to mapOf(
            "fa" to "جستجو در نماد، یادداشت‌ها، تگ‌ها...",
            "en" to "Search symbols, notes, tags...",
            "ar" to "البحث في الرموز، الملاحظات، الوسوم..."
        ),
        "sort_order" to mapOf(
            "fa" to "مرتب‌سازی",
            "en" to "Sort Order",
            "ar" to "الترتيب"
        ),
        "sort_date_desc_text" to mapOf(
            "fa" to "تاریخ (جدید به قدیم)",
            "en" to "Date (New to Old)",
            "ar" to "التاريخ (الأحدث للأقدم)"
        ),
        "sort_date_asc_text" to mapOf(
            "fa" to "تاریخ (قدیم به جدید)",
            "en" to "Date (Old to New)",
            "ar" to "التاريخ (الأقدم للأحدث)"
        ),
        "sort_pnl_desc_text" to mapOf(
            "fa" to "سود (بیشترین به کمترین)",
            "en" to "PnL (High to Low)",
            "ar" to "الربح (من الأعلى للأقل)"
        ),
        "sort_pnl_asc_text" to mapOf(
            "fa" to "سود (کمترین به بیشترین)",
            "en" to "PnL (Low to High)",
            "ar" to "الربح (من الأقل للأعلى)"
        ),
        "advanced_filter_panel" to mapOf(
            "fa" to "فیلترهای پیشرفته معامله",
            "en" to "Advanced Trade Filters",
            "ar" to "تصفية الصفقات المتقدمة"
        ),
        "clear_all" to mapOf(
            "fa" to "پاک کردن همه",
            "en" to "Clear All",
            "ar" to "مسح الكل"
        ),
        "trade_status" to mapOf(
            "fa" to "وضعیت معامله:",
            "en" to "Trade Status:",
            "ar" to "حالة الصفقة:"
        ),
        "win_status" to mapOf(
            "fa" to "برد (WIN)",
            "en" to "WIN",
            "ar" to "ربح (WIN)"
        ),
        "loss_status" to mapOf(
            "fa" to "باخت (LOSS)",
            "en" to "LOSS",
            "ar" to "خسارة (LOSS)"
        ),
        "open_status" to mapOf(
            "fa" to "موقعیت‌های باز",
            "en" to "Open Trades",
            "ar" to "الصفقات المفتوحة"
        ),
        "markets_filter" to mapOf(
            "fa" to "فیلتر بازارها:",
            "en" to "Filter Markets:",
            "ar" to "تصفية الأسواق:"
        ),
        "tags_filter" to mapOf(
            "fa" to "بر اساس استراتژی (برچسب):",
            "en" to "By Strategy (Tag):",
            "ar" to "حسب الاستراتيجية (الوسم):"
        ),
        "date_range" to mapOf(
            "fa" to "بازه زمانی معاملات:",
            "en" to "Trade Date Range:",
            "ar" to "النطاق الزمني للصفقات:"
        ),
        "start_date" to mapOf(
            "fa" to "تاریخ شروع",
            "en" to "Start Date",
            "ar" to "تاريخ البدء"
        ),
        "end_date" to mapOf(
            "fa" to "تاریخ پایان",
            "en" to "End Date",
            "ar" to "تاريخ الانتهاء"
        ),
        "no_trades_match" to mapOf(
            "fa" to "هیچ معامله‌ای با این مشخصات یافت نشد.",
            "en" to "No trades found matching this criteria.",
            "ar" to "لم يتم العثور على صفقات تطابق هذه المعايير."
        ),
        "market_placeholder" to mapOf(
            "fa" to "نماد بازار (مثلا BTC/USDT)",
            "en" to "Market Symbol (e.g., BTC/USDT)",
            "ar" to "رمز السوق (مثل BTC/USDT)"
        ),
        "market_not_found" to mapOf(
            "fa" to "بازاری یافت نشد. برای تعریف سریع کلیک کنید...",
            "en" to "No market found. Click to define quickly...",
            "ar" to "لم يتم العثور على سوق. انقر للتحديد السريع..."
        ),
        "pnl_placeholder" to mapOf(
            "fa" to "مثلا 150.0",
            "en" to "e.g., 150.0",
            "ar" to "مثلاً 150.0"
        ),
        "profit_plus" to mapOf(
            "fa" to "سود (+)",
            "en" to "Profit (+)",
            "ar" to "ربح (+)"
        ),
        "loss_minus" to mapOf(
            "fa" to "زیان (-)",
            "en" to "Loss (-)",
            "ar" to "خسارة (-)"
        ),
        "change_btn" to mapOf(
            "fa" to "تغییر",
            "en" to "Change",
            "ar" to "تغيير"
        ),
        "emotion_calm" to mapOf(
            "fa" to "🟢 آرام",
            "en" to "🟢 Calm",
            "ar" to "🟢 هادئ"
        ),
        "emotion_confident" to mapOf(
            "fa" to "🔵 مطمئن",
            "en" to "🔵 Confident",
            "ar" to "🔵 واثق"
        ),
        "emotion_excited" to mapOf(
            "fa" to "🟡 هیجان‌زده",
            "en" to "🟡 Excited",
            "ar" to "🟡 متحمس"
        ),
        "emotion_anxious" to mapOf(
            "fa" to "🟠 مضطرب",
            "en" to "🟠 Anxious",
            "ar" to "🟠 قلق"
        ),
        "emotion_greedy" to mapOf(
            "fa" to "🔴 حریص",
            "en" to "🔴 Greedy",
            "ar" to "🔴 جشع"
        ),
        "emotion_fearful" to mapOf(
            "fa" to "🟣 ترسیده",
            "en" to "🟣 Fearful",
            "ar" to "🟣 خائف"
        ),
        "rich_notes_header" to mapOf(
            "fa" to "دلایل ورود و یادداشت تفصیلی",
            "en" to "Detailed Entry Reasons & Notes",
            "ar" to "سبب الدخول وملاحظات تفصيلية"
        ),
        "rich_notes_placeholder" to mapOf(
            "fa" to "دلایل ورود، استراتژی تحلیل، احساسات و جزئیات ترید را با قالب‌بندی غنی ثبت کنید...",
            "en" to "Record your entry reasons, analysis strategy, feelings, and trade details using rich formatting...",
            "ar" to "سجل أسباب دخولك، واستراتيجية تحليلك، ومشاعرك وتفاصيل تداولك بتنسيق غني..."
        ),
        "tags_label" to mapOf(
            "fa" to "برچسب‌ها (استراتژی)",
            "en" to "Tags (Strategy)",
            "ar" to "الوسوم (الاستراتيجية)"
        ),
        "new_tag" to mapOf(
            "fa" to "برچسب جدید",
            "en" to "New Tag",
            "ar" to "وسم جديد"
        ),
        "strategy_grade_title" to mapOf(
            "fa" to "استراتژی معاملاتی و نمره‌دهی",
            "en" to "Trading Strategy & Grading",
            "ar" to "استراتيجية التداول والتقييم"
        ),
        "strategy_label" to mapOf(
            "fa" to "استراتژی معاملاتی (مثلاً شکست خط روند یا ICT)",
            "en" to "Trading Strategy (e.g., Trendline Break or ICT)",
            "ar" to "استراتيجية التداول (مثل كسر خط الاتجاه أو ICT)"
        ),
        "setup_grade_label" to mapOf(
            "fa" to "امتیاز (Setup Grade) این موقعیت:",
            "en" to "Setup Grade of this position:",
            "ar" to "تقييم الصفقة لهذا المركز:"
        ),
        "pre_trade_checklist_title" to mapOf(
            "fa" to "تایید چک‌لیست قبل از معامله",
            "en" to "Pre-Trade Checklist Confirmation",
            "ar" to "تأكيد قائمة التحقق قبل التداول"
        ),
        "image_management_title" to mapOf(
            "fa" to "مدیریت تصاویر معامله (قبل، ورود و خروج)",
            "en" to "Trade Images Management (Before, Entry, Exit)",
            "ar" to "إدارة صور الصفقة (قبل، الدخول والخروج)"
        ),
        "image_before_label" to mapOf(
            "fa" to "تصویر قبل از ورود (تحلیل اولیه)",
            "en" to "Before Entry Image (Initial Analysis)",
            "ar" to "صورة قبل الدخول (التحليل الأولي)"
        ),
        "image_entry_label" to mapOf(
            "fa" to "تصویر نقطه ورود (تایید موقعیت)",
            "en" to "Entry Point Image (Position Confirmation)",
            "ar" to "صورة نقطة الدخول (تأكيد المركز)"
        ),
        "image_exit_label" to mapOf(
            "fa" to "تصویر نقطه خروج (نتیجه معامله)",
            "en" to "Exit Point Image (Trade Result)",
            "ar" to "صورة نقطة الخروج (نتيجة الصفقة)"
        ),
        "gallery_btn" to mapOf(
            "fa" to "گالری",
            "en" to "Gallery",
            "ar" to "المعرض"
        ),
        "camera_btn" to mapOf(
            "fa" to "دوربین",
            "en" to "Camera",
            "ar" to "الكاميرا"
        ),
        "post_trade_review_title" to mapOf(
            "fa" to "بررسی بعد از معامله (Post-Trade Review)",
            "en" to "Post-Trade Review",
            "ar" to "مراجعة ما بعد الصفقة (Post-Trade Review)"
        ),
        "post_trade_review_placeholder" to mapOf(
            "fa" to "درس‌های گرفته شده، اشتباهات معاملاتی، مدیریت احساسات...",
            "en" to "Lessons learned, trading mistakes, emotional management...",
            "ar" to "الدروس المستفادة، أخطاء التداول، إدارة العواطف..."
        ),
        "save_position" to mapOf(
            "fa" to "ذخیره موقعیت",
            "en" to "Save Position",
            "ar" to "حفظ المركز"
        ),
        "toast_fill_market" to mapOf(
            "fa" to "لطفاً نماد بازار را مشخص کنید.",
            "en" to "Please specify the market symbol.",
            "ar" to "يرجى تحديد رمز السوق."
        ),
        "toast_fill_volume" to mapOf(
            "fa" to "لطفاً حجم معامله معتبری وارد کنید.",
            "en" to "Please enter a valid volume.",
            "ar" to "يرجى إدخال حجم صفقة صالح."
        ),
        "toast_fill_entry_price" to mapOf(
            "fa" to "لطفاً قیمت ورود معتبری وارد کنید.",
            "en" to "Please enter a valid entry price.",
            "ar" to "يرجى إدخال سعر دخول صالح."
        ),
        "toast_image_attached" to mapOf(
            "fa" to "تصویر با موفقیت ضمیمه شد",
            "en" to "Image attached successfully",
            "ar" to "تم إرفاق الصورة بنجاح"
        ),
        "toast_image_camera" to mapOf(
            "fa" to "تصویر از دوربین ضمیمه شد",
            "en" to "Image attached from camera",
            "ar" to "تم إرفاق الصورة من الكاميرا"
        ),
        "toast_image_error" to mapOf(
            "fa" to "خطا در ضمیمه کردن تصویر:",
            "en" to "Error attaching image:",
            "ar" to "خطأ في إرفاق الصورة:"
        ),
        "add_market_title" to mapOf(
            "fa" to "افزودن بازار جدید",
            "en" to "Add New Market",
            "ar" to "إضافة سوق جديد"
        ),
        "add_market_placeholder" to mapOf(
            "fa" to "مثلا ADA/USDT",
            "en" to "e.g., ADA/USDT",
            "ar" to "مثلاً ADA/USDT"
        ),
        "add_btn" to mapOf(
            "fa" to "افزودن",
            "en" to "Add",
            "ar" to "إضافة"
        ),
        "add_tag_title" to mapOf(
            "fa" to "افزودن برچسب (استراتژی) جدید",
            "en" to "Add New Tag (Strategy)",
            "ar" to "إضافة وسم (استراتيجية) جديد"
        ),
        "add_tag_placeholder" to mapOf(
            "fa" to "مثلا کانال صعودی",
            "en" to "e.g., Ascending Channel",
            "ar" to "مثلاً قناة صاعدة"
        ),
        "strategy_tags" to mapOf(
            "fa" to "برچسب‌های استراتژی:",
            "en" to "Strategy Tags:",
            "ar" to "وسوم الاستراتيجية:"
        ),
        "checklist_confirmed" to mapOf(
            "fa" to "چک‌لیست تایید شده قبل از ورود",
            "en" to "Checklist Confirmed Before Entry",
            "ar" to "قائمة التحقق المؤكدة قبل الدخول"
        ),
        "charts_gallery" to mapOf(
            "fa" to "گالری چارت‌های موقعیت معاملاتی",
            "en" to "Position Charts Gallery",
            "ar" to "معرض مخططات المركز"
        ),
        "zoom_in" to mapOf(
            "fa" to "بزرگ‌نمایی",
            "en" to "Zoom In",
            "ar" to "تكبير"
        ),
        "share_pdf" to mapOf(
            "fa" to "تولید PDF و اشتراک‌گذاری / چاپ گزارش",
            "en" to "Generate PDF, Share & Print",
            "ar" to "إنشاء PDF ومشاركة / طباعة التقرير"
        ),
        "edit_review_title" to mapOf(
            "fa" to "ویرایش بررسی معامله",
            "en" to "Edit Trade Review",
            "ar" to "تعديل مراجعة الصفقة"
        ),
        "edit_review_placeholder" to mapOf(
            "fa" to "مثلا: در این معامله طمع کردم و زود خارج شدم. ریسک به ریوارد مناسب رعایت شد...",
            "en" to "e.g., I got greedy in this trade and exited early. Proper risk-to-reward was maintained...",
            "ar" to "مثلاً: كنت جشعاً في هذه الصفقة وخرجت مبكراً. تم الحفاظ على نسبة المخاطرة إلى العائد المناسبة..."
        ),
        "save_note" to mapOf(
            "fa" to "ثبت یادداشت",
            "en" to "Save Note",
            "ar" to "حفظ الملاحظة"
        ),
        "delete_confirm_desc" to mapOf(
            "fa" to "آیا از حذف کامل این معامله از دفترچه ژورنال خود مطمئن هستید؟ این عمل غیرقابل بازگشت است.",
            "en" to "Are you sure you want to permanently delete this trade from your journal? This action is irreversible.",
            "ar" to "هل أنت متأكد من حذف هذه الصفقة نهائياً من دفتر يومياتك؟ هذا الإجراء لا يمكن التراجع عنه."
        ),
        "yes_delete" to mapOf(
            "fa" to "بله، حذف شود",
            "en" to "Yes, Delete",
            "ar" to "نعم، احذف"
        ),
        "zoom_instructions" to mapOf(
            "fa" to "با دو انگشت بکشید تا بزرگ‌نمایی شود.",
            "en" to "Pinch with two fingers to zoom.",
            "ar" to "اقرص بإصبعين للتكبير."
        ),
        "trade_details_title" to mapOf(
            "fa" to "جزئیات معامله",
            "en" to "Trade Details",
            "ar" to "تفاصيل الصفقة"
        ),
        "trade_type" to mapOf(
            "fa" to "نوع معامله:",
            "en" to "Trade Type:",
            "ar" to "نوع الصفقة:"
        ),
        "open_position" to mapOf(
            "fa" to "تعیین نشده (موقعیت باز)",
            "en" to "Not specified (Open Position)",
            "ar" to "غير محدد (مركز مفتوح)"
        ),
        "trade_time" to mapOf(
            "fa" to "زمان ثبت معامله:",
            "en" to "Trade Time:",
            "ar" to "وقت الصفقة:"
        ),
        "trading_strategy" to mapOf(
            "fa" to "استراتژی معاملاتی:",
            "en" to "Trading Strategy:",
            "ar" to "استراتيجية التداول:"
        ),
        "grade_label" to mapOf(
            "fa" to "امتیاز موقعیت (Grade):",
            "en" to "Setup Grade:",
            "ar" to "تقييم الصفقة:"
        ),
        "chart_before_entry" to mapOf(
            "fa" to "چارت تحلیل قبل از ورود",
            "en" to "Before Entry Analysis Chart",
            "ar" to "مخطط تحليل ما قبل الدخول"
        ),
        "chart_entry_point" to mapOf(
            "fa" to "چارت نقطه ورود و تاییدیه",
            "en" to "Entry Point & Confirmation Chart",
            "ar" to "مخطط نقطة الدخول والتأكيد"
        ),
        "chart_exit_point" to mapOf(
            "fa" to "چارت نقطه خروج و فرجام",
            "en" to "Exit Point & Outcome Chart",
            "ar" to "مخطط نقطة الخروج والنتيجة"
        ),
        "chart_supplementary" to mapOf(
            "fa" to "تصویر تکمیلی چارت",
            "en" to "Supplementary Chart Image",
            "ar" to "صورة المخطط التكميلية"
        ),
        "edit" to mapOf(
            "fa" to "ویرایش",
            "en" to "Edit",
            "ar" to "تعديل"
        ),
        "empty_review" to mapOf(
            "fa" to "هیچ یادداشت و درسی بعد از بسته شدن معامله اضافه نشده است. برای افزودن روی دکمه ادیت بالا بزنید.",
            "en" to "No notes or lessons have been added after closing this trade. Click the edit button above to add some.",
            "ar" to "لم يتم إضافة أي ملاحظات أو دروس بعد إغلاق هذه الصفقة. انقر فوق زر التعديل أعلاه لإضافة بعضها."
        ),
        "generate_pdf_print" to mapOf(
            "fa" to "تولید PDF و اشتراک‌گذاری / چاپ گزارش",
            "en" to "Generate PDF, Share & Print Report",
            "ar" to "إنشاء ملف PDF ومشاركة / طباعة التقرير"
        ),
        "pdf_desc" to mapOf(
            "fa" to "دریافت فایل فرمت شده آماده اشتراک‌گذاری یا چاپ نمودار",
            "en" to "Get a formatted file ready for sharing or printing charts",
            "ar" to "احصل على ملف منسق جاهز للمشاركة أو طباعة المخططات"
        ),
        "share_print_title" to mapOf(
            "fa" to "اشتراک‌گذاری و چاپ گزارش معامله",
            "en" to "Share & Print Trade Report",
            "ar" to "مشاركة وطباعة تقرير الصفقة"
        ),
        "pdf_error" to mapOf(
            "fa" to "خطا در تولید فایل PDF",
            "en" to "Error generating PDF file",
            "ar" to "خطأ في إنشاء ملف PDF"
        ),
        "review_saved" to mapOf(
            "fa" to "بررسی ثبت شد",
            "en" to "Review successfully saved",
            "ar" to "تم حفظ المراجعة بنجاح"
        ),
        "checklist_sl" to mapOf(
            "fa" to "آیا حد ضرر (Stop Loss) مشخص شده است؟",
            "en" to "Is the Stop Loss specified?",
            "ar" to "هل تم تحديد حد الخسارة (Stop Loss)؟"
        ),
        "checklist_tp" to mapOf(
            "fa" to "آیا حد سود (Take Profit) مشخص شده است؟",
            "en" to "Is the Take Profit specified?",
            "ar" to "هل تم تحديد حد الربح (Take Profit)؟"
        ),
        "checklist_risk" to mapOf(
            "fa" to "آیا میزان ریسک معامله بر اساس مدیریت سرمایه است؟",
            "en" to "Is the trade risk aligned with money management?",
            "ar" to "هل حجم مخاطرة الصفقة متوافق مع إدارة رأس المال؟"
        ),
        "checklist_strategy" to mapOf(
            "fa" to "آیا معامله با استراتژی اصلی من همخوانی کامل دارد؟",
            "en" to "Does the trade fully align with my main strategy?",
            "ar" to "هل تتطابق الصفقة تماماً مع استراتيجيتي الرئيسية؟"
        ),
        "checklist_emotions" to mapOf(
            "fa" to "آیا احساس هیجان، طمع یا انتقام در تصمیم من دخیل نیست؟",
            "en" to "Am I free from excitement, greed, or revenge trading?",
            "ar" to "هل أنا خالٍ من مشاعر الحماس، الجشع، أو الرغبة في الانتقام?"
        ),
        "personal_account" to mapOf(
            "fa" to "حساب شخصی",
            "en" to "Personal Account",
            "ar" to "الحساب الشخصي"
        )
    )
}
