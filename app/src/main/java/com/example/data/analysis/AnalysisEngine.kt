package com.example.data.analysis

import com.example.data.model.DailyJournal
import com.example.data.model.Trade
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class InsightType {
    POSITIVE, NEGATIVE, WARNING, INFO
}

data class TradingInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val metricValue: String? = null
)

data class AdvancedStats(
    val winRate: Double,
    val winCount: Int,
    val lossCount: Int,
    val totalTrades: Int,
    val openTradesCount: Int,
    val profitFactor: Double,
    val avgWin: Double,
    val avgLoss: Double,
    val maxDrawdown: Double,
    val bestTradePnl: Double,
    val worstTradePnl: Double,
    val winningStreak: Int,
    val losingStreak: Int,
    val currentWinningStreak: Int,
    val currentLosingStreak: Int,
    val totalProfit: Double,
    val totalLoss: Double,
    val netProfit: Double,
    val winRateByStrategy: Map<String, Double>,
    val winRateBySymbol: Map<String, Double>,
    val winRateByDayOfWeek: Map<Int, Double>, // 1 (Sun) to 7 (Sat)
    val pnlByDayOfWeek: Map<Int, Double>,
    val tradesByDayOfWeek: Map<Int, Int>,
    val longWinRate: Double,
    val shortWinRate: Double,
    val avgRrRatio: Double,
    val disciplineScore: Int,
    val mostCommonMistake: String?,
    val insights: List<TradingInsight>
)

object AnalysisEngine {

