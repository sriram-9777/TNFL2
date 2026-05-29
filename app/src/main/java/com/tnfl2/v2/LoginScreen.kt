package com.tnfl2.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tnfl2.v2.network.AuthRepository
import com.tnfl2.v2.network.LoginRequest
import com.tnfl2.v2.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.tnfl2.v2.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onThemeChange: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Read directly from ThemeManager — guaranteed to recompose when toggled from anywhere
    val isDark = ThemeManager.isDark

    // Color definitions based on Light/Dark Theme
    val brandColor = Color(0xFFB75C1C) // Amber/Brown theme color
    val backgroundColor = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0B1218), Color(0xFF1A2A3A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEBF3F9), Color(0xFFF6F8FA)))
    }

    val circlePeach = if (isDark) Color(0xFF3E2723).copy(alpha = 0.15f) else Color(0xFFF5EFEB)
    val circleBlue = if (isDark) Color(0xFF0D47A1).copy(alpha = 0.12f) else Color(0xFFD6E4F0)

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val inputBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val buttonBg = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0A0F1D)
    val buttonContentColor = if (isDark) Color(0xFF0A0F1D) else Color.White
    val iconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    fun handleLogin() {
        if (username.isNotBlank() && password.isNotBlank()) {
            isLoading = true
            SessionManager.incrementLoading()
            error = null
            scope.launch {
                val result = authRepository.login(LoginRequest(username, password))
                result.fold(
                    onSuccess = { response ->
                        SessionManager.decrementLoading()
                        onLoginSuccess(response.accessToken)
                    },
                    onFailure = { e ->
                        error = when (e) {
                            is IOException -> "Network error. Please check your connection."
                            is HttpException -> {
                                if (e.code() == 400) {
                                    "Invalid credentials. Please try again."
                                } else {
                                    "Login failed: ${e.message()}"
                                }
                            }
                            else -> "An unexpected error occurred."
                        }
                        isLoading = false
                        SessionManager.decrementLoading()
                    }
                )
            }
        } else {
            error = "Please fill in both username and password."
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Decorative background circles
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 160.dp, y = (-70).dp)
                .background(circlePeach, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-100).dp, y = 550.dp)
                .background(circleBlue, shape = CircleShape)
        )
        // Scrollable content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular emblem containing Wine/Liquor Glass
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent)
                        .border(BorderStroke(2.dp, brandColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalBar,
                        contentDescription = "App Logo",
                        tint = brandColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title and Subtitle
                Text(
                    text = "TNFL2",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = brandColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LIQUOR MANAGEMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    // Header text
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Welcome Back",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👋",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in to your account",
                        fontSize = 14.sp,
                        color = labelColor
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Username Field
                    Text(
                        text = "Username",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Enter username", color = labelColor.copy(alpha = 0.7f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = iconColor
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = brandColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password Field
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter password", color = labelColor.copy(alpha = 0.7f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = iconColor
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = iconColor
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                handleLogin()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = brandColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remember Me & Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = { rememberMe = !rememberMe })
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = brandColor,
                                    uncheckedColor = labelColor
                                )
                            )
                            Text(
                                text = "Remember me",
                                fontSize = 14.sp,
                                color = textColor
                            )
                        }

                        Text(
                            text = "Forgot Password?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandColor,
                            modifier = Modifier.clickable {
                                // Forgot password placeholder action
                            }
                        )
                    }

                    // Display error if any
                    error?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button
                    Button(
                        onClick = { handleLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBg,
                            contentColor = buttonContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        enabled = !isLoading
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLoading) "SIGNING IN..." else "SIGN IN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = brandColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Footer Security text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your data is secure and protected",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }

        // Dark/Light Mode toggle in top right (placed here to draw on top and receive clicks)
        IconButton(
            onClick = onThemeChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                contentDescription = "Toggle Theme",
                tint = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
