package com.cjlu.studentapp.ui.screens

import androidx.compose.ui.text.input.KeyboardType
import com.cjlu.studentapp.ui.forms.AgreementBlockUiModel
import com.cjlu.studentapp.ui.forms.FormFieldUiModel
import com.cjlu.studentapp.ui.forms.OptionItem
import com.cjlu.studentapp.ui.forms.PrefillSource
import com.cjlu.studentapp.ui.forms.ServiceFormUiModel
import com.cjlu.studentapp.ui.forms.TextInputValidation
import com.cjlu.studentapp.ui.forms.UiText
import com.cjlu.studentapp.ui.forms.UploadFieldUiModel
import com.cjlu.studentapp.ui.forms.uiText

object ServiceFormCatalog {
    private val targetMajorOptions = listOf(
        option("computer_science", "Computer Science", "计算机科学"),
        option("electrical_engineering", "Electrical Engineering", "电气工程"),
        option("international_economics", "International Economics", "国际经济"),
        option("business_chinese", "Business Chinese", "商务汉语"),
        option("industrial_design", "Industrial Design", "工业设计")
    )

    private val semesterOptions = listOf(
        option("spring_2026", "Spring 2026", "2026年春季学期"),
        option("autumn_2026", "Autumn 2026", "2026年秋季学期"),
        option("spring_2027", "Spring 2027", "2027年春季学期")
    )

    private val scholarshipOptions = listOf(
        option("provincial", "Provincial Scholarship", "省级奖学金"),
        option("president", "President Scholarship", "校长奖学金"),
        option("international", "International Student Scholarship", "国际学生奖学金"),
        option("special", "Special Merit Scholarship", "特别优秀奖学金")
    )

    private val studentCategoryOptions = listOf(
        option("undergraduate", "International Undergraduate", "国际本科生"),
        option("language", "Language Student", "语言生"),
        option("self_funded", "Self-funded Student", "自费生"),
        option("csc", "CSC Scholarship Student", "中国政府奖学金学生")
    )

    private val bankOptions = listOf(
        option("icbc", "ICBC", "工商银行"),
        option("boc", "BOC", "中国银行"),
        option("others", "Others", "其他")
    )

    private val receiverOptions = listOf(
        option("self", "I receive deposit myself", "我本人领取押金"),
        option("other", "Others receive deposit for me", "他人代我领取押金")
    )

    private val formalitiesOptions = listOf(
        option("completed", "Completed", "已完成"),
        option("in_progress", "In progress", "办理中"),
        option("need_help", "Need office support", "需要办公室协助")
    )

    private val genderOptions = listOf(
        option("male", "Male", "男"),
        option("female", "Female", "女"),
        option("other", "Other", "其他")
    )

    private val nationalityOptions = listOf(
        option("morocco", "Morocco", "摩洛哥"),
        option("china", "China", "中国"),
        option("kazakhstan", "Kazakhstan", "哈萨克斯坦"),
        option("uzbekistan", "Uzbekistan", "乌兹别克斯坦"),
        option("other", "Other", "其他")
    )

    private val roomCardTypeOptions = listOf(
        option("lost_card", "Lost room card", "房卡遗失"),
        option("lost_key", "Lost key", "钥匙遗失"),
        option("broken_card", "Broken room card", "房卡损坏"),
        option("broken_key", "Broken key", "钥匙损坏")
    )

    private val returnTransportOptions = listOf(
        option("flight", "Flight", "飞机"),
        option("train", "Train", "火车"),
        option("bus", "Bus / coach", "汽车"),
        option("other_transport", "Other", "其他")
    )

    private val informationConfirmationCategoryOptions = listOf(
        option("enrollment", "Enrollment / registration", "学籍/注册"),
        option("personal_bio", "Personal biographic data", "个人基本信息"),
        option("visa_address", "Visa or address details", "签证或地址信息"),
        option("contact_update", "Contact information update", "联系方式更新"),
        option("other_info", "Other", "其他")
    )

    private val calendarDeliveryOptions = listOf(
        option("digital_email", "Digital copy via email", "电子邮件电子版"),
        option("office_pickup", "Office pickup", "办公室领取"),
        option("notice_reference", "Noticeboard / portal reference only", "公告栏或门户参考")
    )

    private val transcriptTypeOptions = listOf(
        option("english_only", "English transcript", "英文成绩单"),
        option("chinese_only", "Chinese transcript", "中文成绩单"),
        option("both_languages", "Both English and Chinese", "中英文均需要"),
        option("sealed_copies", "Sealed official copies", "密封官方件")
    )

    private val depositRefundReasonOptions = listOf(
        option("graduation_move_out", "Graduation / move-out", "毕业/退宿"),
        option("room_change", "Room change settlement", "换宿结算"),
        option("billing_correction", "Billing correction", "费用更正"),
        option("other_deposit", "Other", "其他")
    )

