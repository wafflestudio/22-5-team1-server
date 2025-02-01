package com.waffletoy.team1server.post.service

import com.waffletoy.team1server.exceptions.*
import com.waffletoy.team1server.post.*
import com.waffletoy.team1server.post.Category
import com.waffletoy.team1server.post.Series
import com.waffletoy.team1server.post.dto.*
import com.waffletoy.team1server.post.persistence.*
import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.dtos.User
import com.waffletoy.team1server.user.persistence.*
import com.waffletoy.team1server.user.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class PostService(
    private val companyRepository: CompanyRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val positionRepository: PositionRepository,
    @Lazy private val userService: UserService,
) {
    @Value("\${custom.page.size:12}")
    private val pageSize: Int = 12

    /**
     * Retrieves detailed information of a specific post by its ID.
     *
     * @param user nullable user field to get bookmark
     * @param postId The unique identifier of the post.
     * @return The detailed [Post] object.
     * @throws PostNotFoundException If the post with the given ID does not exist.
     */
    @Transactional(readOnly = true)
    fun getPageDetail(
        user: User?,
        postId: String,
    ): Post {
        val positionEntity = getPositionEntityOrThrow(postId)
        val bookmarkIds = getBookmarkIds(user)
        return Post.fromEntity(
            entity = positionEntity,
            isBookmarked = positionEntity.id in bookmarkIds,
        )
    }

    /**
     * Retrieves a paginated list of posts based on provided filters.
     *
     * @param user nullable user field to get bookmark
     * @param positions List of position names to filter by.
     * @param investmentMax Maximum investment amount.
     * @param investmentMin Minimum investment amount.
     * @param status Status filter (e.g., active, inactive).
     * @param series List of series names to filter by.
     * @param order sort posts by newest(0) or employmentEndDate(1)
     * @param page The page number to retrieve.
     * @return A paginated [Page] of [Post].
     * @throws PostInvalidFiltersException If invalid filters are provided.
     */
    @Transactional(readOnly = true)
    fun getPosts(
        user: User?,
        positions: List<String>?,
        investmentMax: Int?,
        investmentMin: Int?,
        status: Int?,
        series: List<String>?,
        page: Int = 0,
        order: Int = 0,
    ): Page<Post> {
        // Example validation: investmentMin should not exceed investmentMax
        if (investmentMin != null && investmentMax != null && investmentMin > investmentMax) {
            throw PostInvalidFiltersException(
                details =
                    mapOf(
                        "investmentMin" to investmentMin,
                        "investmentMax" to investmentMax,
                    ),
            )
        }

        val specification =
            PositionSpecification.withFilters(
                positions,
                investmentMax,
                investmentMin,
                status ?: 2,
                series,
                order,
            )

        val validPage = if (page < 0) 0 else page
        val pageable = PageRequest.of(validPage, pageSize)
        val positionPage = positionRepository.findAll(specification, pageable)

        val bookmarkIds = getBookmarkIds(user)

        return positionPage.map { position ->
            Post.fromEntity(
                entity = position,
                isBookmarked = position.id in bookmarkIds,
            )
        }
    }

    /**
     * Adds a bookmark for a user on a specific post.
     *
     * @param userId The unique identifier of the user.
     * @param postId The unique identifier of the post.
     * @throws PostNotFoundException If the post does not exist.
     * @throws UserNotFoundException If the user does not exist.
     * @throws PostAlreadyBookmarkedException If the post is already bookmarked by the user.
     */
    @Transactional
    fun addBookmark(
        userId: String,
        postId: String,
    ) {
        val positionEntity = getPositionEntityOrThrow(postId)

        val userEntity = getUserEntityOrThrow(userId)

        val existingBookmark = bookmarkRepository.findByUserAndPosition(userEntity, positionEntity)
        if (existingBookmark != null) {
            throw PostAlreadyBookmarkedException(
                details = mapOf("userId" to userId, "postId" to postId),
            )
        }

        bookmarkRepository.save(
            BookmarkEntity(
                position = positionEntity,
                user = userEntity,
            ),
        )
    }

    /**
     * Removes a bookmark for a user on a specific post.
     *
     * @param userId The unique identifier of the user.
     * @param postId The unique identifier of the post.
     * @throws PostNotFoundException If the post does not exist.
     * @throws UserNotFoundException If the user does not exist.
     * @throws PostBookmarkNotFoundException If the bookmark does not exist.
     */
    @Transactional
    fun deleteBookmark(
        userId: String,
        postId: String,
    ) {
        val positionEntity = getPositionEntityOrThrow(postId)

        val userEntity = getUserEntityOrThrow(userId)

        val bookmarkEntity =
            bookmarkRepository.findByUserAndPosition(userEntity, positionEntity) ?: throw PostBookmarkNotFoundException(
                details = mapOf("userId" to userId, "postId" to postId),
            )
        bookmarkRepository.delete(bookmarkEntity)
    }

    /**
     * Retrieves a paginated list of bookmarks for a specific user.
     *
     * @param userId The unique identifier of the user.
     * @param page The page number to retrieve.
     * @return A paginated [Page] of [Post] representing bookmarked posts.
     * @throws UserNotFoundException If the user does not exist.
     */
    @Transactional(readOnly = true)
    fun getBookmarks(
        userId: String,
        page: Int,
    ): Page<Post> {
        val userEntity = getUserEntityOrThrow(userId)

        val validPage = if (page < 0) 0 else page
        val pageable = PageRequest.of(validPage, pageSize)
        val positionPage = bookmarkRepository.findPositionsByUser(userEntity, pageable)
        return positionPage.map { position ->
            Post.fromEntity(
                entity = position,
                isBookmarked = true,
            )
        }
    }

    /**
     * Creates dummy posts for testing or development purposes.
     *
     * @param cnt The number of dummy posts to create.
     */
    @Transactional
    fun makeDummyPosts(
        cnt: Int,
        secret: String,
    ) {
        if (secret != devSecret) {
            throw InvalidRequestException(
                details = mapOf("providedSecret" to secret),
            )
        }

        val companies = mutableListOf<CompanyEntity>()
        val positions = mutableListOf<PositionEntity>()

        (1..cnt).forEach { index ->
            val admin = userService.makeDummyUser(index)
            val tags =
                listOf("Tech", "Finance", "Health")
                    .shuffled()
                    .take(2)
                    .map { TagVo(it) }
                    .toMutableList()

            val companyEntity =
                CompanyEntity(
                    admin = admin,
                    companyName = "dummy Company $index",
                    explanation = "Explanation of dummy Company $index",
                    email = "dummy${index}_${Random.nextInt(0, 10001)}@example.com",
                    slogan = "Slogan of dummy$index",
                    investAmount = (1000..5000).random(),
                    investCompany = "Company A$index, Company B$index",
                    series = Series.entries.random(),
                    imageLink = "https://www.company$index/image",
                    tags = tags,
                )
            companies.add(companyEntity)

            Category.entries.shuffled().take((1..3).random()).forEach { category ->
                positions.add(
                    PositionEntity(
                        title = "Title of $index",
                        category = category,
                        detail = "Detail of $category",
                        headcount = "${(1..3).random()}",
                        isActive = true,
                        employmentEndDate = LocalDateTime.now().plusHours((-15..15).random().toLong()),
                        company = companyEntity,
                    ),
                )
            }
        }

        companyRepository.saveAll(companies)
        positionRepository.saveAll(positions)
    }

//    /**
//     * Resets the database by deleting all companies, bookmarks, and positions.
//     */
//    @Transactional
//    fun resetDB(secret: String) {
//        if (secret != resetDbSecret) {
//            throw InvalidRequestException(
//                details = mapOf("providedSecret" to secret),
//            )
//        }
//        companyRepository.deleteAll()
//        bookmarkRepository.deleteAll()
//        positionRepository.deleteAll()
//    }

    fun getUserEntityOrThrow(userId: String): UserEntity =
        userService.getUserEntityByUserId(userId) ?: throw UserNotFoundException(mapOf("userId" to userId))

    fun getPositionEntityOrThrow(postId: String): PositionEntity =
        positionRepository.findByIdOrNull(postId) ?: throw PostNotFoundException(mapOf("postId" to postId))

    fun getPositionEntityByPostId(postId: String): PositionEntity? = positionRepository.findByIdOrNull(postId)

    fun getBookmarkIds(user: User?): Set<String> {
        if (user == null) {
            return emptySet()
        }

        val userEntity = userService.getUserEntityByUserId(user.id)
        return if (userEntity != null) {
            bookmarkRepository.findByUser(userEntity).map { it.position.id }.toSet()
        } else {
            emptySet()
        }
    }

    /**
     * Creates a new company associated with the given user.
     *
     * @param user The authenticated user creating the company.
     * @param request The DTO containing company creation data.
     * @return The created Company DTO.
     * @throws NotAuthorizedException If user is not curator.
     * @throws PostCompanyExistsException If a company with the given email already exists.
     */
    @Transactional
    fun createCompany(
        user: User,
        request: CreateCompanyRequest,
    ): Company {
        if (user.userRole != UserRole.CURATOR) {
            throw NotAuthorizedException()
        }
        val userEntity = userService.getUserEntityByUserId(user.id) ?: throw UserNotFoundException(mapOf("userId" to user.id))
        // Check if a company with the same email already exists
        if (companyRepository.existsByEmail(request.email)) {
            throw PostCompanyExistsException()
        }

        // Map CreateCompanyRequest to CompanyEntity
        val companyEntity =
            CompanyEntity(
                admin = userEntity,
                companyName = request.companyName,
                email = request.email,
                series = request.series,
                explanation = request.explanation,
                slogan = request.slogan,
                investAmount = request.investAmount ?: 0,
                investCompany = request.investCompany,
                imageLink = request.imageLink,
                irDeckLink = request.irDeckLink,
                landingPageLink = request.landingPageLink,
                links = request.links.map { LinkVo(description = it.description, link = it.link) }.toMutableList(),
                tags = request.tags.map { TagVo(tag = it.tag) }.toMutableList(),
            )

        // Save the CompanyEntity
        val savedCompany = companyRepository.save(companyEntity)

        // Convert to Company DTO
        return Company.fromEntity(savedCompany)
    }

    @Transactional
    fun getCompanyByCurator(user: User): List<Company> {
        if (user.userRole != UserRole.CURATOR) {
            throw NotAuthorizedException()
        }
        val userEntity = userService.getUserEntityByUserId(user.id) ?: throw UserNotFoundException(mapOf("userId" to user.id))
        return companyRepository.findAllByAdmin(userEntity).map { Company.fromEntity(it) }
    }

    @Transactional
    fun createPosition(
        user: User,
        companyId: String,
        request: CreatePositionRequest,
    ): Position {
        if (user.userRole != UserRole.CURATOR) {
            throw NotAuthorizedException()
        }
        // Retrieve the company by ID
        val company =
            companyRepository.findById(companyId)
                .orElseThrow { PostCompanyNotFoundException(mapOf("companyId" to companyId)) }

        // Check if the user is the admin of the company
        if (company.admin.id != user.id) {
            throw PostAccessForbiddenException()
        }

        // Map CreatePositionRequest to PositionEntity
        val positionEntity =
            PositionEntity(
                title = request.title,
                category = request.category,
                detail = request.detail,
                headcount = request.headcount,
                employmentEndDate = request.employmentEndDate,
                isActive = request.isActive ?: false,
                company = company,
            )

        // Save the PositionEntity
        val savedPosition = positionRepository.save(positionEntity)

        // Convert to Position DTO
        return Position.fromEntity(savedPosition)
    }

    // normal 유저 탈퇴 시 bookmark 데이터를 삭제
    // curator 유저가 작성한 company, position 데이터는 유지
    @Transactional(propagation = Propagation.REQUIRED)
    fun deleteBookmarkByUser(userEntity: UserEntity) {
        bookmarkRepository.deleteAllByUser(userEntity)
    }

    @Value("\${custom.SECRET}")
    private lateinit var devSecret: String
}
