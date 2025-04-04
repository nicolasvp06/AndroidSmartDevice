package fr.isen.pissavinvernet.androidsmartdevice.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.pissavinvernet.androidsmartdevice.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    status: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    isConnecting: Boolean
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    // Animation Bluetooth pendant la connexion
    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            scope.launch {
                while (isConnecting) {
                    scale.animateTo(1.5f, animationSpec = tween(durationMillis = 300))
                    scale.animateTo(1f, animationSpec = tween(durationMillis = 300))
                }
            }
        } else {
            scale.snapTo(1f)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Connexion BLE", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Statut : $status", fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_blutooth),
                contentDescription = "Bluetooth Logo",
                modifier = Modifier.size((64 * scale.value).dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Connexion Bluetooth")
                }

                Button(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Déconnexion Bluetooth")
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Retour Accueil")
                }
            }
        }
    }
}