    fun analyze(trades: List<Trade>, journals: List<DailyJournal>, lang: String): AdvancedStats {
        val closedTrades = trades.filter { it.exitPrice != null }.sortedBy { it.dateTime }
        val openTrades = trades.filter { it.exitPrice == null }
        
        val totalTrades = closedTrades.size
        val openTradesCount = openTrades.size
        
        var winCount = 0
        var lossCount = 0
        var totalProfit = 0.0
        var totalLoss = 0.0
        var bestTradePnl = 0.0
        var worstTradePnl = 0.0
        
        for (t in closedTrades) {
            val pnl = t.pnl ?: 0.0
            if (pnl >= 0.0) {
                winCount++
                totalProfit += pnl
                if (pnl > bestTradePnl) {
                    bestTradePnl = pnl
                }
            } else {
                lossCount++
                totalLoss += abs(pnl)
                if (pnl < worstTradePnl) {
                    worstTradePnl = pnl
                }
            }
        }
        
        val netProfit = totalProfit - totalLoss
        val winRate = if (totalTrades > 0) (winCount.toDouble() / totalTrades) * 100.0 else 0.0
        val avgWin = if (winCount > 0) totalProfit / winCount else 0.0
        val avgLoss = if (lossCount > 0) totalLoss / lossCount else 0.0
        val profitFactor = if (totalLoss > 0.0) totalProfit / totalLoss else totalProfit
        
        // Max Drawdown (Cash-based)
        var cumulative = 0.0
        var peak = 0.0
        var maxDrawdown = 0.0
        for (t in closedTrades) {
            cumulative += (t.pnl ?: 0.0)
            if (cumulative > peak) {
                peak = cumulative
            }
            val dd = peak - cumulative
            if (dd > maxDrawdown) {
                maxDrawdown = dd
            }
        }
        
        // Streaks
        var winningStreak = 0
        var losingStreak = 0
        var currentWinningStreak = 0
        var currentLosingStreak = 0
        
        var tempWin = 0
        var tempLoss = 0
        for (t in closedTrades) {
            val pnl = t.pnl ?: 0.0
            if (pnl >= 0.0) {
                tempWin++
                tempLoss = 0
                if (tempWin > winningStreak) {
                    winningStreak = tempWin
                }
                currentWinningStreak = tempWin
                currentLosingStreak = 0
            } else {
                tempLoss++
                tempWin = 0
                if (tempLoss > losingStreak) {
                    losingStreak = tempLoss
                }
                currentLosingStreak = tempLoss
                currentWinningStreak = 0
            }
        }
        
        // Grouping
        val cal = Calendar.getInstance()
        val dayGroups = closedTrades.groupBy {
            cal.timeInMillis = it.dateTime
            cal.get(Calendar.DAY_OF_WEEK)
        }
        val winRateByDayOfWeek = dayGroups.mapValues { (_, dayTrades) ->
            val wins = dayTrades.count { (it.pnl ?: 0.0) >= 0.0 }
            (wins.toDouble() / dayTrades.size) * 100.0
        }
        val pnlByDayOfWeek = dayGroups.mapValues { (_, dayTrades) ->
            dayTrades.sumOf { it.pnl ?: 0.0 }
        }
        val tradesByDayOfWeek = dayGroups.mapValues { (_, dayTrades) ->
            dayTrades.size
        }
        
        val strategyGroups = closedTrades.groupBy { it.strategy.ifEmpty { "Other" } }
        val winRateByStrategy = strategyGroups.mapValues { (_, stratTrades) ->
            val wins = stratTrades.count { (it.pnl ?: 0.0) >= 0.0 }
            (wins.toDouble() / stratTrades.size) * 100.0
        }
        
        val symbolGroups = closedTrades.groupBy { it.market }
        val winRateBySymbol = symbolGroups.mapValues { (_, symTrades) ->
            val wins = symTrades.count { (it.pnl ?: 0.0) >= 0.0 }
            (wins.toDouble() / symTrades.size) * 100.0
        }
        
        val longTrades = closedTrades.filter { it.side.uppercase() == "BUY" }
        val shortTrades = closedTrades.filter { it.side.uppercase() == "SELL" }
        val longWinRate = if (longTrades.isNotEmpty()) {
            (longTrades.count { (it.pnl ?: 0.0) >= 0.0 }.toDouble() / longTrades.size) * 100.0
        } else 0.0
        val shortWinRate = if (shortTrades.isNotEmpty()) {
            (shortTrades.count { (it.pnl ?: 0.0) >= 0.0 }.toDouble() / shortTrades.size) * 100.0
        } else 0.0
        
        val avgRrRatio = if (avgLoss > 0.0) avgWin / avgLoss else 0.0
        
        // Mistake Analysis
        val mostCommonMistake = calculateMostCommonMistake(journals, closedTrades, lang)
        
        // Discipline Score
        val disciplineScore = calculateDisciplineScore(closedTrades, avgWin, avgLoss, winRate)
        
        // Insights Generation
        val insights = generateInsights(
            closedTrades = closedTrades,
            longWinRate = longWinRate,
            shortWinRate = shortWinRate,
            winRateByDayOfWeek = winRateByDayOfWeek,
            avgRrRatio = avgRrRatio,
            disciplineScore = disciplineScore,
            mostCommonMistake = mostCommonMistake,
            lang = lang
        )
        
        return AdvancedStats(
            winRate = winRate,
            winCount = winCount,
            lossCount = lossCount,
            totalTrades = totalTrades,
            openTradesCount = openTradesCount,
            profitFactor = profitFactor,
            avgWin = avgWin,
            avgLoss = avgLoss,
            maxDrawdown = maxDrawdown,
            bestTradePnl = bestTradePnl,
            worstTradePnl = worstTradePnl,
            winningStreak = winningStreak,
            losingStreak = losingStreak,
            currentWinningStreak = currentWinningStreak,
            currentLosingStreak = currentLosingStreak,
            totalProfit = totalProfit,
            totalLoss = totalLoss,
            netProfit = netProfit,
            winRateByStrategy = winRateByStrategy,
            winRateBySymbol = winRateBySymbol,
            winRateByDayOfWeek = winRateByDayOfWeek,
            pnlByDayOfWeek = pnlByDayOfWeek,
            tradesByDayOfWeek = tradesByDayOfWeek,
            longWinRate = longWinRate,
            shortWinRate = shortWinRate,
            avgRrRatio = avgRrRatio,
            disciplineScore = disciplineScore,
            mostCommonMistake = mostCommonMistake,
            insights = insights
        )
    }
    
