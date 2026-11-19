package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Customer
import com.example.ui.components.BarbargBottomNav
import com.example.ui.screens.BatchPaymentDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IndividualPaymentDialog
import com.example.ui.screens.MonthlyChargesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsNotificationsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.BarbargTheme
import com.example.ui.theme.LocalBarbarg3DStyle
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BarbargViewModel
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarbargApp()
        }
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0f)
        window.attributes = window.attributes.apply {
            alpha = 1f
        }
    }
}

@Composable
fun BarbargApp(viewModel: BarbargViewModel = viewModel()) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("barberg_preferences", android.content.Context.MODE_PRIVATE) }
    var fontScale by remember { mutableFloatStateOf(preferences.getFloat("font_scale", 1f).coerceIn(0.85f, 1.40f)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val customersListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    var lockEnabled by remember { mutableStateOf(preferences.getBoolean("app_lock_enabled", false)) }
    var biometricEnabled by remember { mutableStateOf(preferences.getBoolean("app_biometric_enabled", false)) }
    var hasPin by remember { mutableStateOf(!preferences.getString("app_pin_hash", null).isNullOrBlank()) }
    var isLocked by remember { mutableStateOf(lockEnabled && hasPin) }
    var pinEntry by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lockEnabled, hasPin) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && lockEnabled && hasPin) {
                isLocked = true
                pinEntry = ""
                pinError = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dialog states
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentTargetCustomerId by remember { mutableStateOf<Long?>(null) }
    var showBatchPaymentDialog by remember { mutableStateOf(false) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showSmsNotifications by remember { mutableStateOf(false) }
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showSmsNotifications = true
    }
    val startupPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }
    LaunchedEffect(Unit) {
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_SMS)
            if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missing.isNotEmpty()) startupPermissionLauncher.launch(missing.toTypedArray())
    }

    BackHandler(enabled = !isLocked) {
        when {
            showSmsNotifications -> showSmsNotifications = false
            uiState.selectedCustomerDetailId != null -> viewModel.closeCustomerDetail()
            uiState.currentScreen != AppScreen.DASHBOARD -> viewModel.setScreen(AppScreen.DASHBOARD)
            else -> showExitConfirmation = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val baseThemeMode = uiState.themeMode.removeSuffix("_3D")
    val isDarkTheme = when (baseThemeMode) {
        "LIGHT", "GLASS_HD", "GOLD_HD", "EMERALD_HD", "ROYAL_PURPLE_HD", "BLUE_GLOSS_HD" -> false
        "DARK", "MIDNIGHT_HD", "CARBON_HD" -> true
        else -> isSystemInDarkTheme()
    }

    BarbargTheme(darkTheme = isDarkTheme, themeMode = uiState.themeMode, fontScale = fontScale) {
        val style3D = LocalBarbarg3DStyle.current

        if (isLocked && lockEnabled && hasPin) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(brush = style3D.backgroundBrush) }
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("قفل برنامه باربرگ", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(8.dp))
                OutlinedTextField(
                    value = pinEntry,
                    onValueChange = { pinEntry = it.filter(Char::isDigit).take(6); pinError = "" },
                    label = { Text("رمز عددی ۴ تا ۶ رقمی") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (pinError.isNotBlank()) Text(pinError, color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val salt = preferences.getString("app_pin_salt", "") ?: ""
                    val expected = preferences.getString("app_pin_hash", "") ?: ""
                    if (pinEntry.length in 4..6 && pinHash(pinEntry, salt) == expected) {
                        isLocked = false; pinEntry = ""; pinError = ""
                    } else pinError = "رمز ورود صحیح نیست."
                }, modifier = Modifier.padding(top = 12.dp)) { Text("ورود") }
                if (biometricEnabled) {
                    Button(onClick = {
                        val activity = context as? FragmentActivity
                        if (activity == null) { pinError = "امکان باز کردن قفل با اثر انگشت وجود ندارد." }
                        else {
                            val manager = BiometricManager.from(context)
                            if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
                                pinError = "اثر انگشت در گوشی تنظیم نشده است؛ با رمز وارد شوید."
                            } else {
                                val prompt = BiometricPrompt(activity, activity.mainExecutor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            isLocked = false; pinEntry = ""; pinError = ""
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            pinError = errString.toString()
                                        }
                                    })
                                val info = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("باز کردن قفل باربرگ")
                                    .setSubtitle("با اثر انگشت وارد شوید")
                                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                    .setNegativeButtonText("استفاده از رمز")
                                    .build()
                                prompt.authenticate(info)
                            }
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("ورود با اثر انگشت") }
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                            Snackbar(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                containerColor = Color(0xFF263238),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = snackbarData.visuals.message,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White,
                                    textAlign = TextAlign.Right,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    if (uiState.selectedCustomerDetailId == null && !showSmsNotifications) {
                        BarbargBottomNav(
                            currentScreen = uiState.currentScreen,
                            onScreenSelected = { screen -> viewModel.setScreen(screen) }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // 1. Theme-specific ambient backdrop gradient
                        drawRect(brush = style3D.backgroundBrush)

                        // 2. Realistic 3D ambient waves with subtle highlights
                        if (baseThemeMode == "GLASS_HD" || baseThemeMode == "BLUE_GLOSS_HD") {
                            val upperWave = Path().apply {
                                moveTo(0f, size.height * 0.22f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.08f,
                                    size.width * 0.58f, size.height * 0.38f,
                                    size.width, size.height * 0.17f
                                )
                                lineTo(size.width, 0f)
                                lineTo(0f, 0f)
                                close()
                            }
                            drawPath(
                                upperWave,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                                    endY = size.height * 0.30f
                                )
                            )

                            val middleWave = Path().apply {
                                moveTo(0f, size.height * 0.52f)
                                cubicTo(
                                    size.width * 0.30f, size.height * 0.35f,
                                    size.width * 0.68f, size.height * 0.68f,
                                    size.width, size.height * 0.43f
                                )
                                lineTo(size.width, size.height * 0.68f)
                                cubicTo(
                                    size.width * 0.62f, size.height * 0.86f,
                                    size.width * 0.28f, size.height * 0.57f,
                                    0f, size.height * 0.76f
                                )
                                close()
                            }
                            drawPath(
                                middleWave,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFBCEBFF).copy(alpha = 0.36f),
                                        Color(0xFF318ED8).copy(alpha = 0.16f),
                                        Color(0xFF06458F).copy(alpha = 0.28f)
                                    )
                                )
                            )

                            val highlight = Path().apply {
                                moveTo(0f, size.height * 0.525f)
                                cubicTo(
                                    size.width * 0.30f, size.height * 0.355f,
                                    size.width * 0.68f, size.height * 0.665f,
                                    size.width, size.height * 0.435f
                                )
                            }
                            drawPath(
                                highlight,
                                color = Color.White.copy(alpha = 0.35f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        } else if (baseThemeMode == "MIDNIGHT_HD") {
                            // Deep ambient neon glow on top and bottom
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.18f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                                    radius = size.width * 0.75f
                                )
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.20f), Color.Transparent),
                                    center = Offset(size.width * 0.15f, size.height * 0.80f),
                                    radius = size.width * 0.80f
                                )
                            )
                        } else if (baseThemeMode == "GOLD_HD") {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFDE68A).copy(alpha = 0.35f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                                    radius = size.width * 0.70f
                                )
                            )
                        } else if (baseThemeMode == "EMERALD_HD") {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF6EE7B7).copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                                    radius = size.width * 0.70f
                                )
                            )
                        } else if (baseThemeMode == "ROYAL_PURPLE_HD") {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFD8B4FE).copy(alpha = 0.30f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                                    radius = size.width * 0.70f
                                )
                            )
                        }
                    }
            ) { innerPadding ->
                val screenModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                if (showSmsNotifications) {
                    SmsNotificationsScreen(viewModel = viewModel, onBack = { showSmsNotifications = false }, modifier = screenModifier)
                } else if (uiState.selectedCustomerDetailId != null) {
                    CustomerDetailScreen(
                        customerId = uiState.selectedCustomerDetailId!!,
                        viewModel = viewModel,
                        onBack = { viewModel.closeCustomerDetail() },
                        onEditCustomer = { customer ->
                            editingCustomer = customer
                            showNewCustomerDialog = true
                        },
                        onOpenPaymentDialog = { custId ->
                            paymentTargetCustomerId = custId
                            showPaymentDialog = true
                        },
                        modifier = screenModifier
                    )
                } else {
                    when (uiState.currentScreen) {
                        AppScreen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { screen -> viewModel.setScreen(screen) },
                            onOpenCustomerDetail = { custId -> viewModel.openCustomerDetail(custId) },
                            onOpenPaymentDialog = { custId ->
                                paymentTargetCustomerId = custId
                                showPaymentDialog = true
                            },
                            onOpenBatchPaymentDialog = { showBatchPaymentDialog = true },
                            onOpenCustomerDialog = { showNewCustomerDialog = true },
                            onOpenSmsNotifications = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                                    showSmsNotifications = true
                                } else smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            },
                            modifier = screenModifier
                        )

                        AppScreen.CUSTOMERS -> CustomersScreen(
                            viewModel = viewModel,
                            listState = customersListState,
                            onOpenCustomerDetail = { custId -> viewModel.openCustomerDetail(custId) },
                            onOpenPaymentDialog = { custId ->
                                paymentTargetCustomerId = custId
                                showPaymentDialog = true
                            },
                            modifier = screenModifier
                        )

                        AppScreen.MONTHLY_CHARGES -> MonthlyChargesScreen(
                            viewModel = viewModel,
                            onOpenCustomerDetail = { custId -> viewModel.openCustomerDetail(custId) },
                            modifier = screenModifier
                        )

                        AppScreen.TRANSACTIONS -> TransactionsScreen(
                            viewModel = viewModel,
                            onOpenCustomerDetail = { custId -> viewModel.openCustomerDetail(custId) },
                            modifier = screenModifier
                        )

                        AppScreen.REPORTS -> ReportsScreen(
                            viewModel = viewModel,
                            modifier = screenModifier
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            fontScale = fontScale,
                            onFontScaleChange = { newScale ->
                                fontScale = newScale.coerceIn(0.85f, 1.40f)
                                preferences.edit().putFloat("font_scale", fontScale).apply()
                            },
                            lockEnabled = lockEnabled,
                            hasPin = hasPin,
                            biometricEnabled = biometricEnabled,
                            onSavePin = { pin ->
                                val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
                                val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
                                preferences.edit().putString("app_pin_salt", salt).putString("app_pin_hash", pinHash(pin, salt)).putBoolean("app_lock_enabled", true).apply()
                                lockEnabled = true; hasPin = true; isLocked = false
                            },
                            onLockEnabledChange = { enabled ->
                                preferences.edit().putBoolean("app_lock_enabled", enabled).apply()
                                lockEnabled = enabled
                                if (!enabled) isLocked = false
                            },
                            onBiometricEnabledChange = { enabled ->
                                preferences.edit().putBoolean("app_biometric_enabled", enabled).apply()
                                biometricEnabled = enabled
                            },
                            modifier = screenModifier
                        )
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("خروج از باربرگ؟") },
            text = { Text("آیا مطمئن هستید که می‌خواهید از برنامه خارج شوید؟") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    (context as? FragmentActivity)?.finish()
                }) { Text("بله، خروج") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("خیر، بازگشت") }
            }
        )
    }

    // Individual Payment Dialog
    if (showPaymentDialog) {
        IndividualPaymentDialog(
            initialCustomerId = paymentTargetCustomerId,
            customers = customers,
            summaries = summaries,
            currencyUnit = uiState.currencyUnit,
            usePersianDigits = uiState.usePersianDigits,
            currentYear = uiState.selectedYear,
            onDismiss = {
                showPaymentDialog = false
                paymentTargetCustomerId = null
            },
            onSavePayment = { custId, amount, date, method, ref, month, year, notes ->
                viewModel.registerPayment(
                    customerId = custId,
                    amount = amount,
                    shamsiDate = date,
                    paymentMethod = method,
                    referenceNumber = ref,
                    forMonth = month,
                    forYear = year,
                    notes = notes
                )
            }
        )
    }

    // Batch Payment Dialog
    if (showBatchPaymentDialog) {
        BatchPaymentDialog(
            customers = customers,
            groups = groups,
            currencyUnit = uiState.currencyUnit,
            usePersianDigits = uiState.usePersianDigits,
            currentYear = uiState.selectedYear,
            onDismiss = { showBatchPaymentDialog = false },
            onSaveBatch = { custIds, amount, date, method, month, year, notes ->
                viewModel.batchRegisterPayments(
                    customerIds = custIds,
                    amountPerCustomer = amount,
                    shamsiDate = date,
                    paymentMethod = method,
                    forMonth = month,
                    forYear = year,
                    notes = notes
                )
            }
        )
    }

    // New Customer Dialog
    if (showNewCustomerDialog) {
        CustomerDialog(
            customer = editingCustomer,
            groups = groups,
            selectedMonth = uiState.selectedMonth,
            selectedYear = uiState.selectedYear,
            currencyUnit = uiState.currencyUnit,
            usePersianDigits = uiState.usePersianDigits,
            onDismiss = {
                showNewCustomerDialog = false
                editingCustomer = null
            },
            onSave = { id, name, phone, groupId, address, notes, monthlyAmount, username, passwordEncrypted ->
                viewModel.saveCustomer(
                    id, name, phone, groupId, address, notes,
                    username = username,
                    passwordEncrypted = passwordEncrypted,
                    monthlyAmount = monthlyAmount,
                    monthlyYear = uiState.selectedYear,
                    monthlyMonth = uiState.selectedMonth
                )
                showNewCustomerDialog = false
            }
        )
    }
}

private fun pinHash(pin: String, salt: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest((salt + ":" + pin).toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
