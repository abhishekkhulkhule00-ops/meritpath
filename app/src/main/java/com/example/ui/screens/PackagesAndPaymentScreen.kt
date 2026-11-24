package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppStrings
import com.example.core.security.SecureScreenEffect
import com.example.data.model.AppConfig
import com.example.data.model.CounsellingPackage
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.StudentPackage
import com.example.ui.components.PackageCard
import com.example.ui.components.PaymentReceiptDialog
import com.example.ui.components.ScreenShieldBanner
import com.example.ui.theme.AmbitiousRose
import com.example.ui.theme.AmbitiousRoseBg
import com.example.ui.theme.ModerateAmber
import com.example.ui.theme.ModerateAmberBg
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenBg
import com.example.ui.viewmodel.CheckoutSessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesAndPaymentScreen(
    packages: List<CounsellingPackage>,
    activePackages: List<StudentPackage>,
    payments: List<PaymentRecord>,
    checkoutState: CheckoutSessionState,
    appConfig: AppConfig,
    screenShieldEnabled: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onSelectPackage: (CounsellingPackage) -> Unit,
    onDismissCheckout: () -> Unit,
    onPromoInputChange: (String) -> Unit,
    onApplyPromo: () -> Unit,
    onExecutePayment: (String, PaymentStatus) -> Unit
) {
    BackHandler(onBack = onBack)
    SecureScreenEffect(enabled = screenShieldEnabled)

    var receiptToView by remember { mutableStateOf<PaymentRecord?>(null) }
    val ownedPkgIds = remember(activePackages) {
        activePackages.filter { it.isUnexpired() }.map { it.packageId }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.explorePackages) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("packages_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (screenShieldEnabled) {
                item { ScreenShieldBanner(strings = strings) }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Try promo codes MERIT200 or SCHOLAR15 at checkout. Payment mode: ${appConfig.paymentMode}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(packages, key = { it.id }) { pkg ->
                PackageCard(
                    pkg = pkg,
                    owned = ownedPkgIds.contains(pkg.id),
                    strings = strings,
                    onSelect = { onSelectPackage(pkg) }
                )
            }

            if (payments.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("Payment History & Receipts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(payments, key = { it.id }) { pay ->
                    val statusColor = when (pay.statusEnum()) {
                        PaymentStatus.SUCCESS -> Pair(SafeGreen, SafeGreenBg)
                        PaymentStatus.PENDING, PaymentStatus.INITIATED -> Pair(ModerateAmber, ModerateAmberBg)
                        else -> Pair(AmbitiousRose, AmbitiousRoseBg)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { receiptToView = pay }
                            .testTag("payment_row_${pay.id}"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pay.packageName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${pay.paymentMethod} • ${pay.gatewayOrderId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "₹${pay.finalAmount}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(color = statusColor.second, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = pay.status,
                                        color = statusColor.first,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Razorpay-Compatible Checkout Sheet
    checkoutState.selectedPackage?.let { pkg ->
        val pricing = checkoutState.pricing
        var selectedMethod by remember { mutableStateOf("UPI") }
        val isLive = appConfig.paymentMode == PaymentMode.LIVE.name && appConfig.backendUrl.isNotBlank()

        ModalBottomSheet(onDismissRequest = onDismissCheckout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Secure Checkout • ${pkg.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isLive) {
                            "Connected to Razorpay Verification Backend (${appConfig.backendUrl})."
                        } else {
                            "Razorpay Sandbox Verification Mode: No real money is charged. Configure Razorpay keys & backend URL in Admin Settings for Live charges."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Promo Code Row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = checkoutState.promoInput,
                        onValueChange = onPromoInputChange,
                        label = { Text("Promo Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("promo_code_input")
                    )
                    Button(
                        onClick = onApplyPromo,
                        modifier = Modifier.testTag("apply_promo_btn")
                    ) {
                        Text(strings.applyPromo)
                    }
                }
                checkoutState.promoMessage?.let { promoMsg ->
                    Text(
                        text = promoMsg,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pricing?.promoError == null) SafeGreen else AmbitiousRose
                    )
                }

                // Price Summary
                if (pricing != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.originalPrice)
                                Text("₹${pricing.originalPrice}", fontFamily = MonoFontFamily)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.discountLabel)
                                Text("-₹${pricing.totalDiscount}", color = SafeGreen, fontFamily = MonoFontFamily)
                            }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.finalPayable, fontWeight = FontWeight.Bold)
                                Text(
                                    "₹${pricing.finalPayable}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = MonoFontFamily
                                )
                            }
                        }
                    }
                }

                // Payment Method
                Text("Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("UPI", "CARD", "NETBANKING").forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m) }
                        )
                    }
                }

                checkoutState.lastResultMessage?.let { resultMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = resultMsg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (checkoutState.processing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Verifying signature & activating package...")
                    }
                } else {
                    Button(
                        onClick = { onExecutePayment(selectedMethod, PaymentStatus.SUCCESS) },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pay_success_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.payNow} (Verify Success) • ₹${pricing?.finalPayable ?: pkg.finalPrice}")
                    }
                    if (!isLive) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onExecutePayment(selectedMethod, PaymentStatus.PENDING) },
                                modifier = Modifier.weight(1f).testTag("simulate_pending_btn")
                            ) {
                                Text("Test Pending")
                            }
                            OutlinedButton(
                                onClick = { onExecutePayment(selectedMethod, PaymentStatus.FAILED) },
                                modifier = Modifier.weight(1f).testTag("simulate_fail_btn")
                            ) {
                                Text("Test Failed")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    receiptToView?.let { pay ->
        PaymentReceiptDialog(payment = pay, onDismiss = { receiptToView = null })
    }
}
