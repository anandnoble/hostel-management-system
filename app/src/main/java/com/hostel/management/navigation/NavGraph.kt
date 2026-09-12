package com.hostel.management.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hostel.management.di.ServiceLocator
import com.hostel.management.presentation.announcement.*
import com.hostel.management.presentation.auth.*
import com.hostel.management.presentation.complaint.*
import com.hostel.management.presentation.dashboard.*
import com.hostel.management.presentation.finance.*
import com.hostel.management.presentation.hostel.*
import com.hostel.management.presentation.profile.*
import com.hostel.management.presentation.student.*
import com.hostel.management.presentation.saas.*

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    // ViewModel Instantiations using custom factory providers linking to ServiceLocator
    val authViewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(ServiceLocator.authRepository) as T
        }
    })

    val studentViewModel: StudentViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudentViewModel(ServiceLocator.hostelRepository) as T
        }
    })

    val hostelViewModel: HostelViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HostelViewModel(ServiceLocator.hostelRepository) as T
        }
    })

    val financeViewModel: FinanceViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceViewModel(ServiceLocator.financeRepository) as T
        }
    })

    val complaintViewModel: ComplaintViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ComplaintViewModel(ServiceLocator.complaintRepository) as T
        }
    })

    val announcementViewModel: AnnouncementViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnnouncementViewModel(ServiceLocator.announcementRepository) as T
        }
    })

    val organizationViewModel: OrganizationViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrganizationViewModel(ServiceLocator.organizationRepository) as T
        }
    })

    // Listen to session state to automatically redirect if user is logged in
    val sessionProfile by authViewModel.currentProfile.collectAsState()
    // Track whether initial session check has already run so we don't
    // prematurely navigate to Login while the app is still loading the session.
    var sessionChecked by remember { mutableStateOf(false) }

    LaunchedEffect(sessionProfile) {
        if (!sessionChecked) {
            // First emission is always null while the session is being restored.
            // Wait until checkSession() coroutine has had a chance to run.
            sessionChecked = true
            return@LaunchedEffect
        }
        val role = sessionProfile?.role?.name
        val destination = when (role) {
            "SUPER_ADMIN" -> Screen.SaasDashboard.route
            "HOSTEL_ADMIN" -> Screen.AdminDashboard.route
            "ACCOUNTANT" -> Screen.AccountantDashboard.route
            "MAINTENANCE_STAFF" -> Screen.MaintenanceDashboard.route
            "STUDENT" -> Screen.StudentDashboard.route
            else -> null
        }
        if (sessionProfile == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (destination != null) {
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        "SUPER_ADMIN" -> Screen.SaasDashboard.route
                        "HOSTEL_ADMIN" -> Screen.AdminDashboard.route
                        "ACCOUNTANT" -> Screen.AccountantDashboard.route
                        "MAINTENANCE_STAFF" -> Screen.MaintenanceDashboard.route
                        "STUDENT" -> Screen.StudentDashboard.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToRegisterClient = {
                    navController.navigate(Screen.RegisterClient.route)
                }
            )
        }

        // Forgot Password Screen
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Register Client Screen (Self-service client onboarding from Login)
        composable(Screen.RegisterClient.route) {
            RegisterClientScreen(
                organizationViewModel = organizationViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = { email ->
                    navController.popBackStack()
                }
            )
        }

        // Student Self-Registration Screen (QR Code / Hostel Link Binding)
        composable(
            route = Screen.StudentSelfRegistration.route,
            arguments = listOf(navArgument("hostelId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val hostelId = backStackEntry.arguments?.getString("hostelId")
            com.hostel.management.presentation.auth.StudentSelfRegistrationScreen(
                hostelId = hostelId,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // -------------------------------------------------------------
        // ROLE DASHBOARDS
        // -------------------------------------------------------------
        composable(Screen.SaasDashboard.route) {
            SaasDashboardScreen(
                authViewModel = authViewModel,
                organizationViewModel = organizationViewModel,
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                authViewModel = authViewModel,
                hostelViewModel = hostelViewModel,
                financeViewModel = financeViewModel,
                studentViewModel = studentViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                authViewModel = authViewModel,
                hostelViewModel = hostelViewModel,
                financeViewModel = financeViewModel,
                complaintViewModel = complaintViewModel,
                announcementViewModel = announcementViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.AccountantDashboard.route) {
            AccountantDashboardScreen(
                authViewModel = authViewModel,
                financeViewModel = financeViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.MaintenanceDashboard.route) {
            MaintenanceDashboardScreen(
                authViewModel = authViewModel,
                complaintViewModel = complaintViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // -------------------------------------------------------------
        // MANAGEMENT SUB-SCREENS
        // -------------------------------------------------------------
        
        // Students List
        composable(Screen.StudentList.route) {
            StudentListScreen(
                studentViewModel = studentViewModel,
                authViewModel = authViewModel,
                onNavigateToDetails = { studentId ->
                    navController.navigate(Screen.StudentDetails.createRoute(studentId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.StudentForm.createRoute())
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Student Details
        composable(
            route = Screen.StudentDetails.route,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            StudentDetailsScreen(
                studentId = studentId,
                studentViewModel = studentViewModel,
                authViewModel = authViewModel,
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.StudentForm.createRoute(id))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Student Form (Add / Edit)
        composable(
            route = Screen.StudentForm.route,
            arguments = listOf(navArgument("studentId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            StudentFormScreen(
                studentId = studentId,
                studentViewModel = studentViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Hostel Structure
        composable(Screen.HostelStructure.route) {
            HostelStructureScreen(
                hostelViewModel = hostelViewModel,
                onNavigateToFloorRooms = { floorId ->
                    navController.navigate(Screen.RoomManagement.createRoute(floorId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Room Management Map (Rooms grid for selected floor)
        composable(
            route = Screen.RoomManagement.route,
            arguments = listOf(navArgument("floorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            RoomManagementScreen(
                floorId = floorId,
                hostelViewModel = hostelViewModel,
                onNavigateToAllocation = { studentId, bedId ->
                    // Pass studentId if pre-selected; bedId is pre-selected via route too
                    navController.navigate(Screen.RoomAllocation.createRoute(studentId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Room Allocation Form
        composable(
            route = Screen.RoomAllocation.route,
            arguments = listOf(navArgument("studentId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")
            RoomAllocationScreen(
                preselectedStudentId = studentId,
                preselectedBedId = null,
                hostelViewModel = hostelViewModel,
                studentViewModel = studentViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Fees List
        composable(Screen.FeesList.route) {
            FeesListScreen(
                financeViewModel = financeViewModel,
                authViewModel = authViewModel,
                onNavigateToRecordPayment = { studentId, invoiceId ->
                    navController.navigate(Screen.RecordPayment.createRoute(studentId, invoiceId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Record Payment Form
        composable(
            route = Screen.RecordPayment.route,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            RecordPaymentScreen(
                studentId = studentId,
                invoiceId = invoiceId,
                financeViewModel = financeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Complaints List
        composable(Screen.ComplaintsList.route) {
            ComplaintsListScreen(
                complaintViewModel = complaintViewModel,
                authViewModel = authViewModel,
                onNavigateToDetails = { complaintId ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateComplaint.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Complaint Details
        composable(
            route = Screen.ComplaintDetails.route,
            arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
        ) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            ComplaintDetailsScreen(
                complaintId = complaintId,
                complaintViewModel = complaintViewModel,
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Create Complaint Form
        composable(Screen.CreateComplaint.route) {
            CreateComplaintScreen(
                complaintViewModel = complaintViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Announcements
        composable(Screen.Announcements.route) {
            AnnouncementsScreen(
                announcementViewModel = announcementViewModel,
                authViewModel = authViewModel,
                hostelViewModel = hostelViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Notifications List
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                announcementViewModel = announcementViewModel,
                studentViewModel = studentViewModel,
                hostelViewModel = hostelViewModel,
                financeViewModel = financeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Audit Logs Trail
        composable(Screen.AuditLogs.route) {
            AuditLogsScreen(
                announcementViewModel = announcementViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Reports View
        composable(Screen.Reports.route) {
            ReportsScreen(
                hostelViewModel = hostelViewModel,
                studentViewModel = studentViewModel,
                financeViewModel = financeViewModel,
                complaintViewModel = complaintViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Backup & Sync Settings Screen
        composable(Screen.BackupSync.route) {
            com.hostel.management.presentation.settings.BackupSyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
