package com.example.komunikav2.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SearchBar(
	value: String,
	onValueChange: (String) -> Unit,
	placeholderText: String,
	modifier: Modifier = Modifier
) {
	OutlinedTextField(
		value = value,
		onValueChange = onValueChange,
		modifier = modifier,
		placeholder = { Text(placeholderText) },
		singleLine = true
	)
}
