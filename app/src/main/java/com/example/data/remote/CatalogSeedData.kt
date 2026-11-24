package com.example.data.remote

import com.example.data.model.AppConfig
import com.example.data.model.AppNotification
import com.example.data.model.College
import com.example.data.model.CollegeBranch
import com.example.data.model.CollegeCutoff
import com.example.data.model.CollegeType
import com.example.data.model.CounsellingPackage
import com.example.data.model.Course
import com.example.data.model.DiscountType
import com.example.data.model.NotificationType
import com.example.data.model.PackageTier
import com.example.data.model.PromoCode

object CatalogSeedData {

    val defaultAppConfig = AppConfig(
        id = "settings",
        whatsappNumber = "+919820012345",
        helplinePhone = "+91 20 6720 4400",
        supportEmail = "counselling@meritpath.in",
        academicYear = "2026-27",
        paymentMode = "SANDBOX",
        backendUrl = "",
        announcementBanner = "CAP Round 1 & NEET UG State Counselling option forms open for Academic Year 2026-27."
    )

    val starterCourses = listOf(
        Course(
            id = "course_btech",
            name = "B.Tech / B.E. Engineering",
            code = "BTECH",
            description = "4-year undergraduate engineering degree across Computer, IT, AI-DS, Electronics, and Mechanical streams.",
            eligibility = "10+2 PCM with min 45% (40% reserved) + valid MHT-CET or JEE Main score.",
            duration = "4 Years (8 Semesters)",
            entranceExam = "MHT-CET / JEE Main",
            active = true,
            displayOrder = 1L
        ),
        Course(
            id = "course_mbbs",
            name = "MBBS Medical",
            code = "MBBS",
            description = "5.5-year Bachelor of Medicine and Bachelor of Surgery program with 1-year compulsory rotating internship.",
            eligibility = "10+2 PCB with min 50% + NEET-UG qualification.",
            duration = "5.5 Years",
            entranceExam = "NEET-UG",
            active = true,
            displayOrder = 2L
        ),
        Course(
            id = "course_bpharm",
            name = "B.Pharm Pharmacy",
            code = "BPHARM",
            description = "4-year pharmaceutical sciences undergraduate degree covering pharmacology, formulation, and clinical trials.",
            eligibility = "10+2 PCM/PCB + MHT-CET / NEET.",
            duration = "4 Years",
            entranceExam = "MHT-CET",
            active = true,
            displayOrder = 3L
        ),
        Course(
            id = "course_bca",
            name = "BCA Computer Applications",
            code = "BCA",
            description = "3-year software engineering, cloud computing, and full-stack application development degree.",
            eligibility = "10+2 in any stream + MAH-BCA-CET.",
            duration = "3 Years",
            entranceExam = "MAH-BCA-CET",
            active = true,
            displayOrder = 4L
        ),
        Course(
            id = "course_mba",
            name = "MBA / MMS Management",
            code = "MBA",
            description = "2-year postgraduate management program in Finance, Marketing, Analytics, and Operations.",
            eligibility = "Bachelor's degree min 50% + MAH-MBA-CET / CAT / CMAT.",
            duration = "2 Years",
            entranceExam = "MAH-MBA-CET / CAT",
            active = true,
            displayOrder = 5L
        )
    )

