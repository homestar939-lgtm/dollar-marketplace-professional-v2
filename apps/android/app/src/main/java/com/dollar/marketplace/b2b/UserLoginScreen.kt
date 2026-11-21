package com.dollar.marketplace.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.Pal

@Composable
fun UserLoginScreen(state: B2bState) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF191307), Pal.Black)))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("دولار", color = Pal.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text("دخول الموظفين والعملاء عبر شبكة الشركة", color = Pal.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(26.dp))
            B2bMessage(state.error, isError = true)
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(18.dp))
            if (state.busy) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Pal.Gold, strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
            }
            B2bPrimaryButton("دخول", !state.busy && username.isNotBlank() && password.isNotBlank()) {
                state.login(username, password)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = state::openServerSettings, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("إعداد عنوان خادم Wi-Fi", color = Pal.Gold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pal.Gold,
    unfocusedBorderColor = Pal.Stroke,
    focusedLabelColor = Pal.Gold,
    unfocusedLabelColor = Pal.Muted,
    focusedTextColor = Pal.Text,
    unfocusedTextColor = Pal.Text,
    cursorColor = Pal.Gold
)
