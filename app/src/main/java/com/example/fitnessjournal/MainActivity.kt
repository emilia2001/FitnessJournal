package com.example.fitnessjournal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessjournal.ui.theme.FitnessJournalTheme
import com.google.firebase.firestore.FirebaseFirestore

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

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavigationGraph(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") { HomeScreen(navController) }
        composable("addExercise") { AddExerciseScreen(navController) }
        composable("progress") { ProgressScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Fitness Journal!", modifier = Modifier.padding(bottom = 32.dp))
        Button(onClick = { navController.navigate("addExercise") }) {
            Text("Add Exercise")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("progress") }) {
            Text("View Progress")
        }
    }
}

@Composable
fun AddExerciseScreen(navController: NavHostController) {
    var exerciseName by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = exerciseName,
            onValueChange = { exerciseName = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it },
            label = { Text("Repetitions") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val db = FirebaseFirestore.getInstance()
            val exercise = hashMapOf(
                "name" to exerciseName,
                "reps" to reps
            )

            // Add logging for debugging
            Log.d("AddExercise", "Attempting to add exercise to Firestore...")

            db.collection("exercises").add(exercise).addOnSuccessListener {
                // Success, log the result and navigate
                Log.d("AddExercise", "Exercise added successfully!")

                navController.navigate("home")
            }.addOnFailureListener { exception ->
                // Failure, log the error
                Log.e("AddExercise", "Error adding exercise: ${exception.message}")
            }
        }) {
            Text("Add Exercise")
        }
    }
}

@Composable
fun ProgressScreen(navController: NavHostController) {
    var exercises by remember { mutableStateOf(listOf<Map<String, String>>()) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("exercises").get().addOnSuccessListener { result ->
            val exerciseList = result.map { doc ->
                mapOf(
                    "name" to doc.getString("name").orEmpty(),
                    "reps" to doc.getString("reps").orEmpty()
                )
            }
            exercises = exerciseList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Progress", modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn {
            items(exercises) { exercise ->
                Text(
                    text = "Exercise: ${exercise["name"]}, Reps: ${exercise["reps"]}",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FitnessJournalTheme {
        AppScaffold()
    }
}