    val starterColleges = listOf(
        College(
            id = "col_coep_pune",
            name = "COEP Technological University",
            code = "6006",
            university = "COEP Technological University (State Unitary)",
            address = "Wellesley Rd, Shivajinagar, Pune 411005",
            city = "Pune",
            district = "Pune",
            state = "Maharashtra",
            collegeType = CollegeType.GOVERNMENT.name,
            courseIds = listOf("course_btech"),
            branches = listOf(
                CollegeBranch("600624510", "Computer Engineering", "course_btech", 150, 18, 85000, 12000, 8500, 38000, 143500),
                CollegeBranch("600637210", "Electronics & Telecom", "course_btech", 120, 22, 85000, 12000, 8500, 38000, 143500),
                CollegeBranch("600661210", "Mechanical Engineering", "course_btech", 120, 25, 85000, 12000, 8500, 38000, 143500)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Computer Engineering", 99.84, 181.0, 412),
                CollegeCutoff("2025-26", "CAP Round 2", "OPEN", "State Level", "course_btech", "Computer Engineering", 99.78, 178.0, 520),
                CollegeCutoff("2025-26", "CAP Round 1", "OBC", "State Level", "course_btech", "Computer Engineering", 99.64, 174.0, 890),
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Electronics & Telecom", 99.45, 168.0, 1340),
                CollegeCutoff("2024-25", "CAP Round 1", "OPEN", "State Level", "course_btech", "Computer Engineering", 99.81, 179.0, 445)
            ),
            scholarships = "EBC / Rajarshi Shahu Maharaj Tuition Fee Waiver, MahaDBT Post-Matric.",
            eligibility = "12th PCM min 45% + MHT-CET / JEE Main CAP Merit.",
            placementAvgLpa = 11.4,
            placementHighLpa = 50.5,
            placementRate = 94,
            hostelAvailable = true,
            hostelSummary = "Separate boys & girls hostels on campus with mess & Wi-Fi.",
            website = "https://www.coeptech.ac.in",
            contactEmail = "admissions@coeptech.ac.in",
            contactPhone = "+91 20 2550 7000",
            description = "Premier autonomous state technological university founded in 1854 with top-tier industry placements.",
            imageKey = "campus",
            active = true
        ),
        College(
            id = "col_vjti_mumbai",
            name = "Veermata Jijabai Technological Institute (VJTI)",
            code = "3012",
            university = "University of Mumbai",
            address = "H R Mahajani Marg, Matunga East, Mumbai 400019",
            city = "Mumbai",
            district = "Mumbai City",
            state = "Maharashtra",
            collegeType = CollegeType.GOVT_AIDED.name,
            courseIds = listOf("course_btech"),
            branches = listOf(
                CollegeBranch("301224510", "Computer Engineering", "course_btech", 60, 8, 69000, 11000, 5500, 32000, 117500),
                CollegeBranch("301224610", "Information Technology", "course_btech", 60, 10, 69000, 11000, 5500, 32000, 117500)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Computer Engineering", 99.89, 184.0, 290),
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Information Technology", 99.76, 177.0, 610),
                CollegeCutoff("2025-26", "CAP Round 1", "SC", "State Level", "course_btech", "Computer Engineering", 97.40, 145.0, 6200)
            ),
            scholarships = "TFWS, MahaDBT, Alumni Merit Grants.",
            eligibility = "12th PCM + MHT-CET Merit.",
            placementAvgLpa = 13.8,
            placementHighLpa = 62.0,
            placementRate = 96,
            hostelAvailable = true,
            hostelSummary = "Limited merit-based hostel blocks inside Matunga campus.",
            website = "https://vjti.ac.in",
            contactEmail = "dean.admissions@vjti.ac.in",
            contactPhone = "+91 22 2419 8101",
            description = "Iconic Mumbai engineering institution renowned for strong alumni network and product engineering placements.",
            imageKey = "campus",
            active = true
        ),
        College(
            id = "col_pict_pune",
            name = "Pune Institute of Computer Technology (PICT)",
            code = "6271",
            university = "Savitribai Phule Pune University",
            address = "Survey No. 27, Dhankawadi, Pune 411043",
            city = "Pune",
            district = "Pune",
            state = "Maharashtra",
            collegeType = CollegeType.PRIVATE.name,
            courseIds = listOf("course_btech"),
            branches = listOf(
                CollegeBranch("627124510", "Computer Engineering", "course_btech", 240, 28, 96000, 14000, 4500, 95000, 209500),
                CollegeBranch("627124610", "Information Technology", "course_btech", 180, 20, 96000, 14000, 4500, 95000, 209500),
                CollegeBranch("627199510", "AI & Data Science", "course_btech", 120, 15, 96000, 14000, 4500, 95000, 209500)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "Home University", "course_btech", "Computer Engineering", 99.55, 170.0, 1180),
                CollegeCutoff("2025-26", "CAP Round 2", "OPEN", "State Level", "course_btech", "Information Technology", 99.28, 162.0, 1950),
                CollegeCutoff("2025-26", "CAP Round 1", "OBC", "Home University", "course_btech", "AI & Data Science", 98.70, 155.0, 3400)
            ),
            scholarships = "EBC, TFWS, Central Sector Scholarship.",
            eligibility = "12th PCM + MHT-CET / JEE Main.",
            placementAvgLpa = 12.1,
            placementHighLpa = 47.2,
            placementRate = 95,
            hostelAvailable = true,
            hostelSummary = "On-campus hostel with twin-sharing rooms and 24x7 study hall.",
            website = "https://pict.edu",
            contactEmail = "registrar@pict.edu",
            contactPhone = "+91 20 2437 1101",
            description = "Focused ICT & Computer Science engineering institute with high competitive programming culture.",
            imageKey = "college",
            active = true
        ),
        College(
            id = "col_wce_sangli",
            name = "Walchand College of Engineering",
            code = "6007",
            university = "Shivaji University (Autonomous)",
            address = "Vishrambag, Sangli 416415",
            city = "Sangli",
            district = "Sangli",
            state = "Maharashtra",
            collegeType = CollegeType.GOVT_AIDED.name,
            courseIds = listOf("course_btech"),
            branches = listOf(
                CollegeBranch("600724510", "Computer Science & Engg", "course_btech", 90, 14, 72000, 10000, 5000, 26000, 113000),
                CollegeBranch("600724610", "Information Technology", "course_btech", 60, 11, 72000, 10000, 5000, 26000, 113000)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Computer Science & Engg", 99.12, 160.0, 2300),
                CollegeCutoff("2025-26", "CAP Round 2", "OPEN", "State Level", "course_btech", "Information Technology", 98.68, 154.0, 3510),
                CollegeCutoff("2025-26", "CAP Round 1", "OBC", "State Level", "course_btech", "Computer Science & Engg", 98.40, 151.0, 4200)
            ),
            scholarships = "TFWS, MahaDBT, Cummins Scholarship.",
            eligibility = "12th PCM + MHT-CET.",
            placementAvgLpa = 9.6,
            placementHighLpa = 43.0,
            placementRate = 91,
            hostelAvailable = true,
            hostelSummary = "90-acre green campus with full residential hostels.",
            website = "https://www.walchandsangli.ac.in",
            contactEmail = "admissions@walchandsangli.ac.in",
            contactPhone = "+91 233 230 0383",
            description = "Sprawling autonomous engineering campus offering strong ROI and core + IT placements.",
            imageKey = "college",
            active = true
        ),
        College(
            id = "col_gmc_mumbai",
            name = "Grant Government Medical College & JJ Hospital",
            code = "1101",
            university = "MUHS Nashik",
            address = "J J Marg, Byculla, Mumbai 400008",
            city = "Mumbai",
            district = "Mumbai City",
            state = "Maharashtra",
            collegeType = CollegeType.GOVERNMENT.name,
            courseIds = listOf("course_mbbs"),
            branches = listOf(
                CollegeBranch("110101", "MBBS (Allopathy)", "course_mbbs", 250, 32, 128000, 10000, 6000, 14000, 158000)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "State Round 1", "OPEN", "85% State Quota", "course_mbbs", "MBBS (Allopathy)", null, 668.0, 2150),
                CollegeCutoff("2025-26", "State Round 1", "OBC", "85% State Quota", "course_mbbs", "MBBS (Allopathy)", null, 654.0, 4120)
            ),
            scholarships = "MahaDBT, Rajarshi Shahu Tuition Fee Scheme.",
            eligibility = "12th PCB + NEET-UG State Merit.",
            placementAvgLpa = 10.5,
            placementHighLpa = 24.0,
            placementRate = 98,
            hostelAvailable = true,
            hostelSummary = "Dedicated UG medical hostels near JJ Hospital.",
            website = "https://ggmcjjh.com",
            contactEmail = "dean@ggmcjjh.com",
            contactPhone = "+91 22 2373 5555",
            description = "One of India's oldest and highest-volume clinical training medical colleges.",
            imageKey = "campus",
            active = true
        ),
        College(
            id = "col_ict_mumbai",
            name = "Institute of Chemical Technology (ICT)",
            code = "3036",
            university = "ICT Deemed University",
            address = "Nathalal Parekh Marg, Matunga, Mumbai 400019",
            city = "Mumbai",
            district = "Mumbai City",
            state = "Maharashtra",
            collegeType = CollegeType.AUTONOMOUS.name,
            courseIds = listOf("course_bpharm", "course_btech"),
            branches = listOf(
                CollegeBranch("303682310", "B.Pharmacy", "course_bpharm", 30, 5, 85350, 5000, 4000, 28000, 122350),
                CollegeBranch("303650710", "Chemical Engineering", "course_btech", 75, 10, 85350, 5000, 4000, 28000, 122350)
            ),
            cutoffs = listOf(
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_bpharm", "B.Pharmacy", 99.45, 166.0, 620),
                CollegeCutoff("2025-26", "CAP Round 1", "OPEN", "State Level", "course_btech", "Chemical Engineering", 99.10, 159.0, 2410)
            ),
            scholarships = "ICT Endowment & MahaDBT Scholarships.",
            eligibility = "12th PCM/PCB + MHT-CET.",
            placementAvgLpa = 8.9,
            placementHighLpa = 29.0,
            placementRate = 93,
            hostelAvailable = true,
            hostelSummary = "Campus hostels for boys and girls.",
            website = "https://www.ictmumbai.edu.in",
            contactEmail = "admission@ictmumbai.edu.in",
            contactPhone = "+91 22 3361 1111",
            description = "India's top-ranked Chemical Engineering and Pharmaceutical Sciences university.",
            imageKey = "college",
            active = true
        )
    )

    val starterPackages = listOf(
        CounsellingPackage(
            id = "pkg_basic",
            name = "Basic Explorer Package",
            description = "Essential CAP cutoff lookup, college directory access, and personal college shortlist.",
            tier = PackageTier.BASIC.name,
            courseIds = listOf("course_btech", "course_mbbs", "course_bpharm", "course_bca", "course_mba"),
            originalPrice = 799L,
            discountType = DiscountType.FIXED.name,
            discountValue = 300L,
            finalPrice = 499L,
            features = listOf(
                "Full College Explorer & Intake Data",
                "Multi-Year CAP Round Cutoff Tables",
                "Personal College Shortlist & Notes",
                "Safe / Moderate / Ambitious Indicators"
            ),
            validityDays = 180L,
            displayOrder = 1L,
            active = true,
            combo = false
        ),
        CounsellingPackage(
            id = "pkg_standard",
            name = "Standard CAP Pro Package",
            description = "Everything in Basic plus side-by-side College Comparison, Fee Breakdown, and Official Preference List Builder.",
            tier = PackageTier.STANDARD.name,
            courseIds = listOf("course_btech", "course_mbbs", "course_bpharm", "course_bca", "course_mba"),
            originalPrice = 1999L,
            discountType = DiscountType.FIXED.name,
            discountValue = 500L,
            finalPrice = 1499L,
            features = listOf(
                "All Basic Package Features",
                "Detailed Tuition, Hostel & Scholarship Fees",
                "Side-by-Side 3-College Comparison",
                "CAP Preference List Builder & Submission",
                "Document Checklist & Deadline Alerts"
            ),
            validityDays = 240L,
            displayOrder = 2L,
            active = true,
            combo = false
        ),
        CounsellingPackage(
            id = "pkg_premium",
            name = "Premium 1-on-1 Counselling",
            description = "Dedicated senior counsellor assignment, option-form audit, priority support, and 1-on-1 appointment scheduling.",
            tier = PackageTier.PREMIUM.name,
            courseIds = listOf("course_btech", "course_mbbs", "course_bpharm", "course_bca", "course_mba"),
            originalPrice = 3999L,
            discountType = DiscountType.PERCENT.name,
            discountValue = 25L,
            finalPrice = 2999L,
            features = listOf(
                "All Standard Package Features",
                "Dedicated Senior Admission Counsellor",
                "1-on-1 Video/Phone Counselling Sessions",
                "Counsellor Audit of Preference List",
                "Priority CAP Round Support & Follow-ups"
            ),
            validityDays = 365L,
            displayOrder = 3L,
            active = true,
            combo = false
        ),
        CounsellingPackage(
            id = "pkg_combo_complete",
            name = "Engineering + Pharmacy Mega Combo",
            description = "Combined Standard + Priority Counselling across Engineering & Pharmacy streams with extra ₹800 combo savings.",
            tier = PackageTier.COMBO.name,
            courseIds = listOf("course_btech", "course_bpharm"),
            originalPrice = 4498L,
            discountType = DiscountType.FIXED.name,
            discountValue = 1200L,
            finalPrice = 3298L,
            features = listOf(
                "Multi-Course Access (B.Tech + B.Pharm)",
                "All Premium 1-on-1 Counselling Features",
                "Dual Preference List Review",
                "Instant Savings of ₹1,200 on Combo"
            ),
            validityDays = 365L,
            displayOrder = 4L,
            active = true,
            combo = true,
            includedPackageIds = listOf("pkg_standard", "pkg_premium"),
            promoCode = "COMBO200"
        )
    )

    val starterPromoCodes = listOf(
        PromoCode(
            id = "promo_merit200",
            code = "MERIT200",
            discountType = DiscountType.FIXED.name,
            discountValue = 200L,
            minOrderAmount = 499L,
            maxUsage = 500L,
            usedCount = 12L,
            active = true,
            expiryEpochMs = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
        ),
        PromoCode(
            id = "promo_scholar15",
            code = "SCHOLAR15",
            discountType = DiscountType.PERCENT.name,
            discountValue = 15L,
            minOrderAmount = 1000L,
            maxUsage = 200L,
            usedCount = 4L,
            active = true,
            expiryEpochMs = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
        )
    )

    val starterNotifications = listOf(
        AppNotification(
            id = "notif_cap_round1",
            title = "CAP Round 1 Option Form Window",
            message = "Keep your verified preference list ready. Ensure at least 15 Safe & Moderate branches before locking choices.",
            type = NotificationType.CAP_ROUND.name,
            targetUserId = "ALL",
            audience = "ALL",
            createdByUid = "system"
        ),
        AppNotification(
            id = "notif_doc_verification",
            title = "Document Scrutiny Reminder",
            message = "Verify Domicile, Non-Creamy Layer (NCL), EWS, and Income certificates at your nearest FC center before Round 1.",
            type = NotificationType.DEADLINE.name,
            targetUserId = "ALL",
            audience = "ALL",
            createdByUid = "system"
        )
    )
}
