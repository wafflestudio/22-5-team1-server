package com.waffletoy.team1server.post.persistence

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "tags")
open class TagEntity(
    @Id
    @Column(name = "ID", nullable = false)
    open val id: String = UUID.randomUUID().toString(),

    @Column(name = "TAG", nullable = false, unique = true)
    val tag: String,

    @ManyToMany(mappedBy = "tags")
    val posts: MutableSet<PostEntity> = mutableSetOf()
) {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        tag = "",
        posts = mutableSetOf()
    )
}

