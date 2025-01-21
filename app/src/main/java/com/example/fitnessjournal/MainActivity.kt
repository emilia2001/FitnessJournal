package com.example.fitnessjournal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.fitnessjournal.ui.theme.FitnessJournalTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessJournalTheme {
                AppScaffold()
            }
        }
    }
}

// ----------------------------------------------------------------------------
// NAVIGARE
// ----------------------------------------------------------------------------

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "progress",
        modifier = modifier
    ) {
        composable("progress") { ProgressScreen(navController) }
        composable("addExercise") { AddExerciseScreen(navController) }
        composable("editExercise/{exerciseId}") { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            EditExerciseScreen(navController, exerciseId)
        }
    }
}

// ----------------------------------------------------------------------------
// ECRAN: VIZUALIZARE PROGRES
// ----------------------------------------------------------------------------

@Composable
fun ProgressScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Stocăm exercițiile (documentele din Firestore)
    var exerciseList by remember { mutableStateOf(listOf<Exercise>()) }

    // Căutăm exercițiile din Firestore
    LaunchedEffect(Unit) {
        db.collection("exercises").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(context, "Eroare la citire: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Exercise::class.java)?.copy(id = doc.id)
                }
                exerciseList = items
            } else {
                exerciseList = emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Lista Exercițiilor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Buton pentru a adăuga un exercițiu
        Button(onClick = { navController.navigate("addExercise") }) {
            Text("Adaugă Exercițiu")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Listă cu exercițiile existente
        LazyColumn {
            itemsIndexed(exerciseList) { _, exercise ->
                ExerciseItem(exercise = exercise,
                    onDelete = {
                        db.collection("exercises").document(exercise.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Exercițiu șters!", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onEdit = {
                        // Navigăm la ecranul de editare
                        navController.navigate("editExercise/${exercise.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Nume: ${exercise.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Serii (kg): ${exercise.weights.joinToString(", ")}")

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onEdit, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Editează")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Șterge")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// ECRAN: ADAUGĂ UN NOU EXERCIȚIU
// ----------------------------------------------------------------------------

@Composable
fun AddExerciseScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var exerciseName by remember { mutableStateOf("") }
    // Lista de kilograme pentru fiecare serie
    var weights by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Adaugă Exercițiu Nou", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = exerciseName,
            onValueChange = { exerciseName = it },
            label = { Text("Nume exercițiu") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Serii (kilograme):", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        // Afișăm câte un câmp pentru fiecare serie deja adăugată
        for ((index, weight) in weights.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newWeight ->
                        weights = weights.toMutableList().apply {
                            set(index, newWeight)
                        }
                    },
                    label = { Text("Serie ${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Buton de ștergere a unei serii
                OutlinedButton(onClick = {
                    weights = weights.toMutableList().apply { removeAt(index) }
                }) {
                    Text("X")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buton pentru a adăuga o nouă serie
        Button(onClick = {
            weights = weights + ""
        }) {
            Text("Adaugă serie +")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buton care salvează în Firestore
        Button(onClick = {
            // Conversie la int? Cei care nu pot fi convertiți -> 0
            val weightInts = weights.map { it.toIntOrNull() ?: 0 }

            val exerciseData = hashMapOf(
                "name" to exerciseName,
                "weights" to weightInts
            )

            db.collection("exercises").add(exerciseData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Exercițiu adăugat!", Toast.LENGTH_SHORT).show()
                    navController.navigate("progress")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Eroare: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }, modifier = Modifier.fillMaxWidth()) {
            Text("Salvează Exercițiul")
        }
    }
}

// ----------------------------------------------------------------------------
// ECRAN: EDITEAZĂ UN EXISTENT
// ----------------------------------------------------------------------------

@Composable
fun EditExerciseScreen(navController: NavHostController, exerciseId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Datele despre exercițiu (nume + serii)
    var exercise by remember { mutableStateOf<Exercise?>(null) }

    // Obținem datele existente din Firestore
    LaunchedEffect(exerciseId) {
        if (exerciseId.isNotEmpty()) {
            db.collection("exercises").document(exerciseId).get()
                .addOnSuccessListener { doc ->
                    val ex = doc.toObject(Exercise::class.java)
                    // Doc ID trebuie stocat și el
                    if (ex != null) {
                        exercise = ex.copy(id = doc.id)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Eroare la încărcare: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Dacă datele nu sunt încă încărcate, afișăm un loader
    if (exercise == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Variabile pentru form (sincronizate cu starea actuală)
    var exerciseName by remember { mutableStateOf(exercise!!.name) }
    var weights by remember { mutableStateOf(exercise!!.weights.map { it.toString() }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Editează Exercițiul", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = exerciseName,
            onValueChange = { exerciseName = it },
            label = { Text("Nume Exercițiu") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Serii (kilograme):", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        // Lista dinamică de serii
        for ((index, weight) in weights.withIndex()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newWeight ->
                        weights = weights.toMutableList().apply {
                            set(index, newWeight)
                        }
                    },
                    label = { Text("Serie ${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    weights = weights.toMutableList().apply { removeAt(index) }
                }) {
                    Text("X")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buton de adăugare serie
        Button(onClick = {
            weights = weights + ""
        }) {
            Text("Adaugă serie +")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buton de salvare (update)
        Button(onClick = {
            val weightInts = weights.map { it.toIntOrNull() ?: 0 }
            val updatedData = mapOf(
                "name" to exerciseName,
                "weights" to weightInts
            )
            db.collection("exercises").document(exerciseId).update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Exercițiu actualizat!", Toast.LENGTH_SHORT).show()
                    navController.navigate("progress")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Eroare la update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Salvează modificările")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buton de ștergere completă a exercițiului
        OutlinedButton(onClick = {
            db.collection("exercises").document(exerciseId).delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Exercițiu șters!", Toast.LENGTH_SHORT).show()
                    navController.navigate("progress")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Eroare la ștergere: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Șterge Exercițiul")
        }
    }
}

// ----------------------------------------------------------------------------
// MODEL DE DATE
// ----------------------------------------------------------------------------

data class Exercise(
    val id: String = "",
    val name: String = "",
    val weights: List<Int> = emptyList()
)

// ----------------------------------------------------------------------------
// PREVIEW
// ----------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FitnessJournalTheme {
        // Afișăm direct un ecran, de ex. AddExerciseScreen
        AddExerciseScreen(navController = rememberNavController())
    }
}
