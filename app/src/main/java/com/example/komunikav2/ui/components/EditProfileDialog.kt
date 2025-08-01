package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.komunikav2.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.res.stringResource

@Composable
fun EditProfileDialog(
    currentName: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var avatar by remember { mutableStateOf(currentAvatar) }
    var nameError by remember { mutableStateOf(false) }
    val avatars = listOf(
        "👤", "😊", "🤖", "🎭", "🦊", "🐱", "🐶", "👓", "🎨", "🌟", "💎", "🔥",
        "😎", "🤠", "👻", "🦄", "🐼", "🐨", "🐯", "🦁", "🐸", "🐙", "🦋", "🦅",
        "🐬", "🦕", "🦖", "🐉", "🦜", "🦢", "🦚", "🦩", "🦘", "🦡", "🦝", "🦨",
        "🦥", "🦦", "🦧", "🐒", "🐵", "🐻", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷",
        "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🐣", "🦆", "🦅", "🦉", "🦇", "🐺"
    )
    
    val isFormValid = name.trim().isNotEmpty() && avatar in avatars
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.edit_profile),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.choose_avatar),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                

                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    itemsIndexed(avatars) { index, avatarEmoji ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (avatar == avatarEmoji) Color(0xFFE3F2FD) 
                                    else Color(0xFFF5F5F5)
                                )
                                .border(
                                    width = if (avatar == avatarEmoji) 2.dp else 1.dp,
                                    color = if (avatar == avatarEmoji) Color(0xFF1565C0) else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { 
                                    avatar = avatarEmoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarEmoji,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.name_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (nameError) Color.Red else Color(0xFF1565C0),
                        unfocusedBorderColor = if (nameError) Color.Red else Color.Gray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = nameError
                )
                
                if (nameError) {
                    Text(
                        text = stringResource(R.string.name_cannot_be_empty),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            nameError = trimmedName.isEmpty()
                            
                            if (!nameError) {
                                onSave(trimmedName, avatar)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFormValid) Color(0xFF1565C0) else Color.Gray
                        ),
                        enabled = isFormValid
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
} 