    private fun calculateMostCommonMistake(journals: List<DailyJournal>, closedTrades: List<Trade>, lang: String): String? {
        val mistakeCounts = mutableMapOf<String, Int>()
        
        // Parse mistakes from daily journals
        journals.forEach { j ->
            if (j.mistakes.isNotEmpty()) {
                val splitMistakes = j.mistakes.split(Regex("[,،\n]"))
                splitMistakes.forEach { m ->
                    val trimmed = m.trim()
                    if (trimmed.length > 3) {
                        mistakeCounts[trimmed] = (mistakeCounts[trimmed] ?: 0) + 1
                    }
                }
            }
        }
        
        // Add data-driven rules
        // 1. Overtrading Rule
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val tradesPerDay = closedTrades.groupBy { sdf.format(Date(it.dateTime)) }
        val overtradingDays = tradesPerDay.filter { it.value.size > 5 }.size
        if (overtradingDays > 0) {
            val label = when (lang) {
                "fa" -> "بیش‌معاملاتی (بیش از ۵ معامله در روز)"
                "ar" -> "الإفراط في التداول (أكثر من 5 صفقات يومياً)"
                else -> "Overtrading (More than 5 trades per day)"
            }
            mistakeCounts[label] = (mistakeCounts[label] ?: 0) + (overtradingDays * 2)
        }
        
        // 2. Bad Risk Reward Rule
        val badRrTrades = closedTrades.filter { t ->
            val pnl = t.pnl ?: 0.0
            pnl < 0.0 && abs(pnl) > (t.entryPrice * 0.1) // loss > 10% of entry price
        }.size
        if (badRrTrades > 2) {
            val label = when (lang) {
                "fa" -> "عدم رعایت حد ضرر و ریسک به ریوارد"
                "ar" -> "عدم الالتزام بوقف الخسارة ونسبة المخاطرة"
                else -> "Ignoring Stop Loss & Bad Risk/Reward"
            }
            mistakeCounts[label] = (mistakeCounts[label] ?: 0) + badRrTrades
        }
        
        return mistakeCounts.maxByOrNull { it.value }?.key
    }
    
    private fun calculateDisciplineScore(closedTrades: List<Trade>, avgWin: Double, avgLoss: Double, winRate: Double): Int {
        if (closedTrades.isEmpty()) return 100
        
        var score = 100
        
        // 1. Risk/Reward ratio penalty
        if (avgLoss > 0.0 && avgWin < avgLoss) {
            score -= 15
        }
        
        // 2. Low win-rate penalty
        if (closedTrades.size >= 5) {
            if (winRate < 40.0) score -= 15
            if (winRate < 30.0) score -= 10
        }
        
        // 3. Overtrading penalty
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val tradesPerDay = closedTrades.groupBy { sdf.format(Date(it.dateTime)) }
        val hasOvertraded = tradesPerDay.any { it.value.size > 5 }
        if (hasOvertraded) {
            score -= 15
        }
        
        // 4. Missing strategies penalty
        val noStrategyCount = closedTrades.count { it.strategy.isEmpty() }
        if (noStrategyCount.toDouble() / closedTrades.size > 0.5) {
            score -= 10
        }
        
        // 5. Emotional trading penalty
        val negativeEmotions = listOf("fomo", "greed", "fear", "impatience", "anger", "طمع", "ترس", "انتقام", "عجله")
        val emotionalTrades = closedTrades.count { t ->
            negativeEmotions.any { t.emotionalState.lowercase().contains(it) }
        }
        if (emotionalTrades.toDouble() / closedTrades.size > 0.3) {
            score -= 15
        }
        
        // 6. Checklist adherence bonus
        var checklistAdherenceSum = 0.0
        var checklistTradesCount = 0
        closedTrades.forEach { t ->
            if (t.checklistResults.isNotEmpty()) {
                val items = t.checklistResults.split(",")
                val checkedCount = items.count { it.endsWith(":true") }
                if (items.isNotEmpty()) {
                    checklistAdherenceSum += checkedCount.toDouble() / items.size
                    checklistTradesCount++
                }
            }
        }
        if (checklistTradesCount > 0) {
            val avgAdherence = checklistAdherenceSum / checklistTradesCount
            if (avgAdherence > 0.75) {
                score += 10
            } else if (avgAdherence < 0.40) {
                score -= 10
            }
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun generateInsights(
        closedTrades: List<Trade>,
        longWinRate: Double,
        shortWinRate: Double,
        winRateByDayOfWeek: Map<Int, Double>,
        avgRrRatio: Double,
        disciplineScore: Int,
        mostCommonMistake: String?,
        lang: String
    ): List<TradingInsight> {
        val insights = mutableListOf<TradingInsight>()
        
        if (closedTrades.size < 3) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "نیاز به دیتای بیشتر" else if (lang == "ar") "بحاجة لمزيد من البيانات" else "More Data Needed",
                    description = if (lang == "fa") "برای فعالسازی آنالیز هوشمند هوش مصنوعی، حداقل ۳ معامله بسته شده ثبت کنید."
                                  else if (lang == "ar") "قم بتسجيل 3 صفقات مغلقة على الأقل لتفعيل التحليل الذكي."
                                  else "Log at least 3 closed trades to unlock rule-based AI performance insights.",
                    type = InsightType.INFO
                )
            )
            return insights
        }
        
