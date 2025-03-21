package com.waffletoy.team1server.post.persistence

import com.waffletoy.team1server.auth.persistence.UserEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["USER_ID", "POSTION_ID"]),
    ],
)
class BookmarkEntity(
    @Id
    @Column(name = "ID", nullable = false)
    open val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    open val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "POSITION_ID", nullable = false)
    open val position: PositionEntity,
)
