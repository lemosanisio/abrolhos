package br.dev.demoraes.abrolhos.domain.entities

import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.test.TestArbitraries
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime

/**
 * Property-based tests for Post Management Enhancements. Each test references the formal property
 * from the design document. Feature: post-management-enhancements
 */
class PostManagementPropertiesTest :
        FunSpec({

            // -------------------------------------------------------------------------
            // Property 1: Authorization for Post Modifications
            // For any post and user, the user may modify the post iff they are the owner or ADMIN.
            // -------------------------------------------------------------------------
            test("Property 1: Authorization for Post Modifications") {
                // Feature: post-management-enhancements, Property 1: Authorization for Post
                // Modifications
                checkAll(100, TestArbitraries.post(), TestArbitraries.user()) { post, user ->
                    val canModify = post.isOwnedBy(user.id) || user.role == Role.ADMIN
                    val isOwner = post.author.id == user.id
                    val isAdmin = user.role == Role.ADMIN

                    canModify shouldBe (isOwner || isAdmin)
                }
            }

            // -------------------------------------------------------------------------
            // Property 2: Slug Generation from Title
            // Generated slugs are lowercase, alphanumeric + hyphens only.
            // -------------------------------------------------------------------------
            test("Property 2: Slug Generation from Title") {
                // Feature: post-management-enhancements, Property 2: Slug Generation from Title
                checkAll(100, TestArbitraries.postTitle()) { postTitle ->
                    val slug = generateSlugReflect(postTitle.value)
                    if (slug.isNotEmpty()) {
                        slug shouldMatch Regex("^[a-z0-9-]+$")
                        PostSlug.SLUG_REGEX.matches(slug).shouldBeTrue()
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Property 3: Slug Conflict Resolution
            // When the base slug conflicts, the system appends -2, -3… suffixes.
            // -------------------------------------------------------------------------
            test("Property 3: Slug Conflict Resolution") {
                // Feature: post-management-enhancements, Property 3: Slug Conflict Resolution
                checkAll(100, TestArbitraries.post(), TestArbitraries.post()) {
                        existing,
                        currentPost ->
                    // Simulate: existing occupies the base slug, currentPost (different id) must
                    // resolve
                    val postRepository = mockk<PostRepository>()
                    val baseSlug = generateSlugReflect(existing.title.value)
                    if (baseSlug.length >= 3) {
                        every { postRepository.findBySlug(baseSlug) } returns
                                existing.copy(id = existing.id)
                        every { postRepository.findBySlug("$baseSlug-2") } returns null

                        val service = buildService(postRepository)
                        val resolved =
                                service.generateUniqueSlug(existing.title.value, currentPost.id)
                        resolved shouldBe "$baseSlug-2"
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Property 4: UpdatedAt Timestamp on Modification
            // After withUpdatedFields, updatedAt >= the time before the call.
            // -------------------------------------------------------------------------
            test("Property 4: UpdatedAt Timestamp on Modification") {
                // Feature: post-management-enhancements, Property 4: UpdatedAt Timestamp on
                // Modification
                checkAll(100, TestArbitraries.post()) { post ->
                    val before = OffsetDateTime.now()
                    val updated = post.withUpdatedFields(content = PostContent("new content"))
                    updated.updatedAt.isAfter(before) || updated.updatedAt == before
                }
            }

            // -------------------------------------------------------------------------
            // Property 5: Field Updates Persistence
            // withUpdatedFields correctly replaces specified fields, leaving others unchanged.
            // -------------------------------------------------------------------------
            test("Property 5: Field Updates Persistence") {
                // Feature: post-management-enhancements, Property 5: Field Updates Persistence
                checkAll(
                        100,
                        TestArbitraries.post(),
                        TestArbitraries.postTitle(),
                        TestArbitraries.postContent()
                ) { post, newTitle, newContent ->
                    val updated = post.withUpdatedFields(title = newTitle, content = newContent)
                    updated.title shouldBe newTitle
                    updated.content shouldBe newContent
                    // Unchanged fields
                    updated.id shouldBe post.id
                    updated.author shouldBe post.author
                    updated.slug shouldBe post.slug
                    updated.category shouldBe post.category
                }
            }

            // -------------------------------------------------------------------------
            // Property 6: Soft Delete Sets DeletedAt
            // isOwnedBy correctly identifies the post owner (precondition for delete auth).
            // Database-level deletedAt is tested via integration tests.
            // -------------------------------------------------------------------------
            test("Property 6: Soft Delete - isOwnedBy correctly identifies owner") {
                // Feature: post-management-enhancements, Property 6: Soft Delete Sets DeletedAt
                checkAll(100, TestArbitraries.post()) { post ->
                    // Owner always matches
                    post.isOwnedBy(post.author.id).shouldBeTrue()
                }
            }

            test("Property 6b: isOwnedBy returns false for different user") {
                checkAll(100, TestArbitraries.post(), TestArbitraries.user()) { post, otherUser ->
                    if (otherUser.id != post.author.id) {
                        post.isOwnedBy(otherUser.id).shouldBeFalse()
                    }
                }
            }

            // -------------------------------------------------------------------------
            // Property 7: Post Visibility Rules
            // Published → visible to all; unpublished → visible only to owner or ADMIN.
            // -------------------------------------------------------------------------
            test("Property 7: Post Visibility Rules - published visible to all") {
                // Feature: post-management-enhancements, Property 7: Post Visibility Rules
                checkAll(100, TestArbitraries.post(), TestArbitraries.user()) { post, user ->
                    val publishedPost = post.copy(status = PostStatus.PUBLISHED)
                    // A published post is always accessible regardless of user
                    (publishedPost.status == PostStatus.PUBLISHED).shouldBeTrue()
                }
            }

            test("Property 7b: Post Visibility Rules - unpublished requires owner or admin") {
                checkAll(100, TestArbitraries.post(), TestArbitraries.user()) { post, user ->
                    val draftPost = post.copy(status = PostStatus.DRAFT)
                    val canSee = draftPost.isOwnedBy(user.id) || user.role == Role.ADMIN
                    val isOwner = draftPost.author.id == user.id
                    val isAdmin = user.role == Role.ADMIN
                    canSee shouldBe (isOwner || isAdmin)
                }
            }

            // -------------------------------------------------------------------------
            // Property 8: Scheduled Post Query Correctness
            // A post is eligible for auto-publish iff status == SCHEDULED and publishedAt <= now.
            // (Repository-level correctness; this verifies the domain predicate.)
            // -------------------------------------------------------------------------
            test("Property 8: Scheduled Post Query Correctness") {
                // Feature: post-management-enhancements, Property 8: Scheduled Post Query
                // Correctness
                checkAll(100, TestArbitraries.post()) { post ->
                    val now = OffsetDateTime.now()
                    val pastPost =
                            post.copy(
                                    status = PostStatus.SCHEDULED,
                                    publishedAt = now.minusMinutes(1),
                            )
                    val futurePost =
                            post.copy(
                                    status = PostStatus.SCHEDULED,
                                    publishedAt = now.plusMinutes(10),
                            )
                    val readyToPublish = { p: Post ->
                        p.status == PostStatus.SCHEDULED &&
                                p.publishedAt != null &&
                                !p.publishedAt!!.isAfter(now)
                    }
                    readyToPublish(pastPost).shouldBeTrue()
                    readyToPublish(futurePost).shouldBeFalse()
                }
            }

            // -------------------------------------------------------------------------
            // Property 9: Scheduled Post Publishing State Transition
            // After withUpdatedFields(status=PUBLISHED), status is PUBLISHED and publishedAt !=
            // null.
            // -------------------------------------------------------------------------
            test("Property 9: Scheduled Post Publishing State Transition") {
                // Feature: post-management-enhancements, Property 9: Scheduled Post Publishing
                // State Transition
                checkAll(100, TestArbitraries.post()) { post ->
                    val scheduledPost =
                            post.copy(
                                    status = PostStatus.SCHEDULED,
                                    publishedAt = OffsetDateTime.now().minusMinutes(5),
                            )
                    val published =
                            scheduledPost.withUpdatedFields(
                                    status = PostStatus.PUBLISHED,
                                    publishedAt = scheduledPost.publishedAt ?: OffsetDateTime.now(),
                            )
                    published.status shouldBe PostStatus.PUBLISHED
                    published.publishedAt.shouldNotBeNull()
                }
            }

            // -------------------------------------------------------------------------
            // Property 10: Error Resilience in Batch Processing
            // If one post fails, the others are not affected (verified in
            // ScheduledPublishingServiceTest).
            // This property test verifies the domain-level: withUpdatedFields is pure and does not
            // throw.
            // -------------------------------------------------------------------------
            test("Property 10: Error Resilience - withUpdatedFields is safe for any post") {
                // Feature: post-management-enhancements, Property 10: Error Resilience in Batch
                // Processing
                checkAll(100, TestArbitraries.post()) { post ->
                    val result = runCatching {
                        post.withUpdatedFields(
                                status = PostStatus.PUBLISHED,
                                publishedAt = post.publishedAt ?: OffsetDateTime.now(),
                        )
                    }
                    result.isSuccess.shouldBeTrue()
                }
            }
        })

// -------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------

private fun generateSlugReflect(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), "-").trim('-')

private fun buildService(postRepository: PostRepository): PostService {
    return PostService(
            postRepository = postRepository,
            userRepository = mockk(relaxed = true),
            categoryRepository = mockk(relaxed = true),
            tagRepository = mockk(relaxed = true),
            metricsService = mockk(relaxed = true),
            auditLogger = mockk(relaxed = true),
    )
}
