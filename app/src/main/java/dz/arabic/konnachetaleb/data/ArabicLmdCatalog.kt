package dz.arabic.konnachetaleb.data

data class CatalogSubject(
    val name: String,
    val nature: String // "محاضرة + أعمال موجّهة" | "محاضرة" | "أعمال موجّهة"
)

data class AcademicTrack(
    val id: String,
    val cycle: String,          // "ليسانس" أو "ماستر"
    val year: String,           // "L1", "L2", "L3", "M1", "M2"
    val specialty: String,
    val oddSemesterCode: String,
    val evenSemesterCode: String,
    val oddSemesterSubjects: List<CatalogSubject>,
    val evenSemesterSubjects: List<CatalogSubject> = emptyList()
)

object ArabicLmdCatalog {

    const val NATURE_BOTH = "محاضرة + أعمال موجّهة"
    const val NATURE_COURS = "محاضرة"
    const val NATURE_TD = "أعمال موجّهة"

    val ALL_NATURES = listOf(NATURE_BOTH, NATURE_COURS, NATURE_TD)

    val NOTEBOOK_COLORS = listOf(
        0xFF1E3A5FL, // كحلي أكاديمي
        0xFF14532DL, // أخضر زمردي
        0xFF7C2D12L, // عنابي دافئ
        0xFF4C1D95L, // بنفسجي ملكي
        0xFF0F766EL, // فيروزي داكن
        0xFF9A3412L, // نحاسي
        0xFF1D4ED8L, // أزرق كلاسيكي
        0xFF831843L, // خمري
        0xFF365314L, // زيتوني
        0xFF334155L  // رمادي أردوازي
    )

    val NOTEBOOK_ICONS = listOf(
        "book", "quill", "scroll", "library", "grammar", "poetry", "research", "language"
    )

    private fun both(name: String) = CatalogSubject(name, NATURE_BOTH)
    private fun cours(name: String) = CatalogSubject(name, NATURE_COURS)
    private fun td(name: String) = CatalogSubject(name, NATURE_TD)

