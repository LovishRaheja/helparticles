# Help Articles App
A Kotlin Multiplatform (KMP) Android app demonstrating offline-first architecture with automatic refresh and background prefetching.

### Key Architecture Decisions

**1. MVVM with UVI (Unidirectional Data Flow)**
- ViewModels expose `StateFlow<UiState>` for reactive UI updates
- Sealed classes for type-safe state management
- Single source of truth pattern

**2. Repository Pattern**
- `ArticlesRepository` orchestrates cache and network operations
- Implements cache-first with automatic fallback strategy
- Handles error mapping and retry logic

**3. KMP Shared Cache**
- Platform-agnostic cache logic in `shared` module
- Simple key-value storage with TTL (Time To Live)
- Android uses `DataStore` for actual persistence

## Error Handling Strategy

### Network vs. Backend Errors

**Network/Transport Errors** (Connectivity Issues):
- No internet connection
- Timeouts (configured at 10s)
- Server 5xx errors
- DNS failures

**Backend Errors** (Business Logic):
- Structured error response with `errorCode`, `errorTitle`, `errorMessage`
- Parsed from API response body
- Examples: "Article not found", "Rate limit exceeded"

### UI Treatment
```kotlin
sealed class UiState {
    data class Success(val data: T) : UiState()
    data class Error(
        val message: String,
        val isNetworkError: Boolean,  // Determines UI treatment
        val canRetry: Boolean
    ) : UiState()
    data object Loading : UiState()
}
```

- **Network errors**: Show "No Connection" icon + "Retry" button
- **Backend errors**: Show error message from server + optional retry
- **Malformed payload**: Safe fallback with generic error message

## Offline & Auto-Refresh

### Staleness Rule (24-hour TTL)
```kotlin
// Cache entry is considered:
// - FRESH: < 24 hours old → Use cached data
// - STALE: ≥ 24 hours old → Fetch new data
// - EXPIRED: 72 hours old → Delete from cache
```

**Rationale**: Help articles are relatively static. 24h balances:
- Reduced server load
- Fresh enough for most use cases
- Good offline experience

### Auto-Refresh Triggers
1. **App resume** (onStart): Check staleness, fetch if needed
2. **Connectivity restored**: Automatic retry if previous fetch failed
3. **Manual refresh**: Pull-to-refresh on list screen
4. **Background worker**: Daily prefetch (see below)

### Cache-First Flow
```
1. Check cache → If fresh data exists → Show immediately
2. In parallel: Check if stale
3. If stale → Fetch from network in background
4. Update cache + UI when new data arrives
5. If network fails → Keep showing cached data
```

## Background Prefetch

### Implementation: WorkManager (PeriodicWorkRequest)
```kotlin
PeriodicWorkRequestBuilder(
    repeatInterval = 24, 
    repeatIntervalTimeUnit = TimeUnit.HOURS,
    flexTimeInterval = 2, // 2-hour flex window
    flexTimeIntervalUnit = TimeUnit.HOURS
)
.setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
)
```

### Why WorkManager?
- **Deferrable**: System schedules optimally (Doze mode, battery saver)
- **Constraint-aware**: Only runs when connected & battery not low
- **Guaranteed execution**: Persists across reboots
- **Flex window**: 2-hour window gives system flexibility to batch work

### Battery Considerations
- Only fetches list (not all details)
- No wake locks
- Respects battery saver mode
- Flex interval allows opportunistic execution

## KMP Cache Design

### Shared Logic (commonMain)
```kotlin
interface ArticleCache {
    suspend fun getArticles(): List?
    suspend fun saveArticles(articles: List)
    suspend fun getArticle(id: String): Article?
    suspend fun saveArticle(article: Article)
    suspend fun isStale(key: String): Boolean
}
```

### Platform Implementation (Android)
- Uses Jetpack DataStore (Preferences) for metadata (timestamps)
- Uses JSON serialization for article storage
- Provides `expect/actual` for platform-specific storage


## Completed Features (4-5h scope)

### ✅ Completed
- [x] List screen with search/filter
- [x] Detail screen with Markdown rendering
- [x] Network vs. backend error distinction
- [x] KMP cache with TTL logic
- [x] Offline mode with fallback
- [x] Auto-refresh on app resume
- [x] Background prefetch (WorkManager)
- [x] Material 3 theming (light/dark)
- [x] Accessibility (semantic labels, touch targets ≥48dp)
- [x] Unit test (shared cache staleness logic)
- [x] UI test (error state + retry)
- [x] Mock API with various error scenarios


