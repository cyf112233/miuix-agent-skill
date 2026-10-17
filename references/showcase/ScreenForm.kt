package showcase

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 表单 / 编辑页。
 * 顶栏：`Close` 取消 + `Ok` 保存（按钮永远可点，校验失败时给可见错误文案，不静默禁用）。
 * 正文：`Card` 包 `TextField` → 错误行 → 选项分组 → 底部主按钮。
 */
@Composable
fun FormScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("开会时自动静音通知，结束后恢复。") }
        var onlyForeground by remember { mutableStateOf(true) }
        var muteNotification by remember { mutableStateOf(true) }
        var scopeIndex by remember { mutableStateOf(0) }
        val nameError = name.isBlank()

        Scaffold(
            topBar = {
                ScreenTopBar(
                    title = "新建规则",
                    scrollBehavior = scrollBehavior,
                    large = false,
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(MiuixIcons.Close, contentDescription = "取消", tint = MiuixTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = "保存",
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                ),
                overscrollEffect = null,
            ) {
                item { SmallTitle("规则信息") }
                item {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "描述",
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    )
                }
                item {
                    if (nameError) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp).padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = "名称不能为空，保存后无法在其他应用中识别",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    }
                }
                item { SmallTitle("生效范围") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(title = "目标应用", summary = "已选择 3 个应用", onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchPreference(
                            title = "仅在前台生效",
                            summary = "应用切到后台后自动恢复",
                            checked = onlyForeground,
                            onCheckedChange = { onlyForeground = it },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        RadioButtonPreference(
                            title = "全天",
                            summary = "不限制时段",
                            selected = scopeIndex == 0,
                            onClick = { scopeIndex = 0 },
                            radioButtonLocation = RadioButtonLocation.End,
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        RadioButtonPreference(
                            title = "自定义时段",
                            summary = "每天 09:00 - 18:00",
                            selected = scopeIndex == 1,
                            onClick = { scopeIndex = 1 },
                            radioButtonLocation = RadioButtonLocation.End,
                        )
                    }
                }
                item { SmallTitle("执行动作") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        CheckboxPreference(
                            title = "静音通知",
                            summary = "不显示横幅与提示音",
                            checked = muteNotification,
                            onCheckedChange = { muteNotification = it },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        CheckboxPreference(
                            title = "暂停自动同步",
                            summary = "规则结束后自动恢复",
                            checked = false,
                            onCheckedChange = {},
                        )
                    }
                }
                item {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.primary,
                            contentColor = MiuixTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "创建规则",
                            style = MiuixTheme.textStyles.button,
                            color = MiuixTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
