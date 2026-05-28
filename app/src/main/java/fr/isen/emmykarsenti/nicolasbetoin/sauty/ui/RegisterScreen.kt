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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollState = rememberScrollState()

    // ÉTATS DES CHAMPS
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var identifiant by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var poids by remember { mutableStateOf("") }
    var taille by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ÉTATS UI
    var isUsernameTaken by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // GESTION PHOTO
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { photoUri = uri; photoBitmap = null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { photoBitmap = bitmap; photoUri = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CRÉER UN COMPTE",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PHOTO DE PROFIL
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (photoBitmap != null) {
                Image(bitmap = photoBitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
            }
        }
        Text("Ajouter une photo", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // CHAMPS IDENTITÉ
        OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = identifiant,
            onValueChange = { identifiant = it; isUsernameTaken = false },
            label = { Text("Identifiant unique") },
            isError = isUsernameTaken,
            modifier = Modifier.fillMaxWidth()
        )
        if (isUsernameTaken) { Text("Cet identifiant est déjà utilisé !", color = Color.Red, fontSize = 12.sp) }

        Spacer(modifier = Modifier.height(16.dp))

        // CHAMPS PHYSIQUES
        Text("Infos physiques (pour le calcul des Kcal)", color = Color(0xFFFA9E1E), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = poids,
                onValueChange = { poids = it },
                label = { Text("Poids (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = taille,
                onValueChange = { taille = it },
                label = { Text("Taille (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateNaissance,
            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) dateNaissance = it },
            label = { Text("Date de naissance (JJ/MM/AAAA)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CONNEXION
        Text("Sécurité", color = Color(0xFFFA9E1E), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Adresse Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z]).{8,}\$".toRegex()
                passwordError = if (!passwordPattern.matches(it)) "8 car., 1 Majuscule, 1 Minuscule" else ""
            },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { if(passwordError.isNotEmpty()) Text(passwordError) }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // BOUTON VALIDER
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty() && identifiant.isNotEmpty() && passwordError.isEmpty()) {
                    isLoading = true
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid ?: ""

                                // Structure complète du profil incluant les objectifs par défaut
                                val userProfile = mapOf(
                                    "prenom" to prenom,
                                    "nom" to nom,
                                    "identifiant" to identifiant,
                                    "dateNaissance" to dateNaissance,
                                    "poids" to poids,
                                    "taille" to taille,
                                    "email" to email,
                                    "objectifs" to mapOf(
                                        "sauts" to 2000,
                                        "minutes" to 30,
                                        "kcal" to 300
                                    )
                                )

                                val database = FirebaseDatabase.getInstance("https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/")
                                // On enregistre sous users/UID/profil
                                val myRef = database.getReference("users").child(userId).child("profil")

                                myRef.setValue(userProfile).addOnCompleteListener { dbTask ->
                                    isLoading = false
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(context, "Bienvenue chez SAUTY !", Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess()
                                    } else {
                                        Toast.makeText(context, "Erreur DB : ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Vérifiez vos informations", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA9E1E)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("S'INSCRIRE", fontWeight = FontWeight.Bold)
            }
        }

        TextButton(onClick = { onBackToLogin() }, enabled = !isLoading) {
            Text("Déjà un compte ? Se connecter", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // DIALOGUE PHOTO
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Photo de profil") },
            text = { Text("Choisissez une source pour votre photo.") },
            confirmButton = { TextButton(onClick = { showDialog = false; cameraLauncher.launch(null) }) { Text("Appareil Photo") } },
            dismissButton = { TextButton(onClick = { showDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Galerie") } }
        )
    }
}

// Transformation pour le format JJ/MM/AAAA
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}