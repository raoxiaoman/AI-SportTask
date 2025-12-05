package pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 分组屏幕
@Composable
fun GroupScreen() {
    // 当前选中的分组，null表示在列表页面
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }

    if (selectedGroup == null) {
        // 分组列表页面
        GroupListScreen(onGroupClick = { group ->
            selectedGroup = group
        })
    } else {
        // 分组详情页面
        GroupDetailScreen(
            group = selectedGroup!!,
            onBackClick = {
                selectedGroup = null
            }
        )
    }
}

// 分组列表页面
@Composable
fun GroupListScreen(onGroupClick: (GroupItem) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("分组管理") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 打开添加分组对话框 */ }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        // 分组列表
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(8.dp)) {
            // 模拟分组数据
            val groups = listOf(
                GroupItem("上肢力量", 5),
                GroupItem("核心训练", 4),
                GroupItem("有氧耐力", 6)
            )

            // 列表
            LazyColumn {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        }
    }
}

// 分组详情页面
@Composable
fun GroupDetailScreen(group: GroupItem, onBackClick: () -> Unit) {
    // 模拟动作数据
    val actions = listOf(
        ActionItem("俯卧撑", "1. 双手与肩同宽\n2. 身体保持直线", 45, 15, 1),
        ActionItem("哑铃划船", "1. 单膝跪地\n2. 背部保持平直", 60, 20, 2),
        ActionItem("平板支撑", "1. 肘部支撑地面\n2. 身体保持一条直线", 60, 30, 3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* 编辑 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "编辑"
                        )
                    }
                    IconButton(onClick = { /* 删除 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 添加动作 */ }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 开始训练按钮
            Button(
                onClick = { /* 开始训练 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("开始训练")
            }

            // 动作列表
            Text(
                text = "动作列表",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(actions) { action ->
                    ActionCard(action = action)
                }
            }
        }
    }
}

// 分组数据类
data class GroupItem(val name: String, val actionCount: Int)

// 动作数据类
data class ActionItem(
    val name: String,
    val stepsText: String,
    val defaultTime: Int,
    val restTime: Int,
    val orderIndex: Int
)

// 分组卡片组件
@Composable
fun GroupCard(group: GroupItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = group.name, style = MaterialTheme.typography.h6)
                Text(
                    text = "${group.actionCount} 个动作",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "详情",
                tint = Color.Gray
            )
        }
    }
}

// 动作卡片组件
@Composable
fun ActionCard(action: ActionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = action.name, style = MaterialTheme.typography.subtitle1)
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "展开详情",
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "默认时长: ${action.defaultTime}秒", style = MaterialTheme.typography.body2)
            Text(text = "休息时间: ${action.restTime}秒", style = MaterialTheme.typography.body2)
            if (action.stepsText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "步骤:", style = MaterialTheme.typography.body2)
                Text(text = action.stepsText, style = MaterialTheme.typography.body2)
            }
        }
    }
}