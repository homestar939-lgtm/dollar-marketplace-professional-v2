package com.dollar.marketplace.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.Pal

@Composable
fun ServerSettingsScreen(state: B2bState) {
    var address by remember { mutableStateOf(state.serverUrl) }
    Column(
        Modifier.fillMaxSize().background(Pal.Black).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("إعداد خادم الشركة", color = Pal.Gold, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("أدخل عنوان API على شبكة Wi-Fi نفسها. مثال: http://192.168.1.50:4000/api", color = Pal.Muted)
        Spacer(Modifier.height(20.dp))
        B2bMessage(state.error, isError = true)
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("عنوان الخادم") },
            placeholder = { Text("http://192.168.1.50:4000/api") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pal.Gold,
                unfocusedBorderColor = Pal.Stroke,
                focusedLabelColor = Pal.Gold,
                unfocusedLabelColor = Pal.Muted,
                focusedTextColor = Pal.Text,
                unfocusedTextColor = Pal.Text,
                cursorColor = Pal.Gold
            )
        )
        Spacer(Modifier.height(16.dp))
        B2bPrimaryButton("حفظ ومتابعة", address.isNotBlank()) { state.saveServerUrl(address) }
        if (state.serverUrl.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            B2bGhostButton("رجوع") { state.closeServerSettings() }
        }
    }
}