        // Insight 1: Long vs Short Bias
        val longTradesCount = closedTrades.count { it.side.uppercase() == "BUY" }
        val shortTradesCount = closedTrades.count { it.side.uppercase() == "SELL" }
        if (longTradesCount >= 2 && shortTradesCount >= 2) {
            if (longWinRate > shortWinRate + 15.0) {
                insights.add(
                    TradingInsight(
                        title = if (lang == "fa") "برتری مطلق در معاملات خرید (Long)" else if (lang == "ar") "تفوق صفقات الشراء (Long)" else "Long Position Dominance",
                        description = if (lang == "fa") "نسبت برد شما در معاملات خرید (${String.format(Locale.US, "%.0f", longWinRate)}%) به شکل چشمگیری بهتر از فروش (${String.format(Locale.US, "%.0f", shortWinRate)}%) است. روی روندهای صعودی تمرکز کنید."
                                      else if (lang == "ar") "نسبة نجاحك في الشراء (${String.format(Locale.US, "%.0f", longWinRate)}%) أعلى بكثير من البيع (${String.format(Locale.US, "%.0f", shortWinRate)}%). ركز على الاتجاهات الصاعدة."
                                      else "Your win rate in Long positions (${String.format(Locale.US, "%.0f", longWinRate)}%) is significantly higher than Shorts (${String.format(Locale.US, "%.0f", shortWinRate)}%). Consider prioritizing uptrend setups.",
                        type = InsightType.POSITIVE,
                        metricValue = "+${String.format(Locale.US, "%.0f", longWinRate - shortWinRate)}%"
                    )
                )
            } else if (shortWinRate > longWinRate + 15.0) {
                insights.add(
                    TradingInsight(
                        title = if (lang == "fa") "برتری مطلق در معاملات فروش (Short)" else if (lang == "ar") "تفوق صفقات البيع (Short)" else "Short Position Dominance",
                        description = if (lang == "fa") "نسبت برد شما در معاملات فروش (${String.format(Locale.US, "%.0f", shortWinRate)}%) به شکل چشمگیری بهتر از خرید (${String.format(Locale.US, "%.0f", longWinRate)}%) است. عملکرد شما در شرایط ریزش بازار بهتر است."
                                      else if (lang == "ar") "نسبة نجاحك في البيع (${String.format(Locale.US, "%.0f", shortWinRate)}%) أعلى بكثير من الشراء (${String.format(Locale.US, "%.0f", longWinRate)}%). تداولك أفضل في الأسواق الهابطة."
                                      else "Your win rate in Short positions (${String.format(Locale.US, "%.0f", shortWinRate)}%) is significantly higher than Longs (${String.format(Locale.US, "%.0f", longWinRate)}%). You perform better during bearish market conditions.",
                        type = InsightType.POSITIVE,
                        metricValue = "+${String.format(Locale.US, "%.0f", shortWinRate - longWinRate)}%"
                    )
                )
            }
        }
        
