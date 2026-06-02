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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de gestion du profil utilisateur.
 * Permet de consulter et de modifier les informations personnelles,
 * ainsi que les objectifs d'entraînement synchronisés avec la base de données Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SautyViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val database = FirebaseDatabase.getInstance("https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/")
    val userRef = user?.uid?.let { database.getReference("users").child(it).child("profil") }

    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var newEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var identifiant by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var dateNaissance by remember { mutableStateOf("") }
    var poids by remember { mutableStateOf("") }
    var taille by remember { mutableStateOf("") }

    var sautsObjectif by remember { mutableStateOf(viewModel.targetJumps.toString()) }
    var tempsObjectif by remember { mutableStateOf(viewModel.targetMinutes.toString()) }
    var kcalObjectif by remember { mutableStateOf(viewModel.targetKcal.toString()) }

    var showDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { photoUri = uri; photoBitmap = null }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { photoBitmap = bitmap; photoUri = null }
    }

    LaunchedEffect(Unit) {
        userRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                prenom = snapshot.child("prenom").getValue(String::class.java) ?: ""
                nom = snapshot.child("nom").getValue(String::class.java) ?: ""
                identifiant = snapshot.child("identifiant").getValue(String::class.java) ?: ""
                dateNaissance = snapshot.child("dateNaissance").getValue(String::class.java) ?: ""
                poids = snapshot.child("poids").getValue(String::class.java) ?: ""
                taille = snapshot.child("taille").getValue(String::class.java) ?: ""

                val objSauts = snapshot.child("objectifs/sauts").getValue(Int::class.java)
                val objTemps = snapshot.child("objectifs/minutes").getValue(Int::class.java)
                val objKcal = snapshot.child("objectifs/kcal").getValue(Int::class.java)

                if (objSauts != null) sautsObjectif = objSauts.toString()
                if (objTemps != null) tempsObjectif = objTemps.toString()
                if (objKcal != null) kcalObjectif = objKcal.toString()

                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) { isLoading = false }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFA9E1E))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PHOTO DE PROFIL
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                        .clickable { if (isEditing) showDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (identifiant.isNotEmpty()) identifiant else "Utilisateur",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(email, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(32.dp))

                if (!isEditing) {
                    // MODE LECTURE
                    ProfileInfoRow("Nom complet", if (prenom.isEmpty() && nom.isEmpty()) "Non renseigné" else "$prenom $nom")
                    ProfileInfoRow("Âge", calculateAge(dateNaissance))
                    ProfileInfoRow("Poids / Taille", if (poids.isEmpty() || taille.isEmpty()) "Non renseigné" else "$poids kg / $taille cm")

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Mes Objectifs", color = Color(0xFFFA9E1E), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileInfoRow("Sauts", sautsObjectif)
                    ProfileInfoRow("Temps", "$tempsObjectif min")
                    ProfileInfoRow("Calories", "$kcalObjectif kcal")

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA9E1E))
                    ) {
                        Text("MODIFIER LE PROFIL", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { auth.signOut(); onLogout() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF521E))
                    ) {
                        Text("SE DÉCONNECTER", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                } else {
                    // MODE ÉDITION
                    OutlinedTextField(
                        value = identifiant,
                        onValueChange = { identifiant = it },
                        label = { Text("Nom d'utilisateur") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prenom,
                        onValueChange = { prenom = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Adresse Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nouveau mot de passe (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Objectifs Quotidiens",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sautsObjectif,
                            onValueChange = { sautsObjectif = it },
                            label = { Text("Sauts") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tempsObjectif,
                            onValueChange = { tempsObjectif = it },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kcalObjectif,
                            onValueChange = { kcalObjectif = it },
                            label = { Text("Kcal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("ANNULER", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val currentUser = auth.currentUser
                                if (newEmail != currentUser?.email && newEmail.isNotEmpty()) {
                                    currentUser?.updateEmail(newEmail)?.addOnFailureListener {
                                        Toast.makeText(context, "Erreur Email : Ré-authentification requise", Toast.LENGTH_LONG).show()
                                    }
                                }
                                if (newPassword.isNotEmpty()) {
                                    currentUser?.updatePassword(newPassword)?.addOnFailureListener {
                                        Toast.makeText(context, "Erreur MDP : Ré-authentification requise", Toast.LENGTH_LONG).show()
                                    }
                                }
                                val updates = mapOf(
                                    "identifiant" to identifiant,
                                    "prenom" to prenom,
                                    "nom" to nom,
                                    "dateNaissance" to dateNaissance,
                                    "poids" to poids,
                                    "taille" to taille,
                                    "objectifs/sauts" to (sautsObjectif.toIntOrNull() ?: 2000),
                                    "objectifs/minutes" to (tempsObjectif.toIntOrNull() ?: 30),
                                    "objectifs/kcal" to (kcalObjectif.toIntOrNull() ?: 300)
                                )
                                userRef?.updateChildren(updates)?.addOnSuccessListener {
                                    viewModel.updateTargets(
                                        jumps = sautsObjectif.toIntOrNull() ?: 2000,
                                        minutes = tempsObjectif.toIntOrNull() ?: 30,
                                        kcal = kcalObjectif.toIntOrNull() ?: 300
                                    )
                                    Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF521E))
                        ) {
                            Text("ENREGISTRER", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Photo de profil") },
            text = { Text("Choisissez une source pour votre photo.") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; cameraLauncher.launch(null) }) {
                    Text("Appareil Photo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Galerie")
                }
            }
        )
    }
}

/**
 * Composant utilitaire pour afficher une ligne d'information du profil
 * avec un libellé et sa valeur correspondante de manière homogène.
 */
@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/**
 * Calcule l'âge de l'utilisateur à partir d'une chaîne de date de naissance.
 * * @param dobStr La date de naissance au format brut "ddMMyyyy".
 * @return L'âge calculé sous forme de chaîne (ex: "25 ans") ou un message d'erreur si le format est invalide.
 */
fun calculateAge(dobStr: String): String {
    if (dobStr.length != 8) return "Non renseigné"
    return try {
        val format = SimpleDateFormat("ddMMyyyy", Locale.FRANCE)
        val dob = format.parse(dobStr) ?: return "Non renseigné"
        val dobCalendar = Calendar.getInstance().apply { time = dob }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) age--
        "$age ans"
    } catch (e: Exception) {
        "Erreur date"
    }
}