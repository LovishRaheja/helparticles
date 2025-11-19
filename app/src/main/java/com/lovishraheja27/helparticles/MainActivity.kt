package com.lovishraheja27.helparticles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lovishraheja27.helparticles.ui.detail.ArticleDetailScreen
import com.lovishraheja27.helparticles.ui.detail.ArticleDetailViewModel
import com.lovishraheja27.helparticles.ui.list.ArticlesListScreen
import com.lovishraheja27.helparticles.ui.list.ArticlesListViewModel
import com.lovishraheja27.helparticles.ui.theme.HelpArticlesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HelpArticlesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HelpArticlesApp()
                }
            }
        }
    }
}

@Composable
fun HelpArticlesApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "articles"
    ) {
        composable("articles") {
            val viewModel = hiltViewModel<ArticlesListViewModel>()

            ArticlesListScreen(
                viewModel = viewModel,
                onArticleClick = { articleId ->
                    navController.navigate("article/$articleId")
                }
            )
        }

        composable(
            route = "article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) {
            val viewModel = hiltViewModel<ArticleDetailViewModel>()

            ArticleDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}