package com.example.komunikav2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConnectedUser(
    val id: String,
    val name: String,
    val avatar: String
)

@Composable
fun MultiPhoneUserDropdown(
    connectedUsers: List<ConnectedUser> = listOf(
        ConnectedUser("1", "John Doe", "👨"),
        ConnectedUser("2", "Jane Smith", "👩"),
        ConnectedUser("3", "Mike Johnson", "👦"),
        ConnectedUser("4", "Sarah Wilson", "👧")
    ),
    onUserClick: (ConnectedUser) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Users",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Dropdown",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👥",
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = connectedUsers.size.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "Connected Users (${connectedUsers.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            connectedUsers.forEach { user ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = user.avatar,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.name,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    },
                    onClick = {
                        onUserClick(user)
                        expanded = false
                    }
                )
            }
            
            Divider(color = Color.LightGray, thickness = 1.dp)
            
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Leave Chat") },
                onClick = {
                    expanded = false
                }
            )
        }
    }
} 