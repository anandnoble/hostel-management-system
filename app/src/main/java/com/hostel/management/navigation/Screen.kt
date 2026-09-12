package com.hostel.management.navigation

sealed class Screen(val route: String) {
    // Auth routes
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")
    object RegisterClient : Screen("register_client")
    
    // Role dashboards
    object SaasDashboard : Screen("saas_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object StudentDashboard : Screen("student_dashboard")
    object AccountantDashboard : Screen("accountant_dashboard")
    object MaintenanceDashboard : Screen("maintenance_dashboard")

    // Management sub-screens
    object StudentList : Screen("student_list")
    object StudentDetails : Screen("student_details/{studentId}") {
        fun createRoute(studentId: String) = "student_details/$studentId"
    }
    object StudentForm : Screen("student_form?studentId={studentId}") {
        fun createRoute(studentId: String? = null) = if (studentId != null) "student_form?studentId=$studentId" else "student_form"
    }

    object HostelStructure : Screen("hostel_structure")
    object RoomManagement : Screen("room_management/{floorId}") {
        fun createRoute(floorId: String) = "room_management/$floorId"
    }
    object RoomAllocation : Screen("room_allocation?studentId={studentId}") {
        fun createRoute(studentId: String? = null) = if (studentId != null) "room_allocation?studentId=$studentId" else "room_allocation"
    }

    object FeesList : Screen("fees_list")
    object RecordPayment : Screen("record_payment?studentId={studentId}&invoiceId={invoiceId}") {
        fun createRoute(studentId: String, invoiceId: String) = "record_payment?studentId=$studentId&invoiceId=$invoiceId"
    }

    object ComplaintsList : Screen("complaints_list")
    object ComplaintDetails : Screen("complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_details/$complaintId"
    }
    object CreateComplaint : Screen("create_complaint")

    object Announcements : Screen("announcements")
    object Notifications : Screen("notifications")
    object AuditLogs : Screen("audit_logs")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
    object BackupSync : Screen("backup_sync")
    object StudentSelfRegistration : Screen("student_self_registration?hostelId={hostelId}") {
        fun createRoute(hostelId: String? = null) = if (hostelId != null) "student_self_registration?hostelId=$hostelId" else "student_self_registration"
    }
}
