
package com.example.ui.screens

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.AlertDialog
import com.example.ui.components.BarbergButton as Button
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.BazaarBillingManager
import ir.cafebazaar.poolakey.Connection
import androidx.activity.result.ActivityResultRegistry
import com.example.ui.theme.BarbargSuccess
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.BackupManager
import com.example.util.BackupStorage
import com.example.util.CurrencyFormatter
import java.io.BufferedReader
import java.io.InputStreamReader

private data class GuideTopic(
    val title: String,
    val keywords: String,
    val body: String
)

@Composable
fun SettingsScreen(
    viewModel: BarbargViewModel,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    lockEnabled: Boolean,
    hasPin: Boolean,
    biometricEnabled: Boolean,
    onSavePin: (String) -> Unit,
    onLockEnabledChange: (Boolean) -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardPreferences = remember(context) {
        context.getSharedPreferences("barberg_preferences", android.content.Context.MODE_PRIVATE)
    }
    var bankCards by remember(context) {
        mutableStateOf(
            runCatching {
                val array = JSONArray(cardPreferences.getString("bank_cards_json", "[]"))
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val title = item.optString("title").trim()
                    val number = item.optString("number").filter { it.isDigit() }
                    if (title.isNotBlank() && number.isNotBlank()) title to number else null
                }
            }.getOrDefault(emptyList<Pair<String, String>>())
        )
    }
    var selectedBankCardNumber by remember(context) {
        mutableStateOf(cardPreferences.getString("selected_bank_card_number", "") ?: "")
    }
    var showAddBankCardDialog by remember { mutableStateOf(false) }
    var newBankCardTitle by remember { mutableStateOf("") }
    var newBankCardNumber by remember { mutableStateOf("") }
    var settingsTitleTaps by remember { mutableStateOf(0) }
    var showManagerDialog by remember { mutableStateOf(false) }
    var managerPassword by remember { mutableStateOf("") }
    var managerDialogError by remember { mutableStateOf("") }

    fun saveBankCards(updated: List<Pair<String, String>>) {
        bankCards = updated
        val array = JSONArray()
        updated.forEach { (title, number) ->
            array.put(JSONObject().put("title", title).put("number", number))
        }
        cardPreferences.edit().putString("bank_cards_json", array.toString()).apply()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val subscription by viewModel.observeSubscription().collectAsStateWithLifecycle(initialValue = null)
    val customerCount = customers.size
    val subscriptionLimit = subscription?.customerLimit ?: 5
    val remainingCapacity = (subscriptionLimit - customerCount).coerceAtLeast(0)
    val billingManager = remember(context) { BazaarBillingManager(context) }
    val activity = context as? ComponentActivity
    var billingConnected by remember { mutableStateOf(false) }
    var billingMessage by remember { mutableStateOf("در حال اتصال به بازار…") }
    var billingConnection by remember { mutableStateOf<Connection?>(null) }

    fun applyOwnedPurchases(purchases: List<ir.cafebazaar.poolakey.entity.PurchaseInfo>) {
        val best = purchases.maxByOrNull { BazaarBillingManager.PRODUCT_LIMITS[it.productId] ?: 0 }
        if (best != null) {
            if (viewModel.saveVerifiedPurchase(best)) {
                billingMessage = "خرید معتبر بازار بازیابی شد."
            } else {
                billingMessage = "اطلاعات خرید معتبر نیست؛ ظرفیت تغییر نکرد."
            }
        }
    }

    DisposableEffect(billingManager) {
        billingConnection = billingManager.connect(
            onConnected = {
                billingConnected = true
                billingMessage = "اتصال به بازار برقرار شد."
                billingManager.restore(
                    onResult = { applyOwnedPurchases(it) },
                    onFailure = { billingMessage = "بازیابی خریدها انجام نشد؛ بعداً دوباره تلاش کنید." }
                )
            },
            onFailure = { billingConnected = false; billingMessage = "اتصال به بازار برقرار نشد. برنامهٔ بازار را بررسی کنید." }
        )
        onDispose {
            billingManager.disconnect(billingConnection)
            billingConnection = null
            billingConnected = false
        }
    }

    var showGroupDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf("") }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinFirst by remember { mutableStateOf("") }
    var pinSecond by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf("") }
    var showThemePicker by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var helpQuery by remember { mutableStateOf("") }
    BackHandler(enabled = showThemePicker || showHelpScreen || selectedCategory != null) {
        when { showHelpScreen -> showHelpScreen = false; showThemePicker -> showThemePicker = false; else -> selectedCategory = null }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    pendingRestoreJson = reader.readText()
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHelpScreen) {
            TextButton(onClick = { showHelpScreen = false }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                Text("بازگشت به تنظیمات")
            }
            Text("راهنما", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "راهنمای بخش‌های برنامه. برای پیدا کردن موضوع، نام صفحه یا واژه‌ای مثل بدهی، مشتری، پشتیبان یا تم را جستجو کن.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = helpQuery,
                onValueChange = { helpQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("جستجو در راهنما") },
                placeholder = { Text("مثلاً: ثبت پرداخت، بدهی، پشتیبان") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            val helpTopics = listOf(
                GuideTopic("صفحه اصلی و سربرگ", "صفحه اصلی داشبورد سال مالی تاریخ ماه خلاصه", "در صفحه اصلی، خلاصه وضعیت مالی و دسترسی به بخش‌های مهم دیده می‌شود. سال مالی و ماه انتخاب‌شده را بررسی کن؛ تغییر ماه، اطلاعات ماهانه همان دوره را نمایش می‌دهد. عنوان و تاریخ سربرگ برای تشخیص دوره فعال هستند."),
                GuideTopic("مشتریان", "مشتری افزودن ویرایش حذف جستجو تلفن آدرس", "در بخش مشتریان، دکمه + برای افزودن مشتری است. نام و اطلاعات تماس را وارد کن و ذخیره را بزن. برای باز کردن پرونده، روی کارت مشتری بزن. از منوی هر مشتری می‌توانی ویرایش، تماس یا حذف را انتخاب کنی. حذف مشتری ممکن است مبالغ ماهانه و تراکنش‌های مرتبط را نیز حذف کند؛ پیش از تأیید دقت کن."),
                GuideTopic("گروه‌های مشتریان", "گروه سرگروه عضو تخصیص فیلتر", "از مدیریت گروه‌ها برای ساخت گروه، تعیین نام و رنگ، و اختصاص مشتریان به گروه استفاده کن. در فهرست مشتریان می‌توانی بر اساس گروه فیلتر کنی. قبل از حذف گروه، اعضا و تنظیمات آن را بررسی کن."),
                GuideTopic("ثبت مبلغ ماهانه", "مبلغ ماهانه باربرگ صورتحساب سال ماه", "در بخش مبالغ ماهانه، سال و ماه موردنظر را انتخاب کن و مبلغ هر مشتری را ثبت یا اصلاح کن. پس از ذخیره، مجموع صورتحساب‌ها در بخش‌های مربوط نمایش داده می‌شود. پیش از ویرایش گروهی، دوره مالی انتخاب‌شده را دوباره بررسی کن."),
                GuideTopic("ثبت پرداخت تکی", "پرداخت دریافت وجه پرداخت مشتری مبلغ", "برای ثبت پرداخت یک مشتری، از کارت مشتری یا پرونده او گزینه پرداخت را باز کن. مبلغ را وارد کن و تاریخ، روش پرداخت و توضیحات را در صورت نیاز تکمیل کن. بعد از ذخیره، پرداخت در پرونده و تاریخچه تراکنش‌ها ثبت می‌شود."),
                GuideTopic("پرداخت گروهی", "پرداخت گروهی ثبت پرداخت چند مشتری", "از گزینه پرداخت گروهی برای ثبت پرداخت چند مشتری استفاده کن. هر مبلغ را به مشتری درست نسبت بده و قبل از نهایی‌کردن، مبلغ‌ها و نام‌ها را بازبینی کن. اگر صفحه سفید یا ناقص دیدی، برنامه را نبند؛ ابتدا بازگشت بزن و دوباره وارد شو."),
                GuideTopic("تراکنش‌ها", "تراکنش تاریخچه جستجو ویرایش حذف پیگیری", "در بخش تراکنش‌ها، پرداخت‌های ثبت‌شده همراه با نام مشتری، مبلغ، تاریخ، روش پرداخت و اطلاعات پیگیری نمایش داده می‌شوند. برای پیدا کردن موردی خاص از جستجو استفاده کن. ویرایش یا حذف را فقط پس از بررسی مبلغ و مشتری انجام بده؛ حذف تراکنش روی مانده حساب اثر می‌گذارد."),
                GuideTopic("گزارش‌ها و بدهی", "گزارش بدهی مانده دریافتی صورتحساب بدهکار", "در گزارش‌ها، صورتحساب، دریافتی و مانده بدهی مشتریان را مقایسه کن. مانده مثبت معمولاً نشان‌دهنده بدهی است؛ اگر حساب تسویه یا بستانکار باشد، همان وضعیت نمایش داده می‌شود. خطوط جداکننده برای خواندن ستون‌ها هستند؛ برای تصمیم مالی، نام مشتری و مبلغ را در یک ردیف با هم بررسی کن."),
                GuideTopic("تم‌ها و ظاهر", "تم رنگ شیشه‌ای آبی گلد روشن تاریک HD", "از تنظیمات و بخش تم‌ها، ظاهر دلخواه را انتخاب کن. انتخاب تم ذخیره می‌شود. اگر متنی در یک تم خوانا نبود، تم دیگری را انتخاب یا اندازه نوشته‌ها را از تنظیمات تغییر بده."),
                GuideTopic("اندازه نوشته و ارقام فارسی", "فونت اندازه نوشته اعداد فارسی تومان", "نوار اندازه نوشته‌ها برای کوچک یا بزرگ‌کردن نوشته‌های برنامه است. گزینه نمایش ارقام فارسی، شکل اعداد را تغییر می‌دهد؛ مقدار مالی را تغییر نمی‌دهد. واحد پول نمایش‌داده‌شده را هنگام ثبت و بررسی مبلغ‌ها دقت کن."),
                GuideTopic("پشتیبان‌گیری", "پشتیبان backup ذخیره فایل JSON export", "در تنظیمات، گزینه تهیه نسخه پشتیبان را انتخاب کن و فایل خروجی را در محل امن ذخیره کن. فایل ZIP پروژه یا APK به‌تنهایی جایگزین فایل پشتیبان اطلاعات ثبت‌شده نیست. پشتیبان اطلاعات را جداگانه و منظم تهیه کن و قبل از تغییرات مهم، نسخه جدیدی نگه دار."),
                GuideTopic("بازیابی اطلاعات", "بازیابی restore وارد کردن فایل پشتیبان", "برای بازیابی، فایل پشتیبان JSON را انتخاب کن. اطلاعات فایل به‌صورت رکوردهای جدید اضافه می‌شوند و اطلاعات فعلی حذف یا جایگزین نمی‌شوند. اگر داده‌ای ناسازگار یا تکراری باشد، کل عملیات لغو می‌شود. پیش از بازیابی، یک پشتیبان تازه تهیه کن."),
                GuideTopic("قفل و اثر انگشت", "امنیت رمز PIN اثر انگشت قفل", "برای فعال‌کردن قفل، ابتدا رمز ۴ تا ۶ رقمی تعیین و سپس قفل را روشن کن. ورود با اثر انگشت فقط در گوشی پشتیبانی‌شده و در صورت ثبت اثر انگشت قابل استفاده است. رمز را در محل امن نگه دار."),
                GuideTopic("ارسال پیامک", "پیامک گروهی سیم کارت مجوز ارسال", "برای پیامک گروهی، شماره مشتریان را بررسی کن و مجوزهای موردنیاز را فقط در صورت تمایل بده. متن پیامک قابل ویرایش است و می‌تواند نام، ماه، سال، مبلغ و واحد را جایگزین کند. ارسال پیامک ممکن است هزینه داشته باشد؛ نتیجه تحویل را در برنامه پیامک گوشی بررسی کن."),
                GuideTopic("ورود گروهی مشتریان", "Excel Word فایل ورود مشتری import تکراری", "از گزینه ورود گروهی، فایل پشتیبانی‌شده را انتخاب و پیش‌نمایش را بررسی کن. موارد دارای نام یا شماره مشابه به‌عنوان تکراری احتمالی مشخص می‌شوند. پیش از افزودن همه ردیف‌ها، موارد تکراری را بررسی کن تا مشتری دوبار ثبت نشود."),
                GuideTopic("خروج از برنامه", "خروج برگشت بله خیر", "با دکمه برگشت، در صورت نمایش پنجره تأیید، گزینه خیر برای ادامه کار و گزینه بله برای خروج است. خروج از برنامه نباید جایگزین پشتیبان‌گیری منظم شود."),
                GuideTopic("اگر مشکلی پیش آمد", "مشکل خطا کمک پشتیبانی اطلاعات گم شده", "اگر صفحه‌ای درست نمایش داده نشد، ابتدا به صفحه قبل برگرد و دوباره وارد شو. برنامه را حذف نکن و داده‌های آن را پاک نکن، چون ممکن است اطلاعات محلی از دست برود. پیش از نصب نسخه جدید، از اطلاعات داخل برنامه پشتیبان بگیر و فایل APK سالم قبلی را نگه دار.")
            )
            val matchingTopics = helpTopics.filter { topic ->
                helpQuery.isBlank() || topic.title.contains(helpQuery.trim(), ignoreCase = true) ||
                    topic.keywords.contains(helpQuery.trim(), ignoreCase = true) ||
                    topic.body.contains(helpQuery.trim(), ignoreCase = true)
            }
            if (matchingTopics.isEmpty()) {
                Text("موضوعی پیدا نشد. واژه کوتاه‌تری جستجو کن.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                matchingTopics.forEach { topic ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(topic.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        } else if (showThemePicker) {
            TextButton(onClick = { showThemePicker = false }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                Text("بازگشت به تنظیمات")
            }
            Text("انتخاب پوسته سه‌بعدی برنامه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "پوسته‌های سه‌بعدی با عمق بصری، نورپردازی طبیعی، برجستگی کلیدها و بازتاب شیشه‌ای طراحی شده‌اند. روی هر پوسته ضربه بزنید تا در کل برنامه اعمال شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val selectedBaseTheme = uiState.themeMode.removeSuffix("_3D")
            val themeOptions = listOf(
                com.example.ui.components.ThemeOptionInfo(
                    id = "MIDNIGHT_HD",
                    title = "نیمه‌شب نئونی (پیش‌فرض)",
                    subtitle = "تاریک عمیق با نور آبی نئونی و کارت‌های سه‌بعدی های‌تک",
                    style = com.example.ui.theme.MidnightHd3DStyle,
                    previewPrimary = Color(0xFF2563EB),
                    previewAccent = Color(0xFF38BDF8)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "GLASS_HD",
                    title = "شیشه‌ای آبی (گلس لوکس)",
                    subtitle = "سطوح کریستالی شفاف، امواج سه‌بعدی و بازتاب براق",
                    style = com.example.ui.theme.GlassHd3DStyle,
                    previewPrimary = Color(0xFF0284C7),
                    previewAccent = Color(0xFF73C9FF)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "GOLD_HD",
                    title = "گلد سلطنتی",
                    subtitle = "حس اشرافی طلا و متالیک با سطوح گرم و صیقلی",
                    style = com.example.ui.theme.GoldHd3DStyle,
                    previewPrimary = Color(0xFFD4A72C),
                    previewAccent = Color(0xFFFDE68A)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "EMERALD_HD",
                    title = "زمردی سلطنتی",
                    subtitle = "سبز زمردی لوکس و پرانرژی با نورپردازی یشمی",
                    style = com.example.ui.theme.EmeraldHd3DStyle,
                    previewPrimary = Color(0xFF059669),
                    previewAccent = Color(0xFF6EE7B7)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "ROYAL_PURPLE_HD",
                    title = "بنفش سلطنتی",
                    subtitle = "رنگ یاقوتی و آمتیست با عمق مخملی و لوکس",
                    style = com.example.ui.theme.RoyalPurpleHd3DStyle,
                    previewPrimary = Color(0xFF7C3AED),
                    previewAccent = Color(0xFFC084FC)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "CARBON_HD",
                    title = "کربن تیره (تیتانیوم)",
                    subtitle = "فیبر کربن مات و مقاوم مناسب کارهای اداری و مالی",
                    style = com.example.ui.theme.CarbonHd3DStyle,
                    previewPrimary = Color(0xFF334155),
                    previewAccent = Color(0xFF94A3B8)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "LIGHT",
                    title = "روشن کلاسیک",
                    subtitle = "پوسته روشن تمیز، ملایم و خوانا با سایه‌های نرم",
                    style = com.example.ui.theme.Light3DStyle,
                    previewPrimary = Color(0xFF1D4ED8),
                    previewAccent = Color(0xFF93C5FD)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "DARK",
                    title = "تاریک کلاسیک",
                    subtitle = "پوسته تاریک استاندارد با کنتراست بهینه برای چشم",
                    style = com.example.ui.theme.Dark3DStyle,
                    previewPrimary = Color(0xFF2563EB),
                    previewAccent = Color(0xFF60A5FA)
                ),
                com.example.ui.components.ThemeOptionInfo(
                    id = "SYSTEM",
                    title = "پیروی از حالت گوشی (خودکار)",
                    subtitle = "هماهنگی هوشمند با تم روشن یا تاریک سیستم عامل",
                    style = if (androidx.compose.foundation.isSystemInDarkTheme()) com.example.ui.theme.Dark3DStyle else com.example.ui.theme.Light3DStyle,
                    previewPrimary = Color(0xFF64748B),
                    previewAccent = Color(0xFF94A3B8)
                )
            )

            themeOptions.forEach { option ->
                val isSelected = selectedBaseTheme == option.id
                com.example.ui.components.ThemePreviewCard(
                    option = option,
                    isSelected = isSelected,
                    onSelect = {
                        val targetMode = if (option.id == "SYSTEM") "SYSTEM_3D" else "${option.id}_3D"
                        viewModel.setThemeMode(targetMode)
                    }
                )
            }
        } else {
        if (selectedCategory == null) {
            Text("تنظیمات برنامه باربرگ", modifier = Modifier.clickable {
                settingsTitleTaps++
                if (settingsTitleTaps >= 30) {
                    settingsTitleTaps = 0
                    managerPassword = ""
                    managerDialogError = ""
                    showManagerDialog = true
                }
            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("یک دسته را انتخاب کن تا فقط تنظیمات همان بخش نمایش داده شود.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (uiState.isManager) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("دسترسی مدیر فعال است؛ محدودیت افزودن مشتری برداشته شده است.", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.revokeManagerAccess() }) { Text("لغو دسترسی مدیر") }
                    }
                }
            }
            if (showManagerDialog) {
                AlertDialog(
                    onDismissRequest = { showManagerDialog = false; managerPassword = "" },
                    title = { Text("ورود مدیر") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("رمز اختصاصی مدیر را وارد کن.")
                            OutlinedTextField(value = managerPassword, onValueChange = { managerPassword = it.take(64); managerDialogError = "" }, singleLine = true, label = { Text("رمز مدیر") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                            if (managerDialogError.isNotBlank()) Text(managerDialogError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val success = viewModel.unlockManager(managerPassword)
                            if (success) { showManagerDialog = false; managerPassword = "" }
                            else managerDialogError = "رمز مدیر نادرست است."
                        }) { Text("ورود") }
                    },
                    dismissButton = { TextButton(onClick = { showManagerDialog = false; managerPassword = "" }) { Text("انصراف") } }
                )
            }
            val settingsCategories = listOf(
                Triple("appearance", "ظاهر و نوشته‌ها", "تم‌ها، اندازه فونت و ارقام فارسی"),
                Triple("security", "امنیت", "رمز ورود و اثر انگشت"),
                Triple("finance", "تنظیمات مالی", "واحد پول، سال مالی و گروه‌های مشتریان"),
                Triple("backup", "پشتیبان‌گیری و بازیابی", "ذخیره و بازیابی اطلاعات"),
                Triple("about", "درباره برنامه", "اطلاعات نسخه باربرگ"),
                Triple("help", "راهنما", "راهنمای استفاده از برنامه"),
                Triple("subscription", "اشتراک و خرید بسته‌ها", "مشاهده بسته فعلی و ظرفیت مشتریان")
            )
            settingsCategories.forEach { (key, title, description) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { if (key == "help") { helpQuery = ""; showHelpScreen = true } else selectedCategory = key }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                    }
                }
            }
        } else {
            TextButton(onClick = { selectedCategory = null }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null); Text("بازگشت به دسته‌های تنظیمات") }
            Text(when (selectedCategory) { "appearance" -> "ظاهر و نوشته‌ها"; "security" -> "امنیت"; "finance" -> "تنظیمات مالی"; "backup" -> "پشتیبان‌گیری"; "subscription" -> "اشتراک و خرید بسته‌ها"; else -> "درباره برنامه" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (selectedCategory == "appearance") {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ظاهر و پوسته",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemePicker = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("تم‌ها و ظاهر برنامه", fontWeight = FontWeight.SemiBold)
                            Text(
                                when (uiState.themeMode.removeSuffix("_3D")) {
                                    "LIGHT" -> "روشن"
                                    "DARK" -> "تاریک"
                                    "GLASS_HD" -> "شیشه‌ای آبی"
                                    "GOLD_HD" -> "گلد"
                                    "MIDNIGHT_HD" -> "نیمه‌شب نئونی"
                                    "EMERALD_HD" -> "زمردی سلطنتی"
                                    "ROYAL_PURPLE_HD" -> "بنفش سلطنتی"
                                    "CARBON_HD" -> "کربن تیره"
                                    else -> "پیروی از تم گوشی (خودکار)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "ورود به بخش تم‌ها", tint = MaterialTheme.colorScheme.primary)
                }

                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اندازه نوشته‌های برنامه", fontWeight = FontWeight.SemiBold)
                        Text("${(fontScale * 100).toInt()}٪", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fontScale,
                        onValueChange = onFontScaleChange,
                        valueRange = 0.85f..1.40f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth().testTag("font_size_slider")
                    )
                    Text(
                        "برای کوچک‌تر یا بزرگ‌تر کردن نوشته‌ها، نوار را جابه‌جا کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Numbers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("نمایش ارقام فارسی", fontWeight = FontWeight.SemiBold)
                            Text(
                                "نمایش اعداد به صورت فارسی (۱۲۳)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.usePersianDigits,
                        onCheckedChange = { viewModel.setUsePersianDigits(it) }
                    )
                }
            }
        }

            }

            if (selectedCategory == "security") {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("امنیت برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("قفل با رمز", fontWeight = FontWeight.SemiBold)
                        Text(if (hasPin) "رمز ورود تنظیم شده است" else "برای فعال‌سازی، ابتدا رمز تعیین کنید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = lockEnabled && hasPin, onCheckedChange = { enabled ->
                        if (enabled && !hasPin) { pinFirst = ""; pinSecond = ""; pinDialogError = ""; showPinDialog = true }
                        else onLockEnabledChange(enabled)
                    })
                }
                Button(onClick = { pinFirst = ""; pinSecond = ""; pinDialogError = ""; showPinDialog = true }) {
                    Text(if (hasPin) "تغییر رمز ورود" else "تنظیم رمز ورود")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ورود با اثر انگشت", fontWeight = FontWeight.SemiBold)
                        Text("در صورت پشتیبانی گوشی و ثبت اثر انگشت", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = biometricEnabled, enabled = hasPin && lockEnabled, onCheckedChange = onBiometricEnabledChange)
                }
                Text("قفل فقط هنگام ورود مجدد به برنامه نمایش داده می‌شود و اطلاعات مشتریان و پرداخت‌ها را حذف نمی‌کند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

            }
            if (selectedCategory == "finance") {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تنظیمات مالی و سال حسابداری",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )


                Divider()

                Text(
                    "مدیریت کارت‌های بانکی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "کارت‌ها در تنظیمات برنامه ذخیره می‌شوند و اطلاعات مشتریان و پرداخت‌ها را تغییر نمی‌دهند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                bankCards.forEach { card ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBankCardNumber == card.second,
                            onClick = {
                                selectedBankCardNumber = card.second
                                cardPreferences.edit()
                                    .putString("selected_bank_card_number", card.second)
                                    .apply()
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.first, fontWeight = FontWeight.SemiBold)
                            Text(card.second, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(
                            onClick = {
                                val updated = bankCards.filterNot { it.second == card.second }
                                saveBankCards(updated)
                                if (selectedBankCardNumber == card.second) {
                                    selectedBankCardNumber = updated.firstOrNull()?.second.orEmpty()
                                    cardPreferences.edit()
                                        .putString("selected_bank_card_number", selectedBankCardNumber)
                                        .apply()
                                }
                            }
                        ) {
                            Text("حذف")
                        }
                    }
                }

                Button(
                    onClick = {
                        newBankCardTitle = ""
                        newBankCardNumber = ""
                        showAddBankCardDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("افزودن کارت بانکی")
                }

                if (showAddBankCardDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddBankCardDialog = false },
                        title = { Text("ثبت کارت بانکی") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = newBankCardTitle,
                                    onValueChange = { newBankCardTitle = it },
                                    label = { Text("عنوان کارت (مثلاً کارت اصلی)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newBankCardNumber,
                                    onValueChange = {
                                        newBankCardNumber = it.filter { ch -> ch.isDigit() }.take(16)
                                    },
                                    label = { Text("شماره ۱۶ رقمی کارت") },
                                    supportingText = { Text("${newBankCardNumber.length} از ۱۶ رقم") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = newBankCardTitle.isNotBlank() && newBankCardNumber.length == 16,
                                onClick = {
                                    val updated = bankCards.filterNot { it.second == newBankCardNumber } +
                                        (newBankCardTitle.trim() to newBankCardNumber)
                                    saveBankCards(updated)
                                    if (selectedBankCardNumber.isBlank() ||
                                        updated.none { it.second == selectedBankCardNumber }
                                    ) {
                                        selectedBankCardNumber = newBankCardNumber
                                        cardPreferences.edit()
                                            .putString("selected_bank_card_number", selectedBankCardNumber)
                                            .apply()
                                    }
                                    showAddBankCardDialog = false
                                }
                            ) {
                                Text("ذخیره کارت")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddBankCardDialog = false }) {
                                Text("انصراف")
                            }
                        }
                    )
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("واحد پول", fontWeight = FontWeight.SemiBold)
                            Text(
                                "واحد محاسبه در فاکتورها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = uiState.currencyUnit == "تومان",
                            onClick = { viewModel.setCurrencyUnit("تومان") },
                            label = { Text("تومان") }
                        )
                        FilterChip(
                            selected = uiState.currencyUnit == "ریال",
                            onClick = { viewModel.setCurrencyUnit("ریال") },
                            label = { Text("ریال") }
                        )
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("سال مالی جاری", fontWeight = FontWeight.SemiBold)
                            Text(
                                "مبالغ و صورتحساب‌های فعال",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1402, 1403, 1404, 1405).forEach { year ->
                            FilterChip(
                                selected = uiState.selectedYear == year,
                                onClick = { viewModel.setSelectedYear(year) },
                                label = {
                                    Text(
                                        CurrencyFormatter.toPersianDigits(
                                            year.toString()
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGroupDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "مدیریت گروه‌های مشتریان",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${groups.size} گروه تعریف شده",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = null
                    )
                }
            }
        }

            }
            if (selectedCategory == "backup") {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "پشتیبان‌گیری و بازیابی اطلاعات (کاملاً آفلاین)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "کلیه اطلاعات شامل مشخصات مشتریان، مبالغ ۱۲ ماهه، پرداخت‌ها و گروه‌ها در یک فایل استاندارد JSON ذخیره می‌شوند و بعد از حذف برنامه یا تعویض گوشی به راحتی قابل بازگردانی هستند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.5f
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.exportBackup { json, fileName ->
                                BackupManager.shareBackupFile(
                                    context,
                                    json,
                                    fileName
                                )
                            }
                        }
                        .testTag("export_backup_button")
                
    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "تهیه نسخه پشتیبان (Backup)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "ذخیره فایل و اشتراک در پیام‌رسان‌ها یا حافظه",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = null
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                filePickerLauncher.launch(
                                    arrayOf("application/json", "text/*", "*/*")
                                )
                            } catch (e: Exception) {
                                showPasteJsonDialog = true
                            }
                        }
                        .testTag("restore_backup_button")
                
    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = BarbargSuccess
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "بازیابی اطلاعات (Restore)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "انتخاب فایل پشتیبان از حافظه گوشی",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = null
                        )
                    }
                }
            }
        }

            }
            if (selectedCategory == "subscription") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                
    ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("وضعیت اشتراک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("بسته فعلی: ${subscription?.planCode ?: "FREE"}", style = MaterialTheme.typography.bodyLarge)
                        Text(if (uiState.isManager) "ظرفیت بسته: نامحدود (دسترسی مدیر)" else "ظرفیت بسته: ${subscriptionLimit} مشتری", style = MaterialTheme.typography.bodyMedium)
                        Text("مشتریان ثبت‌شده: $customerCount", style = MaterialTheme.typography.bodyMedium)
                        if (uiState.isManager) {
                            Text("نیازی به خرید یا تمدید اشتراک نیست.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                        Text("ظرفیت باقی‌مانده: $remainingCapacity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text("بسته‌های قابل تهیه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("۲۵ مشتری — ۲۰۰٬۰۰۰ تومان (دائمی)", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = {
                            if (!billingConnected || activity == null) billingMessage = "اتصال به بازار برقرار نیست."
                            else billingManager.purchase(activity.activityResultRegistry, "sara_hora_25", { purchase -> applyOwnedPurchases(listOf(purchase)); billingManager.restore(::applyOwnedPurchases) { billingMessage = "بازیابی خریدها ناموفق بود." } }, { billingMessage = "خرید تأیید نشد: ${it.message ?: "خطا"}" }, { billingMessage = "خرید لغو شد." })
                        }, enabled = billingConnected && activity != null, modifier = Modifier.fillMaxWidth()) { Text("خرید بسته ۲۵ مشتری") }
                        Text("۵۰ مشتری — ۲۹۰٬۰۰۰ تومان (دائمی)", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = {
                            if (!billingConnected || activity == null) billingMessage = "اتصال به بازار برقرار نیست."
                            else billingManager.purchase(activity.activityResultRegistry, "sara_hora_50", { purchase -> applyOwnedPurchases(listOf(purchase)); billingManager.restore(::applyOwnedPurchases) { billingMessage = "بازیابی خریدها ناموفق بود." } }, { billingMessage = "خرید تأیید نشد: ${it.message ?: "خطا"}" }, { billingMessage = "خرید لغو شد." })
                        }, enabled = billingConnected && activity != null, modifier = Modifier.fillMaxWidth()) { Text("خرید بسته ۵۰ مشتری") }
                        Text("۱۰۰ مشتری — ۴۹۰٬۰۰۰ تومان (دائمی)", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = {
                            if (!billingConnected || activity == null) billingMessage = "اتصال به بازار برقرار نیست."
                            else billingManager.purchase(activity.activityResultRegistry, "sara_hora_100", { purchase -> applyOwnedPurchases(listOf(purchase)); billingManager.restore(::applyOwnedPurchases) { billingMessage = "بازیابی خریدها ناموفق بود." } }, { billingMessage = "خرید تأیید نشد: ${it.message ?: "خطا"}" }, { billingMessage = "خرید لغو شد." })
                        }, enabled = billingConnected && activity != null, modifier = Modifier.fillMaxWidth()) { Text("خرید بسته ۱۰۰ مشتری") }
                        Text("نامحدود — ۸۹۰٬۰۰۰ تومان (دائمی)", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = {
                            if (!billingConnected || activity == null) billingMessage = "اتصال به بازار برقرار نیست."
                            else billingManager.purchase(activity.activityResultRegistry, "sara_hora_unlimited", { purchase -> applyOwnedPurchases(listOf(purchase)); billingManager.restore(::applyOwnedPurchases) { billingMessage = "بازیابی خریدها ناموفق بود." } }, { billingMessage = "خرید تأیید نشد: ${it.message ?: "خطا"}" }, { billingMessage = "خرید لغو شد." })
                        }, enabled = billingConnected && activity != null, modifier = Modifier.fillMaxWidth()) { Text("خرید ظرفیت نامحدود") }
                        Text(billingMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("خریدها دائمی‌اند و پس از اتصال مجدد به حساب بازار بازیابی می‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (selectedCategory == "about") {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        
    ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\u0633\u0627\u0645\u0627\u0646\u0647 \u0645\u062f\u06cc\u0631\u06cc\u062a \u067e\u0631\u062f\u0627\u062e\u062a \u0648 \u0645\u0634\u062a\u0631\u06cc\u0627\u0646",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\u0646\u0633\u062e\u0647 ${LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0).versionName ?: "\u0646\u0627\u0645\u0634\u062e\u0635"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "کاملاً مستقل و آفلاین با پایگاه داده امن SQLite و Room، سازگار کامل با تقویم شمسی و مبالغ ماهانه فروردین تا اسفند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

            }
        Spacer(modifier = Modifier.height(20.dp))
        }
        }
    }

    if (showGroupDialog) {
        GroupManagementDialog(
            groups = groups,
            customers = customers,
            onDismiss = { showGroupDialog = false },
            onSaveGroup = { id, name, desc, color, leaderCustomerId ->
                viewModel.saveGroup(id, name, desc, color, leaderCustomerId)
            },
            onDeleteGroup = { group ->
                viewModel.deleteGroup(group)
            },
            onAssignCustomers = { group, customerIds ->
                viewModel.setGroupMembers(group.id, customerIds)
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showPinDialog = false },
            title = { Text("تنظیم رمز ورود") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = pinFirst, onValueChange = { pinFirst = it.filter(Char::isDigit).take(6) }, label = { Text("رمز جدید (۴ تا ۶ رقم)") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword))
                    OutlinedTextField(value = pinSecond, onValueChange = { pinSecond = it.filter(Char::isDigit).take(6) }, label = { Text("تکرار رمز") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword))
                    if (pinDialogError.isNotBlank()) Text(pinDialogError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(onClick = {
                if (pinFirst.length !in 4..6) pinDialogError = "رمز باید ۴ تا ۶ رقم باشد."
                else if (pinFirst != pinSecond) pinDialogError = "دو رمز یکسان نیستند."
                else { onSavePin(pinFirst); showPinDialog = false }
            }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("انصراف") } }
        )
    }

    if (showRestoreConfirmDialog) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = {
                showRestoreConfirmDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = BarbargSuccess
                )
            },
            title = {
                Text("تأیید بازیابی اطلاعات")
            },
            text = {
                Text(
                    text = "اطلاعات فایل پشتیبان به‌صورت رکوردهای جدید به برنامه اضافه می‌شوند. اطلاعات فعلی حذف یا جایگزین نمی‌شوند. اگر فایل دارای اطلاعات ناسازگار یا تکراری باشد، کل عملیات بازیابی لغو خواهد شد. آیا ادامه می‌دهید؟",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(pendingRestoreJson)
                        showRestoreConfirmDialog = false
                    }
                ) {
                    Text(
                        "بازیابی اطلاعات",
                        fontWeight = FontWeight.Bold,
                        color = BarbargSuccess
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
        }
    }

    if (showPasteJsonDialog) {
        var jsonText by remember { mutableStateOf("") }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = {
                showPasteJsonDialog = false
            },
            title = {
                Text("ورود متن پشتیبان")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "متن فایل پشتیبان (JSON) را در این کادر قرار دهید:"
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            viewModel.restoreBackup(jsonText)
                            showPasteJsonDialog = false
                        }
                    }
                ) {
                    Text("بازیابی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasteJsonDialog = false
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}


