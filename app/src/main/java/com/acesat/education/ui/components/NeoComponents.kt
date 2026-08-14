package com.acesat.education.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acesat.education.ui.theme.*

@Composable
fun NeobrutalistBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardWhite,
    borderColor: Color = BorderBlack,
    shadowColor: Color = BorderBlack,
    borderWidth: Int = 2,
    shadowOffset: Int = 4,
    cornerRadius: Int = 12,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.padding(bottom = shadowOffset.dp, end = shadowOffset.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset.dp, y = shadowOffset.dp)
                .background(shadowColor, shape = RoundedCornerShape(cornerRadius.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = RoundedCornerShape(cornerRadius.dp))
                .border(borderWidth.dp, borderColor, shape = RoundedCornerShape(cornerRadius.dp))
                .clip(RoundedCornerShape(cornerRadius.dp)),
            content = content
        )
    }
}

@Composable
fun NeobrutalistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PurpleAccent,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val bg = if (enabled) backgroundColor else Color.Gray
    NeobrutalistBox(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        backgroundColor = bg,
        cornerRadius = 10,
        shadowOffset = 3
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
