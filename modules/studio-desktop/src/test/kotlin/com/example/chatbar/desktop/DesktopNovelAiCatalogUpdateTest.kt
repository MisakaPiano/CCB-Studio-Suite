package com.example.chatbar.desktop

import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.update.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.*

class DesktopNovelAiCatalogUpdateTest {
    private fun database(root: Path, prefix: String): Pair<Path, DanbooruCatalogMetadata> {
        val file = root.resolve("$prefix-fixture.sqlite")
        DriverManager.getConnection("jdbc:sqlite:$file").use { db ->
            db.createStatement().use { it.execute("CREATE TABLE tags(name TEXT, cn_name TEXT, category INTEGER, post_count INTEGER)") }
            db.autoCommit = false
            db.prepareStatement("INSERT INTO tags VALUES (?, '蓝色', 0, ?)").use { statement ->
                repeat(10000) { i -> statement.setString(1, "${prefix}_$i"); statement.setInt(2, i); statement.executeUpdate() }
            }
            db.commit()
        }
        val bytes = Files.size(file)
        val digest = MessageDigest.getInstance("SHA-1").apply { update("blob $bytes\u0000".toByteArray()); update(Files.readAllBytes(file)) }
        val sha = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        return file to DanbooruCatalogMetadata(sha, "inline", bytes, 10000, "tags")
    }

