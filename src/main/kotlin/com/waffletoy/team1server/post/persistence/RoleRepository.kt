package com.waffletoy.team1server.post.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface RoleRepository : JpaRepository<RoleEntity, String>, JpaSpecificationExecutor<RoleEntity>
