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

data class TraderProfile(
    val title: String,
    val description: String,
    val dominantTrait: String,
    val iconName: String,
    val suggestion: String
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
    val insights: List<TradingInsight>,
    val traderProfile: TraderProfile? = null
)

/**
 * Context container containing all precalculated metrics passed to modular rules.
 */
data class RuleContext(
    val closedTrades: List<Trade>,
    val openTrades: List<Trade>,
    val journals: List<DailyJournal>,
    val winCount: Int,
    val lossCount: Int,
    val totalTrades: Int,
    val winRate: Double,
    val avgWin: Double,
    val avgLoss: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val winningStreak: Int,
    val losingStreak: Int,
    val currentWinningStreak: Int,
    val currentLosingStreak: Int,
    val totalProfit: Double,
    val totalLoss: Double,
    val netProfit: Double,
    val winRateByStrategy: Map<String, Double>,
    val winRateBySymbol: Map<String, Double>,
    val winRateByDayOfWeek: Map<Int, Double>,
    val pnlByDayOfWeek: Map<Int, Double>,
    val tradesByDayOfWeek: Map<Int, Int>,
    val longWinRate: Double,
    val shortWinRate: Double,
    val avgRrRatio: Double,
    val disciplineScore: Int,
    val mostCommonMistake: String?,
    val bestTradePnl: Double,
    val worstTradePnl: Double,
    val revengeTradingDetected: Boolean
)

