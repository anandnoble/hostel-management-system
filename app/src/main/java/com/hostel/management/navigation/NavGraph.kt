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

    // Listen to session state to automatically redirect if user is logged in
    val sessionProfile by authViewModel.currentProfile.collectAsState()

    LaunchedEffect(sessionProfile) {
        if (sessionProfile == null) {
            navController.navigate(Screen.Login.route) {
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
                        "SUPER_ADMIN", "HOSTEL_ADMIN" -> Screen.AdminDashboard.route
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

        // -------------------------------------------------------------
        // ROLE DASHBOARDS
        // -------------------------------------------------------------
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                authViewModel = authViewModel,
                hostelViewModel = hostelViewModel,
                onLogout = {
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
