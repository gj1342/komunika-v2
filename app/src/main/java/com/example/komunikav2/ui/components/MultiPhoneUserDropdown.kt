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
import com.example.komunikav2.data.UserProfile

@Composable
fun MultiPhoneUserDropdown(
    key: Int = 0,
    connectedUsers: List<UserProfile> = emptyList(),
    selectedUser: UserProfile? = null,
    onUserClick: (UserProfile?) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedUser?.name ?: if (connectedUsers.isNotEmpty()) "Select User" else "No Users",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Dropdown",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👥",
                    fontSize = 10.sp
                )
            }
            
            Spacer(modifier = Modifier.width(3.dp))
            
            Text(
                text = connectedUsers.size.toString(),
                fontSize = 12.sp,
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
            
            // Option to deselect current user
            if (selectedUser != null) {
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "Deselect User",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    },
                    onClick = {
                        onUserClick(null)
                        expanded = false
                    }
                )
                
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            
            connectedUsers.forEach { user ->
                val isSelected = selectedUser?.id == user.id
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
                                color = if (isSelected) Color.Blue else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "✓",
                                    fontSize = 16.sp,
                                    color = Color.Blue
                                )
                            }
                        }
                    },
                    onClick = {
                        onUserClick(user)
                        expanded = false
                    }
                )
            }
        }
    }
} 