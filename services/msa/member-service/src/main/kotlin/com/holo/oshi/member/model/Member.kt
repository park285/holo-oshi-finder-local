package com.holo.oshi.member.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table("members")
data class Member(
    @Id
    val id: Long? = null,
    
    @Column("name_en")
    val nameEn: String,
    
    @Column("name_ja")  // DB 컬럼명과 일치, nullable 
    val nameJp: String?,
    
    val generation: String?,  // DB는 varchar(50), nullable
    
    val branch: String?,  // nullable
    
    val unit: String?,
    
    @Column("debut_date")
    val debutDate: LocalDate?,  
    
    val birthday: LocalDate?,   
    
    val height: Int?,
    
    @Column("fanbase_name") 
    val fanbase: String?,
    
    val emoji: String?,
    
    @Column("youtube_channel")
    val youtubeChannel: String?,
    
    @Column("twitter_handle")
    val twitterHandle: String?,
    
    val tags: Array<String>? = null,  // text[]
    
    @Column("personality_traits")
    val personalityTraits: String? = null,  
    
    @Column("activity_status")
    val activityStatus: String? = "active",  // varchar(20)
    
    @Column("graduation_date")
    val graduationDate: LocalDate?,  // DATE -> LocalDate
    
    @Column("graduation_type")
    val graduationType: String? = null,  // varchar(20)
    
    @Column("graduation_reason")
    val graduationReason: String? = null,  // varchar(50)
    
    @Column("is_active")
    val isActive: Boolean = true,
    
    @Column("created_at")
    val createdAt: Instant? = null,  
    
    @Column("updated_at")
    val updatedAt: Instant? = null   
)

// DTO for API responses
data class MemberDto(
    val id: Long?,  // DB의 id 필드 사용
    val nameEn: String,
    val nameJp: String?,  // nullable로 변경
    val generation: String?,  // DB는 varchar(50), nullable
    val branch: String?,  // nullable
    val unit: String?,
    val debutDate: String?,
    val birthday: String?,
    val height: Int?,
    val fanbase: String?,
    val emoji: String?,
    val youtubeChannel: String?,
    val twitterHandle: String?,
    val isActive: Boolean
)

@Table("member_enriched_data")
data class MemberEnrichedData(
    @Id
    @Column("member_id")
    val memberId: Int,  // Primary Key이면서 members.id를 참조
    
    @Column("unified_traits")
    val unifiedTraits: String?,
    
    @Column("categorized_traits")
    val categorizedTraits: String?,
    
    @Column("personality_summary")
    val personalitySummary: String?,
    
    @Column("korean_traits")
    val koreanTraits: String?,
    
    @Column("korean_nicknames")
    val koreanNicknames: String?,
    
    @Column("fan_perception")
    val fanPerception: String?,
    
    @Column("raw_json_data")
    val rawJsonData: String?,
    
    @Column("data_source")
    val dataSource: String? = "json_import",
    
    @Column("created_at")
    val createdAt: String?,
    
    @Column("updated_at")
    val updatedAt: String?
)

// Events for inter-service communication
data class MemberCreatedEvent(
    val id: Long?,  // Long 타입으로 변경
    val member: MemberDto,
    val timestamp: Instant = Instant.now()
)

data class MemberUpdatedEvent(
    val id: Long?,  // Long 타입으로 변경
    val member: MemberDto,
    val changedFields: List<String>,
    val timestamp: Instant = Instant.now()
)

data class MemberDeletedEvent(
    val id: Long,  // Long 타입으로 변경
    val timestamp: Instant = Instant.now()
)

// Combined response model
data class MemberWithEnrichedData(
    val member: MemberDto,
    val enrichedData: MemberEnrichedData
)