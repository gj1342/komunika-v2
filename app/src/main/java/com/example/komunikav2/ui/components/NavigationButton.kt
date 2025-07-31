package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.komunikav2.R

@Composable
fun NavigationButton(
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.gray_light),
            disabledContainerColor = colorResource(R.color.gray_light),
            contentColor = colorResource(R.color.primary_orange),
            disabledContentColor = colorResource(R.color.primary_orange)
        ),
        enabled = false
    ) {
        Text(
            text = stringResource(R.string.navigation),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_orange)
        )
    }
} 