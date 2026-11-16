package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BackupImportMode
import com.example.data.model.BackupImportPreview
import com.example.data.model.BackupImportSummary
import com.example.data.model.CsvExportTarget
import com.example.ui.theme.AlertRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import com.example.util.StockPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BackupPinDialogMode {
    SET_PIN,
    ENTER_PIN,
    CHANGE_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPinDialog(
    mode: BackupPinDialogMode,
    currentLanguage: AppLanguage,
    onSetPin: (pin: String, onDone: () -> Unit) -> Unit,
    onVerifyPin: (pin: String, onResult: (Boolean) -> Unit) -> Unit,
    onChangePin: (oldPin: String, newPin: String, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    customDescription: String? = null
) {
    var currentPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val titleKey = when (mode) {
        BackupPinDialogMode.SET_PIN -> "backup_set_pin_title"
        BackupPinDialogMode.ENTER_PIN -> "backup_enter_pin_title"
        BackupPinDialogMode.CHANGE_PIN -> "backup_change_pin_title"
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VividGreen,
        unfocusedBorderColor = Color(0x36FFFFFF),
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = VividGreen,
        unfocusedLabelColor = TextSecondary,
        cursorColor = VividGreen
    )

    fun sanitizePin(raw: String): String = raw.filter { it.isDigit() }.take(6)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.testTag("backup_pin_dialog")
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF171B17),
            border = BorderStroke(1.dp, Color(0x28FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = ShopLocale.get(titleKey, currentLanguage),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = customDescription ?: ShopLocale.get(
                        if (mode == BackupPinDialogMode.ENTER_PIN) "backup_pin_desc_enter" else "backup_pin_desc_set",
                        currentLanguage
                    ),
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )

                if (mode == BackupPinDialogMode.CHANGE_PIN) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = {
                            currentPin = sanitizePin(it)
                            localError = null
                        },
                        label = {
                            Text(
                                text = ShopLocale.get("backup_current_pin_lbl", currentLanguage),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_current_pin_input")
                    )
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = sanitizePin(it)
                        localError = null
                    },
                    label = {
                        Text(
                            text = ShopLocale.get(
                                when (mode) {
                                    BackupPinDialogMode.ENTER_PIN -> "backup_pin_lbl"
                                    BackupPinDialogMode.SET_PIN,
                                    BackupPinDialogMode.CHANGE_PIN -> "backup_new_pin_lbl"
                                },
                                currentLanguage
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_pin_input")
                )

                if (mode == BackupPinDialogMode.SET_PIN || mode == BackupPinDialogMode.CHANGE_PIN) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            confirmPin = sanitizePin(it)
                            localError = null
                        },
                        label = {
                            Text(
                                text = ShopLocale.get("backup_confirm_pin_lbl", currentLanguage),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_pin_confirm_input")
                    )
                }

                localError?.let { err ->
                    Text(
                        text = err,
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("backup_pin_error_text")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.testTag("backup_pin_cancel_btn")
                    ) {
                        Text(
                            text = ShopLocale.get("cancel", currentLanguage),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            when (mode) {
                                BackupPinDialogMode.SET_PIN -> {
                                    if (!StockPreferences.isValidPinFormat(pinInput)) {
                                        localError = ShopLocale.get("backup_pin_format_err", currentLanguage)
                                    } else if (pinInput != confirmPin) {
                                        localError = ShopLocale.get("backup_pin_mismatch_err", currentLanguage)
                                    } else {
                                        onSetPin(pinInput) {}
                                    }
                                }
                                BackupPinDialogMode.ENTER_PIN -> {
                                    if (!StockPreferences.isValidPinFormat(pinInput)) {
                                        localError = ShopLocale.get("backup_pin_format_err", currentLanguage)
                                    } else {
                                        onVerifyPin(pinInput) { ok ->
                                            if (!ok) {
                                                localError = ShopLocale.get("backup_pin_wrong_err", currentLanguage)
                                            }
                                        }
                                    }
                                }
                                BackupPinDialogMode.CHANGE_PIN -> {
                                    if (!StockPreferences.isValidPinFormat(pinInput)) {
                                        localError = ShopLocale.get("backup_pin_format_err", currentLanguage)
                                    } else if (pinInput != confirmPin) {
                                        localError = ShopLocale.get("backup_pin_mismatch_err", currentLanguage)
                                    } else {
                                        onChangePin(currentPin, pinInput) { ok ->
                                            if (!ok) {
                                                localError = ShopLocale.get("backup_pin_wrong_err", currentLanguage)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = VividGreen),
                        modifier = Modifier.testTag("backup_pin_confirm_btn")
                    ) {
                        Text(
                            text = ShopLocale.get(
                                if (mode == BackupPinDialogMode.ENTER_PIN) "backup_unlock_btn" else "save",
                                currentLanguage
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CsvExportTargetDialog(
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onSelectTarget: (CsvExportTarget) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171B17),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("backup_csv_target_dialog"),
        title = {
            Text(
                text = ShopLocale.get("backup_csv_sheet_title", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CsvExportTarget.entries.forEach { target ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isBusy) { onSelectTarget(target) }
                            .testTag("csv_export_${target.slug}"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF202520),
                        border = BorderStroke(1.dp, Color(0x28FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = ShopLocale.get(target.labelKey, currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                modifier = Modifier.testTag("csv_export_cancel_btn")
            ) {
                Text(
                    text = ShopLocale.get("cancel", currentLanguage),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun BackupImportPreviewDialog(
    preview: BackupImportPreview,
    currentLanguage: AppLanguage,
    onChooseMode: (BackupImportMode) -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = remember(preview.exportedAtMillis) {
        if (preview.exportedAtMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(preview.exportedAtMillis))
        } else {
            "—"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171B17),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("backup_import_preview_dialog"),
        title = {
            Text(
                text = ShopLocale.get("backup_preview_title", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ShopLocale.format(
                        "backup_preview_store_lbl",
                        currentLanguage,
                        preview.sourceStoreName.ifBlank { "StoreOS" }
                    ),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ShopLocale.format(
                        "backup_preview_meta",
                        currentLanguage,
                        preview.version,
                        dateStr,
                        preview.totalRecordsCount
                    ),
                    color = VividGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E231E),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ShopLocale.format(
                            "backup_preview_counts",
                            currentLanguage,
                            preview.productsCount,
                            preview.customersCount,
                            preview.suppliersCount,
                            preview.salesCount,
                            preview.purchasesCount,
                            preview.paymentsCount + preview.supplierPaymentsCount,
                            preview.accountsCount,
                            preview.expensesCount,
                            preview.incomeCount,
                            preview.notesCount
                        ),
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = { onChooseMode(BackupImportMode.ADD_TO_EMPTY_STORE) },
                    enabled = preview.targetStoreIsEmpty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VividGreen,
                        contentColor = Color(0xFF071107),
                        disabledContainerColor = Color(0xFF222822),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("backup_import_mode_empty_btn")
                ) {
                    Text(
                        text = ShopLocale.get("backup_mode_empty_btn", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!preview.targetStoreIsEmpty) {
                    Text(
                        text = ShopLocale.get("backup_mode_empty_hint", currentLanguage),
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { onChooseMode(BackupImportMode.MERGE) },
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VividGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("backup_import_mode_merge_btn")
                ) {
                    Text(
                        text = ShopLocale.get("backup_mode_merge_btn", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                modifier = Modifier.testTag("backup_import_preview_cancel_btn")
            ) {
                Text(
                    text = ShopLocale.get("cancel", currentLanguage),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun BackupImportConfirmDialog(
    preview: BackupImportPreview,
    mode: BackupImportMode,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val msgKey = if (mode == BackupImportMode.ADD_TO_EMPTY_STORE) {
        "backup_confirm_empty_msg"
    } else {
        "backup_confirm_merge_msg"
    }
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = Color(0xFF171B17),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("backup_import_confirm_dialog"),
        title = {
            Text(
                text = ShopLocale.get("backup_confirm_title", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Text(
                text = ShopLocale.format(msgKey, currentLanguage, preview.totalRecordsCount),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = VividGreen),
                modifier = Modifier.testTag("backup_import_final_confirm_btn")
            ) {
                Text(
                    text = ShopLocale.get("backup_confirm_action", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                modifier = Modifier.testTag("backup_import_confirm_cancel_btn")
            ) {
                Text(
                    text = ShopLocale.get("cancel", currentLanguage),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun BackupImportSummaryDialog(
    summary: BackupImportSummary,
    currentLanguage: AppLanguage,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Color(0xFF171B17),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("backup_import_summary_dialog"),
        title = {
            Text(
                text = ShopLocale.get("backup_summary_title", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = ShopLocale.format(
                        "backup_summary_added_skipped",
                        currentLanguage,
                        summary.totalAdded,
                        summary.totalSkipped
                    ),
                    color = VividGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ShopLocale.format(
                        "backup_summary_auto_file",
                        currentLanguage,
                        summary.autoBackupFileName
                    ),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E231E),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ShopLocale.format(
                            "backup_summary_details",
                            currentLanguage,
                            summary.productsAdded, summary.productsSkipped,
                            summary.customersAdded, summary.customersSkipped,
                            summary.suppliersAdded, summary.suppliersSkipped,
                            summary.salesAdded, summary.salesSkipped,
                            summary.purchasesAdded, summary.purchasesSkipped,
                            summary.paymentsAdded + summary.supplierPaymentsAdded,
                            summary.paymentsSkipped + summary.supplierPaymentsSkipped,
                            summary.accountsAdded,
                            summary.expensesAdded,
                            summary.incomeAdded,
                            summary.notesAdded
                        ),
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDone,
                colors = ButtonDefaults.textButtonColors(contentColor = VividGreen),
                modifier = Modifier.testTag("backup_import_summary_done_btn")
            ) {
                Text(
                    text = ShopLocale.get("done", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
