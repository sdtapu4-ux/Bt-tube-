package com.example

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BtTubeRepository
import com.example.data.GoogleSignInHelper
import com.example.data.UserAccountFirestoreRepository
import com.example.ui.BtTubeApp
import com.example.ui.screens.GmailAuthScreen
import com.example.ui.theme.BtTubeTheme
import com.example.ui.viewmodel.BtTubeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        setContent {
            BtTubeRoot()
        }
    }
}

@Composable
fun BtTubeRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseAvailable = remember(context) { GoogleSignInHelper.isFirebaseAvailable(context) }

    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(
            if (firebaseAvailable) runCatching { Firebase.auth.currentUser }.getOrNull() else null
        )
    }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var continueWithoutFirebase by remember { mutableStateOf(false) }

    if (firebaseAvailable) {
        DisposableEffect(Unit) {
            val auth = Firebase.auth
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }
            auth.addAuthStateListener(listener)
            onDispose {
                auth.removeAuthStateListener(listener)
            }
        }

        LaunchedEffect(Unit) {
            if (currentUser == null) {
                GoogleSignInHelper.attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { user ->
                        currentUser = user
                        authError = null
                    },
                    onUnauthenticated = {},
                    scope = scope
                )
            }
        }
    }

    val launchInteractiveGoogleSignIn: () -> Unit = {
        authError = null
        isSigningIn = true
        GoogleSignInHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                isSigningIn = false
                authError = null
                currentUser = user
            },
            onAuthError = { msg ->
                isSigningIn = false
                authError = msg
            },
            scope = scope,
            onAuthCancelled = {
                isSigningIn = false
            }
        )
    }

    val signedInUser = currentUser
    if (signedInUser == null && !(continueWithoutFirebase && !firebaseAvailable)) {
        BtTubeTheme {
            GmailAuthScreen(
                isSigningIn = isSigningIn,
                errorMessage = authError,
                onGoogleSignInClick = launchInteractiveGoogleSignIn,
                onContinueOffline = if (!firebaseAvailable) {
                    { continueWithoutFirebase = true }
                } else {
                    null
                }
            )
        }
    } else {
        val sessionKey = signedInUser?.uid ?: "offline_device_session"
        key(sessionKey) {
            val viewModel: BtTubeViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        if (signedInUser != null) {
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            BtTubeViewModel(
                                repository = BtTubeRepository(),
                                accountRepository = UserAccountFirestoreRepository(db),
                                authenticatedUser = signedInUser
                            )
                        } else {
                            BtTubeViewModel(repository = BtTubeRepository())
                        }
                    }
                }
            )
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            BtTubeTheme(themeMode = settings.themeMode) {
                BtTubeApp(
                    viewModel = viewModel,
                    onGoogleSignIn = launchInteractiveGoogleSignIn,
                    onSignOut = {
                        continueWithoutFirebase = false
                        GoogleSignInHelper.signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = {
                                viewModel.signOut()
                                currentUser = null
                            }
                        )
                    }
                )
            }
        }
    }
}
