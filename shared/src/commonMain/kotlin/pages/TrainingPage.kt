package pages

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// 辅助组件：居中显示文本
@Composable
fun CenterText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text)
    }
}

// 训练屏幕
@Composable
fun TrainingScreen() {
    CenterText("训练界面")
}