        // Insight 2: Risk-Reward Warning
        if (closedTrades.size >= 5 && avgRrRatio < 1.0) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "زنگ خطر ریسک به ریوارد نامناسب" else if (lang == "ar") "إنذار نسبة المخاطرة إلى العائد" else "Suboptimal Risk-to-Reward Ratio",
                    description = if (lang == "fa") "میانگین سود شما کمتر از میانگین زیان شماست (نسبت: ${String.format(Locale.US, "%.2f", avgRrRatio)}). برای سودآوری مستمر، معاملات بازنده را سریعتر قطع کنید و سودها را بزرگتر بردارید."
                                  else if (lang == "ar") "متوسط أرباحك أقل من متوسط خسائرك (النسبة: ${String.format(Locale.US, "%.2f", avgRrRatio)}). لضمان الربحية المستمرة، اقطع خسائرك بسرعة ودع أرباحك تنمو."
                                  else "Your average win is smaller than your average loss (R:R proxy: ${String.format(Locale.US, "%.2f", avgRrRatio)}). Focus on cutting losses quickly and letting profits run.",
                    type = InsightType.WARNING,
                    metricValue = String.format(Locale.US, "%.2f R:R", avgRrRatio)
                )
            )
        }
        
        // Insight 3: Day-of-week analysis
        // Find if there's any day with winrate < 30% and at least 2 trades
        val weakDay = winRateByDayOfWeek.filter { it.value < 35.0 && (winRateByDayOfWeek.size > 1) && (winRateByDayOfWeek.get(it.key) ?: 0.0) >= 0.0 }
            .minByOrNull { it.value }
        
        if (weakDay != null) {
            val dayName = getDayName(weakDay.key, lang)
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "کاهش راندمان در روز $dayName" else if (lang == "ar") "انخفاض الأداء في يوم $dayName" else "Underperformance on $dayName",
                    description = if (lang == "fa") "داده‌های تاریخی نشان می‌دهد درصد موفقیت شما در روزهای $dayName به شدت پایین (${String.format(Locale.US, "%.0f", weakDay.value)}%) است. شاید استراتژی شما با مومنتوم این روز سازگار نیست."
                                  else if (lang == "ar") "تظهر البيانات أن نسبة نجاحك في يوم $dayName منخفضة جداً (${String.format(Locale.US, "%.0f", weakDay.value)}%). فكر في تقليل حجم الصفقات في هذا اليوم."
                                  else "Historical data indicates your win rate on $dayName drops significantly to ${String.format(Locale.US, "%.0f", weakDay.value)}%. Consider reducing position sizes or being highly selective on this day.",
                    type = InsightType.NEGATIVE,
                    metricValue = "${String.format(Locale.US, "%.0f", weakDay.value)}% Win"
                )
            )
        }
        
        // Insight 4: Discipline Score Insight
        if (disciplineScore >= 85) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "انضباط معاملاتی طلایی" else if (lang == "ar") "انضباط تداول ذهبي" else "Golden Discipline Level",
                    description = if (lang == "fa") "با کسب امتیاز انضباط $disciplineScore، شما به خوبی مدیریت هیجان، مدیریت ریسک و استراتژی را رعایت می‌کنید. به همین روند ادامه دهید."
                                  else if (lang == "ar") "بتحقيق معدل انضباط $disciplineScore، أنت تلتزم بشكل رائع بإدارة المخاطر والتحكم بمشاعرك. استمر في ذلك!"
                                  else "With a stellar discipline score of $disciplineScore, you are strictly following risk management, avoiding overtrading, and executing with poise.",
                    type = InsightType.POSITIVE,
                    metricValue = "$disciplineScore/100"
                )
            )
        } else if (disciplineScore < 60) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "کاهش نمره انضباط" else if (lang == "ar") "تراجع مستوى الانضباط" else "Discipline Warning",
                    description = if (lang == "fa") "نمره انضباط شما به $disciplineScore کاهش یافته است. بیش‌معاملاتی، باز نگه‌داشتن ضررها یا ترید بدون مشخص کردن استراتژی از علل اصلی آن است."
                                  else if (lang == "ar") "تراجع معدل انضباطك إلى $disciplineScore. الإفراط في التداول، عدم تحديد الاستراتيجية، أو التداول العاطفي هي الأسباب الرئيسية."
                                  else "Your discipline score has dipped to $disciplineScore. Overtrading, running losses too deep, or trading without pre-defined strategies are key factors.",
                    type = InsightType.WARNING,
                    metricValue = "$disciplineScore/100"
                )
            )
        }
        
        // Insight 5: Most Common Mistake Insight
        if (!mostCommonMistake.isNullOrEmpty()) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "تمرکز روی رفع بزرگترین نقطه ضعف" else if (lang == "ar") "التركيز على حل أكبر نقاط الضعف" else "Addressing Your Main Vulnerability",
                    description = if (lang == "fa") "خطای اصلی شناسایی شده در معاملات شما: «$mostCommonMistake». تلاش برای فیلتر کردن این اشتباه می‌تواند سودآوری شما را دگرگون کند."
                                  else if (lang == "ar") "الخطأ الرئيسي المكتشف في تداولاتك: «$mostCommonMistake». تصفية هذا الخطأ كافية لرفع أرباحك بشكل كبير."
                                  else "Your primary identified trading mistake is: \"$mostCommonMistake\". Focusing exclusively on eliminating this single error will highly boost performance.",
                    type = InsightType.NEGATIVE
                )
            )
        }
        
        return insights
    }
    
    private fun getDayName(dayOfWeek: Int, lang: String): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> if (lang == "fa") "یکشنبه" else if (lang == "ar") "الأحد" else "Sunday"
            Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else if (lang == "ar") "الاثنين" else "Monday"
            Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else if (lang == "ar") "الثلاثاء" else "Tuesday"
            Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else if (lang == "ar") "الأربعاء" else "Wednesday"
            Calendar.THURSDAY -> if (lang == "fa") "پنجشنبه" else if (lang == "ar") "الخميس" else "Thursday"
            Calendar.FRIDAY -> if (lang == "fa") "جمعه" else if (lang == "ar") "الجمعة" else "Friday"
            Calendar.SATURDAY -> if (lang == "fa") "شنبه" else if (lang == "ar") "السبت" else "Saturday"
            else -> ""
        }
    }
}
