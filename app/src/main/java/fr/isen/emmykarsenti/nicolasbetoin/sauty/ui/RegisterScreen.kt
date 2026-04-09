package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current

    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var identifiant by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isUsernameTaken by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // 🎯 Nouvel état pour faire patienter l'utilisateur

    var showDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { photoUri = uri; photoBitmap = null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { photoBitmap = bitmap; photoUri = null }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Créer un compte", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (photoBitmap != null) {
                Image(bitmap = photoBitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.DarkGray)
            }
        }
        Text("Appuyez pour ajouter une photo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Adresse Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = identifiant,
            onValueChange = { identifiant = it; isUsernameTaken = false },
            label = { Text("Identifiant unique") },
            isError = isUsernameTaken,
            modifier = Modifier.fillMaxWidth()
        )
        if (isUsernameTaken) { Text("Cet identifiant est déjà utilisé !", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateNaissance,
            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) dateNaissance = it },
            label = { Text("Date de naissance (JJ/MM/AAAA)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))

        // LE BOUTON MAGIQUE CONNECTÉ À FIREBASE
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty() && identifiant.isNotEmpty()) {
                    isLoading = true

                    // 1. Création du compte sécurisé dans Firebase Auth
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Le compte est créé, on récupère l'ID unique de l'utilisateur
                                val userId = task.result?.user?.uid ?: ""

                                // 2. On prépare les données du profil
                                val userProfile = mapOf(
                                    "prenom" to prenom,
                                    "nom" to nom,
                                    "identifiant" to identifiant,
                                    "dateNaissance" to dateNaissance,
                                    "email" to email
                                )

                                // 3. On sauvegarde dans ta Realtime Database
                                val database = FirebaseDatabase.getInstance("https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/")
                                val myRef = database.getReference("users").child(userId)

                                myRef.setValue(userProfile).addOnCompleteListener { dbTask ->
                                    isLoading = false
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(context, "Inscription réussie !", Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess()
                                    } else {
                                        Toast.makeText(context, "Erreur base de données : ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Erreur création compte : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading // Désactive le bouton pendant le chargement
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("VALIDER L'INSCRIPTION")
            }
        }

        TextButton(onClick = { onBackToLogin() }, enabled = !isLoading) { Text("Déjà un compte ? Se connecter") }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Photo de profil") },
            text = { Text("D'où voulez-vous importer votre photo ?") },
            confirmButton = { TextButton(onClick = { showDialog = false; cameraLauncher.launch(null) }) { Text("Prendre une photo") } },
            dismissButton = { TextButton(onClick = { showDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Photothèque") } }
        )
    }
}

// Outil de formatage pour la date
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) { out += trimmed[i]; if (i == 1 || i == 3) out += "/" }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset; if (offset <= 3) return offset + 1; if (offset <= 8) return offset + 2; return 10
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset; if (offset <= 5) return offset - 1; if (offset <= 10) return offset - 2; return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}