interface InsightRule {
    fun evaluate(context: RuleContext, lang: String): TradingInsight?
}

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
        
        // Check for revenge trading (loss followed by quick reentry on same symbol within 2 hours)
        val revengeTradingDetected = detectRevengeTrading(closedTrades)
        
        // Discipline Score
        val disciplineScore = calculateDisciplineScore(closedTrades, avgWin, avgLoss, winRate, journals, revengeTradingDetected)
        
        val context = RuleContext(
            closedTrades = closedTrades,
            openTrades = openTrades,
            journals = journals,
            winCount = winCount,
            lossCount = lossCount,
            totalTrades = totalTrades,
            winRate = winRate,
            avgWin = avgWin,
            avgLoss = avgLoss,
            profitFactor = profitFactor,
            maxDrawdown = maxDrawdown,
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
            bestTradePnl = bestTradePnl,
            worstTradePnl = worstTradePnl,
            revengeTradingDetected = revengeTradingDetected
        )

        // Generate modular insights
        val insights = generateInsights(context, lang)
        
        // Generate trader personality profile
        val traderProfile = generateTraderProfile(context, lang)
        
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
            insights = insights,
            traderProfile = traderProfile
        )
    }
    
    private fun detectRevengeTrading(closedTrades: List<Trade>): Boolean {
        if (closedTrades.size < 2) return false
        
        for (i in 1 until closedTrades.size) {
            val prev = closedTrades[i - 1]
            val curr = closedTrades[i]
            
            val prevPnl = prev.pnl ?: 0.0
            if (prevPnl < 0.0) { // previous was a loss
                val timeDiffMs = curr.dateTime - prev.dateTime
                val diffMinutes = timeDiffMs / (1000 * 60)
                
                // entered another trade on the same asset within 2 hours
                if (diffMinutes in 1..120 && prev.market == curr.market) {
                    return true
                }
            }
        }
        return false
    }

    private fun calculateMostCommonMistake(journals: List<DailyJournal>, closedTrades: List<Trade>, lang: String): String? {
        val mistakeCounts = mutableMapOf<String, Int>()
        
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
        
        // 1. Overtrading mistake
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
        
        // 2. Bad Risk Reward mistake
        val badRrTrades = closedTrades.filter { t ->
            val pnl = t.pnl ?: 0.0
            pnl < 0.0 && abs(pnl) > (t.entryPrice * 0.1)
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
    
    private fun calculateDisciplineScore(
        closedTrades: List<Trade>,
        avgWin: Double,
        avgLoss: Double,
        winRate: Double,
        journals: List<DailyJournal>,
        revengeTradingDetected: Boolean
    ): Int {
        if (closedTrades.isEmpty()) return 100
        
        var score = 100
        
        if (avgLoss > 0.0 && avgWin < avgLoss) {
            score -= 15
        }
        
        if (closedTrades.size >= 5) {
            if (winRate < 40.0) score -= 15
            if (winRate < 30.0) score -= 10
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val tradesPerDay = closedTrades.groupBy { sdf.format(Date(it.dateTime)) }
        val hasOvertraded = tradesPerDay.any { it.value.size > 5 }
        if (hasOvertraded) {
            score -= 15
        }
        
        if (revengeTradingDetected) {
            score -= 20
        }
        
        val noStrategyCount = closedTrades.count { it.strategy.isEmpty() }
        if (noStrategyCount.toDouble() / closedTrades.size > 0.5) {
            score -= 10
        }
        
        val negativeEmotions = listOf("fomo", "greed", "fear", "impatience", "anger", "طمع", "ترس", "انتقام", "عجله", "furious", "tilt")
        val emotionalTrades = closedTrades.count { t ->
            negativeEmotions.any { t.emotionalState.lowercase().contains(it) }
        }
        if (emotionalTrades.toDouble() / closedTrades.size > 0.3) {
            score -= 15
        }
        
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

    private fun generateInsights(context: RuleContext, lang: String): List<TradingInsight> {
        val insights = mutableListOf<TradingInsight>()
        
        if (context.totalTrades < 3) {
            insights.add(
                TradingInsight(
                    title = if (lang == "fa") "نیاز به دیتای بیشتر" else if (lang == "ar") "بحاجة لمزيد من البيانات" else "More Data Needed",
                    description = if (lang == "fa") "برای فعالسازی آنالیز هوشمند، حداقل ۳ معامله بسته شده ثبت کنید."
                                  else if (lang == "ar") "قم بتسجيل ۳ صفقات مغلقة على الأقل لتفعيل التحليل الذكي."
                                  else "Log at least 3 closed trades to unlock advanced rule-based behavioral insights.",
                    type = InsightType.INFO
                )
            )
            return insights
        }

        // Define modular rule instances
        val rules = listOf(
            LongBiasRule(),
            ShortBiasRule(),
            LowRrRule(),
            WeakDayRule(),
            StrongDayRule(),
            DisciplineGoldRule(),
            DisciplineWarningRule(),
            CommonMistakeRule(),
            SymbolDisasterRule(),
            SymbolGoldmineRule(),
            RevengeTradingRule(),
            OvertradingRule(),
            FomoStateRule(),
            GreedyStateRule(),
            OvertradingAfterLossRule(),
            FridaySlumpRule(),
            WeekendTradingRule(),
            HighChecklistAdherenceRule(),
            LowChecklistAdherenceRule(),
            StopLossNegligenceRule(),
            StreakMomentumRule(),
            TiltWarningRule(),
            HighRiskConcentrationRule(),
            NoStrategyRule()
        )

        for (rule in rules) {
            rule.evaluate(context, lang)?.let { insights.add(it) }
        }

        return insights
    }
    
    private fun generateTraderProfile(context: RuleContext, lang: String): TraderProfile? {
        if (context.totalTrades < 3) return null
        
        val score = context.disciplineScore
        val winRate = context.winRate
        val trades = context.closedTrades
        
        // 1. Calculate Average Trade Frequency per Active Day
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val activeDays = trades.groupBy { sdf.format(Date(it.dateTime)) }
        val avgTradesPerDay = if (activeDays.isNotEmpty()) trades.size.toDouble() / activeDays.size else 0.0
        
        // 2. Checklist adherence
        var totalChecklist = 0
        var checkedChecklist = 0
        trades.forEach { t ->
            if (t.checklistResults.isNotEmpty()) {
                val items = t.checklistResults.split(",")
                totalChecklist += items.size
                checkedChecklist += items.count { it.endsWith(":true") }
            }
        }
        val checklistAdherence = if (totalChecklist > 0) checkedChecklist.toDouble() / totalChecklist else 0.0

        return when {
            // Revenge / FOMO Trader profile
            context.revengeTradingDetected || score < 50 -> {
                TraderProfile(
                    title = if (lang == "fa") "معامله‌گر احساساتی و واکنشی (FOMO / Revenge)" 
                            else if (lang == "ar") "متداول عاطفي وانفعالي (الانتقام/الفومو)" 
                            else "FOMO & Revenge Trader",
                    description = if (lang == "fa") "شما تمایل دارید پس از ضرر، به سرعت وارد معاملات انتقامی شوید یا از روی ترس جاماندن از بازار (FOMO) معامله کنید. انضباط معاملاتی شما نیاز به بازنگری جدی دارد."
                                  else if (lang == "ar") "تميل إلى الدخول في صفقات انتقامية سريعة بعد الخسارة أو التداول بسبب الخوف من فوات الفرصة (FOMO). مستوى انضباطك يحتاج إلى إعادة تقييم."
                                  else "You tend to enter quick emotional trades immediately following a loss, or jump into positions due to fear of missing out (FOMO).",
                    dominantTrait = if (lang == "fa") "ترید احساساتی و شتاب‌زده" else if (lang == "ar") "التداول العاطفي والانفعالي" else "Emotional impulsivity",
                    iconName = "warning",
                    suggestion = if (lang == "fa") "بعد از هر ضرر، حداقل ۲ ساعت چارت را ببندید. هرگز بدون تیک زدن چک‌لیست قبل از ورود، ترید نکنید."
                                 else if (lang == "ar") "أغلق الشاشة لمدة ساعتين على الأقل بعد أي خسارة. لا تتداول أبداً دون تفعيل قائمة مراجعة ما قبل الدخول."
                                 else "Close your trading terminal for at least 2 hours after a losing trade. Practice strict checklist adherence."
                )
            }
            // Aggressive Scalper / Overtrader profile
            avgTradesPerDay > 5.0 -> {
                TraderProfile(
                    title = if (lang == "fa") "اسکالپر پرکار و پرریسک (Aggressive Scalper)" 
                            else if (lang == "ar") "مضارب يومي مفرط (Aggressive Scalper)" 
                            else "Aggressive Scalper & Overtrader",
                    description = if (lang == "fa") "حجم و تعداد معاملات روزانه شما بسیار بالاست (میانگین: ${String.format(Locale.US, "%.1f", avgTradesPerDay)} ترید در روز). این رویکرد ریسک و خستگی ذهنی شما را به شدت افزایش می‌دهد."
                                  else if (lang == "ar") "حجم وتكرار صفقاتك اليومية مرتفع جداً (المتوسط: ${String.format(Locale.US, "%.1f", avgTradesPerDay)} صفقات يومياً). هذا يزيد من التشتت والعمولات."
                                  else "You trade with exceptionally high frequency (average: ${String.format(Locale.US, "%.1f", avgTradesPerDay)} trades per day), which increases commission drag and cognitive fatigue.",
                    dominantTrait = if (lang == "fa") "تعداد بالای معاملات روزانه" else if (lang == "ar") "كثرة الصفقات اليومية" else "High transaction frequency",
                    iconName = "speed",
                    suggestion = if (lang == "fa") "سقف معاملاتی روزانه (حداکثر ۳ ترید) برای خود تعیین کنید و پس از رسیدن به آن، معاملات را متوقف کنید."
                                 else if (lang == "ar") "ضع حداً أقصى لا يتجاوز ۳ صفقات يومياً، وتوقف فوراً بعد الوصول إليه مهما كانت الفرص."
                                 else "Enforce a maximum daily limit of 3 trades. Once reached, close all charts for the day."
                )
            }
            // Disciplined Planner profile
            score >= 80 && checklistAdherence > 0.70 && winRate >= 45.0 -> {
                TraderProfile(
                    title = if (lang == "fa") "معامله‌گر منظم و استراتژیک (Disciplined Planner)" 
                            else if (lang == "ar") "متداول منظم واستراتيجي (Disciplined Planner)" 
                            else "Disciplined & Strategic Planner",
                    description = if (lang == "fa") "آمار نشان می‌دهد شما با انضباط فوق‌العاده بالا ($score/100) ترید می‌کنید، به چک‌لیست وفادار هستید و تصمیمات استراتژیک می‌گیرید. شما الگوی یک تریدر حرفه‌ای هستید."
                                  else if (lang == "ar") "تظهر الإحصاءات أنك تتداول بانضباط عالٍ جداً ($score/100)، وتلتزم بالخطة وتتخذ قرارات استراتيجية مدروسة."
                                  else "You execute with exceptional discipline ($score/100), follow your checklist strictly, and maintain high strategic planning.",
                    dominantTrait = if (lang == "fa") "پایبندی کامل به قوانین" else if (lang == "ar") "الالتزام الكامل بالقوانين" else "Strict rule adherence",
                    iconName = "verified_user",
                    suggestion = if (lang == "fa") "عالی است! همین مسیر منظم را با مدیریت مستمر حجم معاملات برای سود مرکب ادامه دهید."
                                 else if (lang == "ar") "رائع جداً! استمر في هذا المسار مع التركيز على زيادة حجم التداولات تدريجياً لتعظيم العائد."
                                 else "Superb! Maintain this system and slowly compound your account balance by optimizing position sizes."
                )
            }
            // Patient Swing / Sniper
            avgTradesPerDay <= 2.0 && checklistAdherence > 0.60 -> {
                TraderProfile(
                    title = if (lang == "fa") "تریدر صبور و شکارچی موقعیت (Sniper / Patient Swing)" 
                            else if (lang == "ar") "متداول قناص وصبور (Sniper / Swing)" 
                            else "Patient Swing / Sniper",
                    description = if (lang == "fa") "شما بسیار باحوصله هستید، فقط معاملات با کیفیت بالا ثبت می‌کنید (میانگین کمتر از ۲ معامله در روز) و بیهوده سرمایه خود را به خطر نمی‌اندازید."
                                  else if (lang == "ar") "تتميز بالصبر الكبير، ولا تدخل إلا الصفقات عالية الجودة (متوسط أقل من صفقتين يومياً) متجنباً المخاطرة غير الضرورية."
                                  else "You show immense patience, executing selective high-quality setups (less than 2 trades per day) and protecting your trading capital.",
                    dominantTrait = if (lang == "fa") "صبر بالا در انتظار تاییدیه" else if (lang == "ar") "الصبر العالي واختيار الصفقات" else "Patience & high selectivity",
                    iconName = "my_location",
                    suggestion = if (lang == "fa") "همین رویکرد شکارچی‌گونه را حفظ کنید. روی ارتقای نسبت ریسک به ریوارد (R:R) تمرکز کنید تا بازدهی شما چند برابر شود."
                                 else if (lang == "ar") "حافظ على هذا الأسلوب الاستراتيجي. ركز على تحسين نسبة العائد إلى المخاطرة (R:R) لزيادة ربحيتك."
                                 else "Keep hunting premium setups. Optimize your average Risk-to-Reward ratio to further boost returns."
                )
            }
            // General / Learning Trader
            else -> {
                TraderProfile(
                    title = if (lang == "fa") "معامله‌گر در حال رشد و توسعه (Learning / Adaptive)" 
                            else if (lang == "ar") "متداول في مرحلة النمو والتطوير" 
                            else "Adaptive Growing Trader",
                    description = if (lang == "fa") "شما در حال کسب تجربه و تطبیق استراتژی‌ها هستید. آمار شما دارای نقاط قوت مثل پشتکار بالا و پتانسیل پیشرفت بسیار است."
                                  else if (lang == "ar") "أنت في مرحلة ممتازة لبناء المهارات وتطوير استراتيجيتك. تظهر بياناتك إصراراً كبيراً وقدرة عالية على التطور."
                                  else "You are gathering valuable execution data, adapting your tactics, and showing consistent progression in your trading journey.",
                    dominantTrait = if (lang == "fa") "تلاش برای بهبود مستمر" else if (lang == "ar") "السعي للتطوير المستمر" else "Continuous improvement",
                    iconName = "trending_up",
                    suggestion = if (lang == "fa") "سعی کنید تگ‌ها و استراتژی‌های خود را برای هر ترید ثبت کنید تا مربی هوش مصنوعی دقیق‌تر به شما کمک کند."
                                 else if (lang == "ar") "احرص على تسجيل خطتك لكل صفقة حتى يتسنى للمحلل الذكي تقديم توصيات أكثر دقة."
                                 else "Ensure all trades contain explicit strategy classifications to unlock deeper behavioral insights."
                )
            }
        }
    }
}

// ============================================================================
// 24 MODULAR DETAILED OFFLINE RULES IMPLEMENTATIONS
// ============================================================================

class LongBiasRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val buyTrades = context.closedTrades.filter { it.side.uppercase() == "BUY" }
        val sellTrades = context.closedTrades.filter { it.side.uppercase() == "SELL" }
        if (buyTrades.size >= 3 && sellTrades.size >= 3) {
            if (context.longWinRate > context.shortWinRate + 15.0) {
                return TradingInsight(
                    title = if (lang == "fa") "برتری چشمگیر معاملات خرید (Long)" else if (lang == "ar") "أداء ممتاز في صفقات الشراء" else "Long Setup Edge",
                    description = if (lang == "fa") "نسبت برد شما در معاملات خرید (${String.format(Locale.US, "%.0f", context.longWinRate)}%) بسیار بهتر از فروش (${String.format(Locale.US, "%.0f", context.shortWinRate)}%) است. معاملات خرید را ترجیح دهید."
                                  else if (lang == "ar") "نسبة نجاحك في الشراء (${String.format(Locale.US, "%.0f", context.longWinRate)}%) أعلى بوضوح من البيع (${String.format(Locale.US, "%.0f", context.shortWinRate)}%)."
                                  else "Your Long win rate (${String.format(Locale.US, "%.0f", context.longWinRate)}%) is notably higher than Shorts (${String.format(Locale.US, "%.0f", context.shortWinRate)}%). Capitalize on bullish trends.",
                    type = InsightType.POSITIVE,
                    metricValue = "+${String.format(Locale.US, "%.0f", context.longWinRate - context.shortWinRate)}%"
                )
            }
        }
        return null
    }
}

class ShortBiasRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val buyTrades = context.closedTrades.filter { it.side.uppercase() == "BUY" }
        val sellTrades = context.closedTrades.filter { it.side.uppercase() == "SELL" }
        if (buyTrades.size >= 3 && sellTrades.size >= 3) {
            if (context.shortWinRate > context.longWinRate + 15.0) {
                return TradingInsight(
                    title = if (lang == "fa") "برتری چشمگیر معاملات فروش (Short)" else if (lang == "ar") "أداء ممتاز في صفقات البيع" else "Short Setup Edge",
                    description = if (lang == "fa") "نسبت برد شما در معاملات فروش (${String.format(Locale.US, "%.0f", context.shortWinRate)}%) بسیار بالاتر از خرید (${String.format(Locale.US, "%.0f", context.longWinRate)}%) است. عملکرد صعودی خود را فیلتر کنید."
                                  else if (lang == "ar") "نسبة نجاحك في البيع (${String.format(Locale.US, "%.0f", context.shortWinRate)}%) أعلى بكثير من الشراء (${String.format(Locale.US, "%.0f", context.longWinRate)}%)."
                                  else "Your Short win rate (${String.format(Locale.US, "%.0f", context.shortWinRate)}%) significantly outperforms Longs (${String.format(Locale.US, "%.0f", context.longWinRate)}%).",
                    type = InsightType.POSITIVE,
                    metricValue = "+${String.format(Locale.US, "%.0f", context.shortWinRate - context.longWinRate)}%"
                )
            }
        }
        return null
    }
}

class LowRrRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.totalTrades >= 5 && context.avgRrRatio < 1.0) {
            return TradingInsight(
                title = if (lang == "fa") "ناهماهنگی سود به زیان (R:R نامناسب)" else if (lang == "ar") "خلل في نسبة العائد للمخاطرة" else "Suboptimal R:R Ratio",
                description = if (lang == "fa") "متوسط سود شما از متوسط ضررها کمتر است (نسبت: ${String.format(Locale.US, "%.2f", context.avgRrRatio)}). برای رشد بالانس حساب، ضررها را سریع ببندید و اجازه دهید معاملات برنده رشد کنند."
                              else if (lang == "ar") "متوسط أرباحك أقل من خسائرك (النسبة: ${String.format(Locale.US, "%.2f", context.avgRrRatio)}). اقطع الخسائر سريعاً."
                              else "Your average win size is smaller than your average loss size (R:R: ${String.format(Locale.US, "%.2f", context.avgRrRatio)}). Enforce a minimum 1:1.5 or 1:2 R:R.",
                type = InsightType.WARNING,
                metricValue = String.format(Locale.US, "%.2f R:R", context.avgRrRatio)
            )
        }
        return null
    }
}

class WeakDayRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.totalTrades >= 6) {
            val weakDay = context.winRateByDayOfWeek.filter { it.value < 35.0 && (context.tradesByDayOfWeek[it.key] ?: 0) >= 2 }
                .minByOrNull { it.value }
            if (weakDay != null) {
                val dayName = getDayName(weakDay.key, lang)
                return TradingInsight(
                    title = if (lang == "fa") "ضعف عملکرد در روزهای $dayName" else if (lang == "ar") "تراجع الأداء يوم $dayName" else "Low Performance on $dayName",
                    description = if (lang == "fa") "نسبت برد شما در روزهای $dayName معادل ${String.format(Locale.US, "%.0f", weakDay.value)}% است. در این روزها احتیاط بیشتری به خرج دهید یا ترید نکنید."
                                  else if (lang == "ar") "نسبة أرباحك في يوم $dayName منخفضة جداً (${String.format(Locale.US, "%.0f", weakDay.value)}%)."
                                  else "Your win rate on $dayName drops to ${String.format(Locale.US, "%.0f", weakDay.value)}%. Consider sitting on your hands or cutting risk in half on this day.",
                    type = InsightType.NEGATIVE,
                    metricValue = "${String.format(Locale.US, "%.0f", weakDay.value)}% Win"
                )
            }
        }
        return null
    }
    
    private fun getDayName(day: Int, lang: String): String {
        return when (day) {
            Calendar.SUNDAY -> if (lang == "fa") "یکشنبه" else if (lang == "ar") "الأحد" else "Sunday"
            Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else if (lang == "ar") "الاثنين" else "Monday"
            Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else if (lang == "ar") "الثلاثاء" else "Tuesday"
            Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else if (lang == "ar") "الأربعاء" else "Wednesday"
            Calendar.THURSDAY -> if (lang == "fa") "پنجشنبه" else if (lang == "ar") "الخميس" else "Thursday"
            Calendar.FRIDAY -> if (lang == "fa") "جمعه" else if (lang == "ar") "الجمعة" else "Friday"
            Calendar.SATURDAY -> if (lang == "fa") "شنبه" else if (lang == "ar") "السبت" else "Saturday"
            else -> "N/A"
        }
    }
}

class StrongDayRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.totalTrades >= 6) {
            val strongDay = context.winRateByDayOfWeek.filter { it.value > 65.0 && (context.tradesByDayOfWeek[it.key] ?: 0) >= 2 }
                .maxByOrNull { it.value }
            if (strongDay != null) {
                val dayName = getDayName(strongDay.key, lang)
                return TradingInsight(
                    title = if (lang == "fa") "روز طلایی شما: $dayName" else if (lang == "ar") "يومك الذهبي: $dayName" else "Golden Day: $dayName",
                    description = if (lang == "fa") "شما با برد عالی ${String.format(Locale.US, "%.0f", strongDay.value)}% در روزهای $dayName بیشترین بازدهی را دارید. روی موقعیت‌های این روز تمرکز کنید."
                                  else if (lang == "ar") "أداؤك متميز جداً يوم $dayName بنسبة نجاح تفوق ${String.format(Locale.US, "%.0f", strongDay.value)}%."
                                  else "You hit an impressive ${String.format(Locale.US, "%.0f", strongDay.value)}% win rate on $dayName. Your focus is sharpest on this day.",
                    type = InsightType.POSITIVE,
                    metricValue = "${String.format(Locale.US, "%.0f", strongDay.value)}% Win"
                )
            }
        }
        return null
    }

    private fun getDayName(day: Int, lang: String): String {
        return when (day) {
            Calendar.SUNDAY -> if (lang == "fa") "یکشنبه" else if (lang == "ar") "الأحد" else "Sunday"
            Calendar.MONDAY -> if (lang == "fa") "دوشنبه" else if (lang == "ar") "الاثنين" else "Monday"
            Calendar.TUESDAY -> if (lang == "fa") "سه‌شنبه" else if (lang == "ar") "الثلاثاء" else "Tuesday"
            Calendar.WEDNESDAY -> if (lang == "fa") "چهارشنبه" else if (lang == "ar") "الأربعاء" else "Wednesday"
            Calendar.THURSDAY -> if (lang == "fa") "پنجشنبه" else if (lang == "ar") "الخميس" else "Thursday"
            Calendar.FRIDAY -> if (lang == "fa") "جمعه" else if (lang == "ar") "الجمعة" else "Friday"
            Calendar.SATURDAY -> if (lang == "fa") "شنبه" else if (lang == "ar") "السبت" else "Saturday"
            else -> "N/A"
        }
    }
}

class DisciplineGoldRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.disciplineScore >= 85) {
            return TradingInsight(
                title = if (lang == "fa") "انضباط معاملاتی فوق‌العاده" else if (lang == "ar") "مستوى انضباط مثالي" else "Elite Discipline Score",
                description = if (lang == "fa") "امتیاز انضباط شما $ {context.disciplineScore} است. شما به خوبی چک‌لیست‌ها را تایید می‌کنید و از روی هیجان ترید نمی‌کنید. عالی است!"
                              else if (lang == "ar") "معدل انضباطك المتميز ${context.disciplineScore} يوضح مدى احترافيتك."
                              else "With a stellar score of ${context.disciplineScore}, you are adhering strictly to your pre-trade checklist and maintaining structural control.",
                type = InsightType.POSITIVE,
                metricValue = "${context.disciplineScore}/100"
            )
        }
        return null
    }
}

class DisciplineWarningRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.disciplineScore < 60) {
            return TradingInsight(
                title = if (lang == "fa") "زنگ خطر کاهش شدید انضباط" else if (lang == "ar") "تحذير: هبوط مستوى الانضباط" else "Discipline Drop Warning",
                description = if (lang == "fa") "امتیاز انضباط شما به ${context.disciplineScore} سقوط کرده است. ورود بدون استراتژی مشخص، عدم استفاده از چک‌لیست و احساسات شدید علل اصلی هستند."
                              else if (lang == "ar") "تراجع انضباطك إلى ${context.disciplineScore}%. تذكر أن الانضباط يسبق الأرباح."
                              else "Your discipline score dropped to ${context.disciplineScore}. Avoid trading when distracted or emotional, and fill the checklist before entry.",
                type = InsightType.WARNING,
                metricValue = "${context.disciplineScore}/100"
            )
        }
        return null
    }
}

class CommonMistakeRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val mistake = context.mostCommonMistake
        if (!mistake.isNullOrEmpty()) {
            return TradingInsight(
                title = if (lang == "fa") "تمرکز روی بزرگترین خطای مکرر" else if (lang == "ar") "التركيز على حل خطئك المتكرر" else "Focus on Core Vulnerability",
                description = if (lang == "fa") "اشتباه پرتکرار شناسایی شده شما: «$mistake» است. رفع کامل این مورد سودآوری مستمر شما را تضمین می‌کند."
                              else if (lang == "ar") "الخطأ الأكثر تكراراً لديك: «$mistake». تجنب هذا السلوك سيضاعف أرباحك."
                              else "Your most repeated trading error is: \"$mistake\". Design a custom ruleset specifically to combat this mistake.",
                type = InsightType.NEGATIVE
            )
        }
        return null
    }
}

class SymbolDisasterRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val weakSymbol = context.winRateBySymbol.filter { it.value < 35.0 && (context.closedTrades.count { t -> t.market == it.key }) >= 3 }
            .minByOrNull { it.value }
        if (weakSymbol != null) {
            return TradingInsight(
                title = if (lang == "fa") "توقف زیان در نماد ${weakSymbol.key}" else if (lang == "ar") "إنذار خسائر في ${weakSymbol.key}" else "Halt Loss on ${weakSymbol.key}",
                description = if (lang == "fa") "شما در ${weakSymbol.key} درصد برد بسیار ضعیف ${String.format(Locale.US, "%.0f", weakSymbol.value)}% دارید. ترید روی این نماد را متوقف کرده و استراتژی خود را بازبینی کنید."
                              else if (lang == "ar") "تحقق نسبة نجاح متدنية جداً في ${weakSymbol.key} (${String.format(Locale.US, "%.0f", weakSymbol.value)}%)."
                              else "You lose a staggering ${String.format(Locale.US, "%.0f", 100.0 - weakSymbol.value)}% of your ${weakSymbol.key} trades. Consider pausing execution on this asset.",
                type = InsightType.NEGATIVE,
                metricValue = "${String.format(Locale.US, "%.0f", weakSymbol.value)}% Win"
            )
        }
        return null
    }
}

class SymbolGoldmineRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val strongSymbol = context.winRateBySymbol.filter { it.value > 65.0 && (context.closedTrades.count { t -> t.market == it.key }) >= 3 }
            .maxByOrNull { it.value }
        if (strongSymbol != null) {
            return TradingInsight(
                title = if (lang == "fa") "منبع درآمد شما: ${strongSymbol.key}" else if (lang == "ar") "المنجم الخاص بك: ${strongSymbol.key}" else "Your Goldmine: ${strongSymbol.key}",
                description = if (lang == "fa") "درصد برد شما در ${strongSymbol.key} معادل ${String.format(Locale.US, "%.0f", strongSymbol.value)}% است! این نماد کاملاً با روحیه و سیستم معاملاتی شما سازگار است."
                              else if (lang == "ar") "تحقق أرباحاً ممتازة في ${strongSymbol.key} بنسبة نجاح ${String.format(Locale.US, "%.0f", strongSymbol.value)}%."
                              else "Your trades on ${strongSymbol.key} yield an exceptional ${String.format(Locale.US, "%.0f", strongSymbol.value)}% win rate. Prioritize setups on this asset.",
                type = InsightType.POSITIVE,
                metricValue = "${String.format(Locale.US, "%.0f", strongSymbol.value)}% Win"
            )
        }
        return null
    }
}

class RevengeTradingRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.revengeTradingDetected) {
            return TradingInsight(
                title = if (lang == "fa") "شناسایی رفتار ترید انتقامی" else if (lang == "ar") "اكتشاف تداول انتقامي" else "Revenge Trading Detected",
                description = if (lang == "fa") "سیستم رفتارهایی را ثبت کرده که بلافاصله پس از یک معامله زیان‌ده، با حجم مجدد یا شتاب‌زدگی وارد همان بازار شده‌اید. این رفتار قاتل اصلی حساب‌های معاملاتی است."
                              else if (lang == "ar") "لقد دخلت في صفقات سريعة فور الخسارة للانتقام من السوق. هذا يهدد رأس مالك بالإفلاس."
                              else "You entered rapid trades on the same market shortly after a loss, indicating dangerous tilt/revenge trading.",
                type = InsightType.NEGATIVE
            )
        }
        return null
    }
}

class OvertradingRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val activeDays = context.closedTrades.groupBy { sdf.format(Date(it.dateTime)) }
        val avgTrades = if (activeDays.isNotEmpty()) context.closedTrades.size.toDouble() / activeDays.size else 0.0
        if (avgTrades > 4.5) {
            return TradingInsight(
                title = if (lang == "fa") "بیش‌معاملاتی شدید (Overtrading)" else if (lang == "ar") "إفراط شديد في الصفقات (Overtrade)" else "Severe Overtrading Habit",
                description = if (lang == "fa") "شما به طور متوسط ${String.format(Locale.US, "%.1f", avgTrades)} ترید در روز انجام می‌دهید. معاملات خود را کاهش دهید و فقط روی باکیفیت‌ترین‌ها کلیک کنید."
                              else if (lang == "ar") "تتداول بمعدل مرتفع جداً (${String.format(Locale.US, "%.1f", avgTrades)} صفقات يومياً) مما يزيد من تشتتك النفسي."
                              else "You average ${String.format(Locale.US, "%.1f", avgTrades)} trades per day. Filter out lower-probability setups to save commissions and mental bandwidth.",
                type = InsightType.WARNING,
                metricValue = "${String.format(Locale.US, "%.1f", avgTrades)}/Day"
            )
        }
        return null
    }
}

class FomoStateRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val fomoCount = context.closedTrades.count { t ->
            listOf("fomo", "جاماندن", "ترس از جاماندن", "شتاب‌زده").any { t.emotionalState.lowercase().contains(it) }
        }
        val pct = if (context.totalTrades > 0) (fomoCount.toDouble() / context.totalTrades) * 100.0 else 0.0
        if (pct >= 25.0) {
            return TradingInsight(
                title = if (lang == "fa") "سیگنال‌های مکرر ترس از جاماندن (FOMO)" else if (lang == "ar") "مخاطر الخوف من فوات الفرصة (FOMO)" else "Frequent FOMO Trading",
                description = if (lang == "fa") "در حدود ${String.format(Locale.US, "%.0f", pct)}% از معاملات شما به دلیل فومو یا عجله ثبت شده‌اند. به جای تعقیب کندل‌های بزرگ، منتظر پولبک و استراتژی بمانید."
                              else if (lang == "ar") "تتداول بدافع الخوف من فوات الربح بنسبة ${String.format(Locale.US, "%.0f", pct)}% من صفقاتك."
                              else "Approximately ${String.format(Locale.US, "%.0f", pct)}% of your trades are marked as FOMO. Wait for structures to build instead of chasing candle bars.",
                type = InsightType.WARNING,
                metricValue = "${String.format(Locale.US, "%.0f", pct)}%"
            )
        }
        return null
    }
}

class GreedyStateRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val greedCount = context.closedTrades.count { t ->
            listOf("greed", "طمع", "طمع‌کارانه", "بیش‌از‌حد").any { t.emotionalState.lowercase().contains(it) }
        }
        val pct = if (context.totalTrades > 0) (greedCount.toDouble() / context.totalTrades) * 100.0 else 0.0
        if (pct >= 25.0) {
            return TradingInsight(
                title = if (lang == "fa") "هشدار طمع و هضم نکردن سود" else if (lang == "ar") "تحذير: الطمع وعدم جني الأرباح" else "Greed Exposure Alarm",
                description = if (lang == "fa") "طمع در ${String.format(Locale.US, "%.0f", pct)}% از معاملات شما نقش داشته است. سودهای مشخص را برداشت کنید و از جابجا کردن بی‌دلیل تارگت خودداری کنید."
                              else if (lang == "ar") "الطمع يؤثر على صفقاتك بنسبة ${String.format(Locale.US, "%.0f", pct)}%. التزم بالأهداف."
                              else "Greed is present in ${String.format(Locale.US, "%.0f", pct)}% of your trades. Secure partial profits at predetermined key support/resistance levels.",
                type = InsightType.WARNING,
                metricValue = "${String.format(Locale.US, "%.0f", pct)}%"
            )
        }
        return null
    }
}

class OvertradingAfterLossRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val tradesByDay = context.closedTrades.groupBy { sdf.format(Date(it.dateTime)) }
        var overtradingDaysAfterLoss = 0
        tradesByDay.forEach { (_, dayTrades) ->
            if (dayTrades.size >= 4) {
                val firstTrade = dayTrades.firstOrNull()
                if (firstTrade != null && (firstTrade.pnl ?: 0.0) < 0.0) {
                    overtradingDaysAfterLoss++
                }
            }
        }
        if (overtradingDaysAfterLoss >= 2) {
            return TradingInsight(
                title = if (lang == "fa") "بیش‌معاملاتی ناشی از ضرر اول" else if (lang == "ar") "تراكم الخسائر بعد الخسارة الأولى" else "Post-Loss Overtrading Loop",
                description = if (lang == "fa") "شما تمایل دارید روزهایی که اولین ترید آن با باخت همراه است، معاملات زیادی انجام دهید. این چرخه مخرب را با قانون «حداکثر دو باخت روزانه» قطع کنید."
                              else if (lang == "ar") "تميل للإفراط في التداول عندما تخسر صفقتك الأولى في اليوم. حدد خسارتك اليومية بصفقتين فقط."
                              else "You tend to overtrade on days where your first execution is a loss. Enforce a '2-losses-and-done' daily stop rule.",
                type = InsightType.NEGATIVE
            )
        }
        return null
    }
}

class FridaySlumpRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val friWin = context.winRateByDayOfWeek[Calendar.FRIDAY]
        if (friWin != null && friWin < 35.0 && context.winRate > 50.0 && (context.tradesByDayOfWeek[Calendar.FRIDAY] ?: 0) >= 3) {
            return TradingInsight(
                title = if (lang == "fa") "سقوط آزاد بازدهی در روز جمعه" else if (lang == "ar") "انهيار الأداء يوم الجمعة" else "Friday Performance Slump",
                description = if (lang == "fa") "عملکرد کلی شما عالی است، اما در روزهای جمعه درصد برد شما به ${String.format(Locale.US, "%.0f", friWin)}% کاهش می‌یابد. شاید خستگی هفتگی یا کاهش نقدینگی مقصر است."
                              else if (lang == "ar") "تحقق أرباحاً جيدة طوال الأسبوع، لكن يوم الجمعة تهبط نسبة نجاحك إلى ${String.format(Locale.US, "%.0f", friWin)}%."
                              else "Your Friday win rate (${String.format(Locale.US, "%.0f", friWin)}%) underperforms your general average. Consider finishing your week on Thursday night.",
                type = InsightType.NEGATIVE,
                metricValue = "${String.format(Locale.US, "%.0f", friWin)}% Win"
            )
        }
        return null
    }
}

class WeekendTradingRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        val satTrades = context.tradesByDayOfWeek[Calendar.SATURDAY] ?: 0
        val sunTrades = context.tradesByDayOfWeek[Calendar.SUNDAY] ?: 0
        if (satTrades + sunTrades >= 3) {
            val satWin = context.winRateByDayOfWeek[Calendar.SATURDAY] ?: 0.0
            val sunWin = context.winRateByDayOfWeek[Calendar.SUNDAY] ?: 0.0
            val weekendWin = (satWin + sunWin) / 2.0
            if (weekendWin < 40.0) {
                return TradingInsight(
                    title = if (lang == "fa") "ریسک بالای معاملات در آخر هفته" else if (lang == "ar") "مخاطر التداول عطلة نهاية الأسبوع" else "Weekend Low Liquidity Trap",
                    description = if (lang == "fa") "درصد برد شما در تریدهای شنبه و یکشنبه بسیار پایین (${String.format(Locale.US, "%.0f", weekendWin)}%) است. حرکات فیک در کریپتو و بازارهای غیررسمی علت معمول آن است."
                                  else if (lang == "ar") "نسبة نجاح تداولاتك نهاية الأسبوع متدنية (${String.format(Locale.US, "%.0f", weekendWin)}%). تجنب تداولات السبت والأحد."
                                  else "Trading over weekends yields poor returns (${String.format(Locale.US, "%.0f", weekendWin)}% win rate) due to retail traps and lack of institutional volume.",
                    type = InsightType.WARNING,
                    metricValue = "${String.format(Locale.US, "%.0f", weekendWin)}% Win"
                )
            }
        }
        return null
    }
}

class HighChecklistAdherenceRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        var total = 0
        var checked = 0
        var count = 0
        context.closedTrades.forEach { t ->
            if (t.checklistResults.isNotEmpty()) {
                val items = t.checklistResults.split(",")
                total += items.size
                checked += items.count { it.endsWith(":true") }
                count++
            }
        }
        if (count >= 5) {
            val adherence = if (total > 0) (checked.toDouble() / total) * 100.0 else 0.0
            if (adherence >= 80.0) {
                return TradingInsight(
                    title = if (lang == "fa") "پایبندی عالی به چک‌لیست معامله" else if (lang == "ar") "التزام رائع بقائمة المراجعة" else "Checklist Compliance Star",
                    description = if (lang == "fa") "شما با پایبندی ${String.format(Locale.US, "%.0f", adherence)}% به چک‌لیست خود ترید می‌کنید. این سطح انضباط، کلید طلایی ماندگاری در بازار است."
                                  else if (lang == "ar") "تلتزم بنسبة ${String.format(Locale.US, "%.0f", adherence)}% بقائمتك قبل الدخول. استمر على هذا الانضباط."
                                  else "You trade with ${String.format(Locale.US, "%.0f", adherence)}% checklist compliance, filtering out noise and keeping risk tightly controlled.",
                    type = InsightType.POSITIVE,
                    metricValue = "${String.format(Locale.US, "%.0f", adherence)}%"
                )
            }
        }
        return null
    }
}

class LowChecklistAdherenceRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        var total = 0
        var checked = 0
        var count = 0
        context.closedTrades.forEach { t ->
            if (t.checklistResults.isNotEmpty()) {
                val items = t.checklistResults.split(",")
                total += items.size
                checked += items.count { it.endsWith(":true") }
                count++
            }
        }
        if (count >= 5) {
            val adherence = if (total > 0) (checked.toDouble() / total) * 100.0 else 0.0
            if (adherence < 45.0) {
                return TradingInsight(
                    title = if (lang == "fa") "نادیده گرفتن چک‌لیست معاملاتی" else if (lang == "ar") "إهمال قائمة المراجعة" else "Neglected Checklist Alert",
                    description = if (lang == "fa") "شما فقط ${String.format(Locale.US, "%.0f", adherence)}% از شروط چک‌لیست خود را قبل ورود چک می‌کنید. این تریدهای بدون آمادگی، ریسک باخت را بالا می‌برند."
                                  else if (lang == "ar") "تلتزم فقط بنسبة ${String.format(Locale.US, "%.0f", adherence)}% بقائمتك. تداولك يقترب من العشوائية."
                                  else "Your checklist adherence is dangerously low at ${String.format(Locale.US, "%.0f", adherence)}%. Reinforce rules before clicking the buy/sell trigger.",
                    type = InsightType.WARNING,
                    metricValue = "${String.format(Locale.US, "%.0f", adherence)}%"
                )
            }
        }
        return null
    }
}

class StopLossNegligenceRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.lossCount >= 3 && context.worstTradePnl < 0.0) {
            val ratio = abs(context.worstTradePnl) / context.avgLoss
            if (ratio >= 2.8) {
                return TradingInsight(
                    title = if (lang == "fa") "هشدار فاجعه تک‌معامله (بدون Stop Loss)" else if (lang == "ar") "مخاطر الخسارة الكبرى (إهمال الوقف)" else "Stop Loss Negligence Detected",
                    description = if (lang == "fa") "بزرگترین باخت شما ${String.format(Locale.US, "%.1f", ratio)} برابر بزرگتر از میانگین ضررهایتان است! عدم قرار دادن حد ضرر یا جابجا کردن آن در جهت زیان، این فاجعه را ایجاد می‌کند."
                                  else if (lang == "ar") "خسارتك الكبرى تتجاوز متوسط خسائرك بـ ${String.format(Locale.US, "%.1f", ratio)} ضعفاً. ضع وقف الخسارة دائماً."
                                  else "Your single worst loss is ${String.format(Locale.US, "%.1f", ratio)}x larger than your average loss, indicating stop-loss avoidance or manually dragging it down.",
                    type = InsightType.WARNING,
                    metricValue = "${String.format(Locale.US, "%.1f", ratio)}x Avg"
                )
            }
        }
        return null
    }
}

class StreakMomentumRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.currentWinningStreak >= 4) {
            return TradingInsight(
                title = if (lang == "fa") "روی دورِ برد مستمر (تسلط ذهنی)" else if (lang == "ar") "سلسلة أرباح متتالية (تركيز عالٍ)" else "Hot Winning Streak",
                description = if (lang == "fa") "شما در حال حاضر ${context.currentWinningStreak} معامله سودده متوالی ثبت کرده‌اید. ذهن شما کاملاً با بازار هماهنگ است، مغرور نشوید و قوانین مدیریت ریسک را حفظ کنید."
                              else if (lang == "ar") "أنت في سلسلة ربح ممتازة من ${context.currentWinningStreak} صفقات متتالية. التزم بالخطة وتجنب الغرور."
                              else "You are on a beautiful winning streak of ${context.currentWinningStreak} consecutive trades. Keep executing with poise and stay humble.",
                type = InsightType.POSITIVE,
                metricValue = "${context.currentWinningStreak} Wins"
            )
        }
        return null
    }
}

class TiltWarningRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.currentLosingStreak >= 3) {
            return TradingInsight(
                title = if (lang == "fa") "هشدار تیلت (افتادن در لوپ باخت)" else if (lang == "ar") "تحذير من الغضب والاحباط (Tilt)" else "Tilt Alert! Take a Break",
                description = if (lang == "fa") "شما دچار ${context.currentLosingStreak} باخت متوالی پشت سر هم شده‌اید. برای جلوگیری از عصبانیت و نابودی بالانس، پلتفرم معاملاتی را خاموش کرده و استراحت کنید."
                              else if (lang == "ar") "تعرضت لـ ${context.currentLosingStreak} خسائر متتالية. خذ استراحة فوراً لاستعادة توازنك النفسي."
                              else "You have suffered ${context.currentLosingStreak} consecutive losses. Step away from the screen to reset your cognitive emotional state.",
                type = InsightType.WARNING,
                metricValue = "${context.currentLosingStreak} Losses"
            )
        }
        return null
    }
}

class HighRiskConcentrationRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.closedTrades.size >= 5) {
            val avgVol = context.closedTrades.map { it.volume }.average()
            val maxVol = context.closedTrades.map { it.volume }.maxOrNull() ?: 0.0
            if (avgVol > 0.0 && maxVol / avgVol >= 3.5) {
                return TradingInsight(
                    title = if (lang == "fa") "توزیع ریسک نامتعادل و ناگهانی" else if (lang == "ar") "توزيع حجم مخاطرة غير متوازن" else "Risk Concentration Spike",
                    description = if (lang == "fa") "شما معاملاتی انجام داده‌اید که حجم آن بیش از ۳.۵ برابر حجم معمول شماست. این نوسان ناگهانی حجم ترید، شانس موفقیت حساب شما را کاهش می‌دهد."
                                  else if (lang == "ar") "تتداول أحياناً بأحجام ضخمة تفوق متوسطك بـ ۳.۵ ضعفاً. وحد حجم مخاطرتك."
                                  else "Your maximum trade volume is ${String.format(Locale.US, "%.1f", maxVol / avgVol)}x larger than your average volume, exposing you to severe tail risk.",
                    type = InsightType.WARNING,
                    metricValue = "${String.format(Locale.US, "%.1f", maxVol / avgVol)}x Vol"
                )
            }
        }
        return null
    }
}

class NoStrategyRule : InsightRule {
    override fun evaluate(context: RuleContext, lang: String): TradingInsight? {
        if (context.totalTrades >= 4) {
            val emptyStrat = context.closedTrades.count { it.strategy.trim().isEmpty() }
            val pct = (emptyStrat.toDouble() / context.totalTrades) * 100.0
            if (pct > 50.0) {
                return TradingInsight(
                    title = if (lang == "fa") "ضعف در دسته‌بندی استراتژی" else if (lang == "ar") "عدم تصنيف الاستراتيجيات" else "Missing Strategy Classifications",
                    description = if (lang == "fa") "حدود ${String.format(Locale.US, "%.0f", pct)}% از معاملات شما بدون ثبت استراتژی ذخیره شده‌اند. برای آنالیز دقیق، حتماً نام استراتژی استفاده شده را ثبت کنید."
                                  else if (lang == "ar") "تتداول بدون تصنيف استراتيجيتك بنسبة ${String.format(Locale.US, "%.0f", pct)}%."
                                  else "Over ${String.format(Locale.US, "%.0f", pct)}% of your entries lack an explicit strategy classification. Tag them to help the AI optimize your setup metrics.",
                    type = InsightType.WARNING,
                    metricValue = "${String.format(Locale.US, "%.0f", pct)}%"
                )
            }
        }
        return null
    }
}
