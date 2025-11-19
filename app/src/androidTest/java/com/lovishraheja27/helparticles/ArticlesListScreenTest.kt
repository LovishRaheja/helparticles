package com.lovishraheja27.helparticles

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import com.lovishraheja27.helparticles.ui.list.ArticlesListScreen
import com.lovishraheja27.helparticles.ui.list.ArticlesListViewModel
import com.lovishraheja27.helparticles.ui.theme.HelpArticlesTheme
import com.lovishraheja27.shared.models.Article
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ArticlesListScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @BindValue
    val fakeRepository: ArticlesRepository = TODO()

    private val testArticle = Article(
        id = "1",
        title = "How to Reset Password",
        summary = "Learn how to reset your password",
        content = "Content here",
        updatedAt = Clock.System.now().toString(),
        category = "Account"
    )

    @Before
    fun setUp() {
        hiltRule.inject()
    }
    @Test
    fun emptyState_displaysWhenNoArticles() {
        runBlocking {
            val viewModel = ArticlesListViewModel(fakeRepository)

            composeRule.setContent {
                HelpArticlesTheme {
                    ArticlesListScreen(
                        viewModel = viewModel, // pass the test instance
                        onArticleClick = {}
                    )
                }
            }

            composeRule.onNodeWithText("No articles available")
                .assertExists()
        }
    }

    @Test
    fun articleCard_displaysArticles() {
        runBlocking {
            val viewModel = ArticlesListViewModel(fakeRepository)
            composeRule.setContent {
                HelpArticlesTheme {
                    ArticlesListScreen(
                        viewModel = viewModel, // pass the test instance
                        onArticleClick = {}
                    )
                }
            }

            composeRule.onNodeWithText(testArticle.title)
                .assertExists()
        }
    }

}


