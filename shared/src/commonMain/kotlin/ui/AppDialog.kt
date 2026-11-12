package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ui.theme.*

/**
 * 全站统一弹窗体系。
 *
 * 设计约定（详见 docs/UI设计规范.md）：
 * - 容器：surface 底色 + 20dp 圆角 + 24dp 内边距 + 24dp 屏幕留白，最大宽 360dp
 * - 标题：h6 强调字重 / ink；正文：body1 / inkSub；所有间距取 4pt 栅格
 * - 按钮：右对齐；取消为文字按钮，主操作为实心按钮（40dp 高、12dp 圆角）
 * - 危险操作：主按钮换成 danger 底色；禁止直接用红色纯文字当主按钮
 * - 输入框：surfaceAlt 填充 + outline 细边 + 12dp 圆角，与页面搜索框一致
 *
 * 页面禁止再直接调用 Material 的 AlertDialog / TextField / Button 原始样式。
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    confirmDanger: Boolean = false,
    confirmEnabled: Boolean = true,
    busy: Boolean = false,
    /** 左侧辅助操作，例如筛选弹窗的「清除筛选」（不会关闭弹窗） */
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.s6),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = AppDimens.dialogMaxWidth),
                shape = AppShapes.dialog,
                color = AppTheme.colors.surface,
                elevation = AppDimens.headerElevation
            ) {
                Column(modifier = Modifier.padding(AppDimens.s6)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = AppTypeWeight.emphasis,
                        color = AppTheme.colors.ink
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(AppDimens.s1))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimens.s4))
                    content()

                    if (confirmLabel != null || dismissLabel != null) {
                        Spacer(modifier = Modifier.height(AppDimens.s6))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (extraActionLabel != null) {
                                TextButton(
                                    onClick = { onExtraAction?.invoke() },
                                    enabled = !busy,
                                    shape = AppShapes.button,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = AppTheme.colors.inkMuted
                                    ),
                                    modifier = Modifier.height(AppDimens.dialogButtonHeight)
                                ) {
                                    Text(extraActionLabel, style = MaterialTheme.typography.subtitle2)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (dismissLabel != null) {
                                TextButton(
                                    onClick = onDismiss ?: onDismissRequest,
                                    enabled = !busy,
                                    shape = AppShapes.button,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = AppTheme.colors.inkSub
                                    ),
                                    modifier = Modifier.height(AppDimens.dialogButtonHeight)
                                ) {
                                    Text(dismissLabel, style = MaterialTheme.typography.subtitle2)
                                }
                            }
                            if (confirmLabel != null) {
                                if (dismissLabel != null) Spacer(modifier = Modifier.width(AppDimens.s2))
                                Button(
                                    onClick = { onConfirm?.invoke() },
                                    enabled = confirmEnabled && !busy,
                                    shape = AppShapes.button,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (confirmDanger) AppTheme.colors.danger
                                        else AppTheme.colors.brandStrong,
                                        contentColor = AppTheme.colors.onBrand,
                                        disabledBackgroundColor = AppTheme.colors.surfaceAlt,
                                        disabledContentColor = AppTheme.colors.inkMuted
                                    ),
                                    modifier = Modifier.height(AppDimens.dialogButtonHeight)
                                ) {
                                    if (busy) {
                                        CircularProgressIndicator(
                                            color = AppTheme.colors.onBrand,
                                            strokeWidth = AppDimens.divider,
                                            modifier = Modifier.size(AppDimens.iconSmall)
                                        )
                                    } else {
                                        Text(confirmLabel, style = MaterialTheme.typography.subtitle2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 确认类弹窗的便捷封装：标题 + 说明 + 取消/主操作 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "确定",
    dismissLabel: String? = "取消",
    danger: Boolean = false,
    busy: Boolean = false,
    confirmEnabled: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        dismissLabel = dismissLabel,
        onDismiss = onDismissRequest,
        confirmDanger = danger,
        confirmEnabled = confirmEnabled,
        busy = busy
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body1,
            color = AppTheme.colors.inkSub
        )
        extra?.invoke(this)
    }
}

/**
 * 全站统一输入框：surfaceAlt 填充 + outline 细边 + 12dp 圆角。
 * 页面表单与弹窗表单共用，禁止再直接调用 OutlinedTextField / TextField 原始样式。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minHeight: Dp = AppDimens.touchTarget,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        label = label?.let {
            { Text(it, style = MaterialTheme.typography.caption) }
        },
        placeholder = placeholder?.let {
            { Text(it, style = MaterialTheme.typography.body2, color = AppTheme.colors.inkMuted) }
        },
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        shape = AppShapes.button,
        textStyle = MaterialTheme.typography.body2,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = AppTheme.colors.ink,
            backgroundColor = AppTheme.colors.surfaceAlt,
            focusedBorderColor = AppTheme.colors.brand,
            unfocusedBorderColor = AppTheme.colors.outline,
            disabledBorderColor = AppTheme.colors.outline,
            errorBorderColor = AppTheme.colors.danger,
            cursorColor = AppTheme.colors.brand,
            focusedLabelColor = AppTheme.colors.brand,
            unfocusedLabelColor = AppTheme.colors.inkMuted,
            errorLabelColor = AppTheme.colors.danger,
            placeholderColor = AppTheme.colors.inkMuted,
            disabledTextColor = AppTheme.colors.inkMuted
        )
    )
}

/** 弹窗表单的语义别名，保持弹窗侧调用可读 */
@Composable
fun AppDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minHeight: Dp = AppDimens.touchTarget,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        minHeight = minHeight,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon
    )
}