    val tracks: List<AcademicTrack> = listOf(
        // ==================== أولاً: طور الليسانس (L1 — L2 — L3) ====================
        // 1. السنة الأولى ليسانس — جذع مشترك
        AcademicTrack(
            id = "L1_COMMON",
            cycle = "ليسانس",
            year = "L1",
            specialty = "جذع مشترك",
            oddSemesterCode = "S1",
            evenSemesterCode = "S2",
            oddSemesterSubjects = listOf(
                both("نص أدبي قديم (شعر)"),
                both("نقد أدبي قديم 1"),
                both("بلاغة عربية"),
                both("علم الصرف 1"),
                both("عروض وموسيقى الشعر"),
                td("تقنيات البحث 1"),
                td("تقنيات التعبير الشفهي"),
                cours("علوم القرآن"),
                cours("تاريخ الجزائر"),
                cours("إعلام آلي وبرمجة 1")
            )
        ),
        // 2. السنة الثانية ليسانس — دراسات أدبية
        AcademicTrack(
            id = "L2_LIT",
            cycle = "ليسانس",
            year = "L2",
            specialty = "دراسات أدبية",
            oddSemesterCode = "S3",
            evenSemesterCode = "S4",
            oddSemesterSubjects = listOf(
                both("النص الأدبي الحديث"),
                both("النقد الأدبي الحديث"),
                both("لسانيات عامة"),
                both("علم الصرف 2"),
                both("نظرية الأدب"),
                both("المدارس اللسانية"),
                both("مدخل إلى الأدب المقارن"),
                td("مدخل إلى الأدب المغاربي"),
                td("أدب شعبي عام"),
                cours("اللغة الأجنبية")
            )
        ),
        // 3. السنة الثانية ليسانس — دراسات لغوية
        AcademicTrack(
            id = "L2_LING",
            cycle = "ليسانس",
            year = "L2",
            specialty = "دراسات لغوية",
            oddSemesterCode = "S3",
            evenSemesterCode = "S4",
            oddSemesterSubjects = listOf(
                both("علم الصرف 2"),
                both("لسانيات عامة"),
                both("النص الأدبي الحديث"),
                both("النقد الأدبي الحديث"),
                both("المدارس اللسانية"),
                both("مدخل إلى الأدب المقارن"),
                both("نظرية الأدب"),
                td("علم الدلالة"),
                td("أصول النحو"),
                cours("اللغة الأجنبية")
            )
        ),
        // 4. السنة الثانية ليسانس — دراسات نقدية
        AcademicTrack(
            id = "L2_CRIT",
            cycle = "ليسانس",
            year = "L2",
            specialty = "دراسات نقدية",
            oddSemesterCode = "S3",
            evenSemesterCode = "S4",
            oddSemesterSubjects = listOf(
                both("النص الأدبي الحديث"),
                both("النقد الأدبي الحديث"),
                both("مدخل إلى الأدب المقارن"),
                both("المدارس اللسانية"),
                both("علم الصرف 2"),
                both("لسانيات عامة"),
                both("نظرية الأدب"),
                td("نظرية الأجناس الأدبية"),
                td("فلسفة النقد الأدبي"),
                cours("اللغة الأجنبية")
            )
        ),
        // 5. السنة الثالثة ليسانس — أدب عربي
        AcademicTrack(
            id = "L3_ARABIC_LIT",
            cycle = "ليسانس",
            year = "L3",
            specialty = "أدب عربي",
            oddSemesterCode = "S5",
            evenSemesterCode = "S6",
            oddSemesterSubjects = listOf(
                both("قضايا النص الشعري الحديث والمعاصر"),
                both("قضايا النص الشعري القديم"),
                both("السرديات العربية الحديثة والمعاصرة"),
                both("جماليات السرد العربي القديم"),
                both("الآداب العالمية المعاصرة"),
                both("منهجية البحث الأدبي"),
                both("أدب الطفل"),
                td("الأدب الصوفي"),
                td("الأدب العربي والاستشراق"),
                cours("لغة أجنبية")
            )
        ),
        // 6. السنة الثالثة ليسانس — لسانيات عامة
        AcademicTrack(
            id = "L3_LING",
            cycle = "ليسانس",
            year = "L3",
            specialty = "لسانيات عامة",
            oddSemesterCode = "S5",
            evenSemesterCode = "S6",
            oddSemesterSubjects = listOf(
                both("لسانيات عربية"),
                both("منهجية البحث اللغوي"),
                both("لسانيات النص"),
                both("المدارس النحوية"),
                both("المصطلحية"),
                both("المعجمية"),
                both("علم الدلالة 1"),
                td("أدب جزائري"),
                td("نظرية النظم"),
                cours("لغة أجنبية")
            )
        ),
        // 7. السنة الثالثة ليسانس — نقد ودراسات أدبية
        AcademicTrack(
            id = "L3_CRIT",
            cycle = "ليسانس",
            year = "L3",
            specialty = "نقد ودراسات أدبية",
            oddSemesterCode = "S5",
            evenSemesterCode = "S6",
            oddSemesterSubjects = listOf(
                both("النقد ما بعد البنيوية"),
                both("مناهج النقد النسقي"),
                both("مناهج النقد السياقي"),
                both("نقد الشعر"),
                both("نقد النثر الحديث والمعاصر"),
                both("نقد النثر القديم"),
                both("نظرية الشعر عند الفلاسفة المسلمين"),
                td("الأدب الجزائري"),
                td("الأدب المقارن"),
                cours("لغة أجنبية")
            )
        ),

        // ==================== ثانياً: طور الماستر (M1 — M2) ====================
        // 8. السنة الأولى ماستر — أدب عربي حديث ومعاصر
        AcademicTrack(
            id = "M1_MODERN_LIT",
            cycle = "ماستر",
            year = "M1",
            specialty = "أدب عربي حديث ومعاصر",
            oddSemesterCode = "M1-S1",
            evenSemesterCode = "M1-S2",
            oddSemesterSubjects = listOf(
                both("عروض وموسيقى الشعر"),
                both("النقد العربي الحديث"),
                both("الأدب العربي الحديث"),
                both("بيداغوجيا"),
                both("نظرية الأدب"),
                both("طرائق تحليل النصوص"),
                cours("البحث العلمي أصوله ومناهجه"),
                cours("الذكاء الاصطناعي"),
                cours("لسانيات الخطاب"),
                cours("لغة أجنبية")
            )
        ),
        // 9. السنة الأولى ماستر — أدب عربي قديم
        AcademicTrack(
            id = "M1_CLASSICAL_LIT",
            cycle = "ماستر",
            year = "M1",
            specialty = "أدب عربي قديم",
            oddSemesterCode = "M1-S1",
            evenSemesterCode = "M1-S2",
            oddSemesterSubjects = listOf(
                both("موسيقى الشعر 1"),
                both("مناهج تحليل الخطاب"),
                both("منهجية البحث 1"),
                both("البلاغة العربية 1"),
                both("نقد الشعر"),
                both("النص الشعري القديم"),
                cours("الأدب المقارن"),
                cours("تاريخ الأدب القديم 1"),
                cours("الذكاء الاصطناعي"),
                cours("لغة أجنبية")
            )
        ),
        // 10. السنة الأولى ماستر — لسانيات عربية
        AcademicTrack(
            id = "M1_ARABIC_LING",
            cycle = "ماستر",
            year = "M1",
            specialty = "لسانيات عربية",
            oddSemesterCode = "M1-S1",
            evenSemesterCode = "M1-S2",
            oddSemesterSubjects = listOf(
                both("اللسانيات النظرية"),
                both("المصادر الصرفية والنحوية"),
                both("أصول النحو العربي"),
                both("منهجية البحث العلمي"),
                both("تاريخ النحو العربي"),
                cours("الخط العربي وقواعده"),
                cours("تقنيات الكتابة والبحث"),
                cours("تحقيق النصوص والمخطوطات"),
                cours("الذكاء الاصطناعي"),
                cours("لغة أجنبية")
            )
        ),
        // 11. السنة الأولى ماستر — لسانيات الخطاب
        AcademicTrack(
            id = "M1_DISCOURSE_LING",
            cycle = "ماستر",
            year = "M1",
            specialty = "لسانيات الخطاب",
            oddSemesterCode = "M1-S1",
            evenSemesterCode = "M1-S2",
            oddSemesterSubjects = listOf(
                both("منهجية تحليل الخطاب 1"),
                both("منهجية البحث وتقنياته"),
                both("اللسانيات التقابلية"),
                both("فلسفة اللغة"),
                both("الأسلوبيات المقارنة"),
                both("النحو الوظيفي 1"),
                both("الدلاليات الحديثة"),
                cours("نظريات التواصل"),
                cours("الذكاء الاصطناعي"),
                cours("لغة أجنبية")
            )
        ),
        // 12. السنة الأولى ماستر — نقد حديث ومعاصر
        AcademicTrack(
            id = "M1_MODERN_CRIT",
            cycle = "ماستر",
            year = "M1",
            specialty = "نقد حديث ومعاصر",
            oddSemesterCode = "M1-S1",
            evenSemesterCode = "M1-S2",
            oddSemesterSubjects = listOf(
                both("مناهج نقدية"),
                both("النقد الثقافي"),
                both("نظريات التلقي والتأويل"),
                both("علوم السرد"),
                both("المفهوم النقدي"),
                both("النظريات النصية"),
                td("نصوص نقدية معاصرة غربية وعربية"),
                td("تقنيات ترجمة المصطلح النقدي"),
                cours("الذكاء الاصطناعي"),
                cours("لغة أجنبية")
            )
        ),
        // 13. السنة الثانية ماستر — أدب عربي حديث ومعاصر
        AcademicTrack(
            id = "M2_MODERN_LIT",
            cycle = "ماستر",
            year = "M2",
            specialty = "أدب عربي حديث ومعاصر",
            oddSemesterCode = "M2-S3",
            evenSemesterCode = "M2-S4",
            oddSemesterSubjects = listOf(
                both("النص الأدبي وتحليل الخطاب"),
                both("الشعرية"),
                both("بيداغوجيا المهارات"),
                both("تقنيات البحث والتحرير"),
                both("السردية العربية"),
                both("المسرح العربي"),
                cours("منهجية البحث"),
                cours("الأدب والسينما"),
                cours("أخلاقيات المهنة"),
                cours("أدب عربي قديم")
            ),
            evenSemesterSubjects = listOf(both("مذكرة الماستر"))
        ),
        // 14. السنة الثانية ماستر — أدب عربي قديم
        AcademicTrack(
            id = "M2_CLASSICAL_LIT",
            cycle = "ماستر",
            year = "M2",
            specialty = "أدب عربي قديم",
            oddSemesterCode = "M2-S3",
            evenSemesterCode = "M2-S4",
            oddSemesterSubjects = listOf(
                both("نظرية الأدب"),
                both("المخطوط العربي"),
                both("المصادر العربية القديمة"),
                both("منهجية البحث 3"),
                both("بنية الخطاب الأدبي القديم"),
                both("الأدب الجزائري القديم"),
                cours("تاريخ الأدب القديم 3"),
                cours("مصادر النقد والبلاغة"),
                cours("الأدب الشعبي الجزائري القديم"),
                cours("أخلاقيات المهنة")
            ),
            evenSemesterSubjects = listOf(both("مذكرة الماستر"))
        ),
        // 15. السنة الثانية ماستر — لسانيات عربية
        AcademicTrack(
            id = "M2_ARABIC_LING",
            cycle = "ماستر",
            year = "M2",
            specialty = "لسانيات عربية",
            oddSemesterCode = "M2-S3",
            evenSemesterCode = "M2-S4",
            oddSemesterSubjects = listOf(
                both("علم المصطلح"),
                both("اللسانيات وتعليمية اللغة العربية"),
                both("مناهج البحث في اللغة"),
                both("النقد النحوي للجرائد والمجلات"),
                both("اللسانيات التداولية"),
                both("نظريات النحو العربي"),
                cours("فلسفة اللغة"),
                cours("السيميائية"),
                cours("التفكير اللساني في الحضارة العربية"),
                cours("أخلاقيات المهنة")
            ),
            evenSemesterSubjects = listOf(both("مذكرة الماستر"))
        ),
        // 16. السنة الثانية ماستر — لسانيات الخطاب
        AcademicTrack(
            id = "M2_DISCOURSE_LING",
            cycle = "ماستر",
            year = "M2",
            specialty = "لسانيات الخطاب",
            oddSemesterCode = "M2-S3",
            evenSemesterCode = "M2-S4",
            oddSemesterSubjects = listOf(
                both("اللسانيات المعرفية"),
                both("الذكاء الاصطناعي"),
                both("الترجمة الوظيفية"),
                both("السيمائيات التداولية"),
                both("اللسانيات الحاسوبية"),
                both("المعالجة النصية للخطابات"),
                cours("مناهج البحث"),
                cours("التعدد اللغوي وقضاياه"),
                cours("ترجمة"),
                cours("أخلاقيات المهنة")
            ),
            evenSemesterSubjects = listOf(both("مذكرة الماستر"))
        ),
        // 17. السنة الثانية ماستر — نقد حديث ومعاصر
        AcademicTrack(
            id = "M2_MODERN_CRIT",
            cycle = "ماستر",
            year = "M2",
            specialty = "نقد حديث ومعاصر",
            oddSemesterCode = "M2-S3",
            evenSemesterCode = "M2-S4",
            oddSemesterSubjects = listOf(
                both("المنهج التفكيكي"),
                both("تداولية النص الأدبي"),
                both("نظريات الأجناس الأدبية"),
                both("السيميائية السردية"),
                both("نقد النقد"),
                both("لسانيات النص"),
                td("التحليل البنيوي للنصوص"),
                td("تقنيات كتابة مذكرة التخرج"),
                td("مهارات التحرير"),
                cours("أخلاقيات المهنة")
            ),
            evenSemesterSubjects = listOf(both("مذكرة الماستر"))
        )
    )

    fun cycles(): List<String> = listOf("ليسانس", "ماستر")

    fun yearsForCycle(cycle: String): List<String> =
        if (cycle == "ماستر") listOf("M1", "M2") else listOf("L1", "L2", "L3")

    fun specialtiesFor(cycle: String, year: String): List<String> =
        tracks.filter { it.cycle == cycle && it.year == year }.map { it.specialty }.distinct()

    fun trackFor(cycle: String, year: String, specialty: String): AcademicTrack? =
        tracks.firstOrNull { it.cycle == cycle && it.year == year && it.specialty == specialty }

    fun semestersFor(cycle: String, year: String, specialty: String): List<String> {
        val t = trackFor(cycle, year, specialty) ?: return emptyList()
        return listOf(t.oddSemesterCode, t.evenSemesterCode)
    }

    fun subjectsFor(cycle: String, year: String, specialty: String, semester: String): List<CatalogSubject> {
        val t = trackFor(cycle, year, specialty) ?: return emptyList()
        return when (semester) {
            t.oddSemesterCode -> t.oddSemesterSubjects
            t.evenSemesterCode -> t.evenSemesterSubjects
            else -> emptyList()
        }
    }
}
