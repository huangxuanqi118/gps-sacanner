package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.billing.BillingConnectionStatus
import com.example.billing.PlanPeriod
import com.example.billing.SubscriptionPlanOffer
import com.example.billing.SubscriptionUiState
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionBottomSheet(
    billingState: SubscriptionUiState,
    onDismiss: () -> Unit,
    onSubscribeClick: (SubscriptionPlanOffer) -> Unit,
    onRestoreClick: () -> Unit,
    onRetrySyncClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedPeriod by remember(billingState.annualOffer, billingState.monthlyOffer) {
        mutableStateOf(
            if (billingState.annualOffer != null || billingState.monthlyOffer == null) {
                PlanPeriod.ANNUAL
            } else {
                PlanPeriod.MONTHLY
            }
        )
    }

    val selectedOffer = when (selectedPeriod) {
        PlanPeriod.MONTHLY -> billingState.monthlyOffer
        PlanPeriod.ANNUAL -> billingState.annualOffer
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.pro_sheet_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.pro_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProBenefitRow(
                icon = Icons.Rounded.AllInclusive,
                title = stringResource(R.string.pro_benefit_unlimited_title),
                desc = stringResource(R.string.pro_benefit_unlimited_desc)
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProBenefitRow(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.pro_benefit_log_title),
                desc = stringResource(R.string.pro_benefit_log_desc)
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProBenefitRow(
                icon = Icons.Rounded.VerifiedUser,
                title = stringResource(R.string.pro_benefit_clean_title),
                desc = stringResource(R.string.pro_benefit_clean_desc)
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (billingState.isProActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.pro_active_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (billingState.isAutoRenewing) {
                                    stringResource(R.string.pro_active_renewing)
                                } else {
                                    stringResource(R.string.pro_active_cancelled)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/account/subscriptions")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("manage_subscription_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.action_manage_subscription))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlanOptionCard(
                        title = stringResource(R.string.plan_monthly_title),
                        offer = billingState.monthlyOffer,
                        periodSuffix = stringResource(R.string.plan_per_month),
                        badgeText = null,
                        selected = selectedPeriod == PlanPeriod.MONTHLY,
                        onClick = { selectedPeriod = PlanPeriod.MONTHLY },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_monthly_card")
                    )
                    PlanOptionCard(
                        title = stringResource(R.string.plan_annual_title),
                        offer = billingState.annualOffer,
                        periodSuffix = stringResource(R.string.plan_per_year),
                        badgeText = stringResource(R.string.plan_best_value),
                        selected = selectedPeriod == PlanPeriod.ANNUAL,
                        onClick = { selectedPeriod = PlanPeriod.ANNUAL },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_annual_card")
                    )
                }

                if (billingState.connectionStatus != BillingConnectionStatus.READY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = when (billingState.connectionStatus) {
                                    BillingConnectionStatus.CONNECTING ->
                                        stringResource(R.string.billing_connecting)
                                    BillingConnectionStatus.PRODUCTS_NOT_CONFIGURED ->
                                        stringResource(R.string.billing_not_configured)
                                    BillingConnectionStatus.UNAVAILABLE ->
                                        stringResource(R.string.billing_unavailable)
                                    BillingConnectionStatus.READY -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onRetrySyncClick,
                                modifier = Modifier.testTag("retry_billing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.action_retry_store_sync))
                            }
                        }
                    }
                }

                billingState.statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { selectedOffer?.let(onSubscribeClick) },
                    enabled = selectedOffer != null && !billingState.isPurchasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("subscribe_cta_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (billingState.isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        val ctaLabel = if (selectedOffer != null) {
                            if (selectedOffer.trialLabel != null) {
                                stringResource(
                                    R.string.action_subscribe_trial_fmt,
                                    selectedOffer.trialLabel,
                                    selectedOffer.formattedPrice
                                )
                            } else {
                                stringResource(
                                    R.string.action_subscribe_price_fmt,
                                    selectedOffer.formattedPrice
                                )
                            }
                        } else {
                            stringResource(R.string.action_subscribe_unavailable)
                        }
                        Text(text = ctaLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onRestoreClick,
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        Text(text = stringResource(R.string.action_restore_purchases))
                    }
                }

                Text(
                    text = stringResource(R.string.billing_terms_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    offer: SubscriptionPlanOffer?,
    periodSuffix: String,
    badgeText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (badgeText != null) {
                StatusChip(label = badgeText, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = offer?.formattedPrice ?: "--",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = offer?.trialLabel ?: periodSuffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProBenefitRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