    @Test fun `catalog update builds shared ranking before durable switch and corrupt authority is retained`(): Unit = runBlocking {
        val root = Files.createTempDirectory("p7-catalog-")
        try {
            val (old, metadata) = database(root, "old")
            Files.move(old, root.resolve("${metadata.sourceSha}.sqlite"))
            DesktopNovelAiSqlDatabase(root.resolve("${metadata.sourceSha}.sqlite")).use { source ->
                DesktopNovelAiIndexWriter(root.resolve("index-${metadata.sourceSha}.sqlite")).use {
                    buildNovelAiRankedIndex(it, source, "tags", metadata.sourceSha, "danbooru")
                }
            }
            val pointer = root.resolve("active-catalog.json")
            Files.writeString(pointer, Json.encodeToString(DanbooruCatalogMetadata.serializer(), metadata))
            val catalog = DesktopNovelAiTagCatalog(DesktopNovelAiCatalogAssets(root) { error("fixture must not access assets") })
            assertEquals(metadata, catalog.catalogMetadata())
            val (fresh, next) = database(root, "blue")
            val payload = Files.readAllBytes(fresh)
            var calls = 0
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(payload.toResponseBody()).build()
            }.build()
            val info = DanbooruCatalogUpdateInfo(metadata, next.sourceSha, "inline", "https://raw.githubusercontent.com/fixture", "", next.sourceSizeBytes)
            val installer = DesktopNovelAiCatalogUpdate(catalog, client)
            assertFails { installer.install(info.copy(latestSourceSha = "f".repeat(40))) {} }
            assertEquals(metadata, catalog.catalogMetadata())
            val progress=mutableListOf<String>()
            installer.install(info) {progress+=it}
            assertTrue(progress.any{it.startsWith("构建补全索引：已收录")})
            assertTrue(progress.any{it.startsWith("整理查询索引：")})
            assertTrue(progress.contains("正在整理补全索引文件…"))
            assertEquals(next, catalog.catalogMetadata())
            assertEquals(next.sourceSha, catalog.completionVersion.value)
            val found = mutableListOf<String>()
            catalog.streamCompletion("blue_999", { found += it.name }, {})
            assertEquals("blue_9999", found.first())
            assertTrue(Files.exists(root.resolve("${metadata.sourceSha}.sqlite")))
            val restarted = DesktopNovelAiTagCatalog(DesktopNovelAiCatalogAssets(root) { error("no bundled fallback") })
            assertEquals(next, restarted.catalogMetadata())
            Files.writeString(pointer, "corrupt")
            val corrupt = DesktopNovelAiTagCatalog(DesktopNovelAiCatalogAssets(root) { error("no destructive fallback") })
            assertFails { DesktopNovelAiCatalogUpdate(corrupt, client).install(info) {} }
            assertEquals("corrupt", Files.readString(pointer))
            assertEquals(2, calls)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun `official shared checker pins download to commit and compares blob rather than commit sha`(): Unit = runBlocking {
        val metadata = DanbooruCatalogMetadata("a".repeat(40), "inline", 1)
        val lookup = object : NovelAiTagLookup {
            override suspend fun catalogMetadata() = metadata
            override suspend fun exactChineseTranslations(names: Collection<String>) = emptyMap<String, String>()
            override suspend fun search(query: String): NovelAiTagSearchOutcome = error("not used")
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = if (request.url.encodedPath.endsWith("/commits")) """[{"sha":"commit-fixture","commit":{"committer":{"date":"inline"}}}]"""
            else { assertEquals("commit-fixture", request.url.queryParameter("ref")); """{"sha":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","size":25,"download_url":"https://raw.githubusercontent.com/mutable"}""" }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(body.toResponseBody()).build()
        }.build()
        val update = assertNotNull(SharedDanbooruCatalogUpdateChecker(lookup, "fixture", client).checkLatestCatalog())
        assertTrue(update.downloadUrl.endsWith("/commit-fixture/tag.sqlite"))
        assertEquals("b".repeat(40), update.latestSourceSha)
    }

    @Test fun `cancellation and schema failure retain durable authority and remove only staged files`(): Unit = runBlocking {
        val root = Files.createTempDirectory("suite-catalog-cancel-")
        try {
            val (old, metadata) = database(root, "old")
            Files.move(old, root.resolve("${metadata.sourceSha}.sqlite"))
            DesktopNovelAiSqlDatabase(root.resolve("${metadata.sourceSha}.sqlite")).use { source ->
                DesktopNovelAiIndexWriter(root.resolve("index-${metadata.sourceSha}.sqlite")).use {
                    buildNovelAiRankedIndex(it, source, "tags", metadata.sourceSha, "danbooru")
                }
            }
            val pointer = root.resolve("active-catalog.json")
            val original = Json.encodeToString(DanbooruCatalogMetadata.serializer(), metadata)
            Files.writeString(pointer, original)
            val sentinel=root.resolve("unrelated.keep");Files.writeString(sentinel,"retain")
            val catalog = DesktopNovelAiTagCatalog(DesktopNovelAiCatalogAssets(root) { error("no bundled fallback") })
            val (fresh,next)=database(root,"fresh")
            var payload=Files.readAllBytes(fresh)
            val client=OkHttpClient.Builder().addInterceptor { chain ->
                assertNull(chain.request().header("Authorization"))
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(payload.toResponseBody()).build()
            }.build()
            val installer=DesktopNovelAiCatalogUpdate(catalog,client)
            val info=DanbooruCatalogUpdateInfo(metadata,next.sourceSha,"inline","https://raw.githubusercontent.com/fixture","",payload.size.toLong())
            assertFailsWith<kotlinx.coroutines.CancellationException> {
                installer.install(info) { if(it.startsWith("下载词库"))throw kotlinx.coroutines.CancellationException("synthetic cancel") }
            }
            assertEquals(original,Files.readString(pointer));assertEquals(metadata,catalog.catalogMetadata())
            payload="not a sqlite database".toByteArray()
            val digest=MessageDigest.getInstance("SHA-1").apply{update("blob ${payload.size}\u0000".toByteArray());update(payload)}
            val invalidSha=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
            assertFails{installer.install(info.copy(latestSourceSha=invalidSha,sizeBytes=payload.size.toLong())){}}
            assertEquals(original,Files.readString(pointer));assertEquals("retain",Files.readString(sentinel))
            assertEquals(metadata,DesktopNovelAiTagCatalog(DesktopNovelAiCatalogAssets(root){error("no fallback")}).catalogMetadata())
            Files.list(root).use{paths->assertTrue(paths.noneMatch{it.fileName.toString().endsWith(".part")})}
        } finally { root.toFile().deleteRecursively() }
    }
}
