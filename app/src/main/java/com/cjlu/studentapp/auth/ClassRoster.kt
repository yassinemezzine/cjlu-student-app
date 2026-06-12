package com.cjlu.studentapp.auth

/**
 * Official class roster: student ID, name as registered, and teaching section (班级).
 * Password for first sign-in defaults to the student ID (see [AuthManager]).
 */
data class RosterStudent(
    val studentId: String,
    val displayName: String,
    val classSection: String,
)

object ClassRoster {

    val students: List<RosterStudent> = listOf(
        RosterStudent("20220951", "SAYOUTI HATIM", "23计算机L1"),
        RosterStudent("20230901", "ISSADEEN MOHAMED IJAS", "23计算机L1"),
        RosterStudent("20230928", "ZOUHEIR RIHAB", "23计算机L1"),
        RosterStudent("20230932", "LAHRECH YOUSSRA", "23计算机L1"),
        RosterStudent("20230934", "DERAI RAJAE", "23计算机L1"),
        RosterStudent("20230951", "KENFAOUI ISMAIL", "23计算机L1"),
        RosterStudent("20230958", "DAHMOUN NOURA", "23计算机L1"),
        RosterStudent("20230983", "MERNISSI WASSIM", "23计算机L1"),
        RosterStudent("20230986", "EL KHESHEN NADINE", "23计算机L1"),
        RosterStudent("20230994", "ABOUROUH SANAE", "23计算机L1"),
        RosterStudent("20231009", "AMOURI FATIMA", "23计算机L1"),
        RosterStudent("20231056", "LIJOUAB NADA", "23计算机L1"),
        RosterStudent("20231057", "DRIOUCH MANAL", "23计算机L1"),
        RosterStudent("20231100", "LAKRAA HAJAR", "23计算机L1"),
        RosterStudent("20231102", "FELLAH BOUGHABA ABDELLAH", "23计算机L1"),
        RosterStudent("20231103", "DOUASS BOUCHEJRA ROMAISSAE", "23计算机L1"),
        RosterStudent("20231104", "SLIM NADA", "23计算机L1"),
        RosterStudent("20231134", "GUENDOUL NOUR EL HOUDA", "23计算机L1"),
        RosterStudent("20231230", "SADIKEEN RAZIM AHMED", "23计算机L1"),
        RosterStudent("20231472", "SAJJAD FAISAL", "23计算机L1"),
        RosterStudent("20231510", "ALI HAZRAT", "23计算机L1"),
        RosterStudent("20231517", "SHREYA WANAIZA RAHMAN", "23计算机L1"),
        RosterStudent("20230904", "CHAWAIPIRA PANASHE EMMANUEL", "23计算机L2"),
        RosterStudent("20230929", "KHACHINI OTHMANE", "23计算机L2"),
        RosterStudent("20230937", "CHEHABI LAHCEN", "23计算机L2"),
        RosterStudent("20230941", "OUKASMIH MAROUA", "23计算机L2"),
        RosterStudent("20230945", "JOUIBER ANASS", "23计算机L2"),
        RosterStudent("20230946", "AZERKANE AMAL", "23计算机L2"),
        RosterStudent("20230950", "MEZZINE YASSINE", "23计算机L2"),
        RosterStudent("20230954", "EL BARNAOUI EL MEHDI", "23计算机L2"),
        RosterStudent("20230957", "IGNANE RACHID", "23计算机L2"),
        RosterStudent("20230961", "SAYI MICHELLE SANDISWA", "23计算机L2"),
        RosterStudent("20230962", "NADIFI JILALI", "23计算机L2"),
        RosterStudent("20230974", "HALOUANE WIAM", "23计算机L2"),
        RosterStudent("20231010", "NADIR ZINEB", "23计算机L2"),
        RosterStudent("20231019", "KHALDI MOHAMED AMINE", "23计算机L2"),
        RosterStudent("20231024", "MAOULAININE MRABIH RABOU", "23计算机L2"),
        RosterStudent("20231028", "AMCHAT HOUSSAM", "23计算机L2"),
        RosterStudent("20231047", "JAAFARY NOUR", "23计算机L2"),
        RosterStudent("20231064", "AZZAB AHLAM", "23计算机L2"),
        RosterStudent("20231069", "GHANEM MOHAMED", "23计算机L2"),
        RosterStudent("20231093", "LEDARI NISSRINE", "23计算机L2"),
        RosterStudent("20231141", "IGUIDI MOHAMED REDA", "23计算机L2"),
        RosterStudent("20231142", "BAKKARI FATIMA", "23计算机L2"),
        RosterStudent("20231145", "TIZI ILYASS", "23计算机L2"),
    )

    fun find(studentId: String): RosterStudent? =
        students.firstOrNull { it.studentId == studentId.trim() }
}