    fun find(serviceId: String): ServiceFormUiModel? {
        return formMap[serviceId]
    }

    private val forms = listOf(
        ServiceFormUiModel(
            serviceId = "change_major",
            intro = uiText(
                "Submit a target-major request with your reason and supporting form.",
                "填写转专业原因并上传相关表单。"
            ),
            fields = listOf(
                studentIdField(),
                multilineField(
                    key = "reason",
                    en = "Reason for application",
                    zh = "申请原因",
                    required = true,
                    placeholderEn = "Explain why you want to change your major",
                    placeholderZh = "请说明转专业原因"
                ),
                dropdownField(
                    key = "target_major",
                    en = "Target major",
                    zh = "目标专业",
                    options = targetMajorOptions,
                    required = true
                ),
                textField(
                    key = "current_grade",
                    en = "Current grade",
                    zh = "当前年级",
                    prefillSource = PrefillSource.StudyYear,
                    required = true
                ),
                downloadField("change_major_form")
            ),
            uploads = listOf(
                uploadField(
                    key = "change_major_attachment",
                    en = "Attachment upload",
                    zh = "附件上传",
                    required = true,
                    suggestedFileEn = "change_major_supporting.pdf",
                    suggestedFileZh = "转专业支撑材料.pdf"
                )
            ),
            tips = listOf(
                uiText(
                    "Advisor approval can be added later when the workflow becomes real.",
                    "后续接入真实流程时可再加入导师审核。"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "ask_leave",
            intro = uiText(
                "Apply for sick leave when you cannot attend class. Once approved, missed sessions in the covered dates will not lower your attendance record.",
                "因病无法上课时提交病假申请。审批通过后，请假期间缺课不计入出勤扣分。"
            ),
            fields = listOf(
                studentIdField(),
                multilineField(
                    key = "absent_reasons",
                    en = "Illness / symptoms",
                    zh = "病情说明",
                    required = true,
                    placeholderEn = "Describe your illness and symptoms",
                    placeholderZh = "请说明疾病与症状"
                ),
                dateField("start_time", "Sick leave starts", "病假开始", required = true),
                dateField("end_time", "Sick leave ends", "病假结束", required = true),
                textField(
                    key = "days_of_leave",
                    en = "Sick leave days",
                    zh = "病假天数",
                    required = true,
                    digitsOnly = true,
                    maxLength = 3,
                    keyboardType = KeyboardType.Number
                ),
                textField(
                    key = "detail_address",
                    en = "Where you will rest",
                    zh = "病假期间所在地",
                    required = true,
                    placeholderEn = "Dorm room, home address, or hospital",
                    placeholderZh = "宿舍、家庭住址或就医地点"
                ),
                textField(
                    key = "phone_number",
                    en = "Phone number",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "doctor_diagnosis",
                    en = "Medical certificate",
                    zh = "医疗证明",
                    required = true,
                    suggestedFileEn = "medical_certificate.pdf",
                    suggestedFileZh = "医疗证明.pdf"
                )
            ),
            tips = listOf(
                uiText(
                    "Submit before or on the first day you miss class. Unapproved absences still count against attendance.",
                    "请在缺课当天或之前提交。未审批的缺课仍会计入出勤。"
                ),
                uiText(
                    "Upload a doctor's note or hospital record that covers your sick leave dates.",
                    "请上传覆盖病假日期的诊断书或就诊记录。"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "transfer_school",
            intro = uiText(
                "Prepare your transfer target and upload the new-school admission letter.",
                "填写转学目标信息并上传新学校录取通知书。"
            ),
            fields = listOf(
                studentIdField(),
                dateField("leave_time", "Leave time", "离校时间", required = true),
                textField(
                    key = "transfer_to_major",
                    en = "Transfer to major",
                    zh = "转入专业",
                    required = true
                ),
                textField(
                    key = "transfer_to_school",
                    en = "Transfer to school",
                    zh = "转入学校",
                    required = true
                ),
                multilineField(
                    key = "transfer_reason",
                    en = "Reasons of transfer school",
                    zh = "转学原因",
                    required = true,
                    placeholderEn = "Explain why you want to transfer",
                    placeholderZh = "请说明转学原因"
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "admission_letter",
                    en = "Admission letter from new school upload",
                    zh = "新学校录取通知书上传",
                    required = true,
                    suggestedFileEn = "new_school_admission_letter.pdf",
                    suggestedFileZh = "新学校录取通知书.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "quit_school",
            intro = uiText(
                "Record withdrawal details and your current departure formalities status.",
                "填写退学原因并确认当前离校手续状态。"
            ),
            fields = listOf(
                studentIdField(),
                multilineField(
                    key = "quit_reasons",
                    en = "Quit reasons",
                    zh = "退学原因",
                    required = true
                ),
                dateField("leave_time", "Leave time", "离校时间", required = true),
                textField(
                    key = "quit_school_to",
                    en = "Quit school to",
                    zh = "退学去向",
                    required = true
                ),
                dropdownField(
                    key = "departure_formalities",
                    en = "School departure formalities",
                    zh = "离校手续",
                    options = formalitiesOptions,
                    required = true
                ),
                multilineField(
                    key = "note",
                    en = "Note",
                    zh = "备注",
                    placeholderEn = "Add anything the office should know",
                    placeholderZh = "补充其他说明"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "changing_room",
            intro = uiText(
                "Describe the current room issue and attach the signed room-change file.",
                "填写当前宿舍情况并上传换宿申请材料。"
            ),
            fields = listOf(
                textField(
                    key = "current_room",
                    en = "Current room",
                    zh = "当前房间",
                    required = true,
                    placeholderEn = "Example: Dorm 5 - 302",
                    placeholderZh = "例如：5号楼302"
                ),
                multilineField(
                    key = "apply_reason",
                    en = "Apply reason",
                    zh = "申请原因",
                    required = true
                ),
                downloadField("change_room_form")
            ),
            uploads = listOf(
                uploadField(
                    key = "change_room_attachment",
                    en = "Attachment upload",
                    zh = "附件上传",
                    required = true,
                    suggestedFileEn = "room_change_attachment.pdf",
                    suggestedFileZh = "换宿附件.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "repair_request",
            intro = uiText(
                "Report a maintenance issue with a short location note and optional image.",
                "填写报修位置和问题描述，可附上现场或问题相关照片。"
            ),
            fields = listOf(
                textField(
                    key = "location",
                    en = "Location",
                    zh = "报修位置",
                    required = true,
                    placeholderEn = "Dorm 5 - 302 / classroom / hallway",
                    placeholderZh = "宿舍、教室或走廊等具体位置"
                ),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "报修原因",
                    required = true,
                    placeholderEn = "What needs to be repaired?",
                    placeholderZh = "请描述需要维修的问题"
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "repair_upload",
                    en = "Upload file",
                    zh = "上传文件",
                    required = false,
                    suggestedFileEn = "repair_photo.jpg",
                    suggestedFileZh = "报修照片.jpg"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "live_off_campus",
            intro = uiText(
                "Provide your landlord details and upload the lease documents for approval.",
                "填写房东信息并上传租房材料用于校外住宿审批。"
            ),
            fields = listOf(
                textField("address", "Address", "住址", required = true),
                textField("landlord_name", "Landlord name", "房东姓名", required = true),
                textField(
                    key = "landlord_phone_number",
                    en = "Landlord phone number",
                    zh = "房东联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                ),
                textField(
                    key = "landlord_id_number",
                    en = "Landlord id number",
                    zh = "房东身份证号",
                    required = true,
                    maxLength = 32
                ),
                multilineField(
                    key = "apply_reason",
                    en = "Apply reason",
                    zh = "申请原因",
                    required = true
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "lease_contract_attachment",
                    en = "Lease contract attachment",
                    zh = "租房合同附件",
                    required = true,
                    suggestedFileEn = "lease_contract.pdf",
                    suggestedFileZh = "租房合同.pdf"
                ),
                uploadField(
                    key = "landlord_id_attachment",
                    en = "Landlord ID photo attachment",
                    zh = "房东身份证照片附件",
                    required = true,
                    suggestedFileEn = "landlord_id_photo.jpg",
                    suggestedFileZh = "房东身份证照片.jpg"
                )
            ),
            agreements = listOf(
                AgreementBlockUiModel(
                    key = "off_campus_agreement",
                    text = uiText(
                        "I confirm the address is correct and I will follow off-campus accommodation rules.",
                        "我确认以上住址真实有效，并愿意遵守校外住宿管理规定。"
                    )
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "restudy",
            intro = uiText(
                "Use this form for restudying plans and attach the related academic proof.",
                "此表用于重修申请，请附上相关学业证明。"
            ),
            fields = listOf(
                textField(
                    key = "mobile",
                    en = "Mobile",
                    zh = "手机号",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                ),
                textField(
                    key = "email",
                    en = "Email",
                    zh = "邮箱",
                    required = true,
                    keyboardType = KeyboardType.Email
                ),
                textField(
                    key = "grade",
                    en = "Grade",
                    zh = "年级",
                    prefillSource = PrefillSource.StudyYear,
                    required = true
                ),
                textField(
                    key = "major",
                    en = "Major",
                    zh = "专业",
                    prefillSource = PrefillSource.Major,
                    required = true
                ),
                textField("subject", "Subject", "课程", required = true),
                textField(
                    key = "credits",
                    en = "Credits",
                    zh = "学分",
                    required = true,
                    keyboardType = KeyboardType.Number,
                    maxLength = 4
                ),
                dropdownField(
                    key = "retaking_semester",
                    en = "Retaking semester",
                    zh = "重修学期",
                    options = semesterOptions,
                    required = true
                ),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "原因",
                    required = true
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "restudy_attachment",
                    en = "Attachment upload",
                    zh = "附件上传",
                    required = true,
                    suggestedFileEn = "restudy_attachment.pdf",
                    suggestedFileZh = "重修附件.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "self_study",
            intro = uiText(
                "Upload the self-study form and explain your planned study arrangement.",
                "上传自修表格并说明自修安排。"
            ),
            fields = listOf(
                textField("subject", "Subject", "课程", required = true),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "原因",
                    required = true
                ),
                downloadField("self_study_form")
            ),
            uploads = listOf(
                uploadField(
                    key = "self_study_attachment",
                    en = "Attachment",
                    zh = "附件",
                    required = true,
                    suggestedFileEn = "self_study_attachment.pdf",
                    suggestedFileZh = "自修附件.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "delay_exams",
            intro = uiText(
                "Capture the delayed-exam request, note, and your preferred retake date.",
                "填写延考原因、备注和拟参加补考日期。"
            ),
            fields = listOf(
                textField("subject", "Subject", "课程", required = true),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "原因",
                    required = true
                ),
                multilineField(
                    key = "note",
                    en = "Note",
                    zh = "备注"
                ),
                dateField("retake_date", "Retake date", "补考日期", required = true),
                downloadField("delay_exam_form")
            ),
            uploads = listOf(
                uploadField(
                    key = "delay_exam_attachment",
                    en = "Attachment",
                    zh = "附件",
                    required = true,
                    suggestedFileEn = "delay_exam_attachment.pdf",
                    suggestedFileZh = "延考附件.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "suspension_degree",
            intro = uiText(
                "Choose your suspension dates, add a reason, and confirm the agreement.",
                "填写休学时间、原因，并确认相关协议。"
            ),
            fields = listOf(
                dateField("start_date", "Start date", "开始日期", required = true),
                dateField("end_date", "End date", "结束日期", required = true),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "原因",
                    required = true
                ),
                downloadField("suspension_form")
            ),
            uploads = listOf(
                uploadField(
                    key = "suspension_attachment",
                    en = "Attachment",
                    zh = "附件",
                    required = true,
                    suggestedFileEn = "suspension_attachment.pdf",
                    suggestedFileZh = "休学附件.pdf"
                )
            ),
            agreements = listOf(
                AgreementBlockUiModel(
                    key = "suspension_agreement",
                    text = uiText(
                        "I understand the suspension period will affect course registration and attendance.",
                        "我已知晓休学期间会影响选课和出勤记录。"
                    )
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "resume_school",
            intro = uiText(
                "Provide your travel window and confirm your planned return date.",
                "填写所在国家、休学时间和计划返校日期。"
            ),
            fields = listOf(
                dropdownField(
                    key = "country",
                    en = "Country",
                    zh = "国家",
                    options = nationalityOptions,
                    required = true
                ),
                dateField("start_date", "Start date", "开始日期", required = true),
                dateField("end_date", "End date", "结束日期", required = true),
                dateField("return_date", "Return date", "返校日期", required = true),
                multilineField(
                    key = "reason",
                    en = "Reason",
                    zh = "原因",
                    required = true
                )
            ),
            agreements = listOf(
                AgreementBlockUiModel(
                    key = "resume_agreement",
                    text = uiText(
                        "I confirm I am ready to resume classes and comply with university return requirements.",
                        "我确认已做好复学准备，并愿意遵守学校返校要求。"
                    )
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "scholarship",
            intro = uiText(
                "Prepare your attendance, credits, scores, and transcript uploads for scholarship review.",
                "填写出勤、学分、成绩并上传成绩单用于奖学金评审。"
            ),
            fields = listOf(
                studentIdField(),
                textField(
                    key = "full_name",
                    en = "Full name",
                    zh = "姓名",
                    prefillSource = PrefillSource.StudentName,
                    required = true
                ),
                textField(
                    key = "attendance_rate",
                    en = "Average attendance rate",
                    zh = "平均出勤率",
                    required = true,
                    placeholderEn = "Example: 96%",
                    placeholderZh = "例如：96%"
                ),
                dropdownField(
                    key = "scholarship_category",
                    en = "Categories of scholarship",
                    zh = "奖学金类别",
                    options = scholarshipOptions,
                    required = true
                ),
                dropdownField(
                    key = "student_category",
                    en = "Student category",
                    zh = "学生类别",
                    options = studentCategoryOptions,
                    required = true
                ),
                textField(
                    key = "total_credits",
                    en = "Total credits",
                    zh = "总学分",
                    required = true,
                    keyboardType = KeyboardType.Number,
                    maxLength = 4
                ),
                multilineField(
                    key = "courses_scores",
                    en = "All courses and final exam scores",
                    zh = "所有课程及期末成绩",
                    required = true,
                    maxLength = 400
                ),
                multilineField(
                    key = "other_achievements",
                    en = "Other achievements text",
                    zh = "其他成果说明",
                    maxLength = 300
                ),
                multilineField(
                    key = "self_assessment",
                    en = "Self-assessment text",
                    zh = "自我评价",
                    required = true,
                    maxLength = 400
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "transcripts_upload",
                    en = "Transcripts upload",
                    zh = "成绩单上传",
                    required = true,
                    suggestedFileEn = "transcript.pdf",
                    suggestedFileZh = "成绩单.pdf"
                ),
                uploadField(
                    key = "hsk_upload",
                    en = "HSK certificate upload",
                    zh = "HSK证书上传",
                    required = true,
                    suggestedFileEn = "hsk_certificate.pdf",
                    suggestedFileZh = "HSK证书.pdf"
                ),
                uploadField(
                    key = "awards_upload",
                    en = "Other awards/honors upload",
                    zh = "其他获奖或荣誉上传",
                    required = false,
                    suggestedFileEn = "awards_and_honors.pdf",
                    suggestedFileZh = "奖项与荣誉.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "visa_extension",
            intro = uiText(
                "Upload the main passport and residence documents needed for a visa extension review.",
                "上传签证延期所需的主要护照和居留材料。"
            ),
            fields = emptyList(),
            uploads = listOf(
                uploadField(
                    key = "passport_page",
                    en = "Valid passport page upload",
                    zh = "护照有效页上传",
                    required = true,
                    suggestedFileEn = "passport_valid_page.pdf",
                    suggestedFileZh = "护照有效页.pdf"
                ),
                uploadField(
                    key = "residence_page",
                    en = "Current temporary residence permit page upload",
                    zh = "当前临时居留许可页上传",
                    required = true,
                    suggestedFileEn = "current_residence_permit.pdf",
                    suggestedFileZh = "当前居留许可页.pdf"
                ),
                uploadField(
                    key = "entry_stamp",
                    en = "Recent entry China stamp upload",
                    zh = "最近一次入境中国盖章页上传",
                    required = true,
                    suggestedFileEn = "entry_china_stamp.pdf",
                    suggestedFileZh = "入境中国盖章页.pdf"
                ),
                uploadField(
                    key = "physical_exam",
                    en = "Physical examination report upload",
                    zh = "体检报告上传",
                    required = true,
                    suggestedFileEn = "physical_exam_report.pdf",
                    suggestedFileZh = "体检报告.pdf"
                ),
                uploadField(
                    key = "accommodation_registration",
                    en = "Temporary accommodation registration upload for off-campus students",
                    zh = "校外住宿学生临时住宿登记上传",
                    required = false,
                    suggestedFileEn = "temporary_accommodation_registration.pdf",
                    suggestedFileZh = "临时住宿登记表.pdf"
                )
            ),
            tips = listOf(
                uiText(
                    "Off-campus students can add the accommodation registration page before final submission later.",
                    "校外住宿学生后续可补交临时住宿登记页。"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "residence_permit",
            intro = uiText(
                "Fill in your passport details and add the key visa-entry pages for the residence permit process.",
                "填写护照信息并上传签证与入境材料办理居留许可。"
            ),
            fields = listOf(
                textField(
                    key = "name",
                    en = "Name",
                    zh = "姓名",
                    prefillSource = PrefillSource.StudentName,
                    required = true
                ),
                dropdownField(
                    key = "gender",
                    en = "Gender",
                    zh = "性别",
                    options = genderOptions,
                    required = true
                ),
                textField(
                    key = "passport",
                    en = "Passport",
                    zh = "护照号",
                    required = true,
                    maxLength = 20
                ),
                dropdownField(
                    key = "nationality",
                    en = "Nationality",
                    zh = "国籍",
                    options = nationalityOptions,
                    required = true
                ),
                textField(
                    key = "major",
                    en = "Major",
                    zh = "专业",
                    prefillSource = PrefillSource.Major,
                    required = true
                ),
                textField(
                    key = "room_number",
                    en = "Room number",
                    zh = "房间号",
                    required = true
                ),
                textField(
                    key = "chinese_phone_number",
                    en = "Chinese phone number",
                    zh = "中国手机号",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "valid_passport_page",
                    en = "Valid passport page upload",
                    zh = "护照有效页上传",
                    required = true,
                    suggestedFileEn = "valid_passport_page.pdf",
                    suggestedFileZh = "护照有效页.pdf"
                ),
                uploadField(
                    key = "current_visa_page",
                    en = "Current temporary X1/X2 visa page upload",
                    zh = "当前临时X1/X2签证页上传",
                    required = true,
                    suggestedFileEn = "current_x1_x2_visa_page.pdf",
                    suggestedFileZh = "当前X1/X2签证页.pdf"
                ),
                uploadField(
                    key = "recent_entry_stamp",
                    en = "Recent entry China stamp upload",
                    zh = "最近一次入境中国盖章页上传",
                    required = true,
                    suggestedFileEn = "recent_entry_stamp.pdf",
                    suggestedFileZh = "最近入境盖章页.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "room_card_key",
            intro = uiText(
                "Choose the room-card or key issue type and explain what happened.",
                "选择房卡或钥匙问题类型，并说明具体情况。"
            ),
            fields = listOf(
                dropdownField(
                    key = "issue_type",
                    en = "Absent reasons / type",
                    zh = "原因/类型",
                    options = roomCardTypeOptions,
                    required = true
                ),
                multilineField(
                    key = "need_reason",
                    en = "Reason for needing a new room card or key",
                    zh = "需要新房卡或钥匙的原因",
                    required = true
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "broken_card_picture",
                    en = "Upload picture if old card/key is broken",
                    zh = "如旧房卡/钥匙损坏请上传照片",
                    required = false,
                    suggestedFileEn = "broken_room_card.jpg",
                    suggestedFileZh = "损坏房卡照片.jpg"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "back_to_cjlu",
            intro = uiText(
                "Submit your planned return date, travel mode, and contact so the office can confirm your return.",
                "提交计划返校日期、出行方式与联系方式，便于办公室确认返校信息。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "return_transport",
                    en = "Travel mode to campus",
                    zh = "返校交通方式",
                    options = returnTransportOptions,
                    required = true
                ),
                dateField(
                    key = "planned_arrival_date",
                    en = "Planned arrival date on campus",
                    zh = "计划到校日期",
                    required = true
                ),
                textField(
                    key = "flight_train_number",
                    en = "Flight or train reference",
                    zh = "航班或车次信息",
                    placeholderEn = "e.g. CA1234 / G7312",
                    placeholderZh = "例如 CA1234 / G7312",
                    required = false
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                ),
                multilineField(
                    key = "return_notes",
                    en = "Health, quarantine, or other notes for staff",
                    zh = "健康、隔离或其他需要说明的事项",
                    required = true,
                    maxLength = 500
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "travel_proof",
                    en = "Ticket or itinerary (optional)",
                    zh = "机票或行程单（选填）",
                    required = false,
                    suggestedFileEn = "itinerary.pdf",
                    suggestedFileZh = "行程证明.pdf"
                )
            ),
            tips = listOf(
                uiText(
                    "Submit updates early if your arrival date changes.",
                    "如到校日期有变化请尽早更新提交。"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "information_confirmation",
            intro = uiText(
                "Tell us which records need confirmation and what looks incorrect or incomplete.",
                "说明需要确认的记录类型，以及哪些信息可能有误或不完整。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "confirmation_category",
                    en = "Information category",
                    zh = "信息类别",
                    options = informationConfirmationCategoryOptions,
                    required = true
                ),
                multilineField(
                    key = "confirmation_details",
                    en = "Details to verify or correct",
                    zh = "需核实或更正的具体内容",
                    required = true,
                    maxLength = 600
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "supporting_document",
                    en = "Supporting document (optional)",
                    zh = "支撑材料（选填）",
                    required = false,
                    suggestedFileEn = "supporting_info.pdf",
                    suggestedFileZh = "证明材料.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "school_calendar",
            intro = uiText(
                "Request the official school calendar for the semester you need.",
                "申请所需学期的官方校历。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "target_semester",
                    en = "Target semester",
                    zh = "目标学期",
                    options = semesterOptions,
                    required = true
                ),
                dropdownField(
                    key = "calendar_delivery",
                    en = "How you want to receive it",
                    zh = "获取方式",
                    options = calendarDeliveryOptions,
                    required = true
                ),
                multilineField(
                    key = "calendar_notes",
                    en = "Extra notes (optional)",
                    zh = "补充说明（选填）",
                    required = false,
                    maxLength = 300
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "class_schedule",
            intro = uiText(
                "Describe which timetable or classroom change you need help with.",
                "说明需要协助的课程表或教室调整相关问题。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "schedule_semester",
                    en = "Semester",
                    zh = "学期",
                    options = semesterOptions,
                    required = true
                ),
                textField(
                    key = "course_name_or_code",
                    en = "Course name or code (optional)",
                    zh = "课程名称或代码（选填）",
                    required = false,
                    maxLength = 120
                ),
                multilineField(
                    key = "schedule_request",
                    en = "What you need from the schedule office",
                    zh = "需要教务或课程表协助的具体事项",
                    required = true,
                    maxLength = 500
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "schedule_screenshot",
                    en = "Screenshot or document (optional)",
                    zh = "截图或附件（选填）",
                    required = false,
                    suggestedFileEn = "schedule_issue.png",
                    suggestedFileZh = "课表问题截图.png"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "attendance_rate",
            intro = uiText(
                "Request a review of attendance for a specific course and semester.",
                "申请查看指定学期与课程的出勤情况。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "attendance_semester",
                    en = "Semester",
                    zh = "学期",
                    options = semesterOptions,
                    required = true
                ),
                textField(
                    key = "course_name",
                    en = "Course name",
                    zh = "课程名称",
                    required = true,
                    maxLength = 120
                ),
                multilineField(
                    key = "attendance_question",
                    en = "What you want checked or explained",
                    zh = "希望核对或说明的问题",
                    required = true,
                    maxLength = 500
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "attendance_proof",
                    en = "Screenshot or supporting document (optional)",
                    zh = "证明截图或附件（选填）",
                    required = false,
                    suggestedFileEn = "attendance_proof.png",
                    suggestedFileZh = "考勤证明截图.png"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "transcripts",
            intro = uiText(
                "Request official transcripts with language, quantity, and delivery details.",
                "申请成绩单：请选择语言、份数并填写领取或寄送说明。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "transcript_type",
                    en = "Transcript type",
                    zh = "成绩单类型",
                    options = transcriptTypeOptions,
                    required = true
                ),
                textField(
                    key = "number_of_copies",
                    en = "Number of copies",
                    zh = "份数",
                    required = true,
                    digitsOnly = true,
                    maxLength = 2,
                    keyboardType = KeyboardType.Number
                ),
                multilineField(
                    key = "mailing_and_purpose",
                    en = "Purpose, mailing address, or pickup notes",
                    zh = "用途、邮寄地址或自取说明",
                    required = true,
                    maxLength = 600
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "enrollment_proof",
                    en = "Enrollment certificate or ID proof",
                    zh = "在读证明或身份核验材料",
                    required = true,
                    suggestedFileEn = "enrollment_certificate.pdf",
                    suggestedFileZh = "在读证明.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "education_plan",
            intro = uiText(
                "Ask questions about your program plan, credits, or required courses.",
                "咨询培养方案、学分要求或必修课程相关问题。"
            ),
            fields = listOf(
                studentIdField(),
                textField(
                    key = "study_year_note",
                    en = "Current cohort / study year",
                    zh = "当前年级或班级",
                    prefillSource = PrefillSource.StudyYear,
                    required = true,
                    maxLength = 80
                ),
                textField(
                    key = "major",
                    en = "Major / program",
                    zh = "专业/项目",
                    prefillSource = PrefillSource.Major,
                    required = true,
                    maxLength = 120
                ),
                multilineField(
                    key = "education_plan_question",
                    en = "Your question about the education plan",
                    zh = "关于培养方案的具体问题",
                    required = true,
                    maxLength = 600
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "plan_reference",
                    en = "Screenshot or document (optional)",
                    zh = "相关截图或文件（选填）",
                    required = false,
                    suggestedFileEn = "plan_reference.pdf",
                    suggestedFileZh = "培养方案参考.pdf"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "deposit_refund",
            intro = uiText(
                "Start a dormitory or campus deposit refund request with room and bank details.",
                "发起宿舍或校园押金退还申请，并提供房间与收款信息。"
            ),
            fields = listOf(
                studentIdField(),
                dropdownField(
                    key = "refund_reason",
                    en = "Refund reason",
                    zh = "退还原因",
                    options = depositRefundReasonOptions,
                    required = true
                ),
                textField(
                    key = "dorm_building_room",
                    en = "Dormitory building and room",
                    zh = "宿舍楼与房间号",
                    required = true,
                    maxLength = 120
                ),
                multilineField(
                    key = "refund_bank_details",
                    en = "Refund account details (name, bank, last digits if applicable)",
                    zh = "退款账户信息（姓名、银行、卡号后几位等）",
                    required = true,
                    maxLength = 500
                ),
                textField(
                    key = "phone_number",
                    en = "Contact phone",
                    zh = "联系电话",
                    required = true,
                    keyboardType = KeyboardType.Phone,
                    maxLength = 20
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "deposit_receipt",
                    en = "Deposit receipt or contract (optional)",
                    zh = "押金收据或合同（选填）",
                    required = false,
                    suggestedFileEn = "deposit_receipt.pdf",
                    suggestedFileZh = "押金收据.pdf"
                )
            ),
            tips = listOf(
                uiText(
                    "Do not enter full card numbers in notes; use attachments if needed.",
                    "请勿在备注中填写完整卡号；如需可上传附件。"
                )
            )
        ),
        ServiceFormUiModel(
            serviceId = "bank_card_information",
            intro = uiText(
                "Store your bank details for deposit collection and refund follow-up.",
                "填写银行卡信息，用于押金领取和退款跟进。"
            ),
            fields = listOf(
                dropdownField(
                    key = "bank_selection",
                    en = "Bank selection",
                    zh = "银行选择",
                    options = bankOptions,
                    required = true
                ),
                textField(
                    key = "account_number",
                    en = "Account number",
                    zh = "银行卡号",
                    required = true,
                    keyboardType = KeyboardType.Number,
                    maxLength = 32
                ),
                textField(
                    key = "bank_account_name",
                    en = "Bank account name",
                    zh = "账户姓名",
                    prefillSource = PrefillSource.StudentName,
                    required = true
                ),
                radioField(
                    key = "collecting_on_behalf",
                    en = "Collecting on behalf of others",
                    zh = "押金领取方式",
                    options = receiverOptions,
                    required = true
                )
            ),
            uploads = listOf(
                uploadField(
                    key = "bank_receipt_paper",
                    en = "Bank receipt paper upload",
                    zh = "银行回单上传",
                    required = true,
                    suggestedFileEn = "bank_receipt_paper.pdf",
                    suggestedFileZh = "银行回单.pdf"
                )
            )
        )
    )

    private val formMap = forms.associateBy { it.serviceId }

    private fun studentIdField(): FormFieldUiModel.TextInput {
        return textField(
            key = "student_id",
            en = "Student ID",
            zh = "学号",
            required = true,
            digitsOnly = true,
            maxLength = 8,
            keyboardType = KeyboardType.Number,
            prefillSource = PrefillSource.StudentId,
            helperEn = "Format: xxxxssss. xxxx = year of registration, ssss = student number in that grade.",
            helperZh = "格式：xxxxssss。xxxx 为入学年份，ssss 为该年级中的学生编号。",
            validation = TextInputValidation.StudentId
        )
    }

    private fun textField(
        key: String,
        en: String,
        zh: String,
        required: Boolean = false,
        placeholderEn: String? = null,
        placeholderZh: String? = null,
        helperEn: String? = null,
        helperZh: String? = null,
        digitsOnly: Boolean = false,
        maxLength: Int = 80,
        keyboardType: KeyboardType = KeyboardType.Text,
        prefillSource: PrefillSource = PrefillSource.None,
        validation: TextInputValidation = TextInputValidation.None
    ): FormFieldUiModel.TextInput {
        return FormFieldUiModel.TextInput(
            key = key,
            label = uiText(en, zh),
            placeholder = placeholderEn?.let { uiText(it, placeholderZh ?: it) },
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required,
            digitsOnly = digitsOnly,
            maxLength = maxLength,
            keyboardType = keyboardType,
            prefillSource = prefillSource,
            validation = validation
        )
    }

    private fun multilineField(
        key: String,
        en: String,
        zh: String,
        required: Boolean = false,
        placeholderEn: String? = null,
        placeholderZh: String? = null,
        helperEn: String? = null,
        helperZh: String? = null,
        maxLength: Int = 240
    ): FormFieldUiModel.MultilineInput {
        return FormFieldUiModel.MultilineInput(
            key = key,
            label = uiText(en, zh),
            placeholder = placeholderEn?.let { uiText(it, placeholderZh ?: it) },
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required,
            maxLength = maxLength
        )
    }

    private fun dateField(
        key: String,
        en: String,
        zh: String,
        required: Boolean = false,
        helperEn: String? = null,
        helperZh: String? = null
    ): FormFieldUiModel.DatePicker {
        return FormFieldUiModel.DatePicker(
            key = key,
            label = uiText(en, zh),
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required
        )
    }

    private fun dropdownField(
        key: String,
        en: String,
        zh: String,
        options: List<OptionItem>,
        required: Boolean = false,
        helperEn: String? = null,
        helperZh: String? = null
    ): FormFieldUiModel.Dropdown {
        return FormFieldUiModel.Dropdown(
            key = key,
            label = uiText(en, zh),
            options = options,
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required
        )
    }

    private fun radioField(
        key: String,
        en: String,
        zh: String,
        options: List<OptionItem>,
        required: Boolean = false,
        helperEn: String? = null,
        helperZh: String? = null
    ): FormFieldUiModel.RadioGroup {
        return FormFieldUiModel.RadioGroup(
            key = key,
            label = uiText(en, zh),
            options = options,
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required
        )
    }

    private fun downloadField(key: String): FormFieldUiModel.ReadOnly {
        return FormFieldUiModel.ReadOnly(
            key = key,
            label = uiText("Download form", "下载表格"),
            value = uiText("Official office form template is available.", "办事用表模板已提供。"),
            trailingAction = uiText("Download", "下载")
        )
    }

    private fun uploadField(
        key: String,
        en: String,
        zh: String,
        required: Boolean,
        suggestedFileEn: String,
        suggestedFileZh: String,
        helperEn: String? = null,
        helperZh: String? = null,
        dashedBorder: Boolean = true
    ): UploadFieldUiModel {
        return UploadFieldUiModel(
            key = key,
            label = uiText(en, zh),
            helper = helperEn?.let { uiText(it, helperZh ?: it) },
            required = required,
            dashedBorder = dashedBorder,
            suggestedFileName = uiText(suggestedFileEn, suggestedFileZh)
        )
    }

    private fun option(
        id: String,
        en: String,
        zh: String
    ): OptionItem {
        return OptionItem(
            id = id,
            label = UiText(en = en, zh = zh)
        )
    }
}
