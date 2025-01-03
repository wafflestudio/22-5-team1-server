package com.waffletoy.team1server.post.persistence

import jakarta.persistence.*

@Entity(name = "tags")
@Table(name = "tags")
class TagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "name", nullable = false, length = 100, unique = true)
    val name: String,
    @ManyToMany(mappedBy = "tags")
    val posts: Set<PostEntity> = emptySet(),